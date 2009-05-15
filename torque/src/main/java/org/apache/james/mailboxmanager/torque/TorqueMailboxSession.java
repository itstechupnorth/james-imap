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

package org.apache.james.mailboxmanager.torque;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.util.SimpleMailboxNamespace;

/**
 * Describes a mailbox session.
 */
public class TorqueMailboxSession implements MailboxSession, MailboxSession.User {

    private final Collection<Namespace> sharedSpaces;

    private final Namespace otherUsersSpace;

    private final Namespace personalSpace;
    
    private final long sessionId;
    
    private final Log log;

    private final String userName;
    
    private boolean open;
    
    private final List<Locale> localePreferences;

    public TorqueMailboxSession(final long sessionId, final Log log, final String userName, char deliminator,
            final List<Locale> localePreferences) {
        super();
        this.sessionId = sessionId;
        this.log = log;
        this.userName = userName;
        sharedSpaces = new ArrayList<Namespace>();
        otherUsersSpace = null;
        personalSpace = new SimpleMailboxNamespace(deliminator, "");
        this.localePreferences = localePreferences;
    }
    
    
    public Log getLog() {
        return log;
    }



    public void close() {
        open = false;
    }

    public long getSessionId() {
        return sessionId;
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Renders suitably for logging.
     * 
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        final String TAB = " ";

        String retValue = "TorqueMailboxSession ( " + "sessionId = "
                + this.sessionId + TAB + "open = " + this.open + TAB + " )";

        return retValue;
    }

    /**
     * Gets the user executing this session.
     * @return not null
     */
    public User getUser() {
        return this;
    }

    /**
     * Gets the name of the user executing this session.
     * @return not null
     */
	public String getUserName() {
		return userName;
	}
    
    /**
     * @see org.apache.james.imap.mailbox.MailboxSession#getOtherUsersSpace()
     */
    public Namespace getOtherUsersSpace() {
        return otherUsersSpace;
    }

    /**
     * @see org.apache.james.imap.mailbox.MailboxSession#getPersonalSpace()
     */
    public Namespace getPersonalSpace() {
        return personalSpace;
    }

    /**
     * @see org.apache.james.imap.mailbox.MailboxSession#getSharedSpaces()
     */
    public Collection<Namespace> getSharedSpaces() {
        return sharedSpaces;
    }

    /**
     * @see org.apache.james.imap.mailbox.MailboxSession.User#getLocalePreferences()
     */
    public List<Locale> getLocalePreferences() {
        return localePreferences;
    }
}
