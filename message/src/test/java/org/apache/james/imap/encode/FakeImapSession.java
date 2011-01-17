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
package org.apache.james.imap.encode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.imap.api.ImapLineHandler;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.process.SelectedMailbox;

public class FakeImapSession implements ImapSession {
    
    private static final Log LOG = LogFactory.getLog(FakeImapSession.class);
    
    private ImapSessionState state = ImapSessionState.NON_AUTHENTICATED;

    private SelectedMailbox selectedMailbox = null;

    private final Map<String, Object> attributesByKey;

    public FakeImapSession() {
        this.attributesByKey = new ConcurrentHashMap<String, Object>();
    }

    public void logout() {
        closeMailbox();
        state = ImapSessionState.LOGOUT;
    }

    public void authenticated() {
        this.state = ImapSessionState.AUTHENTICATED;
    }

    public void deselect() {
        this.state = ImapSessionState.AUTHENTICATED;
        closeMailbox();
    }

    public void selected(SelectedMailbox mailbox) {
        this.state = ImapSessionState.SELECTED;
        closeMailbox();
        this.selectedMailbox = mailbox;
    }

    public SelectedMailbox getSelected() {
        return this.selectedMailbox;
    }

    public ImapSessionState getState() {
        return this.state;
    }

    public void closeMailbox() {
        if (selectedMailbox != null) {
            selectedMailbox.deselect();
            selectedMailbox = null;
        }
    }

    public Object getAttribute(String key) {
        final Object result = attributesByKey.get(key);
        return result;
    }

    public void setAttribute(String key, Object value) {
        if (value == null) {
            attributesByKey.remove(key);
        } else {
            attributesByKey.put(key, value);
        }
    }

    public Log getLog() {
        return LOG;
    }

    
    public boolean startTLS() {
        return false;
    }

    public boolean supportStartTLS() {
        return false;
    }
    
    public void pushImapLineHandler(ImapLineHandler handler) {
        
    }

    public void popImapLineHandler() {
        
    }

}
