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
package org.apache.james.mailbox.jcr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jcr.mail.JCRMailboxMapper;
import org.apache.james.mailbox.jcr.mail.model.JCRMailbox;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.MapperStoreMessageManager;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.transaction.Mapper;
import org.apache.james.mailbox.util.MailboxEventDispatcher;

/**
 * JCR implementation of a MailboxManager
 * 
 */
public class JCRMailboxManager extends StoreMailboxManager<String> implements JCRImapConstants {

    private final JCRMailboxSessionMapperFactory mapperFactory;
    private final Log logger = LogFactory.getLog(JCRMailboxManager.class);
    
    public JCRMailboxManager(JCRMailboxSessionMapperFactory mapperFactory, final Authenticator authenticator) {
	    this(mapperFactory, authenticator, new JCRVmNodeLocker());
    }

    public JCRMailboxManager(JCRMailboxSessionMapperFactory mapperFactory, final Authenticator authenticator, final NodeLocker locker) {
        super(mapperFactory, authenticator);
        this.mapperFactory = mapperFactory;
    }

    
    @Override
    protected MapperStoreMessageManager<String> createMessageManager(MailboxEventDispatcher dispatcher, Mailbox<String> mailboxEntity, MailboxSession session) throws MailboxException{
        return new JCRMessageManager(mapperFactory, dispatcher, (JCRMailbox) mailboxEntity, logger, getDelimiter());
    }

    @Override
    protected void doCreateMailbox(MailboxPath path, MailboxSession session) throws MailboxException {
        final Mailbox<String> mailbox = new org.apache.james.mailbox.jcr.mail.model.JCRMailbox(path, randomUidValidity(), logger);
        final JCRMailboxMapper mapper = (JCRMailboxMapper) mapperFactory.getMailboxMapper(session);
        mapper.execute(new Mapper.VoidTransaction() {

            public void runVoid() throws MailboxException {
                mapper.save(mailbox);
            }

        });
    }

}
