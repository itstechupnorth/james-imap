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
package org.apache.james.imap.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.james.imap.mailbox.MailboxSession;

/**
 * Maintain {@link EntityManager} instances by {@link MailboxSession}. So only one {@link EntityManager} instance is used
 * in a {@link MailboxSession}. 
 *
 */
public class MailboxSessionEntityManagerFactory {

    protected final static String ENTITYMANAGER ="ENTITYMANAGER";
    private final EntityManagerFactory factory;
    
    public MailboxSessionEntityManagerFactory(final EntityManagerFactory factory) {
        this.factory = factory;
    }
    
    
    /**
     * Create a {@link EntityManager} instance of return the one which exists for the {@link MailboxSession} already
     * 
     * @param session
     * @return manager
     */
    public EntityManager createEntityManager(MailboxSession session) {
        EntityManager manager = (EntityManager) session.getAttributes().get(ENTITYMANAGER);
        
        if (manager == null) {
            manager = factory.createEntityManager();
            session.getAttributes().put(ENTITYMANAGER, manager);
        }
        
        return manager;
    }
    
    /**
     * Close the {@link EntityManager} which exists for the {@link MailboxSession}
     * 
     * @param session
     */
    public void closeEntityManager(MailboxSession session) {
        if (session != null) {
            EntityManager manager = (EntityManager) session.getAttributes().remove(ENTITYMANAGER);
            if (manager != null && manager.isOpen()) {
                manager.close();
            }
        }
    }
}
