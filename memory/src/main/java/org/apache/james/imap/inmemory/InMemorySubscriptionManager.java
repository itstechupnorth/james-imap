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

import org.apache.james.imap.inmemory.user.model.InMemorySubscription;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.MailboxSessionMapperFactory;
import org.apache.james.imap.store.StoreSubscriptionManager;
import org.apache.james.imap.store.transaction.TransactionalMapper.Transaction;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * Stores subscriptions in memory.
 */
public class InMemorySubscriptionManager extends StoreSubscriptionManager<Long> {
    
    public InMemorySubscriptionManager(MailboxSessionMapperFactory<Long> mapperFactory) {
        super(mapperFactory);
    }

    @Override
    protected Subscription createSubscription(MailboxSession session, String mailbox) {
        return new InMemorySubscription(mailbox, session.getUser());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.TransactionalMapper#execute(org.apache.james.imap.store.transaction.TransactionalMapper.Transaction)
     */
    public void execute(Transaction transaction) throws MailboxException {
        transaction.run();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.TransactionalMapper#dispose()
     */
    public void dispose() {
        // do nothing
    }
}
