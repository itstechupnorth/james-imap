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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.MessageResult.FetchGroup;
import org.apache.james.imap.mailbox.util.FetchGroupImpl;
import org.apache.james.imap.mailbox.util.MessageFlags;
import org.apache.james.imap.store.mail.model.Message;

public class ResultIterator implements Iterator {

    private final List<Message> messages;

    private final FetchGroup fetchGroup;

    @SuppressWarnings("unchecked")
    public ResultIterator(final List<Message> messages, final FetchGroup fetchGroup) {
        super();
        if (messages == null) {
            this.messages = Collections.EMPTY_LIST;
        } else {
            this.messages = new ArrayList<Message>(messages);
        }
        this.fetchGroup = fetchGroup;
    }

    public MessageFlags[] getMessageFlags() {
        final MessageFlags[] results = MessageRowUtils.toMessageFlags(messages);
        return results;
    }

    /**
     * Iterates over the contained rows.
     * 
     * @return <code>Iterator</code> for message rows
     */
    public final Iterator iterateRows() {
        return messages.iterator();
    }

    public boolean hasNext() {
        return !messages.isEmpty();
    }

    public Object next() {
        if (messages.isEmpty()) {
            throw new NoSuchElementException("No such element.");
        }
        final Message message = messages.get(0);
        messages.remove(message);
        MessageResult result;
        try {

            result = MessageRowUtils.loadMessageResult(message, this.fetchGroup);
        } catch (MailboxException e) {
            result = new UnloadedMessageResult(message, e);
        }
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("Read only iteration.");
    }

    public List<MessageResult> toList() {
        final List<MessageResult> results = new ArrayList<MessageResult>(messages.size());
        while(hasNext()) {
            results.add((MessageResult) next());
        }
        return results;
    }
    
    private static final class UnloadedMessageResult implements MessageResult {
        private static final FetchGroup FETCH_GROUP = new FetchGroupImpl(
                FetchGroup.INTERNAL_DATE | FetchGroup.SIZE);

        private final MailboxException exception;

        private final Date internalDate;

        private final int size;

        private final long uid;

        public UnloadedMessageResult(final Message message,
                final MailboxException exception) {
            super();
            internalDate = message.getInternalDate();
            size = message.getSize();
            uid = message.getUid();
            this.exception = exception;
        }

        public Flags getFlags() throws MailboxException {
            throw exception;
        }

        public Content getFullContent() throws MailboxException {
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

        public Content getBody() throws MailboxException {
            throw exception;
        }

        public MimeMessage getMimeMessage() throws MailboxException {
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

        public Iterator headers() throws MailboxException {
            throw exception;
        }

        public int compareTo(Object o) {
            MessageResult that = (MessageResult) o;
            // Java 1.5 return (int) Math.signum(uid - that.getUid());
            long diff = uid - that.getUid();
            return (int) diff == 0 ? 0 : diff > 0 ? 1 : -1;
        }

        public Content getMessageBody(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Content getFullContent(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Iterator iterateHeaders(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Iterator iterateMimeHeaders(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Content getBody(MimePath path) throws MailboxException {
            throw exception;
        }

        public Content getMimeBody(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public MimeDescriptor getMimeDescriptor()
                throws MailboxException {
            throw exception;
        }

    }
}
