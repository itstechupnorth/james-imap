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

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.apache.james.imap.api.MailboxPath;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jpa.JPATransactionalMapper;
import org.apache.james.imap.jpa.mail.model.JPAMailbox;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxExistsException;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;

/**
 * Data access management for mailbox.
 */
public class JPAMailboxMapper extends JPATransactionalMapper implements MailboxMapper<Long> {

    private static final char SQL_WILDCARD_CHAR = '%';
    private final char delimiter;
    private String lastMailboxName;
    
    public JPAMailboxMapper(EntityManagerFactory entityManagerFactory, char delimiter) {
        super(entityManagerFactory);
        this.delimiter = delimiter;
    }

    /**
     * Commit the transaction. If the commit fails due a conflict in a unique key constraint a {@link MailboxExistsException}
     * will get thrown
     */
    @Override
    protected void commit() throws MailboxException {
        try {
            getEntityManager().getTransaction().commit();
        } catch (PersistenceException e) {
            if (e instanceof EntityExistsException)
                throw new MailboxExistsException(lastMailboxName);
            if (e instanceof RollbackException) {
                Throwable t = e.getCause();
                if (t != null && t instanceof EntityExistsException)
                    throw new MailboxExistsException(lastMailboxName);
            }
            throw new StorageException(HumanReadableText.COMMIT_TRANSACTION_FAILED, e);
        }
    }
    
    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#save(Mailbox)
     */
    public void save(Mailbox<Long> mailbox) throws StorageException {
        try {
            this.lastMailboxName = mailbox.getName();
            getEntityManager().persist(mailbox);
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxByPath(java.lang.String)
     */
    public Mailbox<Long> findMailboxByPath(MailboxPath mailboxPath) throws StorageException, MailboxNotFoundException {
        try {
            if (mailboxPath.getUser() == null) {
                return (JPAMailbox) getEntityManager().createNamedQuery("findMailboxByName").setParameter("nameParam", mailboxPath.getName()).setParameter("namespaceParam", mailboxPath.getNamespace()).getSingleResult();
            } else {
                return (JPAMailbox) getEntityManager().createNamedQuery("findMailboxByNameWithUser").setParameter("nameParam", mailboxPath.getName()).setParameter("namespaceParam", mailboxPath.getNamespace()).setParameter("userParam", mailboxPath.getUser()).getSingleResult();
            }
        } catch (NoResultException e) {
            throw new MailboxNotFoundException(mailboxPath);
            
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#delete(Mailbox)
     */
    public void delete(Mailbox<Long> mailbox) throws StorageException {
        try {  
            getEntityManager().remove(mailbox);
            getEntityManager().createNamedQuery("deleteMessages").setParameter("idParam", mailbox.getMailboxId()).executeUpdate();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.DELETED_FAILED, e);
        } 
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxWithPathLike(org.apache.james.imap.api.MailboxPath)
     */
    @SuppressWarnings("unchecked")
    public List<Mailbox<Long>> findMailboxWithPathLike(MailboxPath path) throws StorageException {
        try {
            if (path.getUser() == null) {
                return getEntityManager().createNamedQuery("findMailboxWithNameLike").setParameter("nameParam", SQL_WILDCARD_CHAR + path.getName() + SQL_WILDCARD_CHAR).setParameter("namespaceParam", path.getNamespace()).getResultList();
            } else {
                return getEntityManager().createNamedQuery("findMailboxWithNameLikeWithUser").setParameter("nameParam", SQL_WILDCARD_CHAR + path.getName() + SQL_WILDCARD_CHAR).setParameter("namespaceParam", path.getNamespace()).setParameter("userParam", path.getUser()).getResultList();
            }
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#deleteAll()
     */
    public void deleteAll() throws StorageException {
        try {
            getEntityManager().createNamedQuery("deleteAll").executeUpdate();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.DELETED_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxById(long)
     */
    public Mailbox<Long> findMailboxById(Long mailboxId) throws StorageException, MailboxNotFoundException  {
        try {
            return (JPAMailbox) getEntityManager().createNamedQuery("findMailboxById").setParameter("idParam", mailboxId).getSingleResult();
        } catch (NoResultException e) {
            throw new MailboxNotFoundException(mailboxId);   
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        } 
    }

    /**
     * @see org.apache.james.imap.store.mail.MailboxMapper#hasChildren(java.lang.String)
     */
    public boolean hasChildren(Mailbox<Long> mailbox) throws StorageException,
            MailboxNotFoundException {
        final String name = mailbox.getName() + delimiter + SQL_WILDCARD_CHAR; 
        final Long numberOfChildMailboxes;
        if (mailbox.getUser() == null) {
            numberOfChildMailboxes = (Long) getEntityManager().createNamedQuery("countMailboxesWithNameLike").setParameter("nameParam", name).setParameter("namespaceParam", mailbox.getNamespace()).getSingleResult();
        } else {
            numberOfChildMailboxes = (Long) getEntityManager().createNamedQuery("countMailboxesWithNameLikeWithUser").setParameter("nameParam", name).setParameter("namespaceParam", mailbox.getNamespace()).setParameter("userParam", mailbox.getUser()).getSingleResult();
        }
        return numberOfChildMailboxes != null && numberOfChildMailboxes > 0;
    }
}
