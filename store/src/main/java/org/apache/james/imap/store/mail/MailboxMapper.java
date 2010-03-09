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
package org.apache.james.imap.store.mail;

import java.util.List;

import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.TransactionalMapper;

/**
 * Mapper for {@link Mailbox}
 *
 */
public interface MailboxMapper extends TransactionalMapper{
    
    /**
     * Save the give {@link Mailbox} to the underlying storage
     * 
     * @param mailbox
     * @throws StorageException
     */
    public abstract void save(Mailbox mailbox) throws StorageException;

    /**
     * Return the {@link Mailbox} for the given name
     * 
     * @param name 
     * @return mailbox
     * @throws StorageException
     * @throws MailboxNotFoundException
     */
    public abstract Mailbox findMailboxByName(String name)
            throws StorageException, MailboxNotFoundException;

    /**
     * Return if the given {@link Mailbox} has children
     * 
     * @param mailboxName not null
     * @return true when the mailbox has children, false otherwise
     * @throws StorageException
     */
    public abstract boolean existsMailboxStartingWith(String mailboxName) throws StorageException;
    
    /**
     * Delete the given {@link Mailbox} from the underlying storage
     * 
     * @param mailbox
     * @throws StorageException
     */
    public abstract void delete(Mailbox mailbox) throws StorageException;

    /**
     * Return a List of {@link Mailbox} which name is like the given name
     * 
     * @param name
     * @return mailboxList
     * @throws StorageException
     */
    public abstract List<Mailbox> findMailboxWithNameLike(String name)
            throws StorageException;

    /**
     * Delete all {@link Mailbox} objects from the underlying storage
     * 
     * @throws StorageException
     */
    public abstract void deleteAll() throws StorageException;

    /**
     * Return the count of {@link Mailbox} objects with the given name
     * 
     * @param name
     * @return count
     * @throws StorageException
     */
    public abstract long countMailboxesWithName(String name)
            throws StorageException;

    /**
     * Return the {@link Mailbox} for the given id
     * 
     * @param mailboxId
     * @return mailbox
     * @throws StorageException
     * @throws MailboxNotFoundException
     */
    public abstract Mailbox findMailboxById(long mailboxId)
            throws StorageException, MailboxNotFoundException;
}