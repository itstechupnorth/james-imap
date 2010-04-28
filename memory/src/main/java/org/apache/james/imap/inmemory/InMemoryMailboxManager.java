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

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.TransactionalMapper;

public class InMemoryMailboxManager extends StoreMailboxManager<Long> implements MailboxMapper<Long> {

    private static final int INITIAL_SIZE = 128;
    private Map<Long, InMemoryMailbox> mailboxesById;
    private Map<String, InMemoryStoreMailbox> storeMailboxByName;
    private Map<Long, String> idNameMap;
    private MailboxSession session;

    public InMemoryMailboxManager(Authenticator authenticator, Subscriber subscriber) {
        super(authenticator, subscriber);
        mailboxesById = new ConcurrentHashMap<Long, InMemoryMailbox>(INITIAL_SIZE);
        storeMailboxByName = new ConcurrentHashMap<String, InMemoryStoreMailbox>(INITIAL_SIZE);
        idNameMap = new ConcurrentHashMap<Long, String>(INITIAL_SIZE);
    }

    @Override
    protected StoreMailbox<Long> createMailbox(MailboxEventDispatcher dispatcher, Mailbox<Long> mailboxRow) {
        InMemoryStoreMailbox storeMailbox = storeMailboxByName.get(mailboxRow.getName());
        if (storeMailbox == null) {
            storeMailbox = new InMemoryStoreMailbox(dispatcher, (InMemoryMailbox)mailboxRow);
            storeMailboxByName.put(mailboxRow.getName(), storeMailbox);
        }
        
        return storeMailbox;
    }

    @Override
    protected MailboxMapper<Long> createMailboxMapper(MailboxSession session) {
        this.session = session;
        return this;
    }

    @Override
    protected void doCreate(String namespaceName, MailboxSession session) throws StorageException {
        InMemoryMailbox mailbox = new InMemoryMailbox(randomId(), namespaceName, randomUidValidity());
        idNameMap.put(mailbox.getMailboxId(), mailbox.getName());
        save(mailbox);
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#countMailboxesWithName(java.lang.String)
     */
    public long countMailboxesWithName(String name) throws StorageException {
        int total = 0;
        for (final InMemoryMailbox mailbox:mailboxesById.values()) {
            if (mailbox.getName().equals(name)) {
                total++;
            }
        }
        return total;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#delete(org.apache.james.imap.store.mail.model.Mailbox)
     */
    public void delete(Mailbox<Long> mailbox) throws StorageException {
        mailboxesById.remove(mailbox.getMailboxId());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#deleteAll()
     */
    public void deleteAll() throws StorageException {
        mailboxesById.clear();
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxById(java.lang.Object)
     */
    public Mailbox<Long> findMailboxById(Long mailboxId) throws StorageException, MailboxNotFoundException {
        return mailboxesById.get(mailboxesById);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxByName(java.lang.String)
     */
    public synchronized Mailbox<Long> findMailboxByName(String name) throws StorageException, MailboxNotFoundException {
        Mailbox<Long> result = null;
        for (final InMemoryMailbox mailbox:mailboxesById.values()) {
            if (mailbox.getName().equals(name)) {
                result = mailbox;
                break;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxWithNameLike(java.lang.String)
     */
    public List<Mailbox<Long>> findMailboxWithNameLike(String name) throws StorageException {
        final String regex = name.replace("%", ".*");
        List<Mailbox<Long>> results = new ArrayList<Mailbox<Long>>();
        for (final InMemoryMailbox mailbox:mailboxesById.values()) {
            if (mailbox.getName().matches(regex)) {
                results.add(mailbox);
            }
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#existsMailboxStartingWith(java.lang.String)
     */
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

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#save(org.apache.james.imap.store.mail.model.Mailbox)
     */
    public void save(Mailbox<Long> mailbox) throws StorageException {
        mailboxesById.put(mailbox.getMailboxId(), (InMemoryMailbox) mailbox);
        String name = idNameMap.remove(mailbox.getMailboxId());
        if (name != null) {
            InMemoryStoreMailbox m = storeMailboxByName.remove(name);
            if (m!= null) {
                try {
                    m.getMailboxRow(session).setName(mailbox.getName());
                    storeMailboxByName.put(mailbox.getName(), m);
                } catch (MailboxException e) {
                    throw new StorageException(e.getKey(), e);
                } 
            }
        }
        idNameMap.put(mailbox.getMailboxId(), mailbox.getName());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.TransactionalMapper#execute(org.apache.james.imap.store.transaction.TransactionalMapper.Transaction)
     */
    public void execute(Transaction transaction) throws MailboxException {
        transaction.run();
    }

    /**
     * Delete every Mailbox which exists
     * 
     * @throws MailboxException
     */

    public synchronized void deleteEverything() throws MailboxException {
        final MailboxMapper<Long> mapper = createMailboxMapper(null);
        mapper.execute(new TransactionalMapper.Transaction() {

            public void run() throws MailboxException {
                mapper.deleteAll(); 
            }
            
        });
        storeMailboxByName.clear();
        idNameMap.clear();
    }
    
}
