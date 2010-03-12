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

public class JCRMailbox extends StoreMailbox{

    private final Repository repository;
    private final String workspace;
    private final Log log;
    private String uuid;
    
    public JCRMailbox(final org.apache.james.imap.jcr.mail.model.JCRMailbox mailbox, final MailboxSession session, final Repository repository, final String workspace, final Log log) {
        super(mailbox, session );
        this.repository = repository;
        this.workspace = workspace;
        this.log = log;
        this.uuid = mailbox.getUUID();
        
    }

    @Override
    public long getMailboxId() {
        throw new UnsupportedOperationException("Please use getMailboxUUID for this implementation");
    }

    public String getMailboxUUID() {
        return uuid;
    }
    
    @Override
    protected MailboxMembership copyMessage(MailboxMembership originalMessage, long uid) {
        MailboxMembership newRow = new JCRMailboxMembership(uuid, uid, (JCRMailboxMembership) originalMessage, log);
        return newRow;
    }

    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        return new JCRHeader(lineNumber, name, value, log);
    }

    @Override
    protected MailboxMembership createMessage(Date internalDate, long uid, int size, int bodyStartOctet, byte[] document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) {
        final List<JCRHeader> jcrHeaders = new ArrayList<JCRHeader>(headers.size());
        for (Header header: headers) {
            jcrHeaders.add((JCRHeader) header);
        }
        final MailboxMembership message = new JCRMailboxMembership(getMailboxUUID(), uid, internalDate, 
                size, flags, document, bodyStartOctet, jcrHeaders, propertyBuilder, log);
        return message;       
        
    }

    @Override
    protected MessageMapper createMessageMapper(MailboxSession session) throws MailboxException {
        PasswordAwareUser user = (PasswordAwareUser)getMailboxSession().getUser();

        JCRMessageMapper messageMapper = new JCRMessageMapper(getSession(user), getMailboxUUID(), log);
        return messageMapper;
    }

    @Override
    protected Mailbox getMailboxRow() throws MailboxException {
        PasswordAwareUser user = (PasswordAwareUser)getMailboxSession().getUser();
        final JCRMailboxMapper mapper = new JCRMailboxMapper(getSession(user), log);
        return mapper.findMailboxByUUID(getMailboxUUID());
    }

    @Override
    protected Mailbox reserveNextUid() throws MailboxException {
        PasswordAwareUser user = (PasswordAwareUser)getMailboxSession().getUser();
        final JCRMailboxMapper mapper = new JCRMailboxMapper(getSession(user), log);
        return mapper.consumeNextUid(getMailboxUUID());
    }

    /**
     * Return a new JCR Session for the given MailboxSession
     * 
     * @param s
     * @return session
     * @throws MailboxException
     */
    protected Session getSession(PasswordAwareUser user) throws SubscriptionException {
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

    protected Repository getRepository() {
        return repository;
    }
}
