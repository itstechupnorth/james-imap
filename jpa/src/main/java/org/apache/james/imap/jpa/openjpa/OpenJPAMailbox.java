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

package org.apache.james.imap.jpa.openjpa;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.james.imap.jpa.JPAMailbox;
import org.apache.james.imap.jpa.mail.model.AbstractJPAMailboxMembership;
import org.apache.james.imap.jpa.mail.model.JPAHeader;
import org.apache.james.imap.jpa.mail.model.openjpa.JPAStreamingMailboxMembership;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

/**
 * OpenJPA implementation of Mailbox
 *
 */
public class OpenJPAMailbox extends JPAMailbox{

    private final boolean useStreaming;
    public OpenJPAMailbox(MailboxEventDispatcher dispatcher, Mailbox<Long> mailbox,  EntityManager entityManager) {
		this(dispatcher, mailbox, entityManager, false);
	}

    public OpenJPAMailbox(MailboxEventDispatcher dispatcher, Mailbox<Long> mailbox, EntityManager entityManager, final boolean useStreaming) {
        super(dispatcher, mailbox, entityManager);
        this.useStreaming = useStreaming;
    }

    @Override
    protected MailboxMembership<Long> copyMessage(MailboxMembership<Long> originalMessage, long uid, MailboxSession session) throws MailboxException {
        if (useStreaming) {
            return  new JPAStreamingMailboxMembership(getMailboxId(), uid, (AbstractJPAMailboxMembership) originalMessage);
        } else {
            return super.copyMessage(originalMessage, uid, session);
        }
    }

    @Override
    protected MailboxMembership<Long> createMessage(Date internalDate, long uid, int size, int bodyStartOctet, InputStream document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) throws MailboxException {
        if (useStreaming) {
            final List<JPAHeader> jpaHeaders = new ArrayList<JPAHeader>(headers.size());
            for (Header header: headers) {
                jpaHeaders.add((JPAHeader) header);
            }
            return new JPAStreamingMailboxMembership(getMailboxId(), uid, internalDate, size, flags, document, bodyStartOctet, jpaHeaders, propertyBuilder);
        } else {
            return super.createMessage(internalDate, uid, size, bodyStartOctet, document, flags, headers, propertyBuilder);
        }
    }
    
    /**
     * Reserve next Uid in mailbox and return the mailbox. We use a transaction here to be sure we don't get any duplicates
     * 
     */
    protected Mailbox<Long> reserveNextUid(MailboxSession session) throws MailboxException {
        EntityTransaction transaction = manager.getTransaction();
        transaction.begin();
        Query query = manager.createNamedQuery("findMailboxById").setParameter("idParam", getMailboxId());
        org.apache.james.imap.jpa.mail.model.JPAMailbox mailbox = (org.apache.james.imap.jpa.mail.model.JPAMailbox) query.getSingleResult();
        mailbox.consumeUid();
        manager.persist(mailbox);
        manager.flush();
        transaction.commit();
        //oem.close();
        return mailbox;
     
    }

}
