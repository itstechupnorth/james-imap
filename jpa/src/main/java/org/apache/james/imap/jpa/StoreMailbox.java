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

package org.apache.james.imap.jpa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.james.api.imap.AbstractLogEnabled;
import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.jpa.mail.JPAMessageMapper;
import org.apache.james.imap.jpa.mail.map.openjpa.OpenJPAMailboxMapper;
import org.apache.james.imap.jpa.mail.model.JPAHeader;
import org.apache.james.imap.jpa.mail.model.JPAMessage;
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
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.Message;

public class StoreMailbox extends AbstractLogEnabled implements org.apache.james.imap.mailbox.Mailbox {

    private static final int INITIAL_SIZE_HEADERS = 32;

    private long mailboxId;

    private final UidChangeTracker tracker;

    private final MessageSearches searches;

    private final EntityManagerFactory entityManagerFactory;

    StoreMailbox(final Mailbox mailbox, final Log log, final EntityManagerFactory entityManagerfactory) {
        this.searches = new MessageSearches();
        setLog(log);
        this.mailboxId = mailbox.getMailboxId();
        this.tracker = new UidChangeTracker(mailbox.getLastUid());
        this.entityManagerFactory = entityManagerfactory;
    }

    public int getMessageCount(MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper messageMapper = createMessageMapper();
        return (int) messageMapper.countMessagesInMailbox(mailboxId);
    }

    public MessageResult appendMessage(MimeMessage mimeMessage, Date internalDate,
            FetchGroup fetchGroup, MailboxSession mailboxSession)
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
                final int size = size(mimeMessage);
                final byte[] body = body(mimeMessage);
                final Flags flags = mimeMessage.getFlags();
                final List<JPAHeader> headers = headers(mailboxId, uid, mimeMessage);
                final JPAMessage message = new JPAMessage(mailboxId, uid, internalDate, size, flags, body, headers);
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

    private MessageMapper createMessageMapper() {
        final MessageMapper mapper = new JPAMessageMapper(entityManagerFactory.createEntityManager());
        return mapper;
    }

    private byte[] body(MimeMessage message) throws IOException, MessagingException {
        InputStream is = message.getInputStream();
        final byte[] bytes = MessageUtils.toByteArray(is);
        return bytes;
    }

    private List<JPAHeader> headers(long mailboxId, long uid, MimeMessage message) throws MessagingException {
        final List<JPAHeader> results = new ArrayList<JPAHeader>(INITIAL_SIZE_HEADERS);
        int lineNumber = 0;
        for (Enumeration lines = message.getAllHeaderLines(); lines.hasMoreElements();) {
            String line = (String) lines.nextElement();
            int colon = line.indexOf(": ");
            if (colon > 0) {
                final JPAHeader header = new JPAHeader(++lineNumber, line.substring(0, colon), line.substring(colon + 2));
                results.add(header);
            }
        }
        return results;
    }

    private int size(MimeMessage message) throws IOException, MessagingException {
        // TODO very ugly size mesurement
        ByteArrayOutputStream sizeBos = new ByteArrayOutputStream();
        message.writeTo(new CRLFOutputStream(sizeBos));
        final int size = sizeBos.size();
        return size;
    }

    private Mailbox reserveNextUid() throws  MailboxException {
        final MailboxMapper mapper = createMailboxMapper();
        final Mailbox mailbox = mapper.consumeNextUid(mailboxId);
        return mailbox;
    }

    public Iterator getMessages(final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        UidRange range = uidRangeForMessageSet(set);
        final MessageMapper messageMapper = createMessageMapper();
        final List<Message> rows = new ArrayList<Message>(messageMapper.findInMailbox(set, mailboxId));
        return getMessages(fetchGroup, range, rows);
    }

    private ResultIterator getMessages(FetchGroup result, UidRange range, List<Message> messages) {
        final ResultIterator results = getResults(result, messages);
        getUidChangeTracker().found(range, results.getMessageFlags());
        return results;
    }

    private ResultIterator getResults(FetchGroup result, List<Message> messages) {
        Collections.sort(messages, MessageRowUtils.getUidComparator());
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

    public MessageResult fillMessageResult(Message message, FetchGroup result) throws MessagingException,
    MailboxException {
        return MessageRowUtils.loadMessageResult(message, result);
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
        final List<Message> messages = mapper.findRecentMessagesInMailbox(mailboxId);
        final long[] results = new long[messages.size()];

        int count = 0;
        for (Message message:messages) {
            results[count++] = message.getUid();
            if (reset) {
                message.unsetRecent();
            }
        }

        mapper.commit();
        return results;
    }

    public MessageResult getFirstUnseen(FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        try {
            final MessageMapper messageMapper = createMessageMapper();
            final List<Message> messageRows = messageMapper.findUnseenMessagesInMailboxOrderByUid(mailboxId);
            final Iterator<Message> it = messageRows.iterator();
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
        final List<Message> messages = mapper.findMarkedForDeletionInMailbox(set, mailboxId);
        final long[] uids = uids(messages);
        final OrFetchGroup orFetchGroup = new OrFetchGroup(fetchGroup, FetchGroup.FLAGS);
        final ResultIterator resultIterator = new ResultIterator(messages, orFetchGroup);
        // ensure all results are loaded before deletion
        final List<MessageResult> messageResults = resultIterator.toList();

        for (Message message:messages) {
            mapper.delete(message);
        }
        mapper.commit();
        getUidChangeTracker().expunged(uids);
        return messageResults.iterator();
    }


    private long[] uids(List<Message> messages) {
        final int size = messages.size();
        long[] results = new long[size];
        for (int i = 0; i < size; i++) {
            final Message message = messages.get(i);
            results[i] = message.getUid();
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
        final List<Message> messages = mapper.findInMailbox(set, mailboxId);
        UidRange uidRange = uidRangeForMessageSet(set);
        getUidChangeTracker().found(uidRange,
                MessageRowUtils.toMessageFlags(messages));
        for (final Message message:messages) {
            if (replace) {
                message.setFlags(flags);
            } else {
                Flags current = message.createFlags();
                if (value) {
                    current.add(flags);
                } else {
                    current.remove(flags);
                }
                message.setFlags(current);
            }
            mapper.save(message);
        }
        final OrFetchGroup orFetchGroup = new OrFetchGroup(fetchGroup,
                FetchGroup.FLAGS);
        final ResultIterator resultIterator = new ResultIterator(
                messages, orFetchGroup);
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

    private Mailbox getMailboxRow() throws MailboxException {
        final MailboxMapper mapper = createMailboxMapper();
        return mapper.findMailboxById(mailboxId);
    }

    private MailboxMapper createMailboxMapper() {
        final JPAMailboxMapper mapper = new OpenJPAMailboxMapper(entityManagerFactory.createEntityManager());
        return mapper;
    }

    public Iterator search(SearchQuery query, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException {
        final MessageMapper messageMapper = createMessageMapper();
        final List<Message> messages = messageMapper.searchMailbox(mailboxId, query);
        final List<Message> filteredMessages = new ArrayList<Message>(messages.size());
        for (Message message:messages) {
            try {
                if (searches.isMatch(query, message)) {
                    filteredMessages.add(message);
                }
            } catch (MailboxException e) {
                getLog()
                .info(
                        "Cannot test message against search criteria. Will continue to test other messages.",
                        e);
                if (getLog().isDebugEnabled())
                    getLog().debug("UID: " + message.getUid());
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

            List<Message> rows = mapper.findInMailbox(set, mailboxId);
            for (Message originalMessage:rows) {

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

                    Message newRow = new JPAMessage(toMailbox.mailboxId, uid, (JPAMessage) originalMessage);


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
