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

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.PasswordAwareUser;

/**
 * JCR based SubscriptionManager which use the same username and password to
 * obtain a JCR Session for every MailboxSession
 * 
 * 
 */
public class JCRGlobalUserSubscriptionManager extends JCRSubscriptionManager {

    private String username;
    private String password;

    public JCRGlobalUserSubscriptionManager(final Repository repository, final String workspace, final String username, final String password) {
        super(repository, workspace);
        this.username = username;
        this.password = password;
    }

    @Override
    protected Session getSession(PasswordAwareUser user) throws SubscriptionException {
        try {
            return getRepository().login(new SimpleCredentials(username, password.toCharArray()), getWorkspace());
        } catch (LoginException e) {
            throw new SubscriptionException(HumanReadableText.INVALID_LOGIN, e);
        } catch (NoSuchWorkspaceException e) {
            throw new SubscriptionException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);
        } catch (RepositoryException e) {
            throw new SubscriptionException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);

        }
    }

}
