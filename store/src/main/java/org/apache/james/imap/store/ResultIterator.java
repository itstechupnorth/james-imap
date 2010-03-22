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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.mail.Flags;

import org.apache.james.imap.mailbox.Content;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.MimeDescriptor;
import org.apache.james.imap.mailbox.MessageResult.FetchGroup;
import org.apache.james.imap.mailbox.util.FetchGroupImpl;
import org.apache.james.imap.store.mail.model.MailboxMembership;

public class ResultIterator<Id> implements Iterator<MessageResult> {

    private final List<MailboxMembership<Id>> messages;

    private final FetchGroup fetchGroup;

    public ResultIterator(final List<MailboxMembership<Id>> messages, final FetchGroup fetchGroup) {
        super();
        if (messages == null) {
            this.messages = new ArrayList<MailboxMembership<Id>>();
        } else {
            this.messages = new ArrayList<MailboxMembership<Id>>(messages);
        }
        this.fetchGroup = fetchGroup;
    }
    
    /**
     * Iterates over the contained rows.
     * 
     * @return <code>Iterator</code> for message rows
     */
    public final Iterator<MailboxMembership<Id>> iterateRows() {
        return messages.iterator();
    }

    public boolean hasNext() {
        return !messages.isEmpty();
    }

    public MessageResult next() {
        if (messages.isEmpty()) {
            throw new NoSuchElementException("No such element.");
        }
        final MailboxMembership<Id> message = messages.get(0);
        messages.remove(message);
        MessageResult result;
        try {

            result = ResultUtils.loadMessageResult(message, this.fetchGroup);
        } catch (MailboxException e) {
            result = new UnloadedMessageResult<Id>(message, e);
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
    
    private static final class UnloadedMessageResult<Id> implements MessageResult {
        private static final FetchGroup FETCH_GROUP = FetchGroupImpl.MINIMAL;

        private final MailboxException exception;

        private final Date internalDate;

        private final int size;

        private final long uid;

        public UnloadedMessageResult(final MailboxMembership<Id> message,
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
        public Content getBody() throws MailboxException {
            throw exception;
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

        public Iterator<Header> headers() throws MailboxException {
            throw exception;
        }

        public int compareTo(MessageResult that) {
            // Java 1.5 return (int) Math.signum(uid - that.getUid());
            long diff = uid - that.getUid();
            return (int) diff == 0 ? 0 : diff > 0 ? 1 : -1;
        }

        public Content getFullContent(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Iterator<Header> iterateHeaders(MimePath path)
                throws MailboxException {
            throw exception;
        }

        public Iterator<Header> iterateMimeHeaders(MimePath path)
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
