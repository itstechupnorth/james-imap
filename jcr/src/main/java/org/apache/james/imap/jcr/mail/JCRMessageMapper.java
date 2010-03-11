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
package org.apache.james.imap.jcr.mail;

import java.util.List;

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.transaction.NonTransactionalMapper;

public class JCRMessageMapper extends NonTransactionalMapper implements MessageMapper{


    private final Session session;
    private final Log logger;

    public JCRMessageMapper(final Session session, final Log logger) {
        this.session = session;
        this.logger = logger;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox() throws StorageException {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countUnseenMessagesInMailbox()
     */
    public long countUnseenMessagesInMailbox() throws StorageException {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#delete(org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void delete(MailboxMembership message) throws StorageException {
        // TODO Auto-generated method stub
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findInMailbox(org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership> findInMailbox(MessageRange set) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findMarkedForDeletionInMailbox(org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership> findMarkedForDeletionInMailbox(MessageRange set) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findRecentMessagesInMailbox()
     */
    public List<MailboxMembership> findRecentMessagesInMailbox() throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findUnseenMessagesInMailboxOrderByUid()
     */
    public List<MailboxMembership> findUnseenMessagesInMailboxOrderByUid() throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#save(org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void save(MailboxMembership message) throws StorageException {
        // TODO Auto-generated method stub
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache.james.imap.mailbox.SearchQuery)
     */
    public List<MailboxMembership> searchMailbox(SearchQuery query) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }
}
