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

package org.apache.james.imap.jpa.mail.openjpa;

import javax.persistence.EntityManager;

import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.jpa.mail.model.JPAMailbox;

/**
 * OpenJPA implementation of MailboxMapper.
 */
public class OpenJPAMailboxMapper extends JPAMailboxMapper {
    
    public OpenJPAMailboxMapper(EntityManager entityManager) {
        super(entityManager);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.jpa.mail.JPAMailboxMapper#doConsumeNextUid(long)
     */
    public JPAMailbox doConsumeNextUid(long mailboxId) {
        JPAMailbox mailbox = (JPAMailbox) entityManager.createNamedQuery("findMailboxById").setParameter("idParam", mailboxId).getSingleResult();
        mailbox.consumeUid();
        entityManager.persist(mailbox);
        return mailbox;
        
    }
}
