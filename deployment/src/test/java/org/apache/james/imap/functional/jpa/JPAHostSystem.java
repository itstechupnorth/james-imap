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

import java.io.File;
import java.util.HashMap;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.functional.ImapHostSystem;
import org.apache.james.imap.functional.InMemoryUserManager;
import org.apache.james.imap.jpa.JPAMailboxSessionMapperFactory;
import org.apache.james.imap.jpa.JPASubscriptionManager;
import org.apache.james.imap.jpa.openjpa.OpenJPAMailboxManager;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.test.functional.HostSystem;
import org.apache.openjpa.persistence.OpenJPAPersistence;

public class JPAHostSystem extends ImapHostSystem {

    public static final String META_DATA_DIRECTORY = "target/user-meta-data";

    public static HostSystem build() throws Exception {        
        JPAHostSystem host =  new JPAHostSystem();
        return host;
    }
    
    private final OpenJPAMailboxManager mailboxManager;
    private final InMemoryUserManager userManager; 
    private final EntityManagerFactory entityManagerFactory;

    public JPAHostSystem() throws Exception {
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
        userManager = new InMemoryUserManager();
        entityManagerFactory = OpenJPAPersistence.getEntityManagerFactory(properties);
        JPAMailboxSessionMapperFactory mf = new JPAMailboxSessionMapperFactory(entityManagerFactory);
        mailboxManager = new OpenJPAMailboxManager(mf, userManager, new JPASubscriptionManager(mf));
        
        final DefaultImapProcessorFactory defaultImapProcessorFactory = new DefaultImapProcessorFactory();
        resetUserMetaData();
        defaultImapProcessorFactory.configure(mailboxManager);
        configure(new DefaultImapDecoderFactory().buildImapDecoder(),
                new DefaultImapEncoderFactory().buildImapEncoder(),
                defaultImapProcessorFactory.buildImapProcessor());
    }

    public boolean addUser(String user, String password) {
        userManager.addUser(user, password);
        return true;
    }

    public void resetData() throws Exception {
        resetUserMetaData();
        MailboxSession session = mailboxManager.createSystemSession("test", new SimpleLog("TestLog"));
        mailboxManager.startProcessingRequest(session);
        mailboxManager.deleteEverything(session);
        mailboxManager.endProcessingRequest(session);
        mailboxManager.logout(session, false);
        
    }
    
    public void resetUserMetaData() throws Exception {
        File dir = new File(META_DATA_DIRECTORY);
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();
    }

}
