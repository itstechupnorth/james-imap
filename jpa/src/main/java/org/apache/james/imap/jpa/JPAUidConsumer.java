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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;

import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.UidConsumer;
import org.apache.james.imap.store.mail.model.Mailbox;

/**
 * Take care of consume/reserve the next uid for a {@link Mailbox}. This is done by using database locks
 * 
 *
 */
public class JPAUidConsumer implements UidConsumer<Long>{

    private final MailboxSessionEntityManagerFactory factory;

    public JPAUidConsumer(final MailboxSessionEntityManagerFactory factory) {
        this.factory = factory;
    }
    
    /**
     * 
     * Reserve next Uid in mailbox and return the mailbox. We use a PESSIMISTIC_WRITE lock here to be sure we don't see any duplicates here when
     * accessing the database with many different threads / connections
     * 
     * @see org.apache.james.imap.store.UidConsumer#reserveNextUid(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.imap.mailbox.MailboxSession)
     */
    public long reserveNextUid(Mailbox<Long> mailbox, MailboxSession session) {
        EntityManager manager = factory.createEntityManager(session);
        EntityTransaction transaction = manager.getTransaction();
        transaction.begin();

        // we need to set a persimistic write lock to be sure we don't get any problems with dirty reads etc
        org.apache.james.imap.jpa.mail.model.JPAMailbox m = manager.find(org.apache.james.imap.jpa.mail.model.JPAMailbox.class, mailbox.getMailboxId(), LockModeType.PESSIMISTIC_WRITE);
        manager.refresh(m);
        m.consumeUid();
        manager.persist(m);
        manager.flush();
        transaction.commit();
        return m.getLastUid();
    }

}
