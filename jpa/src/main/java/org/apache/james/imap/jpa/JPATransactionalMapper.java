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
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.transaction.AbstractTransactionalMapper;

/**
 * JPA implementation of TransactionMapper. This class is not thread-safe!
 *
 */
public abstract class JPATransactionalMapper extends AbstractTransactionalMapper {

    protected EntityManagerFactory entityManagerFactory;
    protected EntityManager entityManager;
    
    public JPATransactionalMapper(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Return the currently used {@link EntityManager} or a new one if none exists.
     * 
     * @return entitymanger
     */
    public EntityManager getEntityManager() {
        if (entityManager != null)
            return entityManager;
        entityManager = entityManagerFactory.createEntityManager();
        return entityManager;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.AbstractTransactionalMapper#begin()
     */
    protected void begin() throws MailboxException {
        try {
            getEntityManager().getTransaction().begin();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.START_TRANSACTION_FAILED, e);
        }
    }

    /**
     * Commit the Transaction and close the EntityManager
     */
    protected void commit() throws MailboxException {
        try {
            getEntityManager().getTransaction().commit();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.COMMIT_TRANSACTION_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.AbstractTransactionalMapper#rollback()
     */
    protected void rollback() throws MailboxException {
        EntityTransaction transaction = entityManager.getTransaction();
        // check if we have a transaction to rollback
        if (transaction.isActive()) {
            getEntityManager().getTransaction().rollback();
        }
    }

    /**
     * Close open {@link EntityManager}
     */
    public void endRequest() {
        if (entityManager != null) {
            if (entityManager.isOpen())
                entityManager.close();
            entityManager = null;
        }
    }

    
}
