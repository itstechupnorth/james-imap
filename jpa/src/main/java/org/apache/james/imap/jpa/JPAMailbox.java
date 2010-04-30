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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.persistence.EntityManagerFactory;

import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.jpa.mail.JPAMessageMapper;
import org.apache.james.imap.jpa.mail.model.AbstractJPAMailboxMembership;
import org.apache.james.imap.jpa.mail.model.JPAHeader;
import org.apache.james.imap.jpa.mail.model.JPAMailboxMembership;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
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
    
    public JPAMailbox(final MailboxEventDispatcher dispatcher, final Mailbox<Long> mailbox, final EntityManagerFactory entityManagerfactory) {
        super(dispatcher, mailbox);
        this.entityManagerFactory = entityManagerfactory;        
    }  
    
    /**
     * Create MailboxMapper 
     * 
     * @return mapper
     */
    protected abstract JPAMailboxMapper createMailboxMapper(MailboxSession session);

    @Override
    protected Mailbox<Long> getMailboxRow(MailboxSession session) throws MailboxException {
        final MailboxMapper<Long> mapper = createMailboxMapper(session);
        return mapper.findMailboxById(getMailboxId());
    }

    
    @Override
    protected MessageMapper<Long> createMessageMapper(MailboxSession session) {                
        JPAMessageMapper mapper = new JPAMessageMapper(entityManagerFactory, getMailboxId());
       
        return mapper;
    }
    
    @Override
    protected MailboxMembership<Long> createMessage(Date internalDate, final long uid, final int size, int bodyStartOctet, final InputStream document, 
            final Flags flags, final List<Header> headers, PropertyBuilder propertyBuilder) throws MailboxException{
        final List<JPAHeader> jpaHeaders = new ArrayList<JPAHeader>(headers.size());
        for (Header header: headers) {
            jpaHeaders.add((JPAHeader) header);
        }
        final MailboxMembership<Long> message = new JPAMailboxMembership(getMailboxId(), uid, internalDate, size, flags, document, bodyStartOctet, jpaHeaders, propertyBuilder);
        return message;

       
    }
    
    @Override
    protected MailboxMembership<Long> copyMessage(MailboxMembership<Long> originalMessage, long uid, MailboxSession session) throws MailboxException{
        final MailboxMembership<Long> newRow = new JPAMailboxMembership(getMailboxId(), uid, (AbstractJPAMailboxMembership) originalMessage);
        return newRow;
    }
    
    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        final Header header = new JPAHeader(lineNumber, name, value);
        return header;
    }
}
