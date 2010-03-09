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
 * Maps {@link Document} in a {@link Mailbox}.
 */
public interface MessageMapper extends TransactionalMapper {

    /**
     * Return a List of {@link MailboxMembership} which represent the given {@link MessageRange}
     * @param set
     * @return list
     * @throws StorageException
     */
    public abstract List<MailboxMembership> findInMailbox(MessageRange set)
            throws StorageException;

    /**
     * Return a List of {@link MailboxMembership} for the given {@link MessageRange} which are marked for deletion 
     * @param set 
     * @return list
     * @throws StorageException
     */
    public abstract List<MailboxMembership> findMarkedForDeletionInMailbox(
            final MessageRange set)
            throws StorageException;

    /**
     * Return the count of messages in the mailbox
     * 
     * @return count
     * @throws StorageException
     */
    public abstract long countMessagesInMailbox()
            throws StorageException;

    /**
     * Return the count of unseen messages in the mailbox
     * 
     * @return unseenCount
     * @throws StorageException
     */
    public abstract long countUnseenMessagesInMailbox()
            throws StorageException;

    /**
     * Return a List of {@link MailboxMembership} which matched the {@link SearchQuery}
     * @param query
     * @return
     * @throws StorageException
     */
    public abstract List<MailboxMembership> searchMailbox(SearchQuery query) throws StorageException;

    /**
     * Delete the given {@link MailboxMembership}
     * 
     * @param message
     * @throws StorageException
     */
    public abstract void delete(MailboxMembership message) throws StorageException;

    /**
     * Return a List of {@link MailboxMembership} which are unseen. The list is ordered by uid
     * 
     * @return list
     * @throws StorageException
     */
    public abstract List<MailboxMembership> findUnseenMessagesInMailboxOrderByUid() throws StorageException;

    /**
     * Return a List of {@link MailboxMembership} which are recent
     * 
     * @return recentList
     * @throws StorageException
     */
    public abstract List<MailboxMembership> findRecentMessagesInMailbox() throws StorageException;


    /**
     * Save the given {@link MailboxMembership} to the underlying storage
     * 
     * @param message
     * @throws StorageException
     */
    public abstract void save(MailboxMembership message) throws StorageException;

}