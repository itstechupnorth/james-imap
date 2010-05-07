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

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jpa.JPATransactionalMapper;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * JPA implementation of a {@link SubscriptionMapper}. This class is not thread-safe!
 */
public class JPASubscriptionMapper extends JPATransactionalMapper implements SubscriptionMapper {

    public JPASubscriptionMapper(final EntityManager factory) {
        super(factory);
    }


    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.store.user.SubscriptionMapper#findFindMailboxSubscriptionForUser(java.lang.String, java.lang.String)
     */
    public Subscription findFindMailboxSubscriptionForUser(final String user, final String mailbox) throws SubscriptionException {
        try {
            return (Subscription) getManager().createNamedQuery("findFindMailboxSubscriptionForUser")
            .setParameter("userParam", user).setParameter("mailboxParam", mailbox).getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (PersistenceException e) {
            throw new SubscriptionException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.store.user.SubscriptionMapper#save(Subscription)
     */
    public void save(Subscription subscription) throws SubscriptionException {
        try {
            getManager().persist(subscription);
        } catch (PersistenceException e) {
            throw new SubscriptionException(HumanReadableText.SAVE_FAILED, e);
        }
    }

    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.store.user.SubscriptionMapper#findSubscriptionsForUser(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public List<Subscription> findSubscriptionsForUser(String user) throws SubscriptionException {
        try {
            return (List<Subscription>) getManager().createNamedQuery("findSubscriptionsForUser").setParameter("userParam", user).getResultList();
        } catch (PersistenceException e) {
            throw new SubscriptionException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /**
     * @throws SubscriptionException 
     * @see org.apache.james.imap.store.user.SubscriptionMapper#delete(Subscription)
     */
    public void delete(Subscription subscription) throws SubscriptionException {
        try {
            getManager().remove(subscription);
        } catch (PersistenceException e) {
            throw new SubscriptionException(HumanReadableText.DELETED_FAILED, e);
        }
    }
}
