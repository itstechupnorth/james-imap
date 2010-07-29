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
package org.apache.james.imap.api;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This special map uses MailboxPath objects as keys.
 *
 * @param <T> The type of objects to store.
 */
public class MailboxPathHashMap<T> extends HashMap<MailboxPath, T> {

    private static final long serialVersionUID = -7424356447437920137L;

    @Override
    public T get(Object object) {
        if (!(object instanceof MailboxPath))
            throw new IllegalArgumentException("Instance of type MailboxPath expected as key.");
        MailboxPath path = (MailboxPath) object;
        Iterator<MailboxPath> iteraror = this.keySet().iterator();
        while (iteraror.hasNext()) {
            MailboxPath iterPath = iteraror.next();
            if (iterPath.equals(path))
                return super.get(iterPath);
        }
        return null;
    }
    
    @Override
    public T put(MailboxPath mailboxPath, T object) {
        MailboxPath previousKey = null;
        T previousValue = null;
        Iterator<MailboxPath> iteraror = this.keySet().iterator();
        while (iteraror.hasNext()) {
            MailboxPath iterPath = iteraror.next();
            if (iterPath.equals(mailboxPath)) {
                previousKey = iterPath;
                previousValue = super.get(iterPath);
                break;
            }
        }
        if (previousKey == null)
            super.put(mailboxPath, object);
        else
            super.put(previousKey, object);
        return previousValue;
    }
    
}
