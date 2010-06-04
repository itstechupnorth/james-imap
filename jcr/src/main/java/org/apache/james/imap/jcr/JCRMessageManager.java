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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.mail.JCRMailboxMapper;
import org.apache.james.imap.jcr.mail.JCRMessageMapper;
import org.apache.james.imap.jcr.mail.model.JCRHeader;
import org.apache.james.imap.jcr.mail.model.JCRMessage;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.store.StoreMessageManager;
import org.apache.james.imap.store.UidConsumer;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

/**
 * JCR implementation of a {@link StoreMessageManager}
 *
 */
public class JCRMessageManager extends StoreMessageManager<String>{

    private final MailboxSessionJCRRepository repos;
    private final Log log;
    private char delimiter;

    public JCRMessageManager(final MailboxEventDispatcher dispatcher, UidConsumer<String> consumer, final org.apache.james.imap.jcr.mail.model.JCRMailbox mailbox, final MailboxSessionJCRRepository repos, final Log log, final char delimiter) {
        super(dispatcher, consumer, mailbox);
        this.log = log;
        this.repos = repos;
        this.delimiter = delimiter;
        
    }


    @Override
    protected MailboxMembership<String> copyMessage(MailboxMembership<String> originalMessage, long uid, MailboxSession session) throws MailboxException {
        MailboxMembership<String> newRow = new JCRMessage(getMailboxId(), uid, (JCRMessage) originalMessage, log);
        return newRow;
    }

    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        return new JCRHeader(lineNumber, name, value, log);
    }

    @Override
    protected MailboxMembership<String> createMessage(Date internalDate, long uid, int size, int bodyStartOctet, InputStream document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) {
        final List<JCRHeader> jcrHeaders = new ArrayList<JCRHeader>(headers.size());
        for (Header header: headers) {
            jcrHeaders.add((JCRHeader) header);
        }
        final MailboxMembership<String> message = new JCRMessage(getMailboxId(), uid, internalDate, 
                size, flags, document, bodyStartOctet, jcrHeaders, propertyBuilder, log);
        return message;       
        
    }

    @Override
    protected MessageMapper<String> createMessageMapper(MailboxSession session) throws MailboxException {
        try {
            JCRMessageMapper messageMapper = new JCRMessageMapper(repos.login(session), getMailboxId(), log);
            return messageMapper;
        } catch (RepositoryException e) {
            throw new MailboxException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);
        }
        
    }

    /**
     * Ceate a MailboxMapper for the given {@link MailboxSession} 
     * 
     * @param session
     * @return mailboxMapper
     * @throws MailboxException 
     * @throws MailboxException
     */
    protected JCRMailboxMapper createMailboxMapper(MailboxSession session) throws MailboxException {
        try {
            JCRMailboxMapper mapper = new JCRMailboxMapper(repos.login(session), log, delimiter);
            return mapper;

        } catch (RepositoryException e) {
            throw new MailboxException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);
        }
        

    }
}
