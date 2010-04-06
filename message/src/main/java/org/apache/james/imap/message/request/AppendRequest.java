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
package org.apache.james.imap.message.request;

import java.util.Date;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.decode.base.EolInputStream;

public class AppendRequest extends AbstractImapRequest {
    private final String mailboxName;

    private final Flags flags;

    private final Date datetime;

    private final EolInputStream message;

    public AppendRequest(ImapCommand command, String mailboxName, Flags flags,
            Date datetime, EolInputStream message, String tag) {
        super(tag, command);
        this.mailboxName = mailboxName;
        this.flags = flags;
        this.datetime = datetime;
        this.message = message;
    }

    public Date getDatetime() {
        return datetime;
    }

    public Flags getFlags() {
        return flags;
    }

    public String getMailboxName() {
        return mailboxName;
    }

    public EolInputStream getMessage() {
        return message;
    }
    
    
}
