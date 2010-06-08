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
import javax.persistence.EntityManagerFactory;

import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.jpa.mail.JPAMessageMapper;
import org.apache.james.imap.jpa.user.JPASubscriptionMapper;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.MailboxSessionMapperFactory;
import org.apache.james.imap.store.StoreConstants;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.user.SubscriptionMapper;

/**
 * JPA implementation of {@link MailboxSessionMapperFactory}
 *
 */
public class JPAMailboxSessionMapperFactory extends MailboxSessionMapperFactory<Long> implements StoreConstants{

    private final EntityManagerFactory entityManagerFactory;
    private final char delimiter;

    public JPAMailboxSessionMapperFactory(EntityManagerFactory entityManagerFactory) {
        this(entityManagerFactory, DEFAULT_FOLDER_DELIMITER);
    }

    public JPAMailboxSessionMapperFactory(EntityManagerFactory entityManagerFactory, char delimiter) {
        this.entityManagerFactory = entityManagerFactory;
        this.delimiter = delimiter;
    }
    
    @Override
    public MailboxMapper<Long> createMailboxMapper(MailboxSession session) {
        return new JPAMailboxMapper(entityManagerFactory, delimiter);
    }

    @Override
    public MessageMapper<Long> createMessageMapper(MailboxSession session) {
        return new JPAMessageMapper(entityManagerFactory);
    }

    @Override
    public SubscriptionMapper createSubscriptionMapper(MailboxSession session) {
        return new JPASubscriptionMapper(entityManagerFactory);
    }

    /**
     * Return a new {@link EntityManager} instance
     * 
     * @return manager
     */
    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

}
