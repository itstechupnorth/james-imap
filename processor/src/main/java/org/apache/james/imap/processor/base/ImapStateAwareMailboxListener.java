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

import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.mailbox.MailboxListener;

/**
 * Abstract base class for {@link MailboxListener} which should be handled as closed if the {@link ImapSessionState} is {@link ImapSessionState#LOGOUT} is true
 * 
 * This class should be used by all IMAP specifc {@link MailboxListener} implementation!
 */
public abstract class ImapStateAwareMailboxListener implements MailboxListener {

    protected final ImapSession session;

    public ImapStateAwareMailboxListener(ImapSession session) {
        this.session = session;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxListener#isClosed()
     */
    public boolean isClosed() {
        if (ImapSessionState.LOGOUT.equals(session.getState()) || isListenerClosed()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return true if the listener should handled as closed and get unregistered 
     * 
     * @return closed
     */
    protected abstract boolean isListenerClosed();
}
