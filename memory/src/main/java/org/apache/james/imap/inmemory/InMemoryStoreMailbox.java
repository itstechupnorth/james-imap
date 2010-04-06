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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.Flags;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.MailboxMembershipComparator;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

public class InMemoryStoreMailbox extends StoreMailbox<Long> implements MessageMapper<Long> {
    
    private static final int INITIAL_SIZE = 256;
    private Map<Long, MailboxMembership<Long>> membershipByUid;
    private InMemoryMailbox mailbox;

    public InMemoryStoreMailbox(InMemoryMailbox mailbox, MailboxSession session) {
        super(mailbox,session);
        this.mailbox = mailbox;
        this.membershipByUid = new ConcurrentHashMap<Long, MailboxMembership<Long>>(INITIAL_SIZE);
    }

    @Override
    protected MailboxMembership<Long> copyMessage(MailboxMembership<Long> originalMessage, long uid) {
        return new SimpleMailboxMembership(mailboxId, uid, (SimpleMailboxMembership) originalMessage);
    }

    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        return new SimpleHeader(name, lineNumber, value);
    }

    @Override
    protected MailboxMembership<Long> createMessage(Date internalDate, long uid, int size, int bodyStartOctet, 
            InputStream  document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] byteContent;
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = document.read(buf)) != -1) {
                out.write(buf, 0, i);
            }
            byteContent = out.toByteArray();
            if (out != null)
                out.close();

        } catch (Exception e) {
            e.printStackTrace();
            byteContent = new byte[0];
        }
        System.out.println("byte=" + new String(byteContent));
        return new SimpleMailboxMembership(internalDate, uid, size, bodyStartOctet, byteContent, flags, headers, propertyBuilder, mailboxId);


    }

    @Override
    protected MessageMapper<Long> createMessageMapper(MailboxSession session) {
        return this;
    }

    @Override
    protected Mailbox<Long> getMailboxRow() throws MailboxException {
        return mailbox;
    }

    @Override
    protected Mailbox<Long> reserveNextUid() throws MailboxException {
        mailbox.consumeUid();
        return mailbox;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox() throws StorageException {
        return membershipByUid.size();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countUnseenMessagesInMailbox()
     */
    public long countUnseenMessagesInMailbox() throws StorageException {
        long count = 0;
        for(MailboxMembership<Long> member:membershipByUid.values()) {
            if (!member.isSeen()) {
                count++;
            }
        }
        return count;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#delete(org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void delete(MailboxMembership<Long> message) throws StorageException {
        membershipByUid.remove(message.getUid());
    }

    @SuppressWarnings("unchecked")
    public List<MailboxMembership<Long>> findInMailbox(MessageRange set) throws StorageException {
        final List<MailboxMembership<Long>> results;
        final MessageRange.Type type = set.getType();
        switch (type) {
            case ALL:
                results = new ArrayList<MailboxMembership<Long>>(membershipByUid.values());
                break;
            case FROM:
                results = new ArrayList<MailboxMembership<Long>>(membershipByUid.values());
                for (final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
                   if (it.next().getUid()< set.getUidFrom()) {
                       it.remove(); 
                   }
                }
                break;
            case RANGE:
                results = new ArrayList<MailboxMembership<Long>>(membershipByUid.values());
                for (final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
                   final long uid = it.next().getUid();
                if (uid<set.getUidFrom() || uid>set.getUidTo()) {
                       it.remove(); 
                   }
                }
                break;
            case ONE:
                results  = new ArrayList<MailboxMembership<Long>>(1);
                final MailboxMembership member = membershipByUid.get(set.getUidFrom());
                if (member != null) {
                    results.add(member);
                }
                break;
            default:
                results = new ArrayList<MailboxMembership<Long>>();
                break;
        }
        Collections.sort(results, MailboxMembershipComparator.INSTANCE);
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findMarkedForDeletionInMailbox(org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership<Long>> findMarkedForDeletionInMailbox(MessageRange set) throws StorageException {
        final List<MailboxMembership<Long>> results = findInMailbox(set);
        for(final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
            if (!it.next().isDeleted()) {
                it.remove();
            }
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findRecentMessagesInMailbox()
     */
    public List<MailboxMembership<Long>> findRecentMessagesInMailbox() throws StorageException {
        final List<MailboxMembership<Long>> results = new ArrayList<MailboxMembership<Long>>();
        for(MailboxMembership<Long> member:membershipByUid.values()) {
            if (member.isRecent()) {
                results.add(member);
            }
        }
        Collections.sort(results, MailboxMembershipComparator.INSTANCE);

        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findUnseenMessagesInMailbox()
     */
    public List<MailboxMembership<Long>> findUnseenMessagesInMailbox() throws StorageException {
        final List<MailboxMembership<Long>> results = new ArrayList<MailboxMembership<Long>>();
        for(MailboxMembership<Long> member:membershipByUid.values()) {
            if (!member.isSeen()) {
                results.add(member);
            }
        }
        Collections.sort(results, MailboxMembershipComparator.INSTANCE);
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#save(org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void save(MailboxMembership<Long> message) throws StorageException {
        membershipByUid.put(message.getUid(), message);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache.james.imap.mailbox.SearchQuery)
     */
    public List<MailboxMembership<Long>> searchMailbox(SearchQuery query) throws StorageException {
        return new ArrayList<MailboxMembership<Long>>(membershipByUid.values());
    }


    /**
     * There is no really Transaction handling here.. Just run it 
     */
    public void execute(Transaction transaction) throws MailboxException {
        transaction.run();
    }
}
