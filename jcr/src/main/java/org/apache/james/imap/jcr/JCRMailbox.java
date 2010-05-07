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

import javax.jcr.Session;
import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.james.imap.jcr.mail.JCRMailboxMapper;
import org.apache.james.imap.jcr.mail.JCRMessageMapper;
import org.apache.james.imap.jcr.mail.model.JCRHeader;
import org.apache.james.imap.jcr.mail.model.JCRMailboxMembership;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;
import org.apache.james.imap.store.transaction.TransactionalMapper;

/**
 * JCR implementation of a {@link StoreMailbox}
 *
 */
public class JCRMailbox extends StoreMailbox<String>{

    private final Session jcrSession;
    private final Log log;
    private final int scaling;

    public JCRMailbox(final MailboxEventDispatcher dispatcher, final org.apache.james.imap.jcr.mail.model.JCRMailbox mailbox, final Session jcrSession, final int scaling, final Log log) {
        super(dispatcher, mailbox);
        this.log = log;
        this.scaling = scaling;
        this.jcrSession = jcrSession;
        
    }

    /**
     * Return the scaling depth
     * 
     * @return scaling
     */
    protected int getScaling() {
        return scaling;
    }
    

    
    @Override
    protected MailboxMembership<String> copyMessage(MailboxMembership<String> originalMessage, long uid, MailboxSession session) throws MailboxException {
        MailboxMembership<String> newRow = new JCRMailboxMembership(getMailboxId(), uid, (JCRMailboxMembership) originalMessage, log);
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
        final MailboxMembership<String> message = new JCRMailboxMembership(getMailboxId(), uid, internalDate, 
                size, flags, document, bodyStartOctet, jcrHeaders, propertyBuilder, log);
        return message;       
        
    }

    @Override
    protected MessageMapper<String> createMessageMapper(MailboxSession session) throws MailboxException {
        JCRMessageMapper messageMapper = new JCRMessageMapper(jcrSession, getMailboxId(), getScaling(), log);
        
        return messageMapper;

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
        JCRMailboxMapper mapper = new JCRMailboxMapper(jcrSession, getScaling(), log);
        return mapper;

    }

    @Override
    protected Mailbox<String> reserveNextUid(MailboxSession session) throws MailboxException {
        final MailboxMapper<String> mapper = createMailboxMapper(session);
        final org.apache.james.imap.jcr.mail.model.JCRMailbox mailbox = (org.apache.james.imap.jcr.mail.model.JCRMailbox) mapper.findMailboxById(getMailboxId());
        mapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                mailbox.consumeUid();
            }
            
        });
        return mailbox;
    }
}
