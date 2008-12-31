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
import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.james.api.imap.AbstractLogEnabled;
import org.apache.james.imap.jpa.map.MailboxMapper;
import org.apache.james.imap.jpa.map.MessageMapper;
import org.apache.james.imap.jpa.om.Header;
import org.apache.james.imap.jpa.om.Mailbox;
import org.apache.james.imap.jpa.om.Message;
import org.apache.james.imap.jpa.om.openjpa.OpenJPAMailboxMapper;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageRange;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.UidChangeTracker;
import org.apache.james.mailboxmanager.util.UidRange;

public class JPAMailbox extends AbstractLogEnabled implements org.apache.james.mailboxmanager.mailbox.Mailbox {

    private static final int INITIAL_SIZE_HEADERS = 32;

    private long mailboxId;

    private final UidChangeTracker tracker;

    private final MessageSearches searches;

    private final EntityManagerFactory entityManagerFactory;

    JPAMailbox(final Mailbox mailbox, final Log log, final EntityManagerFactory entityManagerfactory) {
        this.searches = new MessageSearches();
        setLog(log);
        this.mailboxId = mailbox.getMailboxId();
        this.tracker = new UidChangeTracker(mailbox.getLastUid());
        this.entityManagerFactory = entityManagerfactory;
    }

    public String getName() throws MailboxManagerException {
        return getMailboxRow().getName();
    }

    public int getMessageCount(MailboxSession mailboxSession)
    throws MailboxManagerException {
        try {
            final MessageMapper messageMapper = new MessageMapper(entityManagerFactory.createEntityManager());
            return (int) messageMapper.countMessagesInMailbox(mailboxId);
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        }
    }

    public MessageResult appendMessage(MimeMessage mimeMessage, Date internalDate,
            FetchGroup fetchGroup, MailboxSession mailboxSession)
    throws MailboxManagerException {
        final Mailbox mailbox = reserveNextUid();

        if (mailbox != null) {
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
                final List<Header> headers = headers(mailboxId, uid, mimeMessage);
                final Message message = new Message(mailboxId, uid, internalDate, size, flags, body, headers);
                try {
                    final MessageMapper mapper = new MessageMapper(entityManagerFactory.createEntityManager());
                    mapper.begin();
                    mapper.save(message);
                    mapper.commit();
                } catch (PersistenceException e) {
                    throw new MailboxManagerException(e);
                }
                MessageResult messageResult = fillMessageResult(message, fetchGroup);
                getUidChangeTracker().found(messageResult);
                return messageResult;
            } catch (IOException e) {
                throw new MailboxManagerException(e);
            } catch (MessagingException e) {
                throw new MailboxManagerException(e);
            }
        } else {
            // mailboxRow==null
            throw new MailboxManagerException("Mailbox has been deleted");
        }
    }

    private byte[] body(MimeMessage message) throws IOException, MessagingException {
        InputStream is = message.getInputStream();
        final byte[] bytes = MessageUtils.toByteArray(is);
        return bytes;
    }

    private List<Header> headers(long mailboxId, long uid, MimeMessage message) throws MessagingException {
        final List<Header> results = new ArrayList<Header>(INITIAL_SIZE_HEADERS);
        int lineNumber = 0;
        for (Enumeration lines = message.getAllHeaderLines(); lines.hasMoreElements();) {
            String line = (String) lines.nextElement();
            int colon = line.indexOf(": ");
            if (colon > 0) {
                final Header header = new Header(++lineNumber, line.substring(0, colon), line.substring(colon + 2));
                results.add(header);
            }
        }
        return results;
    }

    private int size(MimeMessage message) throws IOException,
    MessagingException {
        // TODO very ugly size mesurement
        ByteArrayOutputStream sizeBos = new ByteArrayOutputStream();
        message.writeTo(new CRLFOutputStream(sizeBos));
        final int size = sizeBos.size();
        return size;
    }

    private Mailbox reserveNextUid() throws  MailboxManagerException {
        final Mailbox mailbox;
        try {
            final MailboxMapper mapper = createMailboxMapper();
            mailbox = mapper.consumeNextUid(mailboxId);
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        } 
        return mailbox;
    }

    public Iterator getMessages(final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxManagerException {
        UidRange range = uidRangeForMessageSet(set);
        try {
            final MessageMapper messageMapper = new MessageMapper(entityManagerFactory.createEntityManager());
            final List<Message> rows = new ArrayList<Message>(messageMapper.findInMailbox(set, mailboxId));
            return getMessages(fetchGroup, range, rows);
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        }
    }

    private JPAResultIterator getMessages(FetchGroup result, UidRange range, List<Message> messages) {
        final JPAResultIterator results = getResults(result, messages);
        getUidChangeTracker().found(range, results.getMessageFlags());
        return results;
    }

    private JPAResultIterator getResults(FetchGroup result, List<Message> messages) {
        Collections.sort(messages, MessageRowUtils.getUidComparator());
        final JPAResultIterator results = new JPAResultIterator(messages,result);
        return results;
    }

    private static UidRange uidRangeForMessageSet(MessageRange set)
    throws MailboxManagerException {
        if (set.getType() == MessageRange.TYPE_UID) {
            return new UidRange(set.getUidFrom(), set.getUidTo());
        } else if (set.getType() == MessageRange.TYPE_ALL) {
            return new UidRange(1, -1);
        } else {
            throw new MailboxManagerException("unsupported MessageSet: "
                    + set.getType());
        }
    }

    public MessageResult fillMessageResult(Message message, FetchGroup result) throws MessagingException,
    MailboxManagerException {
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

    public long[] recent(boolean reset, MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            final MessageMapper mapper = new MessageMapper(entityManagerFactory.createEntityManager());
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
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        } 
    }

    public MessageResult getFirstUnseen(FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            final MessageMapper messageMapper = new MessageMapper(entityManagerFactory.createEntityManager());
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
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    public int getUnseenCount(MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            final MessageMapper messageMapper = new MessageMapper(entityManagerFactory.createEntityManager());
            final int count = (int) messageMapper.countUnseenMessagesInMailbox(mailboxId);
            return count;
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        }
    }

    public Iterator expunge(MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxManagerException {
        return doExpunge(set, fetchGroup);
    }

    private Iterator doExpunge(final MessageRange set, FetchGroup fetchGroup)
    throws MailboxManagerException {
        try {
            // TODO put this into a serializable transaction
            final MessageMapper mapper = new MessageMapper(entityManagerFactory.createEntityManager());
            mapper.begin();
            final List<Message> messages = mapper.findMarkedForDeletionInMailbox(set, mailboxId);
            final long[] uids = uids(messages);
            final OrFetchGroup orFetchGroup = new OrFetchGroup(fetchGroup, FetchGroup.FLAGS);
            final JPAResultIterator resultIterator = new JPAResultIterator(messages, orFetchGroup);
            // ensure all results are loaded before deletion
            final List<MessageResult> messageResults = resultIterator.toList();

            for (Message message:messages) {
                mapper.delete(message);
            }
            mapper.commit();
            getUidChangeTracker().expunged(uids);
            return messageResults.iterator();
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        }
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
            MailboxSession mailboxSession) throws MailboxManagerException {
        return doSetFlags(flags, value, replace, set, fetchGroup,
                mailboxSession);
    }

    private Iterator doSetFlags(Flags flags, boolean value, boolean replace,
            final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            // TODO put this into a serializeable transaction
            final MessageMapper mapper = new MessageMapper(entityManagerFactory.createEntityManager());
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
            final JPAResultIterator resultIterator = new JPAResultIterator(
                    messages, orFetchGroup);
            final org.apache.james.mailboxmanager.impl.MessageFlags[] messageFlags = resultIterator
            .getMessageFlags();
            mapper.commit();
            tracker.flagsUpdated(messageFlags, mailboxSession.getSessionId());
            tracker.found(uidRange, messageFlags);
            return resultIterator;
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        }
    }

    public void addListener(MailboxListener listener) throws MailboxManagerException {
        tracker.addMailboxListener(listener);
    }

    public void removeListener(MailboxListener mailboxListener) {
        tracker.removeMailboxListener(mailboxListener);
    }

    public long getUidValidity(MailboxSession mailboxSession) throws MailboxManagerException {
        final long result = getMailboxRow().getUidValidity();
        return result;
    }

    public long getUidNext(MailboxSession mailboxSession) throws MailboxManagerException {
        Mailbox mailbox = getMailboxRow();
        if (mailbox != null) {
            getUidChangeTracker().foundLastUid(mailbox.getLastUid());
            return getUidChangeTracker().getLastUid() + 1;
        } else {
            throw new MailboxManagerException("Mailbox has been deleted");
        }
    }

    protected UidChangeTracker getUidChangeTracker() {
        return tracker;
    }

    private Mailbox getMailboxRow() throws MailboxManagerException {
        try {
            final MailboxMapper mapper = createMailboxMapper();
            return mapper.findMailboxById(mailboxId);
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        }
    }

    private MailboxMapper createMailboxMapper() {
        final MailboxMapper mapper = new OpenJPAMailboxMapper(entityManagerFactory.createEntityManager());
        return mapper;
    }

    public Iterator search(SearchQuery query, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            final MessageMapper messageMapper = new MessageMapper(entityManagerFactory.createEntityManager());
            final List<Message> messages = messageMapper.searchMailbox(mailboxId, query);
            final List<Message> filteredMessages = new ArrayList<Message>(messages.size());
            for (Message message:messages) {
                try {
                    if (searches.isMatch(query, message)) {
                        filteredMessages.add(message);
                    }
                } catch (MailboxManagerException e) {
                    getLog()
                    .info(
                            "Cannot test message against search criteria. Will continue to test other messages.",
                            e);
                    if (getLog().isDebugEnabled())
                        getLog().debug("UID: " + message.getUid());
                }
            }

            return getResults(fetchGroup, filteredMessages);
        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        }
    }

    public boolean isWriteable() {
        return true;
    }

    public void setLog(Log log) {
        super.setLog(log);
        searches.setLog(log);
    }

    public void copyTo(MessageRange set, JPAMailbox toMailbox, MailboxSession session) throws MailboxManagerException {
        try {
            final MessageMapper mapper = new MessageMapper(entityManagerFactory.createEntityManager());
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

                    Message newRow = new Message(toMailbox.mailboxId, uid, originalMessage);


                    mapper.save(newRow);

                    // TODO: Consider detaching instances and moving this code outside the inner loop
                    MessageResult messageResult = toMailbox.fillMessageResult(newRow,
                            FetchGroupImpl.MINIMAL);
                    toMailbox.getUidChangeTracker().found(messageResult);
                }
            }
            
            mapper.commit();

        } catch (PersistenceException e) {
            throw new MailboxManagerException(e);
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    public void deleted(MailboxSession session) {
        tracker.mailboxDeleted(session.getSessionId());
    }
}
