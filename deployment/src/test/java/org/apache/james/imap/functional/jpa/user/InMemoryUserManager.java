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

package org.apache.james.imap.functional.jpa.user;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.Subscriber;

/**
 * Stores users in memory.
 */
public class InMemoryUserManager implements Authenticator, Subscriber {

    private final Map<String, User> users;

    public InMemoryUserManager() {
        this.users = new HashMap<String, User>();
    }

    public boolean isAuthentic(String userid, CharSequence password) {
        User user = (User) users.get(userid);
        final boolean result;
        if (user == null) {
            result = false;
        } else {
            result = user.isPassword(password);
        }
        return result;
    }

    public void subscribe(org.apache.james.imap.mailbox.MailboxSession.User userid, String mailbox)
            throws SubscriptionException {
        User user = (User) users.get(userid);
        if (user == null) {
            user = new User(userid.getUserName());
            users.put(userid.getUserName(), user);
        }
        user.addSubscription(mailbox);
    }

    public Collection<String> subscriptions(org.apache.james.imap.mailbox.MailboxSession.User userid) throws SubscriptionException {
        User user = (User) users.get(userid.getUserName());
        if (user == null) {
            user = new User(userid.getUserName());
            users.put(userid.getUserName(), user);
        }
        return user.getSubscriptions();
    }

    public void unsubscribe(org.apache.james.imap.mailbox.MailboxSession.User userid, String mailbox)
            throws SubscriptionException {
        User user = (User) users.get(userid.getUserName());
        if (user == null) {
            user = new User(userid.getUserName());
            users.put(userid.getUserName(), user);
        }
        user.removeSubscription(mailbox);
    }

    public void addUser(String userid, CharSequence password) {
        User user = (User) users.get(userid);
        if (user == null) {
            user = new User(userid);
            users.put(userid, user);
        }
        user.setPassword(password);
    }

}
