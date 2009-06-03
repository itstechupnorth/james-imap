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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;

public class InMemoryMailboxManager extends StoreMailboxManager implements MailboxMapper {

    private static final int INITIAL_SIZE = 128;
    private Map<Long, InMemoryMailbox> mailboxesById;
    
    public InMemoryMailboxManager(Authenticator authenticator, Subscriber subscriber) {
        super(authenticator, subscriber);
        mailboxesById = new ConcurrentHashMap<Long, InMemoryMailbox>(INITIAL_SIZE);
    }

    @Override
    protected StoreMailbox createMailbox(Mailbox mailboxRow) {
        final InMemoryStoreMailbox storeMailbox = new InMemoryStoreMailbox((InMemoryMailbox)mailboxRow);
        return storeMailbox;
    }

    @Override
    protected MailboxMapper createMailboxMapper() {
        return this;
    }

    @Override
    protected void doCreate(String namespaceName) throws StorageException {
        InMemoryMailbox mailbox = new InMemoryMailbox(randomId(), namespaceName, randomUidValidity());
        save(mailbox);
    }

    public void begin() throws StorageException {}

    public void commit() throws StorageException {}

    public long countMailboxesWithName(String name) throws StorageException {
        int total = 0;
        for (final InMemoryMailbox mailbox:mailboxesById.values()) {
            if (mailbox.getName().equals(name)) {
                total++;
            }
        }
        return total;
    }

    public void delete(Mailbox mailbox) throws StorageException {
        mailboxesById.remove(mailbox.getMailboxId());
    }

    public void deleteAll() throws StorageException {
        mailboxesById.clear();
    }

    public Mailbox findMailboxById(long mailboxId) throws StorageException, MailboxNotFoundException {
        return mailboxesById.get(mailboxesById);
    }

    public Mailbox findMailboxByName(String name) throws StorageException, MailboxNotFoundException {
        Mailbox result = null;
        for (final InMemoryMailbox mailbox:mailboxesById.values()) {
            if (mailbox.getName().equals(name)) {
                result = mailbox;
                break;
            }
        }
        return result;
    }

    public List<Mailbox> findMailboxWithNameLike(String name) throws StorageException {
        final String regex = name.replace("%", ".*");
        List<Mailbox> results = new ArrayList<Mailbox>();
        for (final InMemoryMailbox mailbox:mailboxesById.values()) {
            if (mailbox.getName().matches(regex)) {
                results.add(mailbox);
            }
        }
        return results;
    }

    public boolean existsMailboxStartingWith(String mailboxName) throws StorageException {
        boolean result = false;
        for (final InMemoryMailbox mailbox:mailboxesById.values()) {
            if (mailbox.getName().startsWith(mailboxName)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public void save(Mailbox mailbox) throws StorageException {
        mailboxesById.put(mailbox.getMailboxId(), (InMemoryMailbox) mailbox);
    }

}
