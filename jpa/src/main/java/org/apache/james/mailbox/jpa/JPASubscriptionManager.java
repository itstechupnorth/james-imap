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
package org.apache.james.mailbox.jpa;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jpa.user.model.JPASubscription;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.apache.james.mailbox.store.user.model.Subscription;

/**
 * JPA implementation of {@link StoreSubscriptionManager}
 *
 */
public class JPASubscriptionManager extends StoreSubscriptionManager<Long> {
    
    public JPASubscriptionManager(final JPAMailboxSessionMapperFactory mapperFactory) {
        super(mapperFactory);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.StoreSubscriptionManager#createSubscription(org.apache.james.mailbox.MailboxSession, java.lang.String)
     */
    protected Subscription createSubscription(final MailboxSession session, final String mailbox) {
        final Subscription newSubscription = new JPASubscription(session.getUser().getUserName(), mailbox);
        return newSubscription;
    }
}