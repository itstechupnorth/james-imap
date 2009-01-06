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

import org.apache.james.imap.jpa.user.model.Subscription;

/**
 * Maps data access logic to JPA operations.
 */
public class JPASubscriptionMapper {
    private final EntityManager entityManager;
    
    public JPASubscriptionMapper(final EntityManager entityManager) {
        super();
        this.entityManager = entityManager;
    }
    
    public void begin() {
        entityManager.getTransaction().begin();
    }
    
    public void commit() {
        entityManager.getTransaction().commit();
    }

    /**
     * Finds any subscriptions for a given user to the given mailbox.
     * @param user not null
     * @param mailbox not null
     * @return <code>Subscription</code>, 
     * or null when the user is not subscribed to the given mailbox
     */
    public Subscription findFindMailboxSubscriptionForUser(final String user, final String mailbox) {
        try {
            return (Subscription) entityManager.createNamedQuery("findFindMailboxSubscriptionForUser")
                .setParameter("userParam", user).setParameter("mailboxParam", mailbox).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Saves the given subscription.
     * @param subscription not null
     */
    public void save(Subscription subscription) {
        entityManager.persist(subscription);
    }

    /**
     * Finds subscriptions for the given user.
     * @param user not null
     * @return not null
     */
    @SuppressWarnings("unchecked")
    public List<Subscription> findSubscriptionsForUser(String user) {
        return (List<Subscription>) entityManager.createNamedQuery("findSubscriptionsForUser").setParameter("userParam", user).getResultList();
    }

    /**
     * Deletes the given subscription.
     * @param subscription not null
     */
    public void delete(Subscription subscription) {
        entityManager.remove(subscription);
    }
}
