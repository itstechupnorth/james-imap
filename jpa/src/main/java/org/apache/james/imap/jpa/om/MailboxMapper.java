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

package org.apache.james.imap.jpa.om;

import java.util.List;

import org.apache.torque.TorqueException;
import org.apache.torque.util.CountHelper;
import org.apache.torque.util.Criteria;

/**
 * Data access management for mailbox.
 */
public class MailboxMapper {
 
    /**
     * Finds a mailbox by name.
     * @param name not null
     * @return not null
     * @throws TorqueException
     */
    public MailboxRow findByName(String name) throws TorqueException {
        return MailboxRowPeer.retrieveByName(name);
    }
    
    /**
     * Deletes the given mailbox.
     * @param mailbox not null
     * @throws TorqueException
     */
    public static void delete(MailboxRow mailbox) throws TorqueException {
        MailboxRowPeer.doDelete(mailbox);
    }
    
    public List findNameLike(String name) throws TorqueException {
        Criteria c = new Criteria();
        c.add(MailboxRowPeer.NAME,
                (Object) name,
                Criteria.LIKE);
        List l = MailboxRowPeer.doSelect(c);
        return l;
    }
   
    public void deleteAll() throws TorqueException {
        MailboxRowPeer.doDelete(new Criteria().and(
                MailboxRowPeer.MAILBOX_ID, new Integer(-1),
                Criteria.GREATER_THAN));
    }

    public MailboxRow refresh(MailboxRow mailboxRow) throws TorqueException {
        return MailboxRowPeer.retrieveByPK(mailboxRow.getPrimaryKey());
    }
    
    public int countOnName(String mailboxName) throws TorqueException {
        int count;
        Criteria c = new Criteria();
        c.add(MailboxRowPeer.NAME, mailboxName);
        CountHelper countHelper = new CountHelper();
        count = countHelper.count(c);
        return count;
    }
}
