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

package org.apache.james.imap.mailbox.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.imap.mailbox.MailboxListener;

public class MailboxEventDispatcher implements MailboxListener {

    private final Set<MailboxListener> listeners = new CopyOnWriteArraySet<MailboxListener>();

    private void pruneClosed() {
        final Collection<MailboxListener> closedListeners = new ArrayList<MailboxListener>();
        for (MailboxListener listener:listeners) {
            if (listener.isClosed()) {
                closedListeners.add(listener);
            }
        }
        if (!closedListeners.isEmpty()) {
            listeners.removeAll(closedListeners);
        }
    }
    
    public void addMailboxListener(MailboxListener mailboxListener) {
        pruneClosed();
        listeners.add(mailboxListener);
    }

    public void added(long uid, long sessionId) {
        pruneClosed();
        final AddedImpl added = new AddedImpl(sessionId, uid);
        event(added);
    }

    public void expunged(final long uid, long sessionId) {
        final ExpungedImpl expunged = new ExpungedImpl(sessionId, uid);
        event(expunged);
    }

    public void flagsUpdated(final long uid, long sessionId,
            final Flags original, final Flags updated) {
        final FlagsUpdatedImpl flags = new FlagsUpdatedImpl(sessionId, uid,
                original, updated);
        event(flags);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxListener#event(org.apache.james.imap.mailbox.MailboxListener.Event)
     */
    public void event(Event event) {
        for (Iterator<MailboxListener> iter = listeners.iterator(); iter.hasNext();) {
            MailboxListener mailboxListener = iter.next();
            mailboxListener.event(event);
        }
    }

    public int size() {
        return listeners.size();
    }
    
    public void mailboxRenamed(String to, long sessionId) {
        event(new MailboxRenamedEventImpl(to, sessionId));
    }

    private final static class AddedImpl extends MailboxListener.Added {

        private final long sessionId;

        private final long subjectUid;

        public AddedImpl(final long sessionId, final long subjectUid) {
            super();
            this.sessionId = sessionId;
            this.subjectUid = subjectUid;
        }

        public long getSubjectUid() {
            return subjectUid;
        }

        public long getSessionId() {
            return sessionId;
        }
    }

    private final static class ExpungedImpl extends MailboxListener.Expunged {

        private final long sessionId;

        private final long subjectUid;

        public ExpungedImpl(final long sessionId, final long subjectUid) {
            super();
            this.sessionId = sessionId;
            this.subjectUid = subjectUid;
        }

        public long getSubjectUid() {
            return subjectUid;
        }

        public long getSessionId() {
            return sessionId;
        }
    }

    private final static class FlagsUpdatedImpl extends
            MailboxListener.FlagsUpdated {

        private static final boolean isChanged(final Flags original,
                final Flags updated, Flags.Flag flag) {
            return original != null && updated != null
                    && (original.contains(flag) ^ updated.contains(flag));
        }

        private static final Flags.Flag[] FLAGS = { Flags.Flag.ANSWERED,
                Flags.Flag.DELETED, Flags.Flag.DRAFT, Flags.Flag.FLAGGED,
                Flags.Flag.RECENT, Flags.Flag.SEEN };

        private static final int NUMBER_OF_SYSTEM_FLAGS = 6;

        private final long sessionId;

        private final long subjectUid;

        private final boolean[] modifiedFlags;

        private final Flags newFlags;

        public FlagsUpdatedImpl(final long sessionId, final long subjectUid,
                final Flags original, final Flags updated) {
            this(sessionId, subjectUid, updated, isChanged(original, updated,
                    Flags.Flag.ANSWERED), isChanged(original, updated,
                    Flags.Flag.DELETED), isChanged(original, updated,
                    Flags.Flag.DRAFT), isChanged(original, updated,
                    Flags.Flag.FLAGGED), isChanged(original, updated,
                    Flags.Flag.RECENT), isChanged(original, updated,
                    Flags.Flag.SEEN));
        }

        public FlagsUpdatedImpl(final long sessionId, final long subjectUid,
                final Flags newFlags, boolean answeredUpdated,
                boolean deletedUpdated, boolean draftUpdated,
                boolean flaggedUpdated, boolean recentUpdated,
                boolean seenUpdated) {
            super();
            this.sessionId = sessionId;
            this.subjectUid = subjectUid;
            this.modifiedFlags = new boolean[NUMBER_OF_SYSTEM_FLAGS];
            this.modifiedFlags[0] = answeredUpdated;
            this.modifiedFlags[1] = deletedUpdated;
            this.modifiedFlags[2] = draftUpdated;
            this.modifiedFlags[3] = flaggedUpdated;
            this.modifiedFlags[4] = recentUpdated;
            this.modifiedFlags[5] = seenUpdated;
            this.newFlags = newFlags;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.imap.mailbox.MailboxListener.MessageEvent#getSubjectUid()
         */
        public long getSubjectUid() {
            return subjectUid;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.imap.mailbox.MailboxListener.Event#getSessionId()
         */
        public long getSessionId() {
            return sessionId;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.imap.mailbox.MailboxListener.FlagsUpdated#flagsIterator()
         */
        public Iterator<Flag> flagsIterator() {
            return new FlagsIterator();
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.imap.mailbox.MailboxListener.FlagsUpdated#getNewFlags()
         */
        public Flags getNewFlags() {
            return newFlags;
        }

        private class FlagsIterator implements Iterator<Flag> {
            private int position;

            public FlagsIterator() {
                position = 0;
                nextPosition();
            }

            private void nextPosition() {
                if (position < NUMBER_OF_SYSTEM_FLAGS) {
                    if (!modifiedFlags[position]) {
                        position++;
                        nextPosition();
                    }
                }
            }

            /*
             * (non-Javadoc)
             * @see java.util.Iterator#hasNext()
             */
            public boolean hasNext() {
                return position < NUMBER_OF_SYSTEM_FLAGS;
            }

            /*
             * (non-Javadoc)
             * @see java.util.Iterator#next()
             */
            public Flag next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final Flag result = FLAGS[position++];
                nextPosition();
                return result;
            }

            /*
             * (non-Javadoc)
             * @see java.util.Iterator#remove()
             */
            public void remove() {
                throw new UnsupportedOperationException("Read only");
            }
        }
    }

    public void mailboxDeleted(long sessionId) {
        final MailboxDeletionEventImpl event = new MailboxDeletionEventImpl(
                sessionId);
        event(event);
    }

    private static final class MailboxDeletionEventImpl implements
            MailboxListener.MailboxDeletionEvent {
        private final long sessionId;

        public MailboxDeletionEventImpl(final long sessionId) {
            super();
            this.sessionId = sessionId;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.imap.mailbox.MailboxListener.Event#getSessionId()
         */
        public long getSessionId() {
            return sessionId;
        }
    }

    private static final class MailboxRenamedEventImpl implements MailboxListener.MailboxRenamed {
        private final String newName;
        private final long sessionId;

        public MailboxRenamedEventImpl(final String newName, final long sessionId) {
            super();
            this.newName = newName;
            this.sessionId = sessionId;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.imap.mailbox.MailboxListener.MailboxRenamed#getNewName()
         */
        public String getNewName() {
            return newName;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.imap.mailbox.MailboxListener.Event#getSessionId()
         */
        public long getSessionId() {
            return sessionId;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxListener#isClosed()
     */
    public boolean isClosed() {
        return false;
    }
}
