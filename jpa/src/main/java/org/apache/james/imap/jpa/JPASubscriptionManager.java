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

import org.apache.james.imap.jpa.user.JPASubscriptionMapper;
import org.apache.james.imap.jpa.user.model.JPASubscription;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.StoreSubscriptionManager;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

public class JPASubscriptionManager extends StoreSubscriptionManager {
    private final EntityManagerFactory factory;
    public final static String MAPPER = "SUBSCRIPTION_MAPPER";
    
    public JPASubscriptionManager(final EntityManagerFactory factory) {
        super();
        this.factory = factory;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.StoreSubscriptionManager#createMapper(org.apache.james.imap.mailbox.MailboxSession)
     */
    protected SubscriptionMapper createMapper(MailboxSession session) {
        
        JPASubscriptionMapper  mapper = new JPASubscriptionMapper(factory.createEntityManager());
        
        return mapper;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.StoreSubscriptionManager#createSubscription(org.apache.james.imap.mailbox.MailboxSession, java.lang.String)
     */
    protected Subscription createSubscription(final MailboxSession session, final String mailbox) {
        final Subscription newSubscription = new JPASubscription(session.getUser().getUserName(), mailbox);
        return newSubscription;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.StoreSubscriptionManager#onLogout(org.apache.james.imap.mailbox.MailboxSession)
     */
    protected void onLogout(MailboxSession session) {
        
    }
}
