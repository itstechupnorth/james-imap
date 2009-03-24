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

import javax.persistence.EntityManagerFactory;

import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.jpa.mail.map.openjpa.OpenJPAMailboxMapper;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;

public class JPAMailboxManager extends StoreMailboxManager {

    private final EntityManagerFactory entityManagerFactory;

    public JPAMailboxManager(final Authenticator authenticator, final Subscriber subscriber, 
            final EntityManagerFactory entityManagerFactory) {
        super(authenticator, subscriber);
        this.entityManagerFactory = entityManagerFactory;
    }
    
    @Override
    protected MailboxMapper createMailboxMapper() {
        final JPAMailboxMapper mapper = new OpenJPAMailboxMapper(entityManagerFactory.createEntityManager());
        return mapper;
    }

    @Override
    protected StoreMailbox createMailbox(Mailbox mailboxRow) {
        StoreMailbox result;
        result = new JPAMailbox(mailboxRow, entityManagerFactory);
        return result;
    }
    
    @Override
    protected void doCreate(String namespaceName) throws MailboxException {
        Mailbox mailbox = new org.apache.james.imap.jpa.mail.model.JPAMailbox(namespaceName, Math.abs(random.nextInt()));
        final MailboxMapper mapper = createMailboxMapper();
        mapper.begin();
        mapper.save(mailbox);
        mapper.commit();
    }
}
