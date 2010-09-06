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

import org.apache.james.mailbox.MailboxListener;

//TODO: This is a major memory hog
//TODO: Each concurrent session requires one, and typical clients now open many
public class UidToMsnConverter implements MailboxListener {
    private final SortedMap<Integer, Long> msnToUid;

    private final SortedMap<Long, Integer> uidToMsn;

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

    public long getUid(int msn) {
        if (msn == -1) {
            return -1;
        }
        Long uid = msnToUid.get(msn);
        if (uid != null) {
            return uid.longValue();
        } else {
            if (msn > 0) {
                return highestUid;
            } else {
                return 0;
            }
        }
    }

    public int getMsn(long uid) {
        Integer msn = uidToMsn.get(uid);
        if (msn != null) {
            return msn.intValue();
        } else {
            return -1;
        }

    }

    private void add(int msn, long uid) {
        if (uid > highestUid) {
            highestUid = uid;
        }
        msnToUid.put(msn, uid);
        uidToMsn.put(uid, msn);
    }

    public void expunge(final long uid) {
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

    public void add(long uid) {
        if (!uidToMsn.containsKey(new Long(uid))) {
            highestMsn++;
            add(highestMsn, uid);
        }
    }

    /**
     * @see org.apache.james.mailbox.MailboxListener#event(org.apache.james.mailbox.MailboxListener.Event)
     */
    public void event(Event event) {
        if (event instanceof MessageEvent) {
            final MessageEvent messageEvent = (MessageEvent) event;
            final long uid = messageEvent.getSubjectUid();
            if (event instanceof Added) {
                add(uid);
            }
        }
    }

    public void close() {
        closed = true;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxListener#isClosed()
     */
    public boolean isClosed() {
        return closed;
    }
}
