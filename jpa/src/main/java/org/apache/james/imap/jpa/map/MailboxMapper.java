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

package org.apache.james.imap.jpa.map;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.james.imap.jpa.om.Mailbox;

/**
 * Data access management for mailbox.
 */
public abstract class MailboxMapper extends Mapper {
    
    public MailboxMapper(EntityManager entityManager) {
        super(entityManager);
    }

    public void save(Mailbox mailbox) {
        entityManager.persist(mailbox);
    }

    public Mailbox findMailboxByName(String name) {
        return (Mailbox) entityManager.createNamedQuery("findMailboxByName").setParameter("nameParam", name).getSingleResult();
    }

    public void delete(Mailbox mailbox) {
        entityManager.remove(mailbox);
    }

    @SuppressWarnings("unchecked")
    public List<Mailbox> findMailboxWithNameLike(String name) {
        return entityManager.createNamedQuery("findMailboxWithNameLike").setParameter("nameParam", name).getResultList();
    }

    public void deleteAll() {
        entityManager.createNamedQuery("deleteAll").executeUpdate();
    }

    public long countMailboxesWithName(String name) {
        return (Long) entityManager.createNamedQuery("countMailboxesWithName").setParameter("nameParam", name).getSingleResult();
    }

    public Mailbox findMailboxById(long mailboxId) {
        return (Mailbox) entityManager.createNamedQuery("findMailboxById").setParameter("idParam", mailboxId).getSingleResult();
    }

    /** Locking is required and is implementation specific */
    public abstract Mailbox consumeNextUid(long mailboxId);
}
