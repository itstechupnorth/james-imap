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

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageRange;
import org.apache.torque.TorqueException;
import org.apache.torque.util.CountHelper;
import org.apache.torque.util.Criteria;

/**
 * Data access management for mailbox.
 */
public class MailboxMapper {

    public void save(MailboxRow mailbox) throws TorqueException {
        mailbox.save();
    }
    
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


    public List findInMailbox(MessageRange set, long mailboxId) throws MailboxManagerException, TorqueException {
        Criteria c = criteriaForMessageSet(set);
        c.add(MessageFlagsPeer.MAILBOX_ID, mailboxId);
        List rows = MessageMapper.doSelectJoinMessageFlags(c);
        return rows;
    }
    
    public List findMarkedForDeletionInMailbox(final MessageRange set, final MailboxRow mailboxRow) throws TorqueException, MailboxManagerException {
        final Criteria c = criteriaForMessageSet(set);
        c.addJoin(MessageRowPeer.MAILBOX_ID, MessageFlagsPeer.MAILBOX_ID);
        c.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);
        c.add(MessageRowPeer.MAILBOX_ID, mailboxRow.getMailboxId());
        c.add(MessageFlagsPeer.DELETED, true);

        final List messageRows = mailboxRow.getMessageRows(c);
        return messageRows;
    }
    
    private Criteria criteriaForMessageSet(MessageRange set) throws MailboxManagerException {
        Criteria criteria = new Criteria();
        criteria.addAscendingOrderByColumn(MessageRowPeer.UID);
        if (set.getType() == MessageRange.TYPE_ALL) {
            // empty Criteria = everything
        } else if (set.getType() == MessageRange.TYPE_UID) {

            if (set.getUidFrom() == set.getUidTo()) {
                criteria.add(MessageRowPeer.UID, set.getUidFrom());
            } else {
                Criteria.Criterion criterion1 = criteria.getNewCriterion(
                        MessageRowPeer.UID, new Long(set.getUidFrom()),
                        Criteria.GREATER_EQUAL);
                if (set.getUidTo() > 0) {
                    Criteria.Criterion criterion2 = criteria.getNewCriterion(
                            MessageRowPeer.UID, new Long(set.getUidTo()),
                            Criteria.LESS_EQUAL);
                    criterion1.and(criterion2);
                }
                criteria.add(criterion1);
            }
        } else {
            throw new MailboxManagerException("Unsupported MessageSet: "
                    + set.getType());
        }
        return criteria;
    }

}
