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
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;

/**
 * {@link MailboxListener} implementation which will listen for {@link Event} notifications and 
 * analyze these. It will only act on {@link Event} notifications which are sent for the registered
 * MailboxPath
 */
public class MailboxEventAnalyser implements MailboxListener {

    private final long sessionId;
    private Set<Long> flagUpdateUids;
    private Flags.Flag uninterestingFlag;
    private Set<Long> expungedUids;
    
    private boolean isDeletedByOtherSession = false;
    private boolean sizeChanged = false;
    private boolean silentFlagChanges = false;
    private MailboxPath mailboxPath;
    private boolean closed = false;

    public MailboxEventAnalyser(final long sessionId, final MailboxPath mailboxPath) {
        super();
        this.sessionId = sessionId;
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
    public MailboxPath getMailboxPath() {
        return mailboxPath;
    }

    /**
     * Set the mailbox name of the to observing Mailbox
     * 
     * @param mailboxName
     */
    public void setMailboxPath(MailboxPath mailboxPath) {
        this.mailboxPath = mailboxPath;
    }

    /**
     * Handle the given {@link Event} if it was fired for the mailbox we are observing
     * 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxListener#event(org.apache.james.mailbox.MailboxListener.Event)
     */
    public void event(Event event) {

        // Check if the event was for the mailbox we are observing
        if (event.getMailboxPath().equals(mailboxPath)) {
            final long eventSessionId = event.getSessionId();
            if (event instanceof MessageEvent) {
                final MessageEvent messageEvent = (MessageEvent) event;
                final long uid = messageEvent.getSubjectUid();
                if (messageEvent instanceof Added) {
                    sizeChanged = true;
                } else if (messageEvent instanceof FlagsUpdated) {
                    FlagsUpdated updated = (FlagsUpdated) messageEvent;
                    if (interestingFlags(updated)
                            && (sessionId != eventSessionId || !silentFlagChanges)) {
                        final Long uidObject = new Long(uid);
                        flagUpdateUids.add(uidObject);
                    }
                } else if (messageEvent instanceof Expunged) {
                    final Long uidObject = new Long(uid);
                    expungedUids.add(uidObject);
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

    private boolean interestingFlags(FlagsUpdated updated) {
        final boolean result;
        final Iterator<Flags.Flag> it = updated.flagsIterator();
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
    public void reset() {
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
        return Collections.unmodifiableSet(flagUpdateUids);
    }

    /**
     * Return a unmodifiable {@link Collection} of uids that where expunged
     * 
     * @return uids
     */
    public Collection<Long> expungedUids() {
        return Collections.unmodifiableSet(expungedUids);
    }

    /**
     * Return if the analyzer found expunged uids
     * 
     * @return hasUids
     */
    public boolean hasExpungedUids() {
        return !expungedUids.isEmpty();
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
     * @see org.apache.james.mailbox.MailboxListener#isClosed()
     */
    public synchronized boolean isClosed() {
        return closed;
    }
}
