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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.james.imap.mailbox.MailboxSession;

/**
 * Utilities for JPA
 *
 */
public class JPAUtils {

    public static final String ENTITYMANAGERS = "ENTITYMANAGERS";
    
    /**
     * Return a List of EntityManagers for the given MailboxSession
     * 
     * @param session
     * @return managerList
     */
    @SuppressWarnings("unchecked")
    public static List<EntityManager> getEntityManagers(MailboxSession session) {
        List<EntityManager> managers = null;
        if (session != null) {
            managers = (List<EntityManager>) session.getAttributes().get(ENTITYMANAGERS);
        }
        if (managers == null) {
            managers = new ArrayList<EntityManager>();
        }
        return managers;
    }

    /**
     * Add the EntityManager to the MailboxSession
     * 
     * @param session
     * @param manager
     */
    @SuppressWarnings("unchecked")
    public static void addEntityManager(MailboxSession session, EntityManager manager) {
        if (session != null) {
            List<EntityManager> managers = (List<EntityManager>) session.getAttributes().get(ENTITYMANAGERS); 
            if (managers == null) {
                managers = new ArrayList<EntityManager>();
            }
            managers.add(manager);
            session.getAttributes().put(ENTITYMANAGERS, managers); 
        }
    }
}
