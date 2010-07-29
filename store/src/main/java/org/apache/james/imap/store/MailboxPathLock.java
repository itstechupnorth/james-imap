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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.james.imap.api.MailboxPath;

/**
 * 
 * Helper class which helps to synchronize the access the 
 * same MailboxPath. This is done using one {@link ReentrantLock}
 * per {@link MailboxPath}.
 * 
 * Its very important to call the {@link #unlock(MailboxPath)} method
 * in a finally block to not risk a dead lock
 *
 */
public final class MailboxPathLock {

    private final Map<MailboxPath, ReentrantLock> paths = new HashMap<MailboxPath, ReentrantLock>();
    
    /**
     * Obtain a {@link Lock} for the given path
     * 
     * @param path
     */
    public void lock(MailboxPath path) {
        ReentrantLock lock;
        synchronized (paths) {
            lock = paths.get(path);

            if (lock == null) {
                lock = new ReentrantLock();
                paths.put(path, lock);
            }
        }
        lock.lock();
    }
    
    /**
     * Unlock the previous obtained {@link Lock} for the given path
     * 
     * @param path
     */
    public void unlock(MailboxPath path) {
        synchronized (paths) {
            ReentrantLock lock = paths.remove(path);
            if (lock != null) {
                lock.unlock();
            }
        }
    }
}
