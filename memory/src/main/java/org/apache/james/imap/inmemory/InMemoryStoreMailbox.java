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

package org.apache.james.imap.inmemory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.Flags;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

public class InMemoryStoreMailbox extends StoreMailbox implements MessageMapper {
    
    private static final int INITIAL_SIZE = 256;
    private Map<Long, MailboxMembership> membershipByUid;
    private InMemoryMailbox mailbox;

    public InMemoryStoreMailbox(InMemoryMailbox mailbox) {
        super(mailbox);
        this.mailbox = mailbox;
        this.membershipByUid = new ConcurrentHashMap<Long, MailboxMembership>(INITIAL_SIZE);
    }

    @Override
    protected MailboxMembership copyMessage(MailboxMembership originalMessage, long uid) {
        return new SimpleMailboxMembership(mailboxId, uid, (SimpleMailboxMembership) originalMessage);
    }

    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        return new SimpleHeader(name, lineNumber, value);
    }

    @Override
    protected MailboxMembership createMessage(Date internalDate, long uid, int size, int bodyStartOctet, 
            byte[] document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) {
        return new SimpleMailboxMembership(internalDate, uid, size, bodyStartOctet, document, flags, headers, propertyBuilder, mailboxId);
    }

    @Override
    protected MessageMapper createMessageMapper() {
        return this;
    }

    @Override
    protected Mailbox getMailboxRow() throws MailboxException {
        return mailbox;
    }

    @Override
    protected Mailbox reserveNextUid() throws MailboxException {
        mailbox.consumeUid();
        return mailbox;
    }

    public void begin() throws StorageException {
    }

    public void commit() throws StorageException {
    }

    public long countMessagesInMailbox() throws StorageException {
        return membershipByUid.size();
    }

    public long countUnseenMessagesInMailbox() throws StorageException {
        long count = 0;
        for(MailboxMembership member:membershipByUid.values()) {
            if (!member.isSeen()) {
                count++;
            }
        }
        return count;
    }

    public void delete(MailboxMembership message) throws StorageException {
        membershipByUid.remove(message.getUid());
    }

    public List<MailboxMembership> findInMailbox(MessageRange set) throws StorageException {
        return null;
    }

    public List<MailboxMembership> findMarkedForDeletionInMailbox(MessageRange set) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<MailboxMembership> findRecentMessagesInMailbox() throws StorageException {
        final List<MailboxMembership> results = new ArrayList<MailboxMembership>();
        for(MailboxMembership member:membershipByUid.values()) {
            if (member.isRecent()) {
                results.add(member);
            }
        }
        return results;
    }

    public List<MailboxMembership> findUnseenMessagesInMailboxOrderByUid() throws StorageException {
        final List<MailboxMembership> results = new ArrayList<MailboxMembership>();
        for(MailboxMembership member:membershipByUid.values()) {
            if (member.isRecent()) {
                results.add(member);
            }
        }
        return results;
    }

    public void save(MailboxMembership message) throws StorageException {
        // TODO Auto-generated method stub
        
    }

    public List<MailboxMembership> searchMailbox(SearchQuery query) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }


}
