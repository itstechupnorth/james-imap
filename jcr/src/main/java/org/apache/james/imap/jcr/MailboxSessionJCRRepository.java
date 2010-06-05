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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MailboxSession.User;

/**
 * Manage JCR {@link Session}. It use the username and the password of 
 * the logged in IMAP user to access the {@link Repository}
 *
 */
public class MailboxSessionJCRRepository {
    
    private Repository repository;
    private String workspace;
    
    public MailboxSessionJCRRepository(Repository repository ,String workspace) {
        this.repository = repository;
        this.workspace = workspace;
    }

    /**
     * If no {@link Session} exists for the {@link MailboxSession} one will get created.
     * If one exists it just return the existing.
     * 
     * @param session
     * @return jcrSession
     * @throws RepositoryException
     */
    public Session login(MailboxSession session) throws RepositoryException {
        User user = session.getUser();
        String username = user.getUserName();
        String password = user.getPassword();
        char[] pass = null;
        if (password != null) {
            pass = password.toCharArray();
        }
        return repository.login(new SimpleCredentials(username, pass),
                workspace);
    }

    /**
     * Return the {@link Repository} 
     * 
     * @return repository
     */
    protected Repository getRepository() {
        return repository;
    }
    
    /**
     * Return the workspace
     * 
     * @return workspace
     */
    protected String getWorkspace() {
        return workspace;
    }
}
