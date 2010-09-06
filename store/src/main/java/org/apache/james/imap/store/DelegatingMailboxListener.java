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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;

/**
 * Receive a {@link Event} and delegate it to an other {@link MailboxListener} depending on the registered name
 *
 */
public class DelegatingMailboxListener implements MailboxListener{

    private Map<MailboxPath, List<MailboxListener>> listeners = new HashMap<MailboxPath, List<MailboxListener>>();

    public synchronized void addListener(MailboxPath path, MailboxListener listener) {
        List<MailboxListener> mListeners = listeners.get(path);
        if (mListeners == null) {
            mListeners = new ArrayList<MailboxListener>();
            listeners.put(path, mListeners);
        }
        if (mListeners.contains(listener) == false) {
            mListeners.add(listener);
        }
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxListener#event(org.apache.james.mailbox.MailboxListener.Event)
     */
    public void event(Event event) {
        List<MailboxListener> mListeners = listeners.get(event.getMailboxPath());
        if (mListeners != null && mListeners.isEmpty() == false) {
            for (int i = 0; i < mListeners.size(); i++) {
                MailboxListener l = mListeners.get(i);
                if (l.isClosed()) {
                    mListeners.remove(l);
                } else {
                    l.event(event);
                }
            }
        }
    }

    /**
     * Is never closed
     */
    public boolean isClosed() {
        return false;
    }
    
}
