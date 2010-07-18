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

import org.apache.james.imap.api.MailboxPath;
import org.apache.james.imap.inmemory.mail.model.InMemoryMailbox;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.MailboxSessionMapperFactory;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.james.imap.store.StoreMessageManager;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.UidConsumer;
import org.apache.james.imap.store.mail.model.Mailbox;

public class InMemoryMailboxManager extends StoreMailboxManager<Long> {

    public InMemoryMailboxManager(MailboxSessionMapperFactory<Long> mapperFactory, Authenticator authenticator, Subscriber subscriber) {
        super(mapperFactory, authenticator, subscriber, new UidConsumer<Long>() {

            public long reserveNextUid(Mailbox<Long> mailbox, MailboxSession session) throws MailboxException {
                mailbox.consumeUid();
                return mailbox.getLastUid();
            }
        });
    }

    @Override
    protected StoreMessageManager<Long> createMessageManager(MailboxEventDispatcher dispatcher, UidConsumer<Long> consumer, Mailbox<Long> mailboxRow, MailboxSession session) throws MailboxException {
        return new InMemoryStoreMessageManager(mailboxSessionMapperFactory, dispatcher, consumer, (InMemoryMailbox)mailboxRow, session);
    }

    @Override
    protected void doCreateMailbox(MailboxPath mailboxPath, MailboxSession session) throws StorageException {
        InMemoryMailbox mailbox = new InMemoryMailbox(randomId(), mailboxPath, randomUidValidity());
        try {
            mailboxSessionMapperFactory.getMailboxMapper(session).save(mailbox);
        } catch (MailboxException e) {
        }
    }

    /**
     * Delete every Mailbox which exists
     * 
     * @throws MailboxException
     */

    public synchronized void deleteEverything() throws MailboxException {
        ((InMemoryMailboxSessionMapperFactory) mailboxSessionMapperFactory).deleteAll();
    }


    
}
