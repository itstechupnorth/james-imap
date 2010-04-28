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


import javax.persistence.EntityManager;

import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.TransactionalMapper;


/**
 * JPA implementation of {@link StoreMailboxManager}
 * 
 *
 */
public abstract class JPAMailboxManager extends StoreMailboxManager<Long> {

    protected final MailboxSessionEntityManagerFactory entityManagerFactory;
    public JPAMailboxManager(final Authenticator authenticator, final Subscriber subscriber, 
            final MailboxSessionEntityManagerFactory entityManagerFactory) {
        super(authenticator, subscriber);
        this.entityManagerFactory = entityManagerFactory;
    }
    
    
    @Override
    protected void doCreate(String namespaceName, MailboxSession session) throws MailboxException {
        final Mailbox<Long> mailbox = new org.apache.james.imap.jpa.mail.model.JPAMailbox(namespaceName, randomUidValidity());
        final MailboxMapper<Long> mapper = createMailboxMapper(session);
        mapper.execute(new TransactionalMapper.Transaction(){

            public void run() throws MailboxException {
                mapper.save(mailbox);
            }
            
        });
    }

    public void deleteEverything(MailboxSession maibloxSession) throws MailboxException {
        final MailboxMapper<Long> mapper = createMailboxMapper(maibloxSession);
        mapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                mapper.deleteAll(); 
            }
            
        });
    }

    
    @Override
    public void endProcessingRequest(MailboxSession session) {
        // close the entityManager after each request so we are sure everything is flushed
        entityManagerFactory.closeEntityManager(session);
       
    }
    
    @Override
    protected MailboxMapper<Long> createMailboxMapper(MailboxSession session) {
        EntityManager manager = entityManagerFactory.getEntityManager(session);
        return new JPAMailboxMapper(manager);
    }

    
}
