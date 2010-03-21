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
import java.io.InputStreamReader;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.user.JCRSubscriptionMapper;
import org.apache.james.imap.jcr.user.model.JCRSubscription;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.PasswordAwareUser;
import org.apache.james.imap.store.StoreSubscriptionManager;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * JCR implementation of a SubscriptionManager
 * 
 * 
 */
public class JCRSubscriptionManager extends StoreSubscriptionManager implements JCRImapConstants{
    private final Log logger = LogFactory.getLog(JCRSubscriptionManager.class);
    private final Repository repository;
    private final String workspace;
    private final int scaling;

    public JCRSubscriptionManager(final Repository repository, final String workspace, final int scaling) {
        super();
        this.scaling = scaling;
        this.workspace = workspace;
        this.repository = repository;
        registerCnd();
    }


    public JCRSubscriptionManager(final Repository repository, final String workspace) {
        this(repository, workspace, MAX_SCALING);
    }
    
    protected void registerCnd() {
        try {
            Session session = repository.login(getWorkspace());
            // Register the custom node types defined in the CND file
            InputStream is = Thread.currentThread().getContextClassLoader()
                                  .getResourceAsStream("org/apache/james/imap/jcr/imap.cnd");
            CndImporter.registerNodeTypes(new InputStreamReader(is), session);
            session.logout();
        } catch (Exception e) {
            throw new RuntimeException("Unable to register cnd file");
        }    
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
    protected SubscriptionMapper createMapper(MailboxSession session) throws SubscriptionException {
        Session jcrSession = getSession(session);
        JCRUtils.addJCRSession(session, jcrSession);
        
        
        JCRSubscriptionMapper mapper = new JCRSubscriptionMapper(jcrSession, getScaling(), logger);

        return mapper;
    }

    @Override
    protected Subscription createSubscription(MailboxSession session, String mailbox) {
        return new JCRSubscription(session.getUser().getUserName(), mailbox, logger);
    }

    /**
     * Return a new JCR Session for the given MailboxSession
     * 
     * @param s
     * @return session
     * @throws MailboxException
     */
    protected Session getSession(MailboxSession session) throws SubscriptionException {
        PasswordAwareUser user = (PasswordAwareUser) session.getUser();

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
     * Return the JCR Repository 
     * 
     * @return repository
     */
    protected Repository getRepository() {
        return repository;
    }
}
