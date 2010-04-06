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

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.mail.JCRMailboxMapper;
import org.apache.james.imap.jcr.mail.JCRMessageMapper;
import org.apache.james.imap.jcr.mail.model.JCRHeader;
import org.apache.james.imap.jcr.mail.model.JCRMailboxMembership;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.PasswordAwareUser;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

/**
 * JCR implementation of a {@link StoreMailbox}
 *
 */
public class JCRMailbox extends StoreMailbox<String>{

    private final Repository repository;
    private final String workspace;
    private final Log log;
    private final int scaling;
    
    public JCRMailbox(final org.apache.james.imap.jcr.mail.model.JCRMailbox mailbox, final MailboxSession session, final Repository repository, final String workspace, final int scaling, final Log log) {
        super(mailbox, session );
        this.repository = repository;
        this.workspace = workspace;
        this.log = log;
        this.scaling = scaling;
        
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
    protected MailboxMembership<String> copyMessage(MailboxMembership<String> originalMessage, long uid) {
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
        Session jcrSession = getSession(session);
        JCRUtils.addJCRSession(session, jcrSession);
        
        JCRMessageMapper messageMapper = new JCRMessageMapper(jcrSession, getMailboxId(), getScaling(), log);
        
        return messageMapper;

    }

    /**
     * Ceate a MailboxMapper for the given {@link MailboxSession} 
     * 
     * @param session
     * @return mailboxMapper
     * @throws MailboxException
     */
    protected JCRMailboxMapper createMailboxMapper(MailboxSession session) throws MailboxException {
        Session jcrSession = getSession(session);
        JCRUtils.addJCRSession(session, jcrSession);
        
        JCRMailboxMapper mapper = new JCRMailboxMapper(jcrSession, getScaling(), log);
        return mapper;

    }
    
    @Override
    protected Mailbox<String> getMailboxRow() throws MailboxException {
        final JCRMailboxMapper mapper = createMailboxMapper(getMailboxSession());
        return mapper.findMailboxById(getMailboxId());
    }

    @Override
    protected Mailbox<String> reserveNextUid() throws MailboxException {
        final JCRMailboxMapper mapper = createMailboxMapper(getMailboxSession());
        return mapper.consumeNextUid(getMailboxId());
    }

    /**
     * Return a new JCR Session for the given MailboxSession
     * 
     * @param s
     * @return session
     * @throws MailboxException
     */
    protected Session getSession(MailboxSession session) throws SubscriptionException {
        PasswordAwareUser user = (PasswordAwareUser) getMailboxSession().getUser();

        try {
            return repository.login(new SimpleCredentials(user.getUserName(), user.getPassword().toCharArray()), getWorkspace());
        } catch (LoginException e) {
            throw new SubscriptionException(HumanReadableText.INVALID_LOGIN, e);
        } catch (NoSuchWorkspaceException e) {
            throw new SubscriptionException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);
        } catch (RepositoryException e) {
            throw new SubscriptionException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);

        }
    }

    /**
     * Return the JCR workspace
     * 
     * @return workspace
     */
    protected String getWorkspace() {
        return workspace;
    }

    /**
     * Return JCR Repository
     * 
     * @return repository
     */
    protected Repository getRepository() {
        return repository;
    }

}
