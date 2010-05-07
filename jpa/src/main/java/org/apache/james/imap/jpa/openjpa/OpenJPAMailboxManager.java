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

package org.apache.james.imap.jpa.openjpa;


import org.apache.james.imap.jpa.JPAMailboxManager;
import org.apache.james.imap.jpa.MailboxSessionEntityManagerFactory;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.mail.model.Mailbox;

/**
 * OpenJPA implementation of MailboxManager
 *
 */
public class OpenJPAMailboxManager extends JPAMailboxManager {

    private boolean useStreaming;

    public OpenJPAMailboxManager(Authenticator authenticator, Subscriber subscriber, MailboxSessionEntityManagerFactory entityManagerFactory, boolean useStreaming) {
        super(authenticator, subscriber, entityManagerFactory);
        this.useStreaming = useStreaming;
    }

    public OpenJPAMailboxManager(Authenticator authenticator, Subscriber subscriber, MailboxSessionEntityManagerFactory entityManagerFactory) {
        this(authenticator, subscriber, entityManagerFactory, false);
    }

    protected StoreMailbox<Long> createMailbox(MailboxEventDispatcher dispatcher, Mailbox<Long> mailboxRow, MailboxSession session) {
        StoreMailbox<Long> result =  new OpenJPAMailbox(dispatcher,mailboxRow, entityManagerFactory.createEntityManager(session), useStreaming);
        return result;
    }
}
