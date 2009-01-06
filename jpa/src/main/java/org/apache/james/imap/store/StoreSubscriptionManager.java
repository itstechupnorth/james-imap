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
package org.apache.james.imap.store;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.apache.james.imap.jpa.user.JPASubscriptionMapper;
import org.apache.james.imap.jpa.user.model.JPASubscription;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * Manages subscriptions.
 */
public class StoreSubscriptionManager implements Subscriber {

    private static final int INITIAL_SIZE = 32;
    private final EntityManagerFactory factory;

    public StoreSubscriptionManager(final EntityManagerFactory factory) {
        super();
        this.factory = factory;
    }

    public void subscribe(final String user, final String mailbox) throws SubscriptionException {
        final SubscriptionMapper mapper = createMapper();
        mapper.begin();

        final Subscription subscription = mapper.findFindMailboxSubscriptionForUser(user, mailbox);
        if (subscription == null) {
            final Subscription newSubscription = createSubscription(user, mailbox);
            mapper.save(newSubscription);
            mapper.commit();
        }
    }

    private Subscription createSubscription(final String user, final String mailbox) {
        final Subscription newSubscription = new JPASubscription(user, mailbox);
        return newSubscription;
    }

    private SubscriptionMapper createMapper() {
        final JPASubscriptionMapper mapper = new JPASubscriptionMapper(factory.createEntityManager());
        return mapper;
    }

    public Collection<String> subscriptions(final String user) throws SubscriptionException {
        final SubscriptionMapper mapper = createMapper();
        final List<Subscription> subscriptions = mapper.findSubscriptionsForUser(user);
        final Collection<String> results = new HashSet<String>(INITIAL_SIZE);
        for (Subscription subscription:subscriptions) {
            results.add(subscription.getMailbox());
        }
        return results;
    }

    public void unsubscribe(final String user, final String mailbox) throws SubscriptionException {
        final SubscriptionMapper mapper = createMapper();
        mapper.begin();

        final Subscription subscription = mapper.findFindMailboxSubscriptionForUser(user, mailbox);
        if (subscription != null) {
            mapper.delete(subscription);
            mapper.commit();
        }
    }
}
