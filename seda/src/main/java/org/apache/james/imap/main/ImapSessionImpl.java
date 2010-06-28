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

package org.apache.james.imap.main;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;

/**
 * Implements a session.
 */
public class ImapSessionImpl implements ImapSession, ImapConstants {
    private static final Log IMAP_LOG = LogFactory.getLog("org.apache.james.imap");

    private Log log = IMAP_LOG;
    
    private ImapSessionState state = ImapSessionState.NON_AUTHENTICATED;

    private SelectedMailbox selectedMailbox = null;
    
    private final Map<String, Object> attributesByKey;

    public ImapSessionImpl() {
        this.attributesByKey = new ConcurrentHashMap<String, Object>();    
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.process.ImapSession#getLog()
     */
    public Log getLog() {
        return log;
    }
    
    /*
     * 
     */
    public void setLog(Log log) {
        this.log = log;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.process.ImapSession#logout()
     */
    public void logout() {
        closeMailbox();
        state = ImapSessionState.LOGOUT;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.process.ImapSession#authenticated()
     */
    public void authenticated() {
        this.state = ImapSessionState.AUTHENTICATED;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.process.ImapSession#deselect()
     */
    public void deselect() {
        this.state = ImapSessionState.AUTHENTICATED;
        closeMailbox();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.process.ImapSession#selected(org.apache.james.imap.api.process.SelectedMailbox)
     */
    public void selected(SelectedMailbox mailbox) {
        this.state = ImapSessionState.SELECTED;
        closeMailbox();
        this.selectedMailbox = mailbox;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.process.ImapSession#getSelected()
     */
    public SelectedMailbox getSelected() {
        return this.selectedMailbox;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.process.ImapSession#getState()
     */
    public ImapSessionState getState() {
        return this.state;
    }

    public void closeMailbox() {
        if (selectedMailbox != null) {
            selectedMailbox.deselect();
            selectedMailbox = null;
        }

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.process.ImapSession#getAttribute(java.lang.String)
     */
    public Object getAttribute(String key) {
        final Object result = attributesByKey.get(key);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.process.ImapSession#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String key, Object value) {
        if (value == null) {
            attributesByKey.remove(key);
        } else {
            attributesByKey.put(key, value);
        }
    }


    /**
     * StartTLS is not supported by this implementation
     * 
     * return false
     */
    public boolean startTLS() {
        return false;
    }
}
