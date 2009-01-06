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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.apache.james.imap.jpa.user.JPASubscriptionMapper;
import org.apache.james.imap.jpa.user.model.Subscription;
import org.apache.james.imap.mailbox.SubscriptionException;

/**
 * Manages subscriptions.
 */
public class SubscriptionManager implements Subscriber {

    private static final int INITIAL_SIZE = 32;
    private final EntityManagerFactory factory;
    
    public SubscriptionManager(final EntityManagerFactory factory) {
        super();
        this.factory = factory;
    }

    public void subscribe(final String user, final String mailbox) throws SubscriptionException {
        try {
            final JPASubscriptionMapper mapper = new JPASubscriptionMapper(factory.createEntityManager());
            mapper.begin();
            
            final Subscription subscription = mapper.findFindMailboxSubscriptionForUser(user, mailbox);
            if (subscription == null) {
                final Subscription newSubscription = new Subscription(user, mailbox);
                mapper.save(newSubscription);
                mapper.commit();
            }
        } catch (PersistenceException e) {
            throw new SubscriptionException(e);
        }
    }

    public Collection<String> subscriptions(final String user) throws SubscriptionException {
        try {
            final JPASubscriptionMapper mapper = new JPASubscriptionMapper(factory.createEntityManager());
            final List<Subscription> subscriptions = mapper.findSubscriptionsForUser(user);
            final Collection<String> results = new HashSet<String>(INITIAL_SIZE);
            for (Subscription subscription:subscriptions) {
                results.add(subscription.getMailbox());
            }
            return results;
        }  catch (PersistenceException e) {
            throw new SubscriptionException(e);
        }
    }

    public void unsubscribe(final String user, final String mailbox) throws SubscriptionException {
        try {
            final JPASubscriptionMapper mapper = new JPASubscriptionMapper(factory.createEntityManager());
            mapper.begin();
            
            final Subscription subscription = mapper.findFindMailboxSubscriptionForUser(user, mailbox);
            if (subscription != null) {
                mapper.delete(subscription);
                mapper.commit();
            }
        } catch (PersistenceException e) {
            throw new SubscriptionException(e);
        }
    }

}
