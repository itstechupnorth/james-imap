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

package org.apache.james.imap.jpa.mail;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jpa.JPATransactionalMapper;
import org.apache.james.imap.jpa.mail.model.JPAMailbox;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;

/**
 * Data access management for mailbox.
 */
public abstract class JPAMailboxMapper extends JPATransactionalMapper implements MailboxMapper<Long> {

    private static final char SQL_WILDCARD_CHAR = '%';
    
    public JPAMailboxMapper(EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#hasChildren
     */
    public boolean existsMailboxStartingWith(String mailboxName) throws StorageException {
        
        final String name = mailboxName + SQL_WILDCARD_CHAR; 
        final Long numberOfChildMailboxes = (Long) entityManager.createNamedQuery("countMailboxesWithNameLike").setParameter("nameParam", name).getSingleResult();
        return numberOfChildMailboxes != null && numberOfChildMailboxes > 0;
    }



    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#save(Mailbox)
     */
    public void save(Mailbox<Long> mailbox) throws StorageException {
        try {
            entityManager.persist(mailbox);
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxByName(java.lang.String)
     */
    public Mailbox<Long> findMailboxByName(String name) throws StorageException, MailboxNotFoundException {
        try {
            return (JPAMailbox) entityManager.createNamedQuery("findMailboxByName").setParameter("nameParam", name).getSingleResult();
        } catch (NoResultException e) {
            throw new MailboxNotFoundException(name);
            
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#delete(Mailbox)
     */
    public void delete(Mailbox<Long> mailbox) throws StorageException {
        try {  
            entityManager.remove(mailbox);
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.DELETED_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxWithNameLike(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public List<Mailbox<Long>> findMailboxWithNameLike(String name) throws StorageException {
        try {
            return entityManager.createNamedQuery("findMailboxWithNameLike").setParameter("nameParam", SQL_WILDCARD_CHAR + name + SQL_WILDCARD_CHAR).getResultList();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#deleteAll()
     */
    public void deleteAll() throws StorageException {
        try {
            entityManager.createNamedQuery("deleteAll").executeUpdate();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.DELETED_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#countMailboxesWithName(java.lang.String)
     */
    public long countMailboxesWithName(String name) throws StorageException {
        try {
            return (Long) entityManager.createNamedQuery("countMailboxesWithName").setParameter("nameParam", name).getSingleResult();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxById(long)
     */
    public Mailbox<Long> findMailboxById(Long mailboxId) throws StorageException, MailboxNotFoundException  {
        try {
            return (JPAMailbox) entityManager.createNamedQuery("findMailboxById").setParameter("idParam", mailboxId).getSingleResult();
        } catch (NoResultException e) {
            throw new MailboxNotFoundException(mailboxId);   
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        } 
    }



    /*
     * 
     */
    public Mailbox<Long> consumeNextUid(Long mailboxId) throws StorageException, MailboxNotFoundException {
        try {
            return doConsumeNextUid(mailboxId);
        } catch (NoResultException e) {
            throw new MailboxNotFoundException(mailboxId);
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.COMSUME_UID_FAILED, e);
        } 
    }

    /** Locking is required and is implementation specific */
    protected abstract JPAMailbox doConsumeNextUid(long mailboxId) throws PersistenceException;
}
