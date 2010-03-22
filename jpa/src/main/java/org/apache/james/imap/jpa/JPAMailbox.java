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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.jpa.mail.JPAMessageMapper;
import org.apache.james.imap.jpa.mail.model.JPAHeader;
import org.apache.james.imap.jpa.mail.model.JPAMailboxMembership;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

/**
 * Abstract base class which should be used from JPA implementations
 * 
 *
 */
public abstract class JPAMailbox extends StoreMailbox<Long> {

    protected final EntityManagerFactory entityManagerFactory;

    public JPAMailbox(final Mailbox<Long> mailbox, MailboxSession session, final EntityManagerFactory entityManagerfactory) {
        super(mailbox, session);
        this.entityManagerFactory = entityManagerfactory;
    }    

    /**
     * Create MailboxMapper 
     * 
     * @return mapper
     */
    protected abstract JPAMailboxMapper createMailboxMapper(MailboxSession session);

    @Override
    protected Mailbox<Long> getMailboxRow() throws MailboxException {
        final MailboxMapper<Long> mapper = createMailboxMapper(getMailboxSession());
        return mapper.findMailboxById(mailboxId);
    }

    
    @Override
    protected MessageMapper<Long> createMessageMapper(MailboxSession session) {
        EntityManager manager = entityManagerFactory.createEntityManager();
        
        JPAUtils.addEntityManager(session, manager);
        
        JPAMessageMapper mapper = new JPAMessageMapper(manager, mailboxId);
       
        return mapper;
    }
    
    @Override
    protected MailboxMembership<Long> createMessage(Date internalDate, final long uid, final int size, int bodyStartOctet, final byte[] document, 
            final Flags flags, final List<Header> headers, PropertyBuilder propertyBuilder) {
        final List<JPAHeader> jpaHeaders = new ArrayList<JPAHeader>(headers.size());
        for (Header header: headers) {
            jpaHeaders.add((JPAHeader) header);
        }
        final MailboxMembership<Long> message = new JPAMailboxMembership(mailboxId, uid, internalDate, 
                size, flags, document, bodyStartOctet, jpaHeaders, propertyBuilder);
        return message;
    }
    
    @Override
    protected MailboxMembership<Long> copyMessage(MailboxMembership<Long> originalMessage, long uid) {
        MailboxMembership<Long> newRow = new JPAMailboxMembership(getMailboxId(), uid, (JPAMailboxMembership) originalMessage);
        return newRow;
    }
    
    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        final Header header = new JPAHeader(lineNumber, name, value);
        return header;
    }

    @Override
    protected Mailbox<Long> reserveNextUid() throws MailboxException {
        final JPAMailboxMapper mapper = createMailboxMapper(getMailboxSession());
        final Mailbox<Long> mailbox = mapper.consumeNextUid(mailboxId);
        return mailbox;
    } 
}
