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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.mail.Flags;
import javax.mail.MessagingException;

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
import org.apache.james.imap.mailbox.util.UidRange;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;
import org.apache.james.imap.store.transaction.TransactionalMapper;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.MaximalBodyDescriptor;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.apache.james.mime4j.parser.MimeTokenStream;

import com.sun.mail.imap.protocol.MessageSet;

/**
 * Abstract base class for {@link org.apache.james.imap.mailbox.Mailbox} implementations.
 * 
 * This class provides a high-level api, and is most times the best to just extend
 * 
 *
 */
public abstract class StoreMailbox<Id> implements org.apache.james.imap.mailbox.Mailbox {

    private static final int INITIAL_SIZE_FLAGS = 32;

    private static final int INITIAL_SIZE_HEADERS = 32;

    private final Id mailboxId;
    
    private MailboxEventDispatcher dispatcher;
    
    public StoreMailbox(final MailboxEventDispatcher dispatcher, final Mailbox<Id> mailbox) {
        this.mailboxId = mailbox.getMailboxId();
        this.dispatcher = dispatcher;
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
     * Copy the given {@link MailboxMembership} to a new instance with the given uid
     * 
     * @param originalMessage
     * @param uid
     * @return membershipCopy
     */
    protected abstract MailboxMembership<Id> copyMessage(MailboxMembership<Id> originalMessage, long uid, MailboxSession session) throws MailboxException;
    
    /**
     * Create a new {@link MessageMapper} to use
     * 
     * @return mapper
     */
    protected abstract MessageMapper<Id> createMessageMapper(MailboxSession session) throws MailboxException;
    
    
    /**
     * Return the underlying {@link Mailbox}
     * 
     * @param session
     * @return mailbox
     * @throws MailboxException
     */
    protected abstract Mailbox<Id> getMailboxRow(MailboxSession session) throws MailboxException;

    /**
     * Return the Id of the wrapped {@link Mailbox}
     * 
     * @return id
     */
    protected Id getMailboxId() {
        return mailboxId;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#getMessageCount(org.apache.james.imap.mailbox.MailboxSession)
     */
    public int getMessageCount(MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper<Id> messageMapper = createMessageMapper(mailboxSession);
        return (int) messageMapper.countMessagesInMailbox();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#appendMessage(byte[], java.util.Date, org.apache.james.imap.mailbox.MailboxSession, boolean, javax.mail.Flags)
     */
    public long appendMessage(final InputStream msgIn, final Date internalDate,
            final MailboxSession mailboxSession,final boolean isRecent, final Flags flagsToBeSet)
    throws MailboxException {
        // this will hold the uid after the transaction was complete
        final List<Long> uidHolder = new ArrayList<Long>();
        final MessageMapper<Id> mapper = createMessageMapper(mailboxSession);
        mapper.execute(new TransactionalMapper.Transaction() {
          
            public void run() throws MailboxException {
                final Mailbox<Id> mailbox = reserveNextUid(mailboxSession);
                
                File file = null;
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
                    
                    FileInputStream tmpMsgIn = new FileInputStream(file);
                    // To be thread safe, we first get our own copy and the
                    // exclusive
                    // Uid
                    // TODO create own message_id and assign uid later
                    // at the moment it could lead to the situation that uid 5
                    // is
                    // inserted long before 4, when
                    // mail 4 is big and comes over a slow connection.

                    final long uid = mailbox.getLastUid();
                    final int size = tmpMsgIn.available();
                    final int bodyStartOctet = bodyStartOctet(new FileInputStream(file));

                    // Disable line length... This should be handled by the smtp server component and not the parser itself
                    // https://issues.apache.org/jira/browse/IMAP-122
                    MimeEntityConfig config = new MimeEntityConfig();
                    config.setMaximalBodyDescriptor(true);
                    config.setMaxLineLen(-1);
                    final ConfigurableMimeTokenStream parser = new ConfigurableMimeTokenStream(config);
                   
                    parser.setRecursionMode(MimeTokenStream.M_NO_RECURSE);
                    parser.parse(new FileInputStream(file));
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
                    
                    final MailboxMembership<Id> message = createMessage(internalDate, uid, size, bodyStartOctet, new FileInputStream(file), flags, headers, propertyBuilder);
                    mapper.save(message);
                       
                        
                   
                    dispatcher.added(uid, mailboxSession.getSessionId(), getMailboxRow(mailboxSession).getName());
                    //tracker.found(uid, message.createFlags());
                    uidHolder.add(uid);
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
                    // delete the temporary file if one was specified
                    if (file != null) {
                        file.delete();
                    }
                }
            }
        });
       
        return uidHolder.get(0);
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
        
        // close the stream
        in.close();
        
        return bodyStartOctet;
    }

    /**
     * Create a new {@link MailboxMembership} for the given data
     * 
     * @param internalDate
     * @param uid
     * @param size
     * @param bodyStartOctet
     * @param documentIn
     * @param flags
     * @param headers
     * @param propertyBuilder
     * @return membership
     * @throws MailboxException 
     */
    protected abstract MailboxMembership<Id> createMessage(Date internalDate, final long uid, final int size, int bodyStartOctet, 
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


    /**
     * Reserve the next Uid on the underlying {@link Mailbox} 
     * 
     * @return mailbox
     * @throws MailboxException
     */
    protected abstract Mailbox<Id> reserveNextUid(MailboxSession session) throws  MailboxException;

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#getMessages(org.apache.james.imap.mailbox.MessageRange, org.apache.james.imap.mailbox.MessageResult.FetchGroup, org.apache.james.imap.mailbox.MailboxSession)
     */
    public Iterator<MessageResult> getMessages(final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        UidRange range = uidRangeForMessageSet(set);
        final MessageMapper<Id> messageMapper = createMessageMapper(mailboxSession);
        final List<MailboxMembership<Id>> rows = new ArrayList<MailboxMembership<Id>>(messageMapper.findInMailbox(set));
        return getMessages(fetchGroup, range, rows);
    }

    private ResultIterator<Id> getMessages(FetchGroup result, UidRange range, List<MailboxMembership<Id>> messages) {
        final Map<Long, Flags> flagsByIndex = new HashMap<Long, Flags>();
        for (MailboxMembership<Id> member:messages) {
            flagsByIndex.put(member.getUid(), member.createFlags());
        }
        final ResultIterator<Id> results = getResults(result, messages);
        return results;
    }

    private ResultIterator<Id> getResults(FetchGroup result, List<MailboxMembership<Id>> messages) {
        Collections.sort(messages, ResultUtils.getUidComparator());
        final ResultIterator<Id> results = new ResultIterator<Id>(messages,result);
        return results;
    }

    private static UidRange uidRangeForMessageSet(MessageRange set) throws MailboxException {
        if (set.getType().equals(MessageRange.Type.ALL)) {
            return new UidRange(1, -1);
        } else {
            return new UidRange(set.getUidFrom(), set.getUidTo());
        }
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
        final MessageMapper<Id> mapper = createMessageMapper(mailboxSession);
        final List<Long> results = new ArrayList<Long>();

        mapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                final List<MailboxMembership<Id>> members = mapper.findRecentMessagesInMailbox();

                for (MailboxMembership<Id> member:members) {
                    results.add(member.getUid());
                    if (reset) {
                        member.unsetRecent();
                    }
                    mapper.save(member);
                }
            }
            
        });;
        
        return ArrayUtils.toPrimitive(results.toArray(new Long[results.size()]));
    }

    private Long getFirstUnseen(MailboxSession mailboxSession) throws MailboxException {
        try {
            final MessageMapper<Id> messageMapper = createMessageMapper(mailboxSession);
            final List<MailboxMembership<Id>> members = messageMapper.findUnseenMessagesInMailbox();
            final Iterator<MailboxMembership<Id>> it = members.iterator();
            final Long result;
            if (it.hasNext()) {
                final MailboxMembership<Id> member = it.next();
                result = member.getUid();
            } else {
                result = null;
            }
            return result;
        } catch (MessagingException e) {
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        }
    }

    private int getUnseenCount(MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper<Id> messageMapper = createMessageMapper(mailboxSession);
        final int count = (int) messageMapper.countUnseenMessagesInMailbox();
        return count;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#expunge(org.apache.james.imap.mailbox.MessageRange, org.apache.james.imap.mailbox.MailboxSession)
     */
    public Iterator<Long> expunge(MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        return doExpunge(set, mailboxSession);
    }

    private Iterator<Long> doExpunge(final MessageRange set, MailboxSession mailboxSession)
    throws MailboxException {
        final MessageMapper<Id> mapper = createMessageMapper(mailboxSession);
        final Collection<Long> uids = new TreeSet<Long>();
        
        mapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                final List<MailboxMembership<Id>> members = mapper.findMarkedForDeletionInMailbox(set);
                for (MailboxMembership<Id> message:members) {
                    uids.add(message.getUid());
                    mapper.delete(message);
                }  
            }
            
        });
        
        Iterator<Long> uidIt = uids.iterator();
        while(uidIt.hasNext()) {
            dispatcher.expunged(uidIt.next(), mailboxSession.getSessionId(), getMailboxRow(mailboxSession).getName());
        }
        return uids.iterator();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#setFlags(javax.mail.Flags, boolean, boolean, org.apache.james.imap.mailbox.MessageRange, org.apache.james.imap.mailbox.MailboxSession)
     */
    public Map<Long, Flags> setFlags(Flags flags, boolean value, boolean replace,
            MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        return doSetFlags(flags, value, replace, set, mailboxSession);
    }

    private Map<Long, Flags> doSetFlags(final Flags flags, final boolean value, final boolean replace,
            final MessageRange set, final MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper<Id> mapper = createMessageMapper(mailboxSession);
        final SortedMap<Long, Flags> newFlagsByUid = new TreeMap<Long, Flags>();
        final Map<Long, Flags> originalFlagsByUid = new HashMap<Long, Flags>(INITIAL_SIZE_FLAGS);
        mapper.execute(new TransactionalMapper.Transaction(){

            public void run() throws MailboxException {
                final List<MailboxMembership<Id>> members = mapper.findInMailbox(set);
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
                    mapper.save(member);
                }
            }
            
        });
       
        Iterator<Long> it = newFlagsByUid.keySet().iterator();
        while (it.hasNext()) {
            Long uid = it.next();
            dispatcher.flagsUpdated(uid, mailboxSession.getSessionId(), getMailboxRow(mailboxSession).getName(), originalFlagsByUid.get(uid), newFlagsByUid.get(uid));

        }
        return newFlagsByUid;
    }

    public void addListener(MailboxListener listener) throws MailboxException {
        dispatcher.addMailboxListener(listener);
    }

    private long getUidValidity(MailboxSession mailboxSession) throws MailboxException {
        final long result = getMailboxRow(mailboxSession).getUidValidity();
        return result;
    }

    private long getUidNext(MailboxSession mailboxSession) throws MailboxException {
        Mailbox<Id> mailbox = getMailboxRow(mailboxSession);
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
        final MessageMapper<Id> messageMapper = createMessageMapper(mailboxSession);
        final List<MailboxMembership<Id>> members = messageMapper.searchMailbox(query);
        final Set<Long> uids = new TreeSet<Long>();
        for (MailboxMembership<Id> member:members) {
            try {
                final MessageSearches searches = new MessageSearches();
                searches.setLog(mailboxSession.getLog());
                if (searches.isMatch(query, member)) {
                    uids.add(member.getUid());
                }
            } catch (MailboxException e) {
                mailboxSession.getLog()
                .info(
                        "Cannot test message against search criteria. Will continue to test other messages.",
                        e);
                if (mailboxSession.getLog().isDebugEnabled())
                    mailboxSession.getLog().debug("UID: " + member.getUid());
            }
        }

        return uids.iterator();
    }


    /**
     * This mailbox is writable
     */
    public boolean isWriteable() {
        return true;
    }
    

    private void copy(final List<MailboxMembership<Id>> originalRows, final MailboxSession session) throws MailboxException {
        try {
            final List<MailboxMembership<Id>> copiedRows = new ArrayList<MailboxMembership<Id>>();
            final MessageMapper<Id> mapper = createMessageMapper(session);
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    for (MailboxMembership<Id> originalMessage:originalRows) {

                        final Mailbox<Id> mailbox = reserveNextUid(session );
                        if (mailbox != null) {
                            long uid = mailbox.getLastUid();
                            final MailboxMembership<Id> newRow = copyMessage(originalMessage, uid, session);
                            mapper.save(newRow);
                            copiedRows.add(newRow);
                        }
                    }  
                }
                
            });
            
            
            // Wait until commit before issuing events
            for (MailboxMembership<Id> newMember:copiedRows) {
                dispatcher.added(newMember.getUid(), session.getSessionId(), getMailboxRow(session).getName());
            }
            
        } catch (MessagingException e) {
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        }
    }

    /**
     * Copy the {@link MessageSet} to the {@link StoreMailbox}
     * 
     * @param set
     * @param toMailbox
     * @param session
     * @throws MailboxException
     */
    public void copyTo(MessageRange set, StoreMailbox<Id> toMailbox, MailboxSession session) throws MailboxException {
        try {
            final MessageMapper<Id> mapper = createMessageMapper(session);
            
            final List<MailboxMembership<Id>> originalRows = mapper.findInMailbox(set);
            toMailbox.copy(originalRows, session);

        } catch (MessagingException e) {
            e.printStackTrace();
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
                firstUnseen = getFirstUnseen(mailboxSession);
                unseenCount = 0;
                break;
            default:
                firstUnseen = null;
                unseenCount = 0;
                break;
        }
            
        return new MailboxMetaData(recent, permanentFlags, uidValidity, uidNext, messageCount, unseenCount, firstUnseen, isWriteable());
    }
    
}
