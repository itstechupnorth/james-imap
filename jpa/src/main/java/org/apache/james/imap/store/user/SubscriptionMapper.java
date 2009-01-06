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
package org.apache.james.imap.store.user;

import java.util.List;

import org.apache.james.imap.jpa.user.model.Subscription;

public interface SubscriptionMapper {

    public abstract void begin();

    public abstract void commit();

    /**
     * Finds any subscriptions for a given user to the given mailbox.
     * @param user not null
     * @param mailbox not null
     * @return <code>Subscription</code>, 
     * or null when the user is not subscribed to the given mailbox
     */
    public abstract Subscription findFindMailboxSubscriptionForUser(
            final String user, final String mailbox);

    /**
     * Saves the given subscription.
     * @param subscription not null
     */
    public abstract void save(Subscription subscription);

    /**
     * Finds subscriptions for the given user.
     * @param user not null
     * @return not null
     */
    @SuppressWarnings("unchecked")
    public abstract List<Subscription> findSubscriptionsForUser(String user);

    /**
     * Deletes the given subscription.
     * @param subscription not null
     */
    public abstract void delete(Subscription subscription);

}