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
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.functional.ImapHostSystem;
import org.apache.james.imap.functional.SimpleMailboxManagerProvider;
import org.apache.james.imap.functional.jpa.user.InMemoryUserManager;
import org.apache.james.imap.jpa.JPAMailboxManager;
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
    
    private final JPAMailboxManager mailboxManager;
    private final InMemoryUserManager userManager; 

    public JPAHostSystem() throws Exception {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("openjpa.ConnectionDriverName", "org.h2.Driver");
        properties.put("openjpa.ConnectionURL", "jdbc:h2:mem:imap;DB_CLOSE_DELAY=-1");
        properties.put("openjpa.Log", "JDBC=WARN, SQL=WARN, Runtime=WARN");
        properties.put("openjpa.ConnectionFactoryProperties", "PrettyPrint=true, PrettyPrintLineLength=72");
        properties.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        properties.put("openjpa.MetaDataFactory", "jpa(Types=org.apache.james.imap.jpa.mail.model.Header;org.apache.james.imap.jpa.mail.model.Mailbox;org.apache.james.imap.jpa.mail.model.Message)");
        
        userManager = new InMemoryUserManager();
        final EntityManagerFactory entityManagerFactory = OpenJPAPersistence.getEntityManagerFactory(properties);
        mailboxManager = new JPAMailboxManager(userManager, userManager, entityManagerFactory);
        
        SimpleMailboxManagerProvider provider = new SimpleMailboxManagerProvider();
        final DefaultImapProcessorFactory defaultImapProcessorFactory = new DefaultImapProcessorFactory();
        provider.setMailboxManager(mailboxManager);
        resetUserMetaData();
        defaultImapProcessorFactory.configure(provider);
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
        mailboxManager.deleteEverything();
    }
    
    public void resetUserMetaData() throws Exception {
        File dir = new File(META_DATA_DIRECTORY);
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();
    }

}
