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
package org.apache.james.imap.store;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.user.SubscriptionMapper;

/**
 * Maintain mapper instances by {@link MailboxSession}. So only one mapper instance is used
 * in a {@link MailboxSession}
 */
public abstract class MailboxSessionMapperFactory <Id> {

    protected final static String MESSAGEMAPPER ="MESSAGEMAPPER";
    protected final static String MAILBOXMAPPER ="MAILBOXMAPPER";
    protected final static String SUBSCRIPTIONMAPPER ="SUBSCRIPTIONMAPPER";

    /**
     * Create a {@link MessageMapper} instance of return the one which exists for the {@link MailboxSession} already
     * 
     * @param session
     * @param mailboxId
     * @return mapper
     */
    @SuppressWarnings("unchecked")
    public MessageMapper<Id> getMessageMapper(MailboxSession session) throws MailboxException {
        MessageMapper<Id> mapper = (MessageMapper<Id>) session.getAttributes().get(MESSAGEMAPPER);
        if (mapper == null) {
            mapper = createMessageMapper(session);
            session.getAttributes().put(MESSAGEMAPPER, mapper);
        }
        return mapper;
    }

    /**
     * Create a {@link MessageMapper} instance which will get reused during the whole {@link MailboxSession}
     * 
     * @param session
     * @return messageMapper
     * @throws MailboxException
     */
    protected abstract MessageMapper<Id> createMessageMapper(MailboxSession session) throws MailboxException;

    /**
     * Create a {@link MailboxMapper} instance or return the one which exists for the {@link MailboxSession} already
     * 
     * @param session
     * @return mapper
     */
    @SuppressWarnings("unchecked")
    public MailboxMapper<Id> getMailboxMapper(MailboxSession session) throws MailboxException {
        MailboxMapper<Id> mapper = (MailboxMapper<Id>) session.getAttributes().get(MAILBOXMAPPER);
        if (mapper == null) {
            mapper = createMailboxMapper(session);
            session.getAttributes().put(MAILBOXMAPPER, mapper);
        }
        return mapper;
    }

    /**
     * Create a {@link MailboxMapper} instance which will get reused during the whole {@link MailboxSession}
     * 
     * @param session
     * @return mailboxMapper
     * @throws MailboxException
     */
    protected abstract MailboxMapper<Id> createMailboxMapper(MailboxSession session) throws MailboxException;

    /**
     * Create a {@link SubscriptionMapper} instance or return the one which exists for the {@link MailboxSession} already
     * 
     * @param session
     * @return mapper
     */
    public SubscriptionMapper getSubscriptionMapper(MailboxSession session) throws SubscriptionException {
        SubscriptionMapper mapper = (SubscriptionMapper) session.getAttributes().get(SUBSCRIPTIONMAPPER);
        if (mapper == null) {
            mapper = createSubscriptionMapper(session);
            session.getAttributes().put(SUBSCRIPTIONMAPPER, mapper);
        }
        return mapper;
    }
    
    /**
     * Create a {@link SubscriptionMapper} instance which will get reused during the whole {@link MailboxSession}
     * @param session
     * @return subscriptionMapper
     * @throws SubscriptionException
     */
    protected abstract SubscriptionMapper createSubscriptionMapper(MailboxSession session) throws SubscriptionException;

    /**
     * Callback which needs to get called once an IMAP Request was complete. It will take care of getting rid of all Session-scoped stuff
     * 
     * @param session
     */
    @SuppressWarnings("unchecked")
    public void endRequest(MailboxSession session) {
        if (session == null) return;
        MessageMapper<Id> messageMapper = (MessageMapper) session.getAttributes().get(MESSAGEMAPPER);
        MailboxMapper<Id> mailboxMapper = (MailboxMapper) session.getAttributes().get(MAILBOXMAPPER);
        SubscriptionMapper subscriptionMapper = (SubscriptionMapper) session.getAttributes().get(SUBSCRIPTIONMAPPER);
        if (messageMapper != null)
            messageMapper.endRequest();
        if (mailboxMapper != null)
            mailboxMapper.endRequest();
        if (subscriptionMapper != null)
            subscriptionMapper.endRequest();
    }
    

}
