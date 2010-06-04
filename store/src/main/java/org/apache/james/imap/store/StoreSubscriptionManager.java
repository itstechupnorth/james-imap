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

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.transaction.TransactionalMapper;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * Manages subscriptions.
 */
public abstract class StoreSubscriptionManager<Id> implements Subscriber {

    private static final int INITIAL_SIZE = 32;
    
    protected MailboxSessionMapperFactory<Id> mapperFactory;
    
    public StoreSubscriptionManager(final MailboxSessionMapperFactory<Id> mapperFactory) {
        this.mapperFactory = mapperFactory;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.Subscriber#subscribe(org.apache.james.imap.mailbox.MailboxSession, java.lang.String)
     */
    public void subscribe(final MailboxSession session, final String mailbox) throws SubscriptionException {
        final SubscriptionMapper mapper = mapperFactory.getSubscriptionMapper(session);
        try {
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    final Subscription subscription = mapper.findFindMailboxSubscriptionForUser(session.getUser().getUserName(), mailbox);
                    if (subscription == null) {
                        final Subscription newSubscription = createSubscription(session, mailbox);
                        mapper.save(newSubscription);
                    }
                }
                
            });
        } catch (MailboxException e) {
            throw new SubscriptionException(e.getKey(), e);
        }
    }

    /**
     * Create Subscription for the given user and mailbox
     * 
     * @param session
     * @param mailbox
     * @return subscription 
     */
    protected abstract Subscription createSubscription(final MailboxSession session, final String mailbox);

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.Subscriber#subscriptions(org.apache.james.imap.mailbox.MailboxSession)
     */
    public Collection<String> subscriptions(final MailboxSession session) throws SubscriptionException {
        final SubscriptionMapper mapper = mapperFactory.getSubscriptionMapper(session);
        final List<Subscription> subscriptions = mapper.findSubscriptionsForUser(session.getUser().getUserName());
        final Collection<String> results = new HashSet<String>(INITIAL_SIZE);
        for (Subscription subscription:subscriptions) {
            results.add(subscription.getMailbox());
        }        
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.Subscriber#unsubscribe(java.lang.String, java.lang.String)
     */
    public void unsubscribe(final MailboxSession session, final String mailbox) throws SubscriptionException {
        final SubscriptionMapper mapper = mapperFactory.getSubscriptionMapper(session);
        try {
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    final Subscription subscription = mapper.findFindMailboxSubscriptionForUser(session.getUser().getUserName(), mailbox);
                    if (subscription != null) {
                        mapper.delete(subscription);
                    }
                }

            });
        } catch (MailboxException e) {
            throw new SubscriptionException(e.getKey(), e);
        }
    }
}
