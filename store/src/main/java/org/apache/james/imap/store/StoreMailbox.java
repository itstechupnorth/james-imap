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

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import org.apache.james.imap.mailbox.util.UidChangeTracker;
import org.apache.james.imap.mailbox.util.UidRange;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;
import org.apache.james.imap.store.transaction.TransactionalMapper;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.MaximalBodyDescriptor;
import org.apache.james.mime4j.parser.MimeTokenStream;

public abstract class StoreMailbox implements org.apache.james.imap.mailbox.Mailbox {

    private static final int INITIAL_SIZE_FLAGS = 32;

    private static final int INITIAL_SIZE_HEADERS = 32;

    protected final long mailboxId;

    private final UidChangeTracker tracker;
    private final MailboxSession session;
    
    public StoreMailbox(final Mailbox mailbox, MailboxSession session) {
        this.mailboxId = mailbox.getMailboxId();
        this.tracker = new UidChangeTracker(mailbox.getLastUid());
        this.session = session;
    }

    protected MailboxSession getMailboxSession() {
        return session;
    }
    /**
     * Copy the given {@link MailboxMembership} to a new instance with the given uid
     * 
     * @param originalMessage
     * @param uid
     * @return membershipCopy
     */
    protected abstract MailboxMembership copyMessage(MailboxMembership originalMessage, long uid);
    
    /**
     * Create a new {@link MessageMapper} to use
     * 
     * @return mapper
     */
    protected abstract MessageMapper createMessageMapper(MailboxSession session) throws MailboxException;
    
    
    protected abstract Mailbox getMailboxRow() throws MailboxException;

    /**
     * Return the Id of the wrapped {@link Mailbox}
     * 
     * @return id
     */
    public long getMailboxId() {
        return mailboxId;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#getMessageCount(org.apache.james.imap.mailbox.MailboxSession)
     */
    public int getMessageCount(MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper messageMapper = createMessageMapper(session);
        return (int) messageMapper.countMessagesInMailbox();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#appendMessage(byte[], java.util.Date, org.apache.james.imap.mailbox.MailboxSession, boolean, javax.mail.Flags)
     */
    public long appendMessage(byte[] messageBytes, Date internalDate,
            MailboxSession mailboxSession, boolean isRecent, Flags flagsToBeSet)
    throws MailboxException {
        final Mailbox mailbox = reserveNextUid();

        if (mailbox == null) {
            throw new MailboxNotFoundException("Mailbox has been deleted");
        } else {
            try {
                // To be thread safe, we first get our own copy and the
                // exclusive
                // Uid
                // TODO create own message_id and assign uid later
                // at the moment it could lead to the situation that uid 5
                // is
                // inserted long before 4, when
                // mail 4 is big and comes over a slow connection.

                final long uid = mailbox.getLastUid();
                final int size = messageBytes.length;
                final int bodyStartOctet = bodyStartOctet(messageBytes);
                
                final MimeTokenStream parser = MimeTokenStream.createMaximalDescriptorStream();
                parser.setRecursionMode(MimeTokenStream.M_NO_RECURSE);
                parser.parse(new ByteArrayInputStream(messageBytes));
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
                
                final MailboxMembership message = createMessage(internalDate, uid, size, bodyStartOctet, messageBytes, flags, headers, propertyBuilder);
                final MessageMapper mapper = createMessageMapper(session);

                mapper.execute(new TransactionalMapper.Transaction() {

                    public void run() throws MailboxException {
                        mapper.save(message);
                    }
                    
                });
                tracker.found(uid, message.createFlags());
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
            }
        }
    }

    private int bodyStartOctet(byte[] messageBytes) {
        int bodyStartOctet = messageBytes.length;
        for (int i=0;i<messageBytes.length-4;i++) {
            if (messageBytes[i] == 0x0D) {
                if (messageBytes[i+1] == 0x0A) {
                    if (messageBytes[i+2] == 0x0D) {
                        if (messageBytes[i+3] == 0x0A) {
                            bodyStartOctet = i+4;
                            break;
                        }
                    }
                }
            }
        }
        return bodyStartOctet;
    }

    /**
     * Create a new {@link MailboxMembership} for the given data
     * 
     * @param internalDate
     * @param uid
     * @param size
     * @param bodyStartOctet
     * @param document
     * @param flags
     * @param headers
     * @param propertyBuilder
     * @return membership
     */
    protected abstract MailboxMembership createMessage(Date internalDate, final long uid, final int size, int bodyStartOctet, 
            final byte[] document, final Flags flags, final List<Header> headers, PropertyBuilder propertyBuilder);
    
    /**
     * Create a new {@link Header} for the given data
     * 
     * @param lineNumber
     * @param name
     * @param value
     * @return header
     */
    protected abstract Header createHeader(int lineNumber, String name, String value);


    protected abstract Mailbox reserveNextUid() throws  MailboxException;

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#getMessages(org.apache.james.imap.mailbox.MessageRange, org.apache.james.imap.mailbox.MessageResult.FetchGroup, org.apache.james.imap.mailbox.MailboxSession)
     */
    public Iterator<MessageResult> getMessages(final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        UidRange range = uidRangeForMessageSet(set);
        final MessageMapper messageMapper = createMessageMapper(session);
        final List<MailboxMembership> rows = new ArrayList<MailboxMembership>(messageMapper.findInMailbox(set));
        return getMessages(fetchGroup, range, rows);
    }

    private ResultIterator getMessages(FetchGroup result, UidRange range, List<MailboxMembership> messages) {
        final Map<Long, Flags> flagsByIndex = new HashMap<Long, Flags>();
        for (MailboxMembership member:messages) {
            flagsByIndex.put(member.getUid(), member.createFlags());
        }
        final ResultIterator results = getResults(result, messages);
        tracker.found(range, flagsByIndex);
        return results;
    }

    private ResultIterator getResults(FetchGroup result, List<MailboxMembership> messages) {
        Collections.sort(messages, ResultUtils.getUidComparator());
        final ResultIterator results = new ResultIterator(messages,result);
        return results;
    }

    private static UidRange uidRangeForMessageSet(MessageRange set) throws MailboxException {
        if (set.getType().equals(MessageRange.Type.ALL)) {
            return new UidRange(1, -1);
        } else {
            return new UidRange(set.getUidFrom(), set.getUidTo());
        }
    }

    public Flags getPermanentFlags() {
        Flags permanentFlags = new Flags();
        permanentFlags.add(Flags.Flag.ANSWERED);
        permanentFlags.add(Flags.Flag.DELETED);
        permanentFlags.add(Flags.Flag.DRAFT);
        permanentFlags.add(Flags.Flag.FLAGGED);
        permanentFlags.add(Flags.Flag.SEEN);
        return permanentFlags;
    }

    public long[] recent(final boolean reset, MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper mapper = createMessageMapper(session);
        final List<Long> results = new ArrayList<Long>();

        mapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                final List<MailboxMembership> members = mapper.findRecentMessagesInMailbox();

                for (MailboxMembership member:members) {
                    results.add(member.getUid());
                    if (reset) {
                        member.unsetRecent();
                    }
                }
            }
            
        });;
        
        return ArrayUtils.toPrimitive(results.toArray(new Long[results.size()]));
    }

    public Long getFirstUnseen(MailboxSession mailboxSession) throws MailboxException {
        try {
            final MessageMapper messageMapper = createMessageMapper(session);
            final List<MailboxMembership> members = messageMapper.findUnseenMessagesInMailboxOrderByUid();
            final Iterator<MailboxMembership> it = members.iterator();
            final Long result;
            if (it.hasNext()) {
                final MailboxMembership member = it.next();
                result = member.getUid();
                tracker.found(result, member.createFlags());
            } else {
                result = null;
            }
            return result;
        } catch (MessagingException e) {
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        }
    }

    public int getUnseenCount(MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper messageMapper = createMessageMapper(session);
        final int count = (int) messageMapper.countUnseenMessagesInMailbox();
        return count;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#expunge(org.apache.james.imap.mailbox.MessageRange, org.apache.james.imap.mailbox.MailboxSession)
     */
    public Iterator<Long> expunge(MessageRange set, MailboxSession mailboxSession) throws MailboxException {
        return doExpunge(set);
    }

    private Iterator<Long> doExpunge(final MessageRange set)
    throws MailboxException {
        final MessageMapper mapper = createMessageMapper(session);
        final Collection<Long> uids = new TreeSet<Long>();
        
        mapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                final List<MailboxMembership> members = mapper.findMarkedForDeletionInMailbox(set);
                for (MailboxMembership message:members) {
                    uids.add(message.getUid());
                    mapper.delete(message);
                }  
            }
            
        });
        
        tracker.expunged(uids);
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
        final MessageMapper mapper = createMessageMapper(session);
        final SortedMap<Long, Flags> newFlagsByUid = new TreeMap<Long, Flags>();
        final Map<Long, Flags> originalFlagsByUid = new HashMap<Long, Flags>(INITIAL_SIZE_FLAGS);
        mapper.execute(new TransactionalMapper.Transaction(){

            public void run() throws MailboxException {
                final List<MailboxMembership> members = mapper.findInMailbox(set);
                for (final MailboxMembership member:members) {
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
       
        tracker.flagsUpdated(newFlagsByUid, originalFlagsByUid, mailboxSession.getSessionId());
        return newFlagsByUid;
    }

    public void addListener(MailboxListener listener) throws MailboxException {
        tracker.addMailboxListener(listener);
    }

    public long getUidValidity(MailboxSession mailboxSession) throws MailboxException {
        final long result = getMailboxRow().getUidValidity();
        return result;
    }

    public long getUidNext(MailboxSession mailboxSession) throws MailboxException {
        Mailbox mailbox = getMailboxRow();
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
        final MessageMapper messageMapper = createMessageMapper(session);
        final List<MailboxMembership> members = messageMapper.searchMailbox(query);
        final Set<Long> uids = new TreeSet<Long>();
        for (MailboxMembership member:members) {
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

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Mailbox#isWriteable()
     */
    public boolean isWriteable() {
        return true;
    }
    

    public void copy(final List<MailboxMembership> originalRows, MailboxSession session) throws MailboxException {
        try {
            final List<MailboxMembership> copiedRows = new ArrayList<MailboxMembership>();
            final MessageMapper mapper = createMessageMapper(session);
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    for (MailboxMembership originalMessage:originalRows) {

                        final Mailbox mailbox = reserveNextUid();
                        if (mailbox != null) {
                            long uid = mailbox.getLastUid();
                            final MailboxMembership newRow = copyMessage(originalMessage, uid);
                            mapper.save(newRow);
                            copiedRows.add(newRow);
                        }
                    }  
                }
                
            });
            
            
            // Wait until commit before issuing events
            for (MailboxMembership newMember:copiedRows) {
                tracker.found(newMember.getUid(), newMember.createFlags());
            }
            
        } catch (MessagingException e) {
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        }
    }

    public void copyTo(MessageRange set, StoreMailbox toMailbox, MailboxSession session) throws MailboxException {
        try {
            final MessageMapper mapper = createMessageMapper(session);
            
            final List<MailboxMembership> originalRows = mapper.findInMailbox(set);
            toMailbox.copy(originalRows, session);

        } catch (MessagingException e) {
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        }
    }
    
    public void deleted(MailboxSession session) {
        tracker.mailboxDeleted(session.getSessionId());
    }

    public void reportRenamed(String to) {
        tracker.reportRenamed(to);
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
    
    protected abstract void onLogout(MailboxSession session);
}
