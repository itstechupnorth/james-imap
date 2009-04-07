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

public class SimpleUserManager implements UserManager {

    private final Map<String, UserDetails> users;

    public SimpleUserManager() {
        this.users = new HashMap<String, UserDetails>();
    }

    public boolean isAuthentic(String userid, String passwd) {
        UserDetails user = (UserDetails) users.get(userid);
        final boolean result;
        if (user == null) {
            result = false;
        } else {
            result = (passwd.equals(user.getPassword()));
        }
        return result;
    }

    public void subscribe(String userid, String mailbox)
            throws SubscriptionException {
        UserDetails user = (UserDetails) users.get(userid);
        if (user == null) {
            user = new UserDetails(userid);
            users.put(userid, user);
        }
        user.addSubscription(mailbox);
    }

    public Collection<String> subscriptions(String userid) throws SubscriptionException {
        UserDetails user = (UserDetails) users.get(userid);
        if (user == null) {
            user = new UserDetails(userid);
            users.put(userid, user);
        }
        return user.getSubscriptions();
    }

    public void unsubscribe(String userid, String mailbox)
            throws SubscriptionException {
        UserDetails user = (UserDetails) users.get(userid);
        if (user == null) {
            user = new UserDetails(userid);
            users.put(userid, user);
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

}
