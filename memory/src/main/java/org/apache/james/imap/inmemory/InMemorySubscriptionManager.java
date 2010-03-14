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

package org.apache.james.imap.inmemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MailboxSession.User;
import org.apache.james.imap.store.StoreSubscriptionManager;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * Stores subscriptions in memory.
 */
public class InMemorySubscriptionManager extends StoreSubscriptionManager implements SubscriptionMapper {
    
    private static final int INITIAL_SIZE = 64;
    private final Map<String, List<Subscription>> subscriptionsByUser;
    
    public InMemorySubscriptionManager() {
        super();
        subscriptionsByUser = new ConcurrentHashMap<String, List<Subscription>>(INITIAL_SIZE);
    }

    @Override
    protected SubscriptionMapper createMapper(MailboxSession session) {
        return this;
    }

    @Override
    protected Subscription createSubscription(MailboxSession session, String mailbox) {
        return new InMemorySubscription(mailbox, session.getUser());
    }

    public synchronized void delete(Subscription subscription) {
        final String user = subscription.getUser();
        final List<Subscription> subscriptions = subscriptionsByUser.get(user);
        if (subscriptions != null) {
            subscriptions.remove(subscription);
        }
    }

    public Subscription findFindMailboxSubscriptionForUser(String user, String mailbox) {
        final List<Subscription> subscriptions = subscriptionsByUser.get(user);
        Subscription result = null;
        if (subscriptions != null) {
            for(Subscription subscription:subscriptions) {
                if (subscription.getMailbox().equals(mailbox)) {
                    result = subscription;
                    break;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Subscription> findSubscriptionsForUser(String user) {
        final List<Subscription> subcriptions = subscriptionsByUser.get(user);
        final List<Subscription> results;
        if (subcriptions == null) {
            results = Collections.EMPTY_LIST;
        } else {
            // Make a copy to prevent concurrent modifications
            results = new ArrayList<Subscription>(subcriptions);
        }
        return results;
    }

    public synchronized void save(Subscription subscription) {
        final String user = subscription.getUser();
        final List<Subscription> subscriptions = subscriptionsByUser.get(user);
        if (subscriptions == null) {
            final List<Subscription> newSubscriptions  = new ArrayList<Subscription>();
            newSubscriptions.add(subscription);
            subscriptionsByUser.put(user, newSubscriptions);
        } else {
            subscriptions.add(subscription);
        }
    }

    private final static class InMemorySubscription implements Subscription {

        private final String mailbox;
        private final String user;
        
        public InMemorySubscription(final String mailbox, final User user) {
            super();
            this.mailbox = mailbox;
            this.user = user.getUserName();
        }

        public String getMailbox() {
            return mailbox;
        }

        public String getUser() {
            return user;
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((mailbox == null) ? 0 : mailbox.hashCode());
            result = PRIME * result + ((user == null) ? 0 : user.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final InMemorySubscription other = (InMemorySubscription) obj;
            if (mailbox == null) {
                if (other.mailbox != null)
                    return false;
            } else if (!mailbox.equals(other.mailbox))
                return false;
            if (user == null) {
                if (other.user != null)
                    return false;
            } else if (!user.equals(other.user))
                return false;
            return true;
        }

        /**
         * Representation suitable for logging and debugging.
         * @return a <code>String</code> representation 
         * of this object.
         */
        public String toString()
        {
            return "InMemorySubscription[ "
                + "mailbox = " + this.mailbox + " "
                + "user = " + this.user + " "
                + " ]";
        }
        
    }

    public void execute(Transaction transaction) throws MailboxException {
        transaction.run();
    }

    public void destroy() {
        // Nothing todo
    }

    @Override
    protected void onLogout(MailboxSession session) {
        // Nothing todo
        
    }
}
