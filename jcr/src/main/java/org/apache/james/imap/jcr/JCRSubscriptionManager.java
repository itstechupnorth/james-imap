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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.user.JCRSubscriptionMapper;
import org.apache.james.imap.jcr.user.model.JCRSubscription;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.mailbox.MailboxSession.User;
import org.apache.james.imap.store.PasswordAwareUser;
import org.apache.james.imap.store.StoreSubscriptionManager;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * JCR implementation of a SubscriptionManager
 * 
 *
 */
public class JCRSubscriptionManager extends StoreSubscriptionManager{

    private final Repository repos;

    public JCRSubscriptionManager(final Repository repos) {
        super();

        this.repos = repos;
    }
    
    @Override
    protected SubscriptionMapper createMapper(User user) throws SubscriptionException{
    	PasswordAwareUser pUser = (PasswordAwareUser) user;
    	
		try {
			JCRSubscriptionMapper mapper = new JCRSubscriptionMapper(repos.login(new SimpleCredentials(pUser.getUserName(), pUser.getPassword().toCharArray())));
	        return mapper;

		} catch (LoginException e) {
			throw new SubscriptionException(HumanReadableText.INVALID_LOGIN, e);
		} catch (RepositoryException e) {
			throw new SubscriptionException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);

		}
    }

    @Override
    protected Subscription createSubscription(User user, String mailbox) {
        return new JCRSubscription(user.getUserName(), mailbox);
    }

}
