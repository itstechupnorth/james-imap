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
package org.apache.james.imap.jpa.user;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.apache.james.imap.jpa.user.model.JPASubscription;
import org.apache.james.imap.store.user.SubscriptionMapper;

/**
 * Maps data access logic to JPA operations.
 */
public class JPASubscriptionMapper implements SubscriptionMapper {
    private final EntityManager entityManager;
    
    public JPASubscriptionMapper(final EntityManager entityManager) {
        super();
        this.entityManager = entityManager;
    }
    
    /* (non-Javadoc)
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#begin()
     */
    public void begin() {
        entityManager.getTransaction().begin();
    }
    
    /* (non-Javadoc)
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#commit()
     */
    public void commit() {
        entityManager.getTransaction().commit();
    }

    /* (non-Javadoc)
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#findFindMailboxSubscriptionForUser(java.lang.String, java.lang.String)
     */
    public JPASubscription findFindMailboxSubscriptionForUser(final String user, final String mailbox) {
        try {
            return (JPASubscription) entityManager.createNamedQuery("findFindMailboxSubscriptionForUser")
                .setParameter("userParam", user).setParameter("mailboxParam", mailbox).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#save(org.apache.james.imap.jpa.user.model.Subscription)
     */
    public void save(JPASubscription subscription) {
        entityManager.persist(subscription);
    }

    /* (non-Javadoc)
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#findSubscriptionsForUser(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public List<JPASubscription> findSubscriptionsForUser(String user) {
        return (List<JPASubscription>) entityManager.createNamedQuery("findSubscriptionsForUser").setParameter("userParam", user).getResultList();
    }

    /* (non-Javadoc)
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#delete(org.apache.james.imap.jpa.user.model.Subscription)
     */
    public void delete(JPASubscription subscription) {
        entityManager.remove(subscription);
    }
}
