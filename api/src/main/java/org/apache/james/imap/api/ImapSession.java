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

package org.apache.james.imap.api;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.process.SelectedMailbox;

/**
 * Encapsulates all state held for an ongoing Imap session, which commences when
 * a client first establishes a connection to the Imap server, and continues
 * until that connection is closed.
 * 
 * @version $Revision: 109034 $
 */
public interface ImapSession {
    

    /**
     * Gets the context sensitive log for this session.
     * Understanding the context of a log message is an important
     * comprehension aid when analying multi-threaded systems.
     * Using this log allows context information to be associated.
     * @return context sensitive log, not null
     */
    Log getLog();

    /**
     * Logs out the session. Marks the connection for closure;
     */
    void logout();

    /**
     * Gets the current client state.
     * 
     * @return Returns the current state of this session.
     */
    ImapSessionState getState();

    /**
     * Moves the session into {@link ImapSessionState#AUTHENTICATED} state.
     */
    void authenticated();

    /**
     * Moves this session into {@link ImapSessionState#SELECTED} state and sets
     * the supplied mailbox to be the currently selected mailbox.
     * 
     * @param mailbox
     *            The selected mailbox.
     * @param readOnly
     *            If <code>true</code>, the selection is set to be read only.
     * @throws MailboxManagerException
     */
    void selected(SelectedMailbox mailbox);

    /**
     * Moves the session out of {@link ImapSessionState#SELECTED} state and back
     * into {@link ImapSessionState#AUTHENTICATED} state. The selected mailbox
     * is cleared.
     */
    void deselect();

    /**
     * Provides the selected mailbox for this session, or <code>null</code> if
     * this session is not in {@link ImapSessionState#SELECTED} state.
     * 
     * @return the currently selected mailbox.
     */
    SelectedMailbox getSelected();

    /**
     * Gets an attribute of this session by name. Implementations should ensure
     * that access is thread safe.
     * 
     * @param key
     *            name of the key, not null
     * @return <code>Object</code> value or null if this attribute has
     *         unvalued
     */
    public Object getAttribute(String key);

    /**
     * Sets an attribute of this session by name. Implementations should ensure
     * that access is thread safe.
     * 
     * @param key
     *            name of the key, not null
     * @param value
     *            <code>Object</code> value or null to set this attribute as
     *            unvalued
     */
    public void setAttribute(String key, Object value);
    
    /**
     * Start TLS encryption of the session after the next response was written. So you must make sure 
     * the next response will get send in clear text
     * 
     * @return true if the encryption of the session was successfully
     */
    public boolean startTLS();
    
    /**
     * Support startTLS ? 
     * 
     * @return true if startTLS is supported
     */
    public boolean supportStartTLS();
    
    
    public void pushImapLineHandler(ImapLineHandler handler);
    
    public void popImapLineHandler();
}
