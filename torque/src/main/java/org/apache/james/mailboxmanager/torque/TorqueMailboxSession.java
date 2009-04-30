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

import org.apache.commons.logging.Log;
import org.apache.james.imap.mailbox.MailboxSession;

/**
 * Describes a mailbox session.
 */
public class TorqueMailboxSession implements MailboxSession {

    private final long sessionId;
    
    private final Log log;

    private final String userName;
    
    private boolean open;


    public TorqueMailboxSession(final long sessionId, final Log log, final String userName) {
        super();
        this.sessionId = sessionId;
        this.log = log;
        this.userName = userName;
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
     * Gets the name of the user executing this session.
     * @return not null
     */
	public String getUserName() {
		return userName;
	}

}
