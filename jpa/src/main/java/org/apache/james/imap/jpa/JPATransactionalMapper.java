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

    private final EntityManagerFactory factory;
    private EntityManager entityManager;
    public JPATransactionalMapper(final EntityManagerFactory factory) {
        this.factory = factory;
    }

    /**
     * Return the currently used {@link EntityManager}. If the currently used {@link EntityManager} is null or is closed a new will get obtained from the {@link EntityManagerFactory}
     * 
     * @return entitymanger
     */
    protected EntityManager getManager() {
        if (entityManager == null || entityManager.isOpen() == false) {
            entityManager = factory.createEntityManager();
        }
        return entityManager;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.AbstractTransactionalMapper#begin()
     */
    protected void begin() throws MailboxException {
        try {
            getManager().getTransaction().begin();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.START_TRANSACTION_FAILED, e);
        }
    }
    

    /**
     * Commit the Transaction and close the EntityManager
     */
    protected void commit() throws MailboxException {
        try {
            getManager().getTransaction().commit();
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
            getManager().getTransaction().rollback();
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.TransactionalMapper#dispose()
     */
    public void dispose() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

}
