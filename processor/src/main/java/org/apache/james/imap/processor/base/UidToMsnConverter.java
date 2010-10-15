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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.mailbox.MailboxListener;

/**
 * {@link MailboxListener} which takes care of maintaining a mapping between message uids and msn (index)
 * 
 *
 * TODO: This is a major memory hog
 * TODO: Each concurrent session requires one, and typical clients now open many
 */
public class UidToMsnConverter implements MailboxListener {
    private SortedMap<Integer, Long> msnToUid;

    private SortedMap<Long, Integer> uidToMsn;

    private long highestUid = 0;

    private int highestMsn = 0;

    private boolean closed = false;

    public UidToMsnConverter(final Iterator<Long> uids) {
        msnToUid = new TreeMap<Integer, Long>();
        uidToMsn = new TreeMap<Long, Integer>();
        if (uids != null) {
            int msn = 1;
            while (uids.hasNext()) {
                final Long uid = uids.next();
                highestUid = uid.longValue();
                highestMsn = msn;
                msnToUid.put(msn, uid);
                uidToMsn.put(uid, msn);
                
                msn++;
            }

        }
    }

    /**
     * @see SelectedMailbox#uid(int)
     * 
     */
    public synchronized long getUid(int msn) {
        if (msn == -1) {
            return SelectedMailbox.NO_SUCH_MESSAGE;
        }
        Long uid = msnToUid.get(msn);
        if (uid != null) {
            return uid.longValue();
        } else {
            return SelectedMailbox.NO_SUCH_MESSAGE;
        }
    }

    /**
     * @see SelectedMailbox#msn(int)
     */
    public synchronized int getMsn(long uid) {
        Integer msn = uidToMsn.get(uid);
        if (msn != null) {
            return msn.intValue();
        } else {
            return SelectedMailbox.NO_SUCH_MESSAGE;
        }

    }

    private void add(int msn, long uid) {
        if (uid > highestUid) {
            highestUid = uid;
        }
        msnToUid.put(msn, uid);
        uidToMsn.put(uid, msn);
    }

    /**
     * Expunge the message with the given uid
     * 
     * @param uid
     */
    public synchronized void expunge(final long uid) {
        final int msn = getMsn(uid);
        remove(msn, uid);
        final List<Integer> renumberMsns = new ArrayList<Integer>(msnToUid
                .tailMap(new Integer(msn + 1)).keySet());
        for (final Integer msnInteger: renumberMsns) {
            int aMsn = msnInteger.intValue();
            long aUid = getUid(aMsn);
            remove(aMsn, aUid);
            add(aMsn - 1, aUid);
        }
        highestMsn--;
    }

    private void remove(int msn, long uid) {
        uidToMsn.remove(uid);
        msnToUid.remove(msn);
    }

    /**
     * Add the give uid
     * 
     * @param uid
     */
    public synchronized void add(long uid) {
        if (!uidToMsn.containsKey(new Long(uid))) {
            highestMsn++;
            add(highestMsn, uid);
        }
    }

    /**
     * @see org.apache.james.mailbox.MailboxListener#event(org.apache.james.mailbox.MailboxListener.Event)
     */
    public synchronized void event(Event event) {
        if (event instanceof MessageEvent) {
            final MessageEvent messageEvent = (MessageEvent) event;
            final long uid = messageEvent.getSubjectUid();
            if (event instanceof Added) {
                add(uid);
            }
        }
    }

    
    /**
     * @see SelectedMailbox#getFirstUid()
     */
    public synchronized long getFirstUid() {
        if (uidToMsn.isEmpty()) {
            return -1;
        } else {
            return uidToMsn.firstKey();
        }
    }
    
    
    /**
     * @see SelectedMailbox#getLastUid()
     */
    public synchronized long getLastUid() {
        if (uidToMsn.isEmpty()) {
            return -1;
        } else {
            return uidToMsn.lastKey();
        }
    }
    /**
     * Close this {@link MailboxListener} and dispose all stored stuff 
     */
    public synchronized void close() {
        uidToMsn.clear();
        uidToMsn = null;
        msnToUid.clear();
        msnToUid = null;
        closed = true;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxListener#isClosed()
     */
    public synchronized boolean isClosed() {
        return closed;
    }
}
