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
import javax.persistence.EntityManagerFactory;

import org.apache.james.imap.jpa.JPAMailbox;
import org.apache.james.imap.jpa.JPAUtils;
import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.jpa.mail.model.AbstractJPAMailboxMembership;
import org.apache.james.imap.jpa.mail.model.JPAHeader;
import org.apache.james.imap.jpa.mail.model.openjpa.JPAStreamingMailboxMembership;
import org.apache.james.imap.jpa.mail.openjpa.OpenJPAMailboxMapper;
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

    public final static String MAILBOX_MAPPER = "MAILBOX_MAPPER";
    private final boolean useStreaming;
    public OpenJPAMailbox(MailboxEventDispatcher dispatcher, Mailbox<Long> mailbox,  EntityManagerFactory entityManagerfactory) {
		this(dispatcher, mailbox, entityManagerfactory, false);
	}

    public OpenJPAMailbox(MailboxEventDispatcher dispatcher, Mailbox<Long> mailbox, EntityManagerFactory entityManagerfactory, final boolean useStreaming) {
        super(dispatcher, mailbox, entityManagerfactory);
        this.useStreaming = useStreaming;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.jpa.JPAMailbox#createMailboxMapper(org.apache.james.imap.mailbox.MailboxSession)
     */
	protected JPAMailboxMapper createMailboxMapper(MailboxSession session) {
	    EntityManager manager = entityManagerFactory.createEntityManager();
	    
	    JPAUtils.addEntityManager(session, manager);
	    
        JPAMailboxMapper mapper = new OpenJPAMailboxMapper(manager);

        return mapper;
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

}
