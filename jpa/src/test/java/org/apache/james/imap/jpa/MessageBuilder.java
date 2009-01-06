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
package org.apache.james.imap.jpa;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.jpa.mail.model.JPAHeader;
import org.apache.james.imap.jpa.mail.model.JPAMessage;

public class MessageBuilder {
    
    public long mailboxId = 113;
    public long uid = 776;
    public Date internalDate = new Date();
    public int size = 8867;
    public Flags flags = new Flags();
    public byte[] body = {};
    public final List<JPAHeader> headers = new ArrayList<JPAHeader>();
    public int lineNumber = 0;
    
    public JPAMessage build() {
        JPAMessage result = new JPAMessage(mailboxId, uid, internalDate, size, flags, body, headers);
        return result;
    }
    
    public JPAHeader header(String field, String value) {
        JPAHeader header = new JPAHeader(++lineNumber, field, value);
        headers.add(header);
        return header;
    }

    public void setKey(int mailboxId, int uid) {
        this.uid = uid;
        this.mailboxId = mailboxId;
    }
    
    public void setFlags(boolean seen, boolean flagged, boolean answered,
            boolean draft, boolean deleted, boolean recent) {
        if (seen) flags.add(Flags.Flag.SEEN);
        if (flagged) flags.add(Flags.Flag.FLAGGED);
        if (answered) flags.add(Flags.Flag.ANSWERED);
        if (draft) flags.add(Flags.Flag.DRAFT);
        if (deleted) flags.add(Flags.Flag.DELETED);
        if (recent) flags.add(Flags.Flag.RECENT);
    }
}
