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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.james.api.imap.AbstractLogEnabled;
import org.apache.james.imap.jpa.om.MailboxMapper;
import org.apache.james.imap.jpa.om.MailboxRow;
import org.apache.james.imap.jpa.om.MessageBody;
import org.apache.james.imap.jpa.om.MessageFlags;
import org.apache.james.imap.jpa.om.MessageHeader;
import org.apache.james.imap.jpa.om.MessageMapper;
import org.apache.james.imap.jpa.om.MessageRow;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageRange;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.UidChangeTracker;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
import org.apache.james.mailboxmanager.util.UidRange;
import org.apache.torque.TorqueException;

import com.workingdogs.village.DataSetException;

public class JPAMailbox extends AbstractLogEnabled implements Mailbox {

    private long mailboxId;

    private final UidChangeTracker tracker;

    private final MessageSearches searches;

    private final MailboxMapper mapper;
    private final MessageMapper messageMapper;

    JPAMailbox(final MailboxRow mailboxRow, final Log log) {
        this.searches = new MessageSearches();
        setLog(log);
        this.mailboxId = mailboxRow.getMailboxId();
        this.tracker = new UidChangeTracker(mailboxRow.getLastUid());
        this.mapper = new MailboxMapper();
        this.messageMapper = new MessageMapper();
    }

    public String getName() throws MailboxManagerException {
        try {
            return getMailboxRow().getName();
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
    }

    public int getMessageCount(MailboxSession mailboxSession)
    throws MailboxManagerException {
        try {
            return messageMapper.countMessages(mailboxId);
        } catch (Exception e) {
            throw new MailboxManagerException(e);
        }
    }

    public MessageResult appendMessage(MimeMessage message, Date internalDate,
            FetchGroup fetchGroup, MailboxSession mailboxSession)
    throws MailboxManagerException {
        final MailboxRow mailbox = reserveNextUid();

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
                long uid = mailbox.getLastUid();

                MessageRow messageRow = new MessageRow();
                messageRow.setMailboxId(mailboxId);
                messageRow.setUid(uid);
                messageRow.setInternalDate(internalDate);

                final int size = size(message);
                messageRow.setSize(size);
                populateFlags(message, messageRow);

                addHeaders(message, messageRow);

                MessageBody mb = populateBody(message);
                messageRow.addMessageBody(mb);

                save(messageRow);
                MessageResult messageResult = fillMessageResult(messageRow,
                        fetchGroup);
                getUidChangeTracker().found(messageResult);
                return messageResult;
            } catch (Exception e) {
                throw new MailboxManagerException(e);
            }
        } else {
            // mailboxRow==null
            throw new MailboxManagerException("Mailbox has been deleted");
        }
    }

    private void populateFlags(MimeMessage message, MessageRow messageRow)
    throws MessagingException, TorqueException {
        final Flags flags = message.getFlags();
        buildFlags(messageRow, flags);
    }

    private void buildFlags(MessageRow messageRow, final Flags flags)
    throws TorqueException {
        MessageFlags messageFlags = new MessageFlags();
        messageFlags.setFlags(flags);
        messageRow.addMessageFlags(messageFlags);
    }

    private MessageBody populateBody(MimeMessage message) throws IOException,
    MessagingException {
        MessageBody mb = new MessageBody();

        InputStream is = message.getInputStream();

        final byte[] bytes = MessageUtils.toByteArray(is);
        mb.setBody(bytes);
        return mb;
    }

    private void addHeaders(MimeMessage message, MessageRow messageRow)
    throws MessagingException, TorqueException {
        int line_number = 0;

        for (Enumeration lines = message.getAllHeaderLines(); lines
        .hasMoreElements();) {
            String line = (String) lines.nextElement();
            int colon = line.indexOf(": ");
            if (colon > 0) {
                line_number++;
                MessageHeader mh = new MessageHeader();
                mh.setLineNumber(line_number);
                mh.setField(line.substring(0, colon));
                // TODO avoid unlikely IOOB Exception
                mh.setValue(line.substring(colon + 2));
                messageRow.addMessageHeader(mh);
            }
        }
    }

    private int size(MimeMessage message) throws IOException,
    MessagingException {
        // TODO very ugly size mesurement
        ByteArrayOutputStream sizeBos = new ByteArrayOutputStream();
        message.writeTo(new CRLFOutputStream(sizeBos));
        final int size = sizeBos.size();
        return size;
    }

    private void save(MessageRow messageRow) throws TorqueException {
        messageMapper.save(messageRow);
    }

    private MailboxRow reserveNextUid() throws  MailboxManagerException {
        final MailboxRow mailboxRow;
        try {
            mailboxRow = messageMapper.consumeNextUid(mailboxId);
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (SQLException e) {
            throw new MailboxManagerException(e);
        } 
        return mailboxRow;
    }


    public Iterator getMessages(final MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxManagerException {
        UidRange range = uidRangeForMessageSet(set);
        try {
            List rows = mapper.findInMailbox(set, mailboxId);
            return getMessages(fetchGroup, range, rows);
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    private JPAResultIterator getMessages(FetchGroup result, UidRange range,List rows) 
    throws TorqueException, MessagingException, MailboxManagerException {
        final JPAResultIterator results = getResults(result, rows);
        getUidChangeTracker().found(range, results.getMessageFlags());
        return results;
    }

    private JPAResultIterator getResults(FetchGroup result, List rows)
    throws TorqueException {
        Collections.sort(rows, MessageRowUtils.getUidComparator());
        final JPAResultIterator results = new JPAResultIterator(rows,
                result);
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

    public MessageResult fillMessageResult(MessageRow messageRow,
            FetchGroup result) throws TorqueException, MessagingException,
            MailboxManagerException {
        return MessageRowUtils.loadMessageResult(messageRow, result);
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

    public long[] recent(boolean reset, MailboxSession mailboxSession)
    throws MailboxManagerException {
        try {
            final List messageRows = messageMapper.findRecent(mailboxId);
            final long[] results = new long[messageRows.size()];
            int count = 0;
            for (Iterator it = messageRows.iterator(); it.hasNext();) {
                final MessageRow row = (MessageRow) it.next();
                results[count++] = row.getUid();
            }

            if (reset) {
                messageMapper.resetRecent(mailboxId);
            }
            return results;
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } 
    }

    public MessageResult getFirstUnseen(FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            List messageRows = messageMapper.findUnseen(mailboxId);
            if (messageRows.size() > 0) {
                MessageResult messageResult = fillMessageResult(
                        (MessageRow) messageRows.get(0), fetchGroup);
                if (messageResult != null) {
                    getUidChangeTracker().found(messageResult);
                }

                return messageResult;
            } else {
                return null;
            }
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    public int getUnseenCount(MailboxSession mailboxSession)
    throws MailboxManagerException {
        try {
            final int count = messageMapper.countMessages(
                    new Flags(Flags.Flag.SEEN), false, mailboxId);
            return count;
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (DataSetException e) {
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
            final List messageRows = mapper.findMarkedForDeletionInMailbox(set, mailboxId);
            final long[] uids = uids(messageRows);
            final OrFetchGroup orFetchGroup = new OrFetchGroup(fetchGroup,
                    FetchGroup.FLAGS);
            final JPAResultIterator resultIterator = new JPAResultIterator(
                    messageRows, orFetchGroup);
            // ensure all results are loaded before deletion
            Collection messageResults = IteratorUtils.toList(resultIterator);

            for (Iterator iter = messageRows.iterator(); iter.hasNext();) {
                MessageRow messageRow = (MessageRow) iter.next();
                messageMapper.delete(messageRow);
            }
            getUidChangeTracker().expunged(uids);
            return messageResults.iterator();
        } catch (Exception e) {
            throw new MailboxManagerException(e);
        }
    }

    private long[] uids(List messageRows) {
        final int size = messageRows.size();
        long[] results = new long[size];
        for (int i = 0; i < size; i++) {
            final MessageRow messageRow = (MessageRow) messageRows.get(i);
            results[i] = (messageRow).getUid();
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
            final List messageRows = mapper.findInMailbox(set, mailboxId);
            UidRange uidRange = uidRangeForMessageSet(set);
            getUidChangeTracker().found(uidRange,
                    MessageRowUtils.toMessageFlags(messageRows));
            for (Iterator iter = messageRows.iterator(); iter.hasNext();) {
                final MessageRow messageRow = (MessageRow) iter.next();
                final MessageFlags messageFlags = messageRow.getMessageFlags();
                if (messageFlags != null) {
                    if (replace) {
                        messageFlags.setFlags(flags);
                    } else {
                        Flags current = messageFlags.getFlagsObject();
                        if (value) {
                            current.add(flags);
                        } else {
                            current.remove(flags);
                        }
                        messageFlags.setFlags(current);
                    }
                    messageMapper.save(messageFlags);
                }
            }
            final OrFetchGroup orFetchGroup = new OrFetchGroup(fetchGroup,
                    FetchGroup.FLAGS);
            final JPAResultIterator resultIterator = new JPAResultIterator(
                    messageRows, orFetchGroup);
            final org.apache.james.mailboxmanager.impl.MessageFlags[] messageFlags = resultIterator
            .getMessageFlags();
            tracker.flagsUpdated(messageFlags, mailboxSession.getSessionId());
            tracker.found(uidRange, messageFlags);
            return resultIterator;
        } catch (Exception e) {
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
        try {
            final long result = getMailboxRow().getUidValidity();
            return result;
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }

    }

    public long getUidNext(MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            MailboxRow mailbox = getMailboxRow();
            if (mailbox != null) {
                getUidChangeTracker().foundLastUid(mailbox.getLastUid());
                return getUidChangeTracker().getLastUid() + 1;
            } else {
                throw new MailboxManagerException(
                        "Mailbox has been deleted");
            }
        }  catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
    }

    protected UidChangeTracker getUidChangeTracker() {
        return tracker;
    }

    private MailboxRow getMailboxRow() throws TorqueException {
        return mapper.findById(mailboxId);
    }

    public Iterator search(SearchQuery query, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxManagerException {
        try {

            final List rows = messageMapper.find(query);
            final List filteredMessages = new ArrayList();
            for (Iterator it = rows.iterator(); it.hasNext();) {
                final MessageRow row = (MessageRow) it.next();
                try {
                    if (searches.isMatch(query, row)) {
                        filteredMessages.add(row);
                    }
                } catch (TorqueException e) {
                    getLog()
                    .info(
                            "Cannot test message against search criteria. Will continue to test other messages.",
                            e);
                    if (getLog().isDebugEnabled())
                        getLog().debug("UID: " + row.getUid());
                }
            }

            return getResults(fetchGroup, filteredMessages);
        } catch (TorqueException e) {
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

    public void copyTo(MessageRange set, JPAMailbox toMailbox,
            MailboxSession session) throws MailboxManagerException {
        try {
            List rows = mapper.findInMailbox(set, mailboxId);
            toMailbox.copy(rows, session);
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    private void copy(List rows, MailboxSession session)
    throws MailboxManagerException {
        try {
            for (Iterator iter = rows.iterator(); iter.hasNext();) {
                MessageRow fromRow = (MessageRow) iter.next();
                final MailboxRow mailbox = reserveNextUid();

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

                    MessageRow newRow = new MessageRow();
                    newRow.setMailboxId(mailboxId);
                    newRow.setUid(uid);
                    newRow.setInternalDate(fromRow.getInternalDate());
                    newRow.setSize(fromRow.getSize());
                    buildFlags(newRow, fromRow.getMessageFlags()
                            .getFlagsObject());

                    final List headers = fromRow.getMessageHeaders();
                    for (Iterator iterator = headers.iterator(); iterator
                    .hasNext();) {
                        final MessageHeader fromHeader = (MessageHeader) iterator
                        .next();
                        final MessageHeader newHeader = new MessageHeader(
                                fromHeader.getField(), fromHeader.getValue(),
                                fromHeader.getLineNumber());
                        newRow.addMessageHeader(newHeader);
                    }

                    MessageBody mb = new MessageBody(fromRow.getBodyContent());
                    newRow.addMessageBody(mb);

                    save(newRow);
                    MessageResult messageResult = fillMessageResult(newRow,
                            FetchGroupImpl.MINIMAL);
                    getUidChangeTracker().found(messageResult);
                }
            }
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    public void deleted(MailboxSession session) {
        tracker.mailboxDeleted(session.getSessionId());
    }
}
