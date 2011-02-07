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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.SearchQuery;

/**
 * {@link UidToMsnConverter} takes care of maintaining a mapping between message uids and msn (index) by register a {@link MailboxListener} for a {@link MailboxPath}
 * 
 * See {@link UidToMsnConverter} is shared across different {@link MailboxSession}'s for the same {@link MailboxPath} to reduce memory usage. See IMAP-255
 * 
 *
 */
public class UidToMsnConverter {
    // Hold all cached UidToMsnConverter
    private final static Map<MailboxPath, UidToMsnConverter> converters = new HashMap<MailboxPath, UidToMsnConverter>();
    
    private SortedMap<Integer, Long> msnToUid;

    private SortedMap<Long, Integer> uidToMsn;

    private long highestUid = 0;

    private int highestMsn = 0;

    // hold the reference count for a shared UidToMsnConverter
    private AtomicInteger references = new AtomicInteger(0);
    
    /**
     * Return a instance of the {@link UidToMsnConverter} which can be a new one or a shared if there is already a {@link UidToMsnConverter} for the given
     * {@link MailboxPath}.
     * 
     * @param manager
     * @param path
     * @param session
     * @return converter
     * @throws MailboxException
     */
    public static UidToMsnConverter get(MailboxManager manager, MailboxPath path, MailboxSession session) throws MailboxException {
        boolean found = false;
        UidToMsnConverter converter = null;
        
        // see if we have a converter for the path
        synchronized (converters) {
            converter = converters.get(path);
            if (converter == null) {
                converter = new UidToMsnConverter();
                converters.put(path, converter);
            } else {
                found = true;
            }
        }
        
        // now be sure we only return the converter if the one which exists is init. So to be sure we synchronize on it
        synchronized (converter) {
            final UidToMsnConverter c = converter;
            // the converter was not found before so we need to init it to get a list of all uids
            if (!found) {
                converter.init(manager.getMailbox(path, session), session);
                manager.addListener(path, new MailboxListener() {
                    
                    /*
                     * (non-Javadoc)
                     * @see org.apache.james.mailbox.MailboxListener#isClosed()
                     */
                    public synchronized boolean isClosed() {
                        return c.references.get() < 1;
                    }
                    
                    /*
                     * (non-Javadoc)
                     * @see org.apache.james.mailbox.MailboxListener#event(org.apache.james.mailbox.MailboxListener.Event)
                     */
                    public void event(Event event) {
                        if (event instanceof MessageEvent) {
                            final MessageEvent messageEvent = (MessageEvent) event;
                            final long uid = messageEvent.getSubjectUid();
                            if (event instanceof Added) {
                                c.add(uid);
                            }

                        }                    
                    }
                }, session);
            }
            
            // increment the reference count so can handle the close operations in the right manner
            converter.references.incrementAndGet();
            return converter;
        }
        
    }
    
    // Should only get accessed throw the static factory method
    private UidToMsnConverter() {
    }

    private void init(MessageManager mailbox, MailboxSession mailboxSession) throws MailboxException {
        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.all());

        // use search here to allow implementation a better way to improve
        // selects on mailboxes.
        // See https://issues.apache.org/jira/browse/IMAP-192
        final Iterator<Long> uids = mailbox.search(query, mailboxSession);

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
                .tailMap(msn + 1).keySet());
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
        if (!uidToMsn.containsKey(uid)) {
            highestMsn++;
            add(highestMsn, uid);
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
        if (references.incrementAndGet() == 0) {
            uidToMsn.clear();
            msnToUid.clear();
        }
    }
    

}
