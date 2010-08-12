/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.imap.store;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.util.SharedFileInputStream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxListener;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.MessageResult.FetchGroup;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;
import org.apache.james.imap.store.streaming.ConfigurableMimeTokenStream;
import org.apache.james.imap.store.streaming.CountingInputStream;
import org.apache.james.imap.store.transaction.TransactionalMapper;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.MaximalBodyDescriptor;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.apache.james.mime4j.parser.MimeTokenStream;

/**
 * Abstract base class for {@link org.apache.james.imap.mailbox.Mailbox} implementations.
 * 
 * This class provides a high-level api, and is most times the best to just extend
 * 
 *
 */
public abstract class StoreMessageManager<Id> implements org.apache.james.imap.mailbox.Mailbox {

    private static final int INITIAL_SIZE_FLAGS = 32;

    private static final int INITIAL_SIZE_HEADERS = 32;
    
    private MessageMapper<Id> messageMapper;

    protected final Mailbox<Id> mailbox;
    
    private MailboxEventDispatcher dispatcher;

    
    
    public StoreMessageManager(MailboxSessionMapperFactory<Id> mapperFactory, final MailboxEventDispatcher dispatcher, final Mailbox<Id> mailbox, MailboxSession session) throws MailboxException {
        this.mailbox = mailbox;
        this.dispatcher = dispatcher;
        this.messageMapper = mapperFactory.getMessageMapper(session);
    }

    protected MessageMapper<Id> getMessageMapper() {
        return messageMapper;
    }
    
    /**
     * Return the {@link MailboxEventDispatcher} for this Mailbox
     * 
     * @return dispatcher
     */
    protected MailboxEventDispatcher getDispatcher() {
        return dispatcher;
    }
    
    /**
     * Return the underlying {@link Mailbox}
     * 
     * @param session
     * @return mailbox
     * @throws MailboxException
     */
    
    public Mailbox<Id> getMailboxEntity() throws MailboxException {
        return mailbox;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#getMessageCount(org.apache.james.imap.mailbox.MailboxSession)
     */
    public int getMessageCount(MailboxSession mailboxSession) throws MailboxException {
        return (int) messageMapper.countMessagesInMailbox(mailbox);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#appendMessage(byte[], java.util.Date, org.apache.james.imap.mailbox.MailboxSession, boolean, javax.mail.Flags)
     */
    public long appendMessage(final InputStream msgIn, final Date internalDate,
            final MailboxSession mailboxSession,final boolean isRecent, final Flags flagsToBeSet)
    throws MailboxException {
        File file = null;
        SharedFileInputStream tmpMsgIn = null;
        try {
            // Create a temporary file and copy the message to it. We will work with the file as
            // source for the InputStream
            file = File.createTempFile("imap", ".msg");
            FileOutputStream out = new FileOutputStream(file);
            
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = msgIn.read(buf)) != -1) {
                out.write(buf, 0, i);
            }
            out.flush();
            out.close();
            
            tmpMsgIn = new SharedFileInputStream(file);
           
            final int size = tmpMsgIn.available();
            final int bodyStartOctet = bodyStartOctet(tmpMsgIn);

            // Disable line length... This should be handled by the smtp server component and not the parser itself
            // https://issues.apache.org/jira/browse/IMAP-122
            MimeEntityConfig config = new MimeEntityConfig();
            config.setMaximalBodyDescriptor(true);
            config.setMaxLineLen(-1);
            final ConfigurableMimeTokenStream parser = new ConfigurableMimeTokenStream(config);
           
            parser.setRecursionMode(MimeTokenStream.M_NO_RECURSE);
            parser.parse(tmpMsgIn.newStream(0, -1));
            final List<Header> headers = new ArrayList<Header>(INITIAL_SIZE_HEADERS);
            
            int lineNumber = 0;
            int next = parser.next();
            while (next != MimeTokenStream.T_BODY
                    && next != MimeTokenStream.T_END_OF_STREAM
                    && next != MimeTokenStream.T_START_MULTIPART) {
                if (next == MimeTokenStream.T_FIELD) {
                    String fieldValue = parser.getField().getBody();
                    if (fieldValue.endsWith("\r\f")) {
                        fieldValue = fieldValue.substring(0,fieldValue.length() - 2);
                    }
                    if (fieldValue.startsWith(" ")) {
                        fieldValue = fieldValue.substring(1);
                    }
                    final Header header 
                        = createHeader(++lineNumber, parser.getField().getName(), 
                            fieldValue);
                    headers.add(header);
                }
                next = parser.next();
            }
            final MaximalBodyDescriptor descriptor = (MaximalBodyDescriptor) parser.getBodyDescriptor();
            final PropertyBuilder propertyBuilder = new PropertyBuilder();
            final String mediaType;
            final String mediaTypeFromHeader = descriptor.getMediaType();
            final String subType;
            if (mediaTypeFromHeader == null) {
                mediaType = "text";
                subType = "plain";
            } else {
                mediaType = mediaTypeFromHeader;
                subType = descriptor.getSubType();
            }
            propertyBuilder.setMediaType(mediaType);
            propertyBuilder.setSubType(subType);
            propertyBuilder.setContentID(descriptor.getContentId());
            propertyBuilder.setContentDescription(descriptor.getContentDescription());
            propertyBuilder.setContentLocation(descriptor.getContentLocation());
            propertyBuilder.setContentMD5(descriptor.getContentMD5Raw());
            propertyBuilder.setContentTransferEncoding(descriptor.getTransferEncoding());
            propertyBuilder.setContentLanguage(descriptor.getContentLanguage());
            propertyBuilder.setContentDispositionType(descriptor.getContentDispositionType());
            propertyBuilder.setContentDispositionParameters(descriptor.getContentDispositionParameters());
            propertyBuilder.setContentTypeParameters(descriptor.getContentTypeParameters());
            // Add missing types
            final String codeset = descriptor.getCharset();
            if (codeset == null) {
                if ("TEXT".equalsIgnoreCase(mediaType)) {
                    propertyBuilder.setCharset("us-ascii");
                }
            } else {
                propertyBuilder.setCharset(codeset);
            }
            
            final String boundary = descriptor.getBoundary();
            if (boundary != null) {
                propertyBuilder.setBoundary(boundary);
            }   
            if ("text".equalsIgnoreCase(mediaType)) {
                final CountingInputStream bodyStream = new CountingInputStream(parser.getInputStream());
                bodyStream.readAll();
                long lines = bodyStream.getLineCount();
                
                next = parser.next();
                if (next == MimeTokenStream.T_EPILOGUE)  {
                    final CountingInputStream epilogueStream = new CountingInputStream(parser.getInputStream());
                    epilogueStream.readAll();
                    lines+=epilogueStream.getLineCount();
                }
                propertyBuilder.setTextualLineCount(lines);
            }
            
            final Flags flags;
            if (flagsToBeSet == null) {
                flags = new Flags();
            } else {
                flags = flagsToBeSet;
            }
            if (isRecent) {
                flags.add(Flags.Flag.RECENT);
            }
            final MailboxMembership<Id> message = createMessage(internalDate, size, bodyStartOctet, tmpMsgIn.newStream(0, -1), flags, headers, propertyBuilder);
            final List<Long> uids = new ArrayList<Long>();
            messageMapper.execute(new TransactionalMapper.Transaction() {
                
                public void run() throws MailboxException {
                    uids.add(messageMapper.save(mailbox, message));
                }
            });
            long uid = uids.get(0);
            dispatcher.added(uid, mailboxSession.getSessionId(), new StoreMailboxPath<Id>(getMailboxEntity()));
            return uid;
        } catch (IOException e) {
            e.printStackTrace();
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        } catch (MimeException e) {
            e.printStackTrace();
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        } finally {
            if (tmpMsgIn != null) {
                try {
                    tmpMsgIn.close();
                } catch (IOException e) {
                    // ignore on close
                }
            }
            // delete the temporary file if one was specified
            if (file != null) {
                file.delete();
            }
        }

    }

    /**
     * Return the position in the given {@link InputStream} at which the Body of the 
     * Message starts
     * 
     * @param msgIn
     * @return bodyStartOctet
     * @throws IOException
     */
    private int bodyStartOctet(InputStream msgIn) throws IOException{
        // we need to pushback maximal 3 bytes
        PushbackInputStream in = new PushbackInputStream(msgIn,3);
        
        int bodyStartOctet = in.available();
        int i = -1;
        int count = 0;
        while ((i = in.read()) != -1 && in.available() > 4) {
            if (i == 0x0D) {
                int a = in.read();
                if (a == 0x0A) {
                    int b = in.read();

                    if (b == 0x0D) {
                        int c = in.read();

                        if (c == 0x0A) {
                            bodyStartOctet = count+4;
                            break;
                        }
                        in.unread(c);
                    }
                    in.unread(b);
                }
                in.unread(a);
            }
            count++;
        }
        
        return bodyStartOctet;
    }

    /**
     * Create a new {@link MailboxMembership} for the given data
     * 
     * @param internalDate
     * @param size
     * @param bodyStartOctet
     * @param documentIn
     * @param flags
     * @param headers
     * @param propertyBuilder
     * @return membership
     * @throws MailboxException 
     */
    protected abstract MailboxMembership<Id> createMessage(Date internalDate, final int size, int bodyStartOctet, 
            final InputStream documentIn, final Flags flags, final List<Header> headers, PropertyBuilder propertyBuilder) throws MailboxException;
    
    /**
     * Create a new {@link Header} for the given data
     * 
     * @param lineNumber
     * @param name
     * @param value
     * @return header
     */
    protected abstract Header createHeader(int lineNumber, String name, String value);


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#getMessages(org.apache.james.imap.mailbox.MessageRange, org.apache.james.imap.mailbox.MessageResult.FetchGroup, org.apache.james.imap.mailbox.MailboxSession)
     */
    public Iterator<MessageResult> getMessages(final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        final List<MailboxMembership<Id>> rows = messageMapper.findInMailbox(mailbox, set);
        return new ResultIterator<Id>(rows.iterator(), fetchGroup);
    }

 
    private Flags getPermanentFlags() {
        Flags permanentFlags = new Flags();
        permanentFlags.add(Flags.Flag.ANSWERED);
        permanentFlags.add(Flags.Flag.DELETED);
        permanentFlags.add(Flags.Flag.DRAFT);
        permanentFlags.add(Flags.Flag.FLAGGED);
        permanentFlags.add(Flags.Flag.SEEN);
        return permanentFlags;
    }

    private long[] recent(final boolean reset, MailboxSession mailboxSession) throws MailboxException {
        final List<Long> results = new ArrayList<Long>();

        messageMapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                final List<MailboxMembership<Id>> members = messageMapper.findRecentMessagesInMailbox(mailbox, -1);

                for (MailboxMembership<Id> member:members) {
                    results.add(member.getUid());
                    if (reset) {
                        member.unsetRecent();
                        
                        // only call save if we need to
                        messageMapper.save(mailbox, member);
                    }
                }
            }
            
        });;
        
        return ArrayUtils.toPrimitive(results.toArray(new Long[results.size()]));
    }


    private int getUnseenCount(MailboxSession mailboxSession) throws MailboxException {
        final int count = (int) messageMapper.countUnseenMessagesInMailbox(mailbox);
        return count;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#expunge(org.apache.james.imap.mailbox.MessageRange, org.apache.james.imap.mailbox.MailboxSession)
     */
    public Iterator<Long> expunge(final MessageRange set, MailboxSession mailboxSession) throws MailboxException {
    	final Collection<Long> uids = new TreeSet<Long>();
        
        messageMapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                final List<MailboxMembership<Id>> members = messageMapper.findMarkedForDeletionInMailbox(mailbox, set);
                for (MailboxMembership<Id> message:members) {
                    uids.add(message.getUid());
                    messageMapper.delete(mailbox, message);
                }  
            }
            
        });
        
        Iterator<Long> uidIt = uids.iterator();
        while(uidIt.hasNext()) {
            dispatcher.expunged(uidIt.next(), mailboxSession.getSessionId(), new StoreMailboxPath<Id>(getMailboxEntity()));
        }
        return uids.iterator();    
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#setFlags(javax.mail.Flags, boolean, boolean, org.apache.james.imap.mailbox.MessageRange, org.apache.james.imap.mailbox.MailboxSession)
     */
    public Map<Long, Flags> setFlags(final Flags flags, final boolean value, final boolean replace,
            final MessageRange set, MailboxSession mailboxSession) throws MailboxException {
    	final SortedMap<Long, Flags> newFlagsByUid = new TreeMap<Long, Flags>();
        final Map<Long, Flags> originalFlagsByUid = new HashMap<Long, Flags>(INITIAL_SIZE_FLAGS);
        messageMapper.execute(new TransactionalMapper.Transaction(){

            public void run() throws MailboxException {
                final List<MailboxMembership<Id>> members = messageMapper.findInMailbox(mailbox, set);
                for (final MailboxMembership<Id> member:members) {
                    originalFlagsByUid.put(member.getUid(), member.createFlags());
                    if (replace) {
                        member.setFlags(flags);
                    } else {
                        Flags current = member.createFlags();
                        if (value) {
                            current.add(flags);
                        } else {
                            current.remove(flags);
                        }
                        member.setFlags(current);
                    }
                    newFlagsByUid.put(member.getUid(), member.createFlags());
                    messageMapper.save(mailbox, member);
                }
            }
            
        });
       
        Iterator<Long> it = newFlagsByUid.keySet().iterator();
        while (it.hasNext()) {
            Long uid = it.next();
            dispatcher.flagsUpdated(uid, mailboxSession.getSessionId(), new StoreMailboxPath<Id>(getMailboxEntity()), originalFlagsByUid.get(uid), newFlagsByUid.get(uid));

        }
        return newFlagsByUid;
    }


    public void addListener(MailboxListener listener) throws MailboxException {
        dispatcher.addMailboxListener(listener);
    }

    private long getUidValidity(MailboxSession mailboxSession) throws MailboxException {
        final long result = getMailboxEntity().getUidValidity();
        return result;
    }

    private long getUidNext(MailboxSession mailboxSession) throws MailboxException {
        Mailbox<Id> mailbox = getMailboxEntity();
        if (mailbox == null) {
            throw new MailboxNotFoundException("Mailbox has been deleted");
        } else {
            final long lastUid = mailbox.getLastUid();
            return lastUid + 1;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#search(org.apache.james.imap.mailbox.SearchQuery, org.apache.james.imap.mailbox.MailboxSession)
     */
    public Iterator<Long> search(SearchQuery query, MailboxSession mailboxSession) throws MailboxException {
        return messageMapper.searchMailbox(mailbox, query);    
    }

    /**
     * This mailbox is writable
     */
    public boolean isWriteable(MailboxSession session) {
        return true;
    }

    private void copy(final List<MailboxMembership<Id>> originalRows, final MailboxSession session) throws MailboxException {
        try {
            final List<MailboxMembership<Id>> copiedRows = new ArrayList<MailboxMembership<Id>>();

            for (final MailboxMembership<Id> originalMessage:originalRows) {
                messageMapper.execute(new TransactionalMapper.Transaction() {

                    public void run() throws MailboxException {
                        final MailboxMembership<Id> newRow = messageMapper.copy(mailbox, originalMessage);
                        copiedRows.add(newRow);
                    }
                    
                });
            }
            // Wait until commit before issuing events
            for (MailboxMembership<Id> newMember:copiedRows) {
                dispatcher.added(newMember.getUid(), session.getSessionId(), new StoreMailboxPath<Id>(getMailboxEntity()));
            }
        } catch (MessagingException e) {
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        }
    }

    /**
     * Copy the {@link MessageSet} to the {@link StoreMessageManager}
     * 
     * @param set
     * @param toMailbox
     * @param session
     * @throws MailboxException
     */
    public void copyTo(MessageRange set, StoreMessageManager<Id> toMailbox, MailboxSession session) throws MailboxException {
        try {
            final List<MailboxMembership<Id>> originalRows = messageMapper.findInMailbox(mailbox, set);
            toMailbox.copy(originalRows, session);

        } catch (MessagingException e) {
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        }
    }
    
    /**
     * @see {@link Mailbox#getMetaData(boolean, MailboxSession, FetchGroup)}
     */
    public MetaData getMetaData(boolean resetRecent, MailboxSession mailboxSession, 
            org.apache.james.imap.mailbox.Mailbox.MetaData.FetchGroup fetchGroup) throws MailboxException {
        final long[] recent = recent(resetRecent, mailboxSession);
        final Flags permanentFlags = getPermanentFlags();
        final long uidValidity = getUidValidity(mailboxSession);
        final long uidNext = getUidNext(mailboxSession);
        final int messageCount = getMessageCount(mailboxSession);
        final int unseenCount;
        final Long firstUnseen;
        switch (fetchGroup) {
            case UNSEEN_COUNT:
                unseenCount = getUnseenCount(mailboxSession);
                firstUnseen = null;
                break;
            case FIRST_UNSEEN:
                firstUnseen = getMessageMapper().findFirstUnseenMessageUid(mailbox);
                unseenCount = 0;
                break;
            default:
                firstUnseen = null;
                unseenCount = 0;
                break;
        }
        return new MailboxMetaData(recent, permanentFlags, uidValidity, uidNext, messageCount, unseenCount, firstUnseen, isWriteable(mailboxSession));
    }
}