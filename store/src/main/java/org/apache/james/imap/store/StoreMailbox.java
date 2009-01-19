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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.james.api.imap.AbstractLogEnabled;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxListener;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.MessageResult.FetchGroup;
import org.apache.james.imap.mailbox.util.FetchGroupImpl;
import org.apache.james.imap.mailbox.util.UidChangeTracker;
import org.apache.james.imap.mailbox.util.UidRange;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.mime4j.parser.MimeTokenStream;

public abstract class StoreMailbox extends AbstractLogEnabled implements org.apache.james.imap.mailbox.Mailbox {

    private static final int INITIAL_SIZE_HEADERS = 32;

    protected final long mailboxId;

    private final UidChangeTracker tracker;

    private final MessageSearches searches;

    public StoreMailbox(final Mailbox mailbox, final Log log) {
        this.searches = new MessageSearches();
        setLog(log);
        this.mailboxId = mailbox.getMailboxId();
        this.tracker = new UidChangeTracker(mailbox.getLastUid());
    }

    protected abstract MailboxMembership copyMessage(StoreMailbox toMailbox, MailboxMembership originalMessage, long uid);
    
    protected abstract MessageMapper createMessageMapper();
    
    protected abstract Mailbox getMailboxRow() throws MailboxException;

    protected abstract MailboxMapper createMailboxMapper();
    
    public long getMailboxId() {
        return mailboxId;
    }

    public int getMessageCount(MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper messageMapper = createMessageMapper();
        return (int) messageMapper.countMessagesInMailbox(mailboxId);
    }

    public MessageResult appendMessage(byte[] messageBytes, Date internalDate,
            FetchGroup fetchGroup, MailboxSession mailboxSession, boolean isRecent)
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
                final MimeTokenStream parser = new MimeTokenStream();
                parser.setRecursionMode(MimeTokenStream.M_NO_RECURSE);
                parser.parse(new ByteArrayInputStream(messageBytes));
                final List<Header> headers = new ArrayList<Header>(INITIAL_SIZE_HEADERS);
                
                int lineNumber = 0;
                int next = parser.next();
                while (next != MimeTokenStream.T_BODY
                        && next != MimeTokenStream.T_END_OF_STREAM
                        && next != MimeTokenStream.T_START_MULTIPART) {
                    if (next == MimeTokenStream.T_FIELD) {
                        String fieldValue = parser.getFieldValue();
                        if (fieldValue.endsWith("\r\f")) {
                            fieldValue = fieldValue.substring(0,fieldValue.length() - 2);
                        }
                        if (fieldValue.startsWith(" ")) {
                            fieldValue = fieldValue.substring(1);
                        }
                        final Header header 
                            = createHeader(++lineNumber, parser.getFieldName(), 
                                fieldValue);
                        headers.add(header);
                    }
                    next = parser.next();
                }
                final InputStream bodyStream = parser.getInputStream();
                final ByteArrayOutputStream out = ContentUtils.out(bodyStream);
                next = parser.next();
                if (next == MimeTokenStream.T_EPILOGUE)  {
                    final InputStream epilogueStream = parser.getInputStream();
                    ContentUtils.out(epilogueStream, out);
                }   
                final byte[] body = out.toByteArray();
                
                final Flags flags = new Flags();
                if (isRecent) {
                    flags.add(Flags.Flag.RECENT);
                }
                final MailboxMembership message = createMessage(internalDate, uid, size, body, flags, headers);
                final MessageMapper mapper = createMessageMapper();

                mapper.begin();
                mapper.save(message);
                mapper.commit();

                final MessageResult messageResult = fillMessageResult(message, fetchGroup);
                getUidChangeTracker().found(messageResult);

                return messageResult;
            } catch (IOException e) {
                throw new MailboxException(e);
            } catch (MessagingException e) {
                throw new MailboxException(e);
            }    
        }
    }

    protected abstract MailboxMembership createMessage(Date internalDate, final long uid, final int size, final byte[] body, 
            final Flags flags, final List<Header> headers);
    
    protected abstract Header createHeader(int lineNumber, String name, String value);

    private Mailbox reserveNextUid() throws  MailboxException {
        final MailboxMapper mapper = createMailboxMapper();
        final Mailbox mailbox = mapper.consumeNextUid(mailboxId);
        return mailbox;
    }

    public Iterator getMessages(final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        UidRange range = uidRangeForMessageSet(set);
        final MessageMapper messageMapper = createMessageMapper();
        final List<MailboxMembership> rows = new ArrayList<MailboxMembership>(messageMapper.findInMailbox(set, mailboxId));
        return getMessages(fetchGroup, range, rows);
    }

    private ResultIterator getMessages(FetchGroup result, UidRange range, List<MailboxMembership> messages) {
        final ResultIterator results = getResults(result, messages);
        getUidChangeTracker().found(range, results.getMessageFlags());
        return results;
    }

    private ResultIterator getResults(FetchGroup result, List<MailboxMembership> messages) {
        Collections.sort(messages, ResultUtils.getUidComparator());
        final ResultIterator results = new ResultIterator(messages,result);
        return results;
    }

    private static UidRange uidRangeForMessageSet(MessageRange set)
    throws MailboxException {
        if (set.getType() == MessageRange.TYPE_UID) {
            return new UidRange(set.getUidFrom(), set.getUidTo());
        } else if (set.getType() == MessageRange.TYPE_ALL) {
            return new UidRange(1, -1);
        } else {
            throw new MailboxException("unsupported MessageSet: "
                    + set.getType());
        }
    }

    public MessageResult fillMessageResult(MailboxMembership message, FetchGroup result) throws MessagingException,
    MailboxException {
        return ResultUtils.loadMessageResult(message, result);
    }

    public synchronized Flags getPermanentFlags() {
        Flags permanentFlags = new Flags();
        permanentFlags.add(Flags.Flag.ANSWERED);
        permanentFlags.add(Flags.Flag.DELETED);
        permanentFlags.add(Flags.Flag.DRAFT);
        permanentFlags.add(Flags.Flag.FLAGGED);
        permanentFlags.add(Flags.Flag.SEEN);
        return permanentFlags;
    }

    public long[] recent(boolean reset, MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper mapper = createMessageMapper();
        mapper.begin();
        final List<MailboxMembership> members = mapper.findRecentMessagesInMailbox(mailboxId);
        final long[] results = new long[members.size()];

        int count = 0;
        for (MailboxMembership member:members) {
            results[count++] = member.getUid();
            if (reset) {
                member.unsetRecent();
            }
        }

        mapper.commit();
        return results;
    }

    public MessageResult getFirstUnseen(FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        try {
            final MessageMapper messageMapper = createMessageMapper();
            final List<MailboxMembership> messageRows = messageMapper.findUnseenMessagesInMailboxOrderByUid(mailboxId);
            final Iterator<MailboxMembership> it = messageRows.iterator();
            final MessageResult result;
            if (it.hasNext()) {
                result = fillMessageResult(it.next(), fetchGroup);
                if (result != null) {
                    getUidChangeTracker().found(result);
                }
            } else {
                result = null;
            }
            return result;
        } catch (MessagingException e) {
            throw new MailboxException(e);
        }
    }

    public int getUnseenCount(MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper messageMapper = createMessageMapper();
        final int count = (int) messageMapper.countUnseenMessagesInMailbox(mailboxId);
        return count;
    }

    public Iterator expunge(MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        return doExpunge(set, fetchGroup);
    }

    private Iterator doExpunge(final MessageRange set, FetchGroup fetchGroup)
    throws MailboxException {
        final MessageMapper mapper = createMessageMapper();
        mapper.begin();
        final List<MailboxMembership> members = mapper.findMarkedForDeletionInMailbox(set, mailboxId);
        final long[] uids = uids(members);
        final OrFetchGroup orFetchGroup = new OrFetchGroup(fetchGroup, FetchGroup.FLAGS);
        final ResultIterator resultIterator = new ResultIterator(members, orFetchGroup);
        // ensure all results are loaded before deletion
        final List<MessageResult> messageResults = resultIterator.toList();

        for (MailboxMembership message:members) {
            mapper.delete(message);
        }
        mapper.commit();
        getUidChangeTracker().expunged(uids);
        return messageResults.iterator();
    }


    private long[] uids(List<MailboxMembership> members) {
        final int size = members.size();
        long[] results = new long[size];
        for (int i = 0; i < size; i++) {
            final MailboxMembership member = members.get(i);
            results[i] = member.getUid();
        }
        return results;
    }

    public Iterator setFlags(Flags flags, boolean value, boolean replace,
            MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        return doSetFlags(flags, value, replace, set, fetchGroup,
                mailboxSession);
    }

    private Iterator doSetFlags(Flags flags, boolean value, boolean replace,
            final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper mapper = createMessageMapper();
        mapper.begin();
        final List<MailboxMembership> members = mapper.findInMailbox(set, mailboxId);
        UidRange uidRange = uidRangeForMessageSet(set);
        getUidChangeTracker().found(uidRange,
                ResultUtils.toMessageFlags(members));
        for (final MailboxMembership member:members) {
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
            mapper.save(member);
        }
        final OrFetchGroup orFetchGroup = new OrFetchGroup(fetchGroup,
                FetchGroup.FLAGS);
        final ResultIterator resultIterator = new ResultIterator(
                members, orFetchGroup);
        final org.apache.james.imap.mailbox.util.MessageFlags[] messageFlags = resultIterator
        .getMessageFlags();
        mapper.commit();
        tracker.flagsUpdated(messageFlags, mailboxSession.getSessionId());
        tracker.found(uidRange, messageFlags);
        return resultIterator;
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
            getUidChangeTracker().foundLastUid(mailbox.getLastUid());
            return getUidChangeTracker().getLastUid() + 1;
        }
    }

    protected UidChangeTracker getUidChangeTracker() {
        return tracker;
    }

    public Iterator search(SearchQuery query, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper messageMapper = createMessageMapper();
        final List<MailboxMembership> members = messageMapper.searchMailbox(mailboxId, query);
        final List<MailboxMembership> filteredMessages = new ArrayList<MailboxMembership>(members.size());
        for (MailboxMembership member:members) {
            try {
                if (searches.isMatch(query, member)) {
                    filteredMessages.add(member);
                }
            } catch (MailboxException e) {
                getLog()
                .info(
                        "Cannot test message against search criteria. Will continue to test other messages.",
                        e);
                if (getLog().isDebugEnabled())
                    getLog().debug("UID: " + member.getUid());
            }
        }

        return getResults(fetchGroup, filteredMessages);
    }

    public boolean isWriteable() {
        return true;
    }

    public void setLog(Log log) {
        super.setLog(log);
        searches.setLog(log);
    }

    public void copyTo(MessageRange set, StoreMailbox toMailbox, MailboxSession session) throws MailboxException {
        try {
            final MessageMapper mapper = createMessageMapper();
            mapper.begin();

            List<MailboxMembership> rows = mapper.findInMailbox(set, mailboxId);
            for (MailboxMembership originalMessage:rows) {

                final Mailbox mailbox = toMailbox.reserveNextUid();
                if (mailbox != null) {
                    // To be thread safe, we first get our own copy and the
                    // exclusive
                    // Uid
                    // TODO create own message_id and assign uid later
                    // at the moment it could lead to the situation that uid 5
                    // is
                    // inserted long before 4, when
                    // mail 4 is big and comes over a slow connection.
                    long uid = mailbox.getLastUid();

                    MailboxMembership newRow = copyMessage(toMailbox, originalMessage, uid);


                    mapper.save(newRow);

                    // TODO: Consider detaching instances and moving this code outside the inner loop
                    MessageResult messageResult = toMailbox.fillMessageResult(newRow,
                            FetchGroupImpl.MINIMAL);
                    toMailbox.getUidChangeTracker().found(messageResult);
                }
            }

            mapper.commit();

        } catch (MessagingException e) {
            throw new MailboxException(e);
        }
    }
    
    public void deleted(MailboxSession session) {
        tracker.mailboxDeleted(session.getSessionId());
    }

    public void reportRenamed(String to) {
        tracker.reportRenamed(to);
    }
}
