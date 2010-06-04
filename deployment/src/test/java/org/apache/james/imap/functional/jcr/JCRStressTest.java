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

import javax.jcr.RepositoryException;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.james.imap.functional.AbstractStressTest;
import org.apache.james.imap.jcr.GlobalMailboxSessionJCRRepository;
import org.apache.james.imap.jcr.JCRMailboxManager;
import org.apache.james.imap.jcr.JCRMailboxSessionMapperFactory;
import org.apache.james.imap.jcr.JCRSubscriptionManager;
import org.apache.james.imap.jcr.JCRUtils;
import org.apache.james.imap.jcr.MailboxSessionJCRRepository;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.StoreMailboxManager;
import org.junit.After;
import org.junit.Before;
import org.xml.sax.InputSource;

public class JCRStressTest extends AbstractStressTest{
    
    private JCRMailboxManager mailboxManager;

    private static final String JACKRABBIT_HOME = "deployment/target/jackrabbit";
    public static final String META_DATA_DIRECTORY = "target/user-meta-data";
    private RepositoryImpl repository;
   
    
    @Before
    public void setUp() throws RepositoryException {

        new File(JACKRABBIT_HOME).delete();

        String user = "user";
        String pass = "pass";
        String workspace = null;
        RepositoryConfig config = RepositoryConfig.create(new InputSource(this.getClass().getClassLoader().getResourceAsStream("test-repository.xml")), JACKRABBIT_HOME);
        repository = RepositoryImpl.create(config);

        // Register imap cnd file
        JCRUtils.registerCnd(repository, workspace, user, pass);

        MailboxSessionJCRRepository sessionRepos = new GlobalMailboxSessionJCRRepository(repository, workspace, user, pass);
        JCRMailboxSessionMapperFactory mf = new JCRMailboxSessionMapperFactory(sessionRepos);
        mailboxManager = new JCRMailboxManager(mf, null, new JCRSubscriptionManager(mf));

    }
    
    
    @After
    
    public void tearDown() {
        MailboxSession session = mailboxManager.createSystemSession("test", new SimpleLog("Test"));
        session.close();
        repository.shutdown();
        new File(JACKRABBIT_HOME).delete();

    }

    
    @Override
    protected StoreMailboxManager<?> getMailboxManager() {
        return mailboxManager;
    }

 
}
