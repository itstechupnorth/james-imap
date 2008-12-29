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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.apache.james.imap.jpa.om.MessageRow;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.MessageFlags;
import org.apache.torque.TorqueException;

public class JPAResultIterator implements Iterator {

    private final Buffer messageRows;

    private final FetchGroup fetchGroup;

    public JPAResultIterator(final Collection messageRows,
            final FetchGroup fetchGroup) {
        super();
        if (messageRows == null || messageRows.isEmpty()) {
            this.messageRows = BufferUtils.EMPTY_BUFFER;
        } else {
            this.messageRows = new BoundedFifoBuffer(messageRows);
        }
        this.fetchGroup = fetchGroup;
    }

    public MessageFlags[] getMessageFlags() throws TorqueException {
        final MessageFlags[] results = MessageRowUtils
                .toMessageFlags(messageRows);
        return results;
    }

    /**
     * Iterates over the contained rows.
     * 
     * @return <code>Iterator</code> for message rows
     */
    public final Iterator iterateRows() {
        return messageRows.iterator();
    }

    public boolean hasNext() {
        return !messageRows.isEmpty();
    }

    public Object next() {
        if (messageRows.isEmpty()) {
            throw new NoSuchElementException("No such element.");
        }
        final MessageRow messageRow = (MessageRow) messageRows.remove();
        MessageResult result;
        try {

            result = MessageRowUtils.loadMessageResult(messageRow,
                    this.fetchGroup);
        } catch (TorqueException e) {
            result = new UnloadedMessageResult(messageRow,
                    new MailboxManagerException(e));
        } catch (MailboxManagerException e) {
            result = new UnloadedMessageResult(messageRow, e);
        }
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("Read only iteration.");
    }

    private static final class UnloadedMessageResult implements MessageResult {
        private static final FetchGroup FETCH_GROUP = new FetchGroupImpl(
                FetchGroup.INTERNAL_DATE | FetchGroup.SIZE);

        private final MailboxManagerException exception;

        private final Date internalDate;

        private final int size;

        private final long uid;

        public UnloadedMessageResult(final MessageRow row,
                final MailboxManagerException exception) {
            super();
            internalDate = row.getInternalDate();
            size = row.getSize();
            uid = row.getUid();
            this.exception = exception;
        }

        public Flags getFlags() throws MailboxManagerException {
            throw exception;
        }

        public Content getFullContent() throws MailboxManagerException {
            throw exception;
        }

        public FetchGroup getIncludedResults() {
            return FETCH_GROUP;
        }

        public Date getInternalDate() {
            return internalDate;
        }

        public String getKey() {
            return null;
        }

        public Content getBody() throws MailboxManagerException {
            throw exception;
        }

        public MimeMessage getMimeMessage() throws MailboxManagerException {
            throw exception;
        }

        public int getMsn() {
            return 0;
        }

        public int getSize() {
            return size;
        }

        public long getUid() {
            return uid;
        }

        public long getUidValidity() {
            return 0;
        }

        public Iterator headers() throws MailboxManagerException {
            throw exception;
        }

        public int compareTo(Object o) {
            MessageResult that = (MessageResult) o;
            // Java 1.5 return (int) Math.signum(uid - that.getUid());
            long diff = uid - that.getUid();
            return (int) diff == 0 ? 0 : diff > 0 ? 1 : -1;
        }

        public Content getMessageBody(MimePath path)
                throws MailboxManagerException {
            throw exception;
        }

        public Content getFullContent(MimePath path)
                throws MailboxManagerException {
            throw exception;
        }

        public Iterator iterateHeaders(MimePath path)
                throws MailboxManagerException {
            throw exception;
        }

        public Iterator iterateMimeHeaders(MimePath path)
                throws MailboxManagerException {
            throw exception;
        }

        public Content getBody(MimePath path) throws MailboxManagerException {
            throw exception;
        }

        public Content getMimeBody(MimePath path)
                throws MailboxManagerException {
            throw exception;
        }

        public MimeDescriptor getMimeDescriptor()
                throws MailboxManagerException {
            throw exception;
        }

    }
}