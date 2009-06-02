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

package org.apache.james.imap.inmemory;

import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

public class InMemoryStoreMailbox extends StoreMailbox {

    public InMemoryStoreMailbox(Mailbox mailbox) {
        super(mailbox);
    }

    @Override
    protected MailboxMembership copyMessage(StoreMailbox toMailbox, MailboxMembership originalMessage, long uid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected MailboxMapper createMailboxMapper() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected MailboxMembership createMessage(Date internalDate, long uid, int size, int bodyStartOctet, byte[] document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected MessageMapper createMessageMapper() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Mailbox getMailboxRow() throws MailboxException {
        // TODO Auto-generated method stub
        return null;
    }


}
