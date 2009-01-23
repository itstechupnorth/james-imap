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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.james.imap.mailbox.Constants;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MailboxListener;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageResult;

public class UidChangeTracker implements Constants {

    private final MailboxEventDispatcher eventDispatcher;

    private final TreeMap<Long, Flags> cache;

    private long lastUidAtStart;

    private long lastUid;

    private long lastScannedUid = 0;

    public UidChangeTracker(long lastUid) {
        this.lastUidAtStart = lastUid;
        this.lastUid = lastUid;
        eventDispatcher = new MailboxEventDispatcher();
        cache = new TreeMap<Long, Flags>();
    }

    public synchronized void expunged(final Collection<Long> uidsExpunged) {
        for (Long uid:uidsExpunged) {
            cache.remove(uid);
            eventDispatcher.expunged(uid, 0);
        }
    }

    /**
     * Indicates that the flags on the given messages may have been updated.
     * 
     * @param messageFlags
     *            flags
     * @param sessionId
     *            id of the session upating the flags
     * @see #flagsUpdated(MessageResult, long)
     */
    public synchronized void flagsUpdated(SortedMap<Long,Flags> newFlagsByUid, Map<Long,Flags> originalFlagsByUid, long sessionId) {
        if (newFlagsByUid != null) {
            for(Map.Entry<Long, Flags> entry:newFlagsByUid.entrySet()) {
                final Long uid = entry.getKey();
                final Flags newFlags = entry.getValue();
                final Flags cachedFlags = cache.get(uid);
                final Flags lastFlags;
                if (cachedFlags == null) {
                    lastFlags = originalFlagsByUid.get(uid);
                } else {
                    lastFlags = cachedFlags;
                }
                if (!newFlags.equals(lastFlags)) {
                    eventDispatcher.flagsUpdated(uid, sessionId, lastFlags, newFlags);
                }
                cache.put(uid, newFlags);
            }
        }
    }

    /**
     * Indicates that the flags on the given messages may have been updated.
     * 
     * @param messageResults
     *            results
     * @param sessionId
     *            id of the session upating the flags
     * @throws MailboxException
     * @see #flagsUpdated(MessageResult, long)
     */
    public synchronized void flagsUpdated(Collection messageResults,
            long sessionId) throws MailboxException {
        if (messageResults != null) {
            for (final Iterator it = messageResults.iterator(); it.hasNext();) {
                final MessageResult result = (MessageResult) it.next();
                flagsUpdated(result, sessionId);
            }
        }
    }

    /**
     * Indicates that the flags on the given message may have been updated.
     * 
     * @param messageResult
     *            result of update
     * @param sessionId
     *            id of the session updating the flags
     * @throws MailboxException
     */
    public synchronized void flagsUpdated(MessageResult messageResult,
            long sessionId) throws MailboxException {
        if (messageResult != null) {
            final Flags flags = messageResult.getFlags();
            final long uid = messageResult.getUid();
            final Long uidLong = new Long(uid);
            updatedFlags(uid, flags, uidLong, sessionId);
        }
    }

    public synchronized void found(UidRange range,
            final Collection<MessageResult> messageResults) throws MessagingException {
        final Map<Long, Flags> flagsByIndex = new HashMap<Long, Flags>();
        for (MessageResult result: messageResults) {
            flagsByIndex.put(result.getUid(), result.getFlags());
        }
        found(range, flagsByIndex);
    }

    public synchronized void found(UidRange range,
            final Map<Long, Flags> flagsByIndex) {
        Set<Long> expectedSet = getSubSet(range);
        for (Map.Entry<Long, Flags> entry:flagsByIndex.entrySet()) {
            long uid = entry.getKey();
            if (uid > lastScannedUid) {
                lastScannedUid = uid;
            }
            final Flags flags = entry.getValue();
            final Long uidLong = new Long(uid);
            if (expectedSet.contains(uidLong)) {
                expectedSet.remove(uidLong);
                updatedFlags(uid, flags, uidLong, Mailbox.ANONYMOUS_SESSION);
            } else {
                cache.put(uidLong, flags);
                if (uid > lastUidAtStart) {
                    eventDispatcher.added(uid, Mailbox.ANONYMOUS_SESSION);
                }
            }
        }

        if (lastScannedUid > lastUid) {
            lastUid = lastScannedUid;
        }
        if (range.getToUid() == UID_INFINITY || range.getToUid() >= lastUid) {
            lastScannedUid = lastUid;
        } else if (range.getToUid() != UID_INFINITY
                && range.getToUid() < lastUid
                && range.getToUid() > lastScannedUid) {
            lastScannedUid = range.getToUid();
        }

        for (Iterator iter = expectedSet.iterator(); iter.hasNext();) {
            long uid = ((Long) iter.next()).longValue();
            eventDispatcher.expunged(uid, Mailbox.ANONYMOUS_SESSION);
        }
    }

    private void updatedFlags(final long uid, final Flags flags,
            final Long uidLong, final long sessionId) {
        if (flags != null) {
            Flags cachedFlags = cache.get(uidLong);
            if (cachedFlags == null || !flags.equals(cachedFlags)) {
                eventDispatcher.flagsUpdated(uid, sessionId, cachedFlags, flags);
                cache.put(uidLong, flags);
            }
        }
    }

    private SortedSet<Long> getSubSet(UidRange range) {
        final Long rangeStartLong = new Long(range.getFromUid());
        if (range.getToUid() > 0) {
            final long nextUidAfterRange = range.getToUid() + 1;
            final Long nextUidAfterRangeLong = new Long(nextUidAfterRange);
            final SortedMap<Long, Flags> subMap = cache.subMap(rangeStartLong,
                    nextUidAfterRangeLong);
            final Set<Long> keySet = subMap.keySet();
            return new TreeSet<Long>(keySet);
        } else {
            return new TreeSet<Long>(cache.tailMap(rangeStartLong).keySet());
        }

    }

    public synchronized void found(MessageResult messageResult)
            throws MessagingException {
        if (messageResult != null) {
            long uid = messageResult.getUid();
            Collection<MessageResult> results = new ArrayList<MessageResult>();
            results.add(messageResult);
            found(new UidRange(uid, uid), results);
        }
    }

    public synchronized long getLastUid() {
        return lastUid;
    }

    public synchronized void foundLastUid(long foundLastUid) {
        if (foundLastUid > lastUid) {
            lastUid = foundLastUid;
        }
    }

    public synchronized long getLastScannedUid() {
        return lastScannedUid;
    }

    public synchronized void addMailboxListener(MailboxListener listener) {
        eventDispatcher.addMailboxListener(listener);
    }
    
    public synchronized void mailboxDeleted(long sessionId) {
        eventDispatcher.mailboxDeleted(sessionId);
    }

    public void reportRenamed(String to) {
        eventDispatcher.mailboxRenamed(to, Mailbox.ANONYMOUS_SESSION);
    }

}
