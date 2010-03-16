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
import java.util.Locale;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.mail.JCRMailboxMapper;
import org.apache.james.imap.mailbox.BadCredentialsException;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.PasswordAwareMailboxSession;
import org.apache.james.imap.store.PasswordAwareUser;
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
public class JCRMailboxManager extends StoreMailboxManager {

    private final Repository repository;
    private final String workspace;
    private final Log logger = LogFactory.getLog(JCRMailboxManager.class);
    public JCRMailboxManager(final Authenticator authenticator, final Subscriber subscriber, final Repository repository, final String workspace) {
        super(authenticator, subscriber);
        this.repository = repository;
        this.workspace = workspace;
    }

    
    @Override
    protected StoreMailbox createMailbox(Mailbox mailboxRow, MailboxSession session) {
        JCRMailbox mailbox = new JCRMailbox((org.apache.james.imap.jcr.mail.model.JCRMailbox) mailboxRow, session, getRepository(), getWorkspace(), getLog());
        return mailbox;
    }

    @Override
    protected MailboxMapper createMailboxMapper(MailboxSession session) throws MailboxException {

        JCRMailboxMapper mapper = new JCRMailboxMapper(getSession(session), getLog());
        return mapper;

    }

    /**
     * Return a new JCR Session for the given MailboxSession
     * 
     * @param s
     * @return session
     * @throws MailboxException
     */
    protected Session getSession(MailboxSession s) throws MailboxException {
        PasswordAwareUser user = (PasswordAwareUser) s.getUser();
        try {
            return repository.login(new SimpleCredentials(user.getUserName(), user.getPassword().toCharArray()), getWorkspace());
        } catch (LoginException e) {
            throw new MailboxException(HumanReadableText.INVALID_LOGIN, e);
        } catch (NoSuchWorkspaceException e) {
            throw new MailboxException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);
        } catch (RepositoryException e) {
            throw new MailboxException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);

        }
    }

    @Override
    protected void doCreate(String namespaceName, MailboxSession session) throws MailboxException {
        final Mailbox mailbox = new org.apache.james.imap.jcr.mail.model.JCRMailbox(namespaceName, randomUidValidity(), logger);
        final JCRMailboxMapper mapper = (JCRMailboxMapper)createMailboxMapper(session);
        mapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                mapper.save(mailbox);
            }

        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.mailbox.MailboxManager#login(java.lang.String,
     * java.lang.String, org.apache.commons.logging.Log)
     */
    public MailboxSession login(String userid, String passwd, Log log) throws BadCredentialsException, MailboxException {
        if (login(userid, passwd)) {
            return new PasswordAwareMailboxSession(randomId(), userid, passwd, log, getDelimiter(), new ArrayList<Locale>());
        } else {
            throw new BadCredentialsException();
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

    @Override
    protected void onLogout(MailboxSession session) {
        
    }
}
