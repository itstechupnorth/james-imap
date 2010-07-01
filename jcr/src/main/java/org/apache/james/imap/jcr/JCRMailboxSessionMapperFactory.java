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
import org.apache.james.imap.jcr.mail.JCRMessageMapper;
import org.apache.james.imap.jcr.user.JCRSubscriptionMapper;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.MailboxSessionMapperFactory;
import org.apache.james.imap.store.StoreConstants;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.user.SubscriptionMapper;

/**
 * JCR implementation of a {@link MailboxSessionMapperFactory}
 * 
 *
 */
public class JCRMailboxSessionMapperFactory extends MailboxSessionMapperFactory<String> implements StoreConstants{

    private final MailboxSessionJCRRepository repository;
    private final Log logger;
    private final char delimiter;
    private final NodeLocker locker;
    private final int DEFAULT_SCALING = 2;

    public JCRMailboxSessionMapperFactory(final MailboxSessionJCRRepository repository, final NodeLocker locker) {
        this(repository, locker, DEFAULT_FOLDER_DELIMITER);
    }

    public JCRMailboxSessionMapperFactory(final MailboxSessionJCRRepository repository, final NodeLocker locker, final char delimiter) {
        this.repository = repository;
        this.logger = LogFactory.getLog(JCRSubscriptionManager.class);
        this.delimiter = delimiter;
        this.locker = locker;
    }

    
    @Override
    public MailboxMapper<String> createMailboxMapper(MailboxSession session) throws MailboxException {
        JCRMailboxMapper mapper = new JCRMailboxMapper(repository, session, locker, DEFAULT_SCALING, logger, delimiter);
        return mapper;
    }

    @Override
    public MessageMapper<String> createMessageMapper(MailboxSession session) throws MailboxException {
        JCRMessageMapper messageMapper = new JCRMessageMapper(repository, session, locker, logger);
        return messageMapper;
    }

    @Override
    public SubscriptionMapper createSubscriptionMapper(MailboxSession session) throws SubscriptionException {
        JCRSubscriptionMapper mapper = new JCRSubscriptionMapper(repository, session, locker, DEFAULT_SCALING, logger);
        return mapper;
    }
    
    public MailboxSessionJCRRepository getRepository() {
        return repository;
    }

}
