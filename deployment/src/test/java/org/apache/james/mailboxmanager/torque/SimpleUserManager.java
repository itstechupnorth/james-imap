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

package org.apache.james.mailboxmanager.torque;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.mailbox.MailboxSession.User;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.Subscriber;

public class SimpleUserManager implements Subscriber, Authenticator {

    private final Map<String, UserDetails> users;

    public SimpleUserManager() {
        this.users = new HashMap<String, UserDetails>();
    }
    
    public void subscribe(User u, String mailbox)
            throws SubscriptionException {
        UserDetails user = (UserDetails) users.get(u.getUserName());
        if (user == null) {
            user = new UserDetails(u.getUserName());
            users.put(u.getUserName(), user);
        }
        user.addSubscription(mailbox);
    }

    public Collection<String> subscriptions(User u) throws SubscriptionException {
        UserDetails user = (UserDetails) users.get(u.getUserName());
        if (user == null) {
            user = new UserDetails(u.getUserName());
            users.put(u.getUserName(), user);
        }
        return user.getSubscriptions();
    }

    public void unsubscribe(User userid, String mailbox)
            throws SubscriptionException {
        UserDetails user = (UserDetails) users.get(userid.getUserName());
        if (user == null) {
            user = new UserDetails(userid.getUserName());
            users.put(userid.getUserName(), user);
        }
        user.removeSubscription(mailbox);
    }

    public void addUser(String userid, String password) {
        UserDetails user = (UserDetails) users.get(userid);
        if (user == null) {
            user = new UserDetails(userid);
            users.put(userid, user);
        }
        user.setPassword(password);
    }

	public boolean isAuthentic(String userid, CharSequence passwd) {
		 UserDetails user = (UserDetails) users.get(userid);
	        final boolean result;
		if (user == null) {
			result = false;
		} else {
			result = (passwd.toString().equals(user.getPassword()));
		}
		return result;
	}

}
