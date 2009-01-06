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

package org.apache.james.imap.mailbox;

/**
 * Indicates that the failure is caused by a reference to a mailbox which does
 * not exist.
 */
public class MailboxNotFoundException extends MailboxException {

    private static final long serialVersionUID = -8493370806722264915L;

    private static String message(String mailboxName) {
        final String result;
        if (mailboxName == null) {
            result = "Mailbox not found";
        } else {
            result = "Mailbox '" + mailboxName + "' not found.";
        }
        return result;
    }

    private final String mailboxName;

    private final long id;
    
    public MailboxNotFoundException(long id) {
        super(message(Long.toString(id)));
        this.id = id;
        mailboxName = null;
    }
    
    /**
     * 
     * @param mailboxName
     *            name of the mailbox, not null
     */
    public MailboxNotFoundException(String mailboxName) {
        super(message(mailboxName));
        this.mailboxName = mailboxName;
        this.id = 0;
    }

    /**
     * Gets the name of the mailbox which cannot be found.
     * 
     * @return name or null when only mailbox ID is known
     */
    public final String getMailboxName() {
        return mailboxName;
    }

    /**
     * Gets the storage id of the mailbox.
     * @return storage id, or zero when this is not known
     */
    public long getId() {
        return id;
    }
}
