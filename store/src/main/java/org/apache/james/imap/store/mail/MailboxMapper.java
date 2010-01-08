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

public interface MailboxMapper {
    public abstract void begin() throws StorageException;
    
    public abstract void commit() throws StorageException;
    
    public abstract void save(Mailbox mailbox) throws StorageException;

    public abstract Mailbox findMailboxByName(String name)
            throws StorageException, MailboxNotFoundException;

    /**
     * Does the given mailbox have children?
     * @param mailboxName not null
     * @return true when the mailbox has children, false otherwise
     * @throws StorageException
     */
    public abstract boolean existsMailboxStartingWith(String mailboxName) throws StorageException;
    
    public abstract void delete(Mailbox mailbox) throws StorageException;

    public abstract List<Mailbox> findMailboxWithNameLike(String name)
            throws StorageException;

    public abstract void deleteAll() throws StorageException;

    public abstract long countMailboxesWithName(String name)
            throws StorageException;

    public abstract Mailbox findMailboxById(long mailboxId)
            throws StorageException, MailboxNotFoundException;
}