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
package org.apache.james.imap.functional.jcr;

import java.io.File;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.functional.ImapHostSystem;
import org.apache.james.imap.functional.jpa.user.InMemoryUserManager;
import org.apache.james.imap.jcr.JCRGlobalUserMailboxManager;
import org.apache.james.imap.jcr.JCRGlobalUserSubscriptionManager;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.test.functional.HostSystem;
import org.xml.sax.InputSource;

public class JCRHostSystem extends ImapHostSystem{


    public static final String META_DATA_DIRECTORY = "target/user-meta-data";
    private static JCRHostSystem host;
    public static HostSystem build() throws Exception { 
    	if (host == null) host =  new JCRHostSystem();
        return host;
    }
    
    private final JCRGlobalUserMailboxManager mailboxManager;
    private final InMemoryUserManager userManager; 

 private static final String JACKRABBIT_HOME = "target/jackrabbit";
    
    private RepositoryImpl repository;
    
    public JCRHostSystem() throws Exception {
        
        userManager = new InMemoryUserManager();
        File home = new File(JACKRABBIT_HOME);
        if (home.exists()) {
            delete(home);
        } 
        home.mkdir();
        
        RepositoryConfig config = RepositoryConfig.create(new InputSource(this.getClass().getClassLoader().getResourceAsStream("test-repository.xml")), JACKRABBIT_HOME);
        repository = RepositoryImpl.create(config);
        
        mailboxManager = new JCRGlobalUserMailboxManager(userManager, new JCRGlobalUserSubscriptionManager(repository, "default", "user", "pass"), repository, "default", "user", "pass");
        
        final DefaultImapProcessorFactory defaultImapProcessorFactory = new DefaultImapProcessorFactory();
        resetUserMetaData();
        defaultImapProcessorFactory.configure(mailboxManager);
        configure(new DefaultImapDecoderFactory().buildImapDecoder(),
                new DefaultImapEncoderFactory().buildImapEncoder(),
                defaultImapProcessorFactory.buildImapProcessor());
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            for (int i = 0; i < contents.length; i++) {
                delete(contents[i]);
            }
        } 
        file.delete();
    }
    public boolean addUser(String user, String password) {
        userManager.addUser(user, password);
        return true;
    }

    public void resetData() throws Exception {
        resetUserMetaData();

    	Session session = repository.login(new SimpleCredentials("user", new char[0]), "default");
        javax.jcr.Node root = session.getRootNode();
        if (root.hasNode("mailboxes")) {
        	root.getNode("mailboxes").remove();
        }
        session.save();
        //repository.shutdown();
    }
    
    public void resetUserMetaData() throws Exception {
        File dir = new File(META_DATA_DIRECTORY);
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();
    }

	@Override
	protected void stop() throws Exception {
		//repository.shutdown();
		System.out.println("HERE");
	}
    
    

}
