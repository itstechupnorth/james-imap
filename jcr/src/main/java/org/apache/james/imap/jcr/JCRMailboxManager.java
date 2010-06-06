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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.imap.jcr.mail.JCRMailboxMapper;
import org.apache.james.imap.jcr.mail.model.JCRMailbox;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.james.imap.store.StoreMessageManager;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.UidConsumer;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.TransactionalMapper;

/**
 * JCR implementation of a MailboxManager
 * 
 * 
 */
public class JCRMailboxManager extends StoreMailboxManager<String> implements JCRImapConstants{

    private final JCRMailboxSessionMapperFactory mapperFactory;
    private final Log logger = LogFactory.getLog(JCRMailboxManager.class);
    
    public JCRMailboxManager(JCRMailboxSessionMapperFactory mapperFactory, final Authenticator authenticator, final Subscriber subscriber) {
	    this(mapperFactory, authenticator, subscriber, new JCRVmNodeLocker(), new JCRUidConsumer(mapperFactory.getRepository(),  new JCRVmNodeLocker()));
    }

    public JCRMailboxManager(JCRMailboxSessionMapperFactory mapperFactory, final Authenticator authenticator, final Subscriber subscriber, final NodeLocker locker, final UidConsumer<String> consumer) {
        super(mapperFactory, authenticator, subscriber, consumer);
        this.mapperFactory = mapperFactory;
    }

    
    @Override
    protected StoreMessageManager<String> createMessageManager(MailboxEventDispatcher dispatcher, UidConsumer<String> consumer, Mailbox<String> mailboxEntity, MailboxSession session) throws MailboxException{
        return new JCRMessageManager(mapperFactory, dispatcher, consumer, (JCRMailbox) mailboxEntity, logger, getDelimiter(), session);
    }

    @Override
    protected void doCreateMailbox(String namespaceName, MailboxSession session) throws MailboxException {
        final Mailbox<String> mailbox = new org.apache.james.imap.jcr.mail.model.JCRMailbox(namespaceName, randomUidValidity(), logger);
        final JCRMailboxMapper mapper = (JCRMailboxMapper) mapperFactory.getMailboxMapper(session);
        mapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                mapper.save(mailbox);
            }

        });
    }

}
