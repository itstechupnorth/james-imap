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

import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;

/**
 * Mailbox session which holds a PasswordAwareUser
 * 
 *
 */
// TODO: Why does a session implement a user? This is not needed, I think.
public class PasswordAwareMailboxSession extends SimpleMailboxSession implements
        PasswordAwareUser {

    private final String password;

    public PasswordAwareMailboxSession(long sessionId, String userName,
            String password, Log log, char deliminator,
            List<Locale> localePreferences) {
        super(sessionId, userName, log, deliminator, localePreferences);
        this.password = password;
    }

    /**
     * Return the User which is bound the the MailboxSession. This User is in
     * fact an instance of PasswordAwareUser.
     * 
     * return user
     */
    public User getUser() {
        return this;
    }

    /**
     * Return the Password for the logged in user
     * 
     * @return password
     */
    public String getPassword() {
        return password;
    }
}
