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
import javax.persistence.EntityManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.jpa.mail.JPAMessageMapper;
import org.apache.james.imap.jpa.mail.map.openjpa.OpenJPAMailboxMapper;
import org.apache.james.imap.jpa.mail.model.JPAHeader;
import org.apache.james.imap.jpa.mail.model.JPAMessage;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.Message;

public class JPAMailbox extends StoreMailbox {

    private final EntityManagerFactory entityManagerFactory;

    public JPAMailbox(final Mailbox mailbox, final Log log, final EntityManagerFactory entityManagerfactory) {
        super(mailbox, log);
        this.entityManagerFactory = entityManagerfactory;
    }    

    @Override
    protected Mailbox getMailboxRow() throws MailboxException {
        final MailboxMapper mapper = createMailboxMapper();
        return mapper.findMailboxById(mailboxId);
    }

    @Override
    protected MailboxMapper createMailboxMapper() {
        final JPAMailboxMapper mapper = new OpenJPAMailboxMapper(entityManagerFactory.createEntityManager());
        return mapper;
    }
    
    @Override
    protected MessageMapper createMessageMapper() {
        final MessageMapper mapper = new JPAMessageMapper(entityManagerFactory.createEntityManager());
        return mapper;
    }
    
    @Override
    protected Message createMessage(Date internalDate, final long uid, final int size, final byte[] body, final Flags flags, final List<Header> headers) {
        final List<JPAHeader> jpaHeaders = new ArrayList<JPAHeader>(headers.size());
        for (Header header: headers) {
            jpaHeaders.add((JPAHeader) header);
        }
        final Message message = new JPAMessage(mailboxId, uid, internalDate, size, flags, body, jpaHeaders);
        return message;
    }
    
    @Override
    protected Message copyMessage(StoreMailbox toMailbox, Message originalMessage, long uid) {
        Message newRow = new JPAMessage(toMailbox.getMailboxId(), uid, (JPAMessage) originalMessage);
        return newRow;
    }
    
    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        final Header header = new JPAHeader(lineNumber, name, value);
        return header;
    }    
}
