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

package org.apache.james.mailboxmanager.mock;

import java.util.Collection;

import org.apache.james.experimental.imapserver.ExperimentalHostSystem;
import org.apache.james.experimental.imapserver.HostSystemFactory;
import org.apache.james.mailboxmanager.impl.DefaultMailboxManagerProvider;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.mailboxmanager.manager.SubscriptionException;
import org.apache.james.mailboxmanager.torque.TorqueMailboxManager;

public class TorqueMailboxManagerProviderSingleton {
   
    private static TorqueMailboxManager torqueMailboxManager;
    private static SimpleUserManager userManager;
    private static DefaultMailboxManagerProvider defaultMailboxManagerProvider;
    public static final ExperimentalHostSystem host = new ExperimentalHostSystem();

    public synchronized static MailboxManagerProvider getTorqueMailboxManagerProviderInstance() throws Exception {
        if (defaultMailboxManagerProvider==null) {
        	getMailboxManager();
            defaultMailboxManagerProvider=new DefaultMailboxManagerProvider();
            defaultMailboxManagerProvider.setMailboxManagerInstance(torqueMailboxManager);
        }
        return defaultMailboxManagerProvider;
        
    }

    public static void addUser(String user, String password) {
    	userManager.addUser(user, password);
    }
    
    private static TorqueMailboxManager getMailboxManager() throws Exception {
        if (torqueMailboxManager == null) {
        	userManager = new SimpleUserManager();
            torqueMailboxManager=new TorqueMailboxManager(userManager);
        }
        return torqueMailboxManager;
    }

    public static void reset() throws Exception {
    	getMailboxManager().deleteEverything();
    }

}
