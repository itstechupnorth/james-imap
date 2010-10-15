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

package org.apache.james.mailbox.store;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;

/**
 * {@link MailboxListener} which takes care of update the lastUid for a Mailbox.
 * 
 * 
 *
 */
public class LastUidTracker implements MailboxListener {

    private final ConcurrentMap<MailboxPath, AtomicLong> lastUids;
    private final MailboxSession session;

    public LastUidTracker(ConcurrentMap<MailboxPath, AtomicLong> lastUids, MailboxSession session) {
        this.lastUids = lastUids;
        this.session = session;
    }
    
    public void event(Event event) {
        if (event instanceof Added) {
            Added addedEvent = (Added) event;
            lastUids.putIfAbsent(addedEvent.getMailboxPath(), new AtomicLong(0));
            AtomicLong lastUid = lastUids.get(addedEvent.getMailboxPath());
            long uid = ((Added) event).getSubjectUid();
            if (uid > lastUid.get()) {
                lastUid.set(uid);
            }
        } else if (event instanceof MailboxDeletion) {
            // remove the lastUid if the Mailbox was deleted
            lastUids.remove(((MailboxDeletion) event).getMailboxPath());
        } else if (event instanceof MailboxRenamed) {
            // If the mailbox was renamed we need take care of update the lastUid
            // and move it to the new MailboxPath
            MailboxRenamed rEvent = (MailboxRenamed) event;
            AtomicLong oldLastUid = lastUids.remove(rEvent.getMailboxPath());
            if (oldLastUid == null) {
                oldLastUid = new AtomicLong(0);
            }
            lastUids.putIfAbsent(rEvent.getNewPath(), oldLastUid);
        }
    }

    public boolean isClosed() {
        if (session == null || session.isOpen() == false) {
            return true;
        }
        return false;
    }
    
}
