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
package org.apache.james.imap.jcr;

import javax.jcr.Session;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.TransactionalMapper;

/**
 * JCR implementation of a MailboxManager
 * 
 *
 */
public class JCRMailboxManager extends StoreMailboxManager{

    private final Session session;

    public JCRMailboxManager(final Authenticator authenticator, final Subscriber subscriber, final Session session) {
        super(authenticator, subscriber);
        this.session = session;
    }

    @Override
    protected StoreMailbox createMailbox(Mailbox mailboxRow) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected MailboxMapper createMailboxMapper() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void doCreate(String namespaceName) throws MailboxException {
        final Mailbox mailbox = new org.apache.james.imap.jcr.mail.model.JCRMailbox(namespaceName, randomUidValidity());
        final MailboxMapper mapper = createMailboxMapper();
        mapper.execute(new TransactionalMapper.Transaction(){

            public void run() throws MailboxException {
                mapper.save(mailbox);
            }
            
        });
    }

}
