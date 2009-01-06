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
import javax.persistence.PersistenceException;

import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * Maps data access logic to JPA operations.
 */
public class JPASubscriptionMapper implements SubscriptionMapper {
    private final EntityManager entityManager;

    public JPASubscriptionMapper(final EntityManager entityManager) {
        super();
        this.entityManager = entityManager;
    }

    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#begin()
     */
    public void begin() throws SubscriptionException {
        try {
            entityManager.getTransaction().begin();
        } catch (PersistenceException e) {
            throw new SubscriptionException(e);
        }
    }

    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#commit()
     */
    public void commit() throws SubscriptionException {
        try {
            entityManager.getTransaction().commit();
        } catch (PersistenceException e) {
            throw new SubscriptionException(e);
        }
    }

    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#findFindMailboxSubscriptionForUser(java.lang.String, java.lang.String)
     */
    public Subscription findFindMailboxSubscriptionForUser(final String user, final String mailbox) throws SubscriptionException {
        try {
            return (Subscription) entityManager.createNamedQuery("findFindMailboxSubscriptionForUser")
            .setParameter("userParam", user).setParameter("mailboxParam", mailbox).getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (PersistenceException e) {
            throw new SubscriptionException(e);
        }
    }

    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#save(org.apache.james.imap.jpa.user.model.Subscription)
     */
    public void save(Subscription subscription) throws SubscriptionException {
        try {
            entityManager.persist(subscription);
        } catch (PersistenceException e) {
            throw new SubscriptionException(e);
        }
    }

    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#findSubscriptionsForUser(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public List<Subscription> findSubscriptionsForUser(String user) throws SubscriptionException {
        try {
            return (List<Subscription>) entityManager.createNamedQuery("findSubscriptionsForUser").setParameter("userParam", user).getResultList();
        } catch (PersistenceException e) {
            throw new SubscriptionException(e);
        }
    }

    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.jpa.user.SubscriptionManager#delete(org.apache.james.imap.jpa.user.model.Subscription)
     */
    public void delete(Subscription subscription) throws SubscriptionException {
        try {
            entityManager.remove(subscription);
        } catch (PersistenceException e) {
            throw new SubscriptionException(e);
        }
    }
}
