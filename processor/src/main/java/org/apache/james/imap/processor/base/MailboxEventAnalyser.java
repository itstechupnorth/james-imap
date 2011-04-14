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

package org.apache.james.imap.processor.base;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.UpdatedFlags;

/**
 * {@link MailboxListener} implementation which will listen for {@link Event}
 * notifications and analyze these. It will only act on {@link Event}
 * notifications which are sent for the registered MailboxPath
 */
public class MailboxEventAnalyser extends ImapStateAwareMailboxListener {

    private final long sessionId;
    private Set<Long> flagUpdateUids;
    private Flags.Flag uninterestingFlag;
    private Set<Long> expungedUids;

    private boolean isDeletedByOtherSession = false;
    private boolean sizeChanged = false;
    private boolean silentFlagChanges = false;
    private MailboxPath mailboxPath;
    private boolean closed = false;

    public MailboxEventAnalyser(final ImapSession session, final MailboxPath mailboxPath) {
        super(session);
        this.sessionId = ImapSessionUtils.getMailboxSession(session).getSessionId();
        flagUpdateUids = new TreeSet<Long>();
        expungedUids = new TreeSet<Long>();
        uninterestingFlag = Flags.Flag.RECENT;
        this.mailboxPath = mailboxPath;
    }

    /**
     * Return the name of the to observing Mailbox
     * 
     * @return name
     */
    public synchronized MailboxPath getMailboxPath() {
        return mailboxPath;
    }

    /**
     * Set the mailbox name of the to observing Mailbox
     * 
     * @param mailboxName
     */
    public synchronized void setMailboxPath(MailboxPath mailboxPath) {
        this.mailboxPath = mailboxPath;
    }

    /**
     * Handle the given {@link Event} if it was fired for the mailbox we are
     * observing
     * 
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MailboxListener#event(org.apache.james.mailbox.MailboxListener.Event)
     */
    public void event(Event event) {

        // Check if the event was for the mailbox we are observing
        if (event.getMailboxPath().equals(getMailboxPath())) {
            final long eventSessionId = event.getSession().getSessionId();
            if (event instanceof MessageEvent) {
                final MessageEvent messageEvent = (MessageEvent) event;
                // final List<Long> uids = messageEvent.getUids();
                if (messageEvent instanceof Added) {
                    sizeChanged = true;
                } else if (messageEvent instanceof FlagsUpdated) {
                    FlagsUpdated updated = (FlagsUpdated) messageEvent;
                    List<UpdatedFlags> uFlags = updated.getUpdatedFlags();
                    if (sessionId != eventSessionId || !silentFlagChanges) {
                        for (int i = 0; i < uFlags.size(); i++) {
                            UpdatedFlags u = uFlags.get(i);
                            if (interestingFlags(u)) {
                                synchronized (flagUpdateUids) {
                                    flagUpdateUids.add(u.getUid());
                                }
                            }
                        }
                    }

                    SelectedMailbox sm = session.getSelected();
                    if (sm != null) {
                        // We need to add the UID of the message to the recent
                        // list if we receive an flag update which contains a
                        // \RECENT flag
                        // See IMAP-287
                        List<UpdatedFlags> uflags = updated.getUpdatedFlags();
                        for (int i = 0; i < uflags.size(); i++) {
                            UpdatedFlags u = uflags.get(i);
                            Iterator<Flag> flags = u.iterator();

                            while (flags.hasNext()) {
                                if (Flag.RECENT.equals(flags.next())) {
                                    MailboxPath path = sm.getPath();
                                    if (path != null && path.equals(event.getMailboxPath())) {
                                        sm.addRecent(u.getUid());
                                    }
                                }
                            }
                        }
                    }
                } else if (messageEvent instanceof Expunged) {
                    synchronized (expungedUids) {
                        expungedUids.addAll(messageEvent.getUids());
                    }
                }
            } else if (event instanceof MailboxDeletion) {
                if (eventSessionId != sessionId) {
                    isDeletedByOtherSession = true;
                }
            } else if (event instanceof MailboxRenamed) {
                final MailboxRenamed mailboxRenamed = (MailboxRenamed) event;
                setMailboxPath(mailboxRenamed.getNewPath());
            }
        }
    }

    private boolean interestingFlags(UpdatedFlags updated) {
        final boolean result;
        final Iterator<Flags.Flag> it = updated.iterator();
        if (it.hasNext()) {
            final Flags.Flag flag = it.next();
            if (flag.equals(uninterestingFlag)) {
                result = false;
            } else {
                result = true;
            }
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Reset the analyzer
     */
    public synchronized void reset() {
        sizeChanged = false;
        flagUpdateUids.clear();
        expungedUids.clear();
        isDeletedByOtherSession = false;
    }

    /**
     * Are flag changes from current session ignored?
     * 
     * @return true if any flag changes from current session will be ignored,
     *         false otherwise
     */
    public final boolean isSilentFlagChanges() {
        return silentFlagChanges;
    }

    /**
     * Sets whether changes from current session should be ignored.
     * 
     * @param silentFlagChanges
     *            true if any flag changes from current session should be
     *            ignored, false otherwise
     */
    public final void setSilentFlagChanges(boolean silentFlagChanges) {
        this.silentFlagChanges = silentFlagChanges;
    }

    /**
     * Has the size of the mailbox changed?
     * 
     * @return true if new messages have been added, false otherwise
     */
    public final boolean isSizeChanged() {
        return sizeChanged;
    }

    /**
     * Is the mailbox deleted?
     * 
     * @return true when the mailbox has been deleted by another session, false
     *         otherwise
     */
    public final boolean isDeletedByOtherSession() {
        return isDeletedByOtherSession;
    }

    /**
     * Return a unmodifiable {@link Collection} of uids which have updated flags
     * 
     * @return uids
     */

    public Collection<Long> flagUpdateUids() {
        synchronized (flagUpdateUids) {
            // copy the TreeSet to fix possible
            // java.util.ConcurrentModificationException
            // See IMAP-278
            return Collections.unmodifiableSet(new TreeSet<Long>(flagUpdateUids));
        }
    }

    /**
     * Return a unmodifiable {@link Collection} of uids that where expunged
     * 
     * @return uids
     */
    public Collection<Long> expungedUids() {
        synchronized (expungedUids) {
            // copy the TreeSet to fix possible
            // java.util.ConcurrentModificationException
            // See IMAP-278
            return Collections.unmodifiableSet(new TreeSet<Long>(expungedUids));
        }
    }

    /**
     * Return if the analyzer found expunged uids
     * 
     * @return hasUids
     */
    public boolean hasExpungedUids() {
        synchronized (expungedUids) {
            return !expungedUids.isEmpty();
        }
    }

    /**
     * Mark the listener as closed and dispose all stored stuff
     */
    public synchronized void close() {
        closed = true;
        flagUpdateUids.clear();
        flagUpdateUids = null;

        uninterestingFlag = null;
        expungedUids.clear();
        expungedUids = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.processor.base.ImapStateAwareMailboxListener#
     * isListenerClosed()
     */
    protected synchronized boolean isListenerClosed() {
        return closed;
    }
}
