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
package org.apache.james.imap.jcr;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.functional.ImapHostSystem;
import org.apache.james.imap.functional.InMemoryUserManager;
import org.apache.james.imap.jcr.GlobalMailboxSessionJCRRepository;
import org.apache.james.imap.jcr.JCRMailboxManager;
import org.apache.james.imap.jcr.JCRMailboxSessionMapperFactory;
import org.apache.james.imap.jcr.JCRSubscriptionManager;
import org.apache.james.imap.jcr.JCRUtils;
import org.apache.james.imap.jcr.JCRVmNodeLocker;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.test.functional.HostSystem;
import org.xml.sax.InputSource;

public class JCRHostSystem extends ImapHostSystem{

    public static HostSystem build() throws Exception { 
        return new JCRHostSystem();
    }
    
    private final JCRMailboxManager mailboxManager;
    private final InMemoryUserManager userManager; 

    private static final String JACKRABBIT_HOME = "target/jackrabbit";
    public static final String META_DATA_DIRECTORY = "target/user-meta-data";
    private RepositoryImpl repository;
    
    public JCRHostSystem() throws Exception {

        delete(new File(JACKRABBIT_HOME));
        
        try {
            
            String user = "user";
            String pass = "pass";
            String workspace = null;
            RepositoryConfig config = RepositoryConfig.create(new InputSource(this.getClass().getClassLoader().getResourceAsStream("test-repository.xml")), JACKRABBIT_HOME);
            repository =  RepositoryImpl.create(config);
            GlobalMailboxSessionJCRRepository sessionRepos = new GlobalMailboxSessionJCRRepository(repository, workspace, user, pass);
            
            // Register imap cnd file
            JCRUtils.registerCnd(repository, workspace, user, pass);
            
            userManager = new InMemoryUserManager();
            JCRMailboxSessionMapperFactory mf = new JCRMailboxSessionMapperFactory(sessionRepos, new JCRVmNodeLocker());

            //TODO: Fix the scaling stuff so the tests will pass with max scaling too
            mailboxManager = new JCRMailboxManager(mf, userManager, new JCRSubscriptionManager(mf));
            final DefaultImapProcessorFactory defaultImapProcessorFactory = new DefaultImapProcessorFactory();
            resetUserMetaData();
            MailboxSession session = mailboxManager.createSystemSession("test", new SimpleLog("TestLog"));
            mailboxManager.startProcessingRequest(session);
            //mailboxManager.deleteEverything(session);
            mailboxManager.endProcessingRequest(session);
            mailboxManager.logout(session, false);
            
            defaultImapProcessorFactory.configure(mailboxManager);
            configure(new DefaultImapDecoderFactory().buildImapDecoder(), new DefaultImapEncoderFactory().buildImapEncoder(), defaultImapProcessorFactory.buildImapProcessor());
        } catch (Exception e) {
            shutdownRepository();
            throw e;
        }
    }

   
    public boolean addUser(String user, String password) {
        userManager.addUser(user, password);
        return true;
    }

    public void resetData() throws Exception {
        resetUserMetaData();
      
    }
    
    public void resetUserMetaData() throws Exception {
        File dir = new File(META_DATA_DIRECTORY);
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();
    }


    @Override
    public void afterTests() throws Exception {
        shutdownRepository();
    }
    
    private void shutdownRepository() throws Exception{
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
    }
    
    private void delete(File home) throws Exception{
        if (home.exists()) {
            File[] files = home.listFiles();
            if (files == null) return;
            for (int i = 0;i < files.length; i++) {
                File f = files[i];
                if (f.isDirectory()) {
                    delete(f);
                } else {
                    f.delete();
                }            
            }
            home.delete();
        }
    }


    
}
