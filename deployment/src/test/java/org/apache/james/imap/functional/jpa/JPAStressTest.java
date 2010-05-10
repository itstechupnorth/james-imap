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
package org.apache.james.imap.functional.jpa;

import java.util.HashMap;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.imap.functional.AbstractStressTest;
import org.apache.james.imap.jpa.JPASubscriptionManager;
import org.apache.james.imap.jpa.MailboxSessionEntityManagerFactory;
import org.apache.james.imap.jpa.openjpa.OpenJPAMailboxManager;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.junit.After;
import org.junit.Before;

/**
 * Proof of bug https://issues.apache.org/jira/browse/IMAP-137
 */
public class JPAStressTest extends AbstractStressTest{

    
    private OpenJPAMailboxManager mailboxManager;

    
    @Before
    public void setUp() {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("openjpa.ConnectionDriverName", "org.h2.Driver");
        properties.put("openjpa.ConnectionURL", "jdbc:h2:mem:imap;DB_CLOSE_DELAY=-1");
        properties.put("openjpa.Log", "JDBC=WARN, SQL=WARN, Runtime=WARN");
        properties.put("openjpa.ConnectionFactoryProperties", "PrettyPrint=true, PrettyPrintLineLength=72");
        properties.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        properties.put("openjpa.MetaDataFactory", "jpa(Types=org.apache.james.imap.jpa.mail.model.JPAHeader;" +
                "org.apache.james.imap.jpa.mail.model.JPAMailbox;" +
                "org.apache.james.imap.jpa.mail.model.AbstractJPAMailboxMembership;" +
                "org.apache.james.imap.jpa.mail.model.JPAMailboxMembership;" +
                "org.apache.james.imap.jpa.mail.model.AbstractJPAMessage;" +
                "org.apache.james.imap.jpa.mail.model.JPAMessage;" +
                "org.apache.james.imap.jpa.mail.model.JPAProperty;" +
                "org.apache.james.imap.jpa.user.model.JPASubscription)");
        /*
        // persimistic locking..
        properties.put("openjpa.LockManager", "pessimistic");
        properties.put("openjpa.ReadLockLevel", "read");
        properties.put("openjpa.WriteLockLevel", "write");
        properties.put("openjpa.jdbc.TransactionIsolation", "repeatable-read");
        */
        EntityManagerFactory entityManagerFactory = OpenJPAPersistence.getEntityManagerFactory(properties);
        MailboxSessionEntityManagerFactory emf = new MailboxSessionEntityManagerFactory(entityManagerFactory);
        mailboxManager = new OpenJPAMailboxManager(null, new JPASubscriptionManager(emf), emf);
    }
    
 
    @After
    public void tearDown() {
        MailboxSession session = mailboxManager.createSystemSession("test", new SimpleLog("Test"));
        try {
            mailboxManager.deleteEverything(session);
        } catch (MailboxException e) {
            e.printStackTrace();
        }
        session.close();
    }

    @Override
    protected StoreMailboxManager<?> getMailboxManager() {
        return mailboxManager;
    }
   
}
