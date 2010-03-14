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

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class JCRSubscriptionManager extends StoreSubscriptionManager {
    private Log logger = LogFactory.getLog(JCRSubscriptionManager.class);

    public final static String MAPPER = "SUBSCRIPTION_MAPPER";
    private final Repository repository;
    private String workspace;

    public JCRSubscriptionManager(final Repository repository, final String workspace) {
        super();
        this.workspace = workspace;
        this.repository = repository;
    }

    @Override
    protected SubscriptionMapper createMapper(MailboxSession session) throws SubscriptionException {
        PasswordAwareUser pUser = (PasswordAwareUser) session.getUser();
        
        // check if we have already a mapper for the session
        JCRSubscriptionMapper mapper = (JCRSubscriptionMapper) session.getAttributes().get(MAPPER);
        if (mapper == null) {
            // no mapper found so create one an store it
            mapper = new JCRSubscriptionMapper(getSession(pUser), logger);
            session.getAttributes().put(MAPPER, mapper);
        }
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

    @Override
    protected void onLogout(MailboxSession session) {
        JCRSubscriptionMapper mapper = (JCRSubscriptionMapper) session.getAttributes().get(MAPPER);
        if (mapper != null) {
            mapper.destroy();
        }
    }
}
