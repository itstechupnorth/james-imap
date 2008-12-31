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

package org.apache.james.imap.jpa.om.openjpa;

import javax.persistence.EntityManager;

import org.apache.james.imap.jpa.map.MailboxMapper;
import org.apache.james.imap.jpa.om.Mailbox;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;

/**
 * Data access management for mailbox.
 */
public class OpenJPAMailboxMapper extends MailboxMapper {
    
    public OpenJPAMailboxMapper(EntityManager entityManager) {
        super(entityManager);
    }

    public Mailbox consumeNextUid(long mailboxId) {
        OpenJPAEntityManager oem = OpenJPAPersistence.cast(entityManager);
        final boolean originalLocking = oem.getOptimistic();
        oem.setOptimistic(false);
        oem.getTransaction().begin();
        try {
            Mailbox mailbox = (Mailbox) entityManager.createNamedQuery("findMailboxById").setParameter("idParam", mailboxId).getSingleResult();
            mailbox.consumeUid();
            oem.persist(mailbox);
            oem.getTransaction().commit();
            return mailbox;
        } finally {
            oem.setOptimistic(originalLocking);
        }
    }
}
