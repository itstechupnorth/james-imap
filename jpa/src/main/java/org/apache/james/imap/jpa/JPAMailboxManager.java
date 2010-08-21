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
package org.apache.james.imap.jpa;

import org.apache.james.imap.api.MailboxPath;
import org.apache.james.imap.jpa.mail.model.JPAMailbox;
import org.apache.james.imap.jpa.user.model.JPASubscription;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.TransactionalMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * JPA implementation of {@link StoreMailboxManager}
 */
public abstract class JPAMailboxManager extends StoreMailboxManager<Long> {
    
    public JPAMailboxManager(JPAMailboxSessionMapperFactory mailboxSessionMapperFactory,
            final Authenticator authenticator, Subscriber subscripter) {
        super(mailboxSessionMapperFactory, authenticator, subscripter);
    }
    
    @Override
    protected void doCreateMailbox(MailboxPath path, MailboxSession session) throws MailboxException {
        final Mailbox<Long> mailbox = new JPAMailbox(path, randomUidValidity());
        final MailboxMapper<Long> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        mapper.execute(new TransactionalMapper.VoidTransaction(){

            public void runVoid() throws MailboxException {
                mapper.save(mailbox);
            }
            
        });
    }

    /**
     * Delete all mailboxes 
     * 
     * @param maibloxSession
     * @throws MailboxException
     */
    public void deleteEverything(MailboxSession mailboxSession) throws MailboxException {
        final MailboxMapper<Long> mapper = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession);
        mapper.execute(new TransactionalMapper.VoidTransaction() {

            public void runVoid() throws MailboxException {
                mapper.deleteAll(); 
            }
            
        });
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.StoreMailboxManager#createSubscription(org.apache.james.imap.mailbox.MailboxSession, java.lang.String)
     */
    protected Subscription createSubscription(final MailboxSession session, final String mailbox) {
        final Subscription newSubscription = new JPASubscription(session.getUser().getUserName(), mailbox);
        return newSubscription;
    }
    
}
