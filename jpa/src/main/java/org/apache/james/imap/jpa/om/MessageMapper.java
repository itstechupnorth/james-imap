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

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

public class MessageMapper {
    
    public List findUnseen(MailboxRow row) throws TorqueException {
        Criteria c = new Criteria();
        c.addAscendingOrderByColumn(MessageRowPeer.UID);
        c.setLimit(1);
        c.setSingleRecord(true);

        c.addJoin(MessageFlagsPeer.MAILBOX_ID,
                MessageRowPeer.MAILBOX_ID);
        c.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);

        MessageMapper.addFlagsToCriteria(new Flags(Flags.Flag.SEEN),
                false, c);
        List messageRows = row.getMessageRows(c);
        return messageRows;
    }
    
    public List findRecent(final MailboxRow mailboxRow) throws TorqueException {
        final Criteria criterion = new Criteria();
        criterion.addJoin(MessageFlagsPeer.MAILBOX_ID,
                MessageRowPeer.MAILBOX_ID);
        criterion.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);
        
        MessageMapper.addFlagsToCriteria(new Flags(Flags.Flag.RECENT), true,
                criterion);
        final List messageRows = mailboxRow.getMessageRows(criterion);
        return messageRows;
    }
    
    static void addFlagsToCriteria(Flags flags, boolean value, Criteria c) {
        if (flags.contains(Flags.Flag.ANSWERED)) {
            c.add(MessageFlagsPeer.ANSWERED, value);
        }
        if (flags.contains(Flags.Flag.DELETED)) {
            c.add(MessageFlagsPeer.DELETED, value);
        }
        if (flags.contains(Flags.Flag.DRAFT)) {
            c.add(MessageFlagsPeer.DRAFT, value);
        }
        if (flags.contains(Flags.Flag.FLAGGED)) {
            c.add(MessageFlagsPeer.FLAGGED, value);
        }
        if (flags.contains(Flags.Flag.RECENT)) {
            c.add(MessageFlagsPeer.RECENT, value);
        }
        if (flags.contains(Flags.Flag.SEEN)) {
            c.add(MessageFlagsPeer.SEEN, value);
        }
    }
}
