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
 * Make sure the {@link EntityManager} is always the same till the {@link MailboxSession} change
 *
 */
public class MailboxSessionEntityManagerFactory {

    private EntityManagerFactory factory;
    private final static String ENTITYMANAGER = "ENTITYMANAGER";
    
    public MailboxSessionEntityManagerFactory(EntityManagerFactory factory) {
        this.factory = factory;
    }
    
    /**
     * Return the {@link EntityManager} for this session. If not exists create one and save it in the session. If one is found in the session return it
     * 
     * @param session
     * @return manager
     */
    public EntityManager getEntityManager(MailboxSession session) {
        EntityManager manager = (EntityManager) session.getAttributes().get(ENTITYMANAGER);
        if (manager == null || manager.isOpen() == false) {
            manager = factory.createEntityManager();
            session.getAttributes().put(ENTITYMANAGER, manager);
            
        } 
        
        return manager;
    }
    
    /**
     * Close the {@link EntityManager} stored in the session if one exists and is open
     * 
     * @param session
     */
    public void closeEntityManager(MailboxSession session) {
        if (session != null) {
            EntityManager manager = (EntityManager) session.getAttributes().remove(ENTITYMANAGER);
            if ( manager != null && manager.isOpen()) {
                manager.close();
            }
        } 
    }
}
