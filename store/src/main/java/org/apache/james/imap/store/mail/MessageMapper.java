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

import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.mail.model.Document;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.transaction.TransactionalMapper;

/**
 * Maps {@link Document} in a {@link Mailbox}. A {@link MessageMapper} has a lifecycle from the start of a request 
 * to the end of the request.
 */
public interface MessageMapper<Id> extends TransactionalMapper {

    /**
     * Return a List of {@link MailboxMembership} which represent the given {@link MessageRange}
     * The list must be ordered by the {@link Document} uid
     * 
     * @param set
     * @return list
     * @throws StorageException
     */
    public abstract List<MailboxMembership<Id>> findInMailbox(Id mailboxId, MessageRange set)
            throws StorageException;

    /**
     * Return a List of {@link MailboxMembership} for the given {@link MessageRange} which are marked for deletion
     * The list must be ordered by the {@link Document} uid
     * @param set 
     * @return list
     * @throws StorageException
     */
    public abstract List<MailboxMembership<Id>> findMarkedForDeletionInMailbox(
            Id mailboxId, final MessageRange set)
            throws StorageException;

    /**
     * Return the count of messages in the mailbox
     * 
     * @return count
     * @throws StorageException
     */
    public abstract long countMessagesInMailbox(Id mailboxId)
            throws StorageException;

    /**
     * Return the count of unseen messages in the mailbox
     * 
     * @return unseenCount
     * @throws StorageException
     */
    public abstract long countUnseenMessagesInMailbox(Id mailboxId)
            throws StorageException;

    /**
     * Return a List of {@link MailboxMembership} which matched the {@link SearchQuery}
     * The list must be ordered by the {@link Document} uid
     * @param query
     * @return
     * @throws StorageException
     */
    public abstract List<MailboxMembership<Id>> searchMailbox(Id mailboxId, SearchQuery query) throws StorageException;

    /**
     * Delete the given {@link MailboxMembership}
     * 
     * @param message
     * @throws StorageException
     */
    public abstract void delete(Id mailboxId, MailboxMembership<Id> message) throws StorageException;

    /**
     * Return a List of {@link MailboxMembership} which are unseen. 
     * The list must be ordered by the {@link Document} uid.
     * If a limit was given the list will maximal be the size of the limit. Id a 
     * limit smaller then 1 is given the List must contain all messages
     * 
     * @param mailboxId
     * @param limit
     * @return list
     * @throws StorageException
     */
    public abstract List<MailboxMembership<Id>> findUnseenMessagesInMailbox(Id mailboxId, int limit) throws StorageException;

    /**
     * Return a List of {@link MailboxMembership} which are recent.
     * The list must be ordered by the {@link Document} uid. 
     * If a limit was given the list will maximal be the size of the limit. Id a 
     * limit smaller then 1 is given the List must contain all messages
     * 
     * @param mailboxId
     * @param limit
     * @return recentList
     * @throws StorageException
     */
    public abstract List<MailboxMembership<Id>> findRecentMessagesInMailbox(Id mailboxId, int limit) throws StorageException;


    /**
     * Save the given {@link MailboxMembership} to the underlying storage
     * 
     * @param message
     * @throws StorageException
     */
    public abstract void save(Id mailboxId, MailboxMembership<Id> message) throws StorageException;

}