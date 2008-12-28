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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.mail.Flags;

import org.apache.torque.TorqueException;
import org.apache.torque.om.Persistent;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;

import com.workingdogs.village.DataSetException;
import com.workingdogs.village.Record;

/**
 * The skeleton for this class was autogenerated by Torque on:
 * 
 * [Wed Sep 06 08:50:08 CEST 2006]
 * 
 * You should add additional methods to this class to meet the application
 * requirements. This class will only be generated as long as it does not
 * already exist in the output directory.
 */
public class MailboxRow extends
        org.apache.james.imap.jpa.om.BaseMailboxRow implements
        Persistent {

    private static final long serialVersionUID = -8207690877715465485L;

    public MailboxRow(String string, long uidValidity) {
        this();
        setName(string);
        setUidValidity(uidValidity);
    }

    public MailboxRow() {
        super();
    }

    public MailboxRow consumeNextUid() throws SQLException, TorqueException {
        Connection c = Transaction.begin(MailboxRowPeer.DATABASE_NAME);
        int ti = c.getTransactionIsolation();
        boolean ac = c.getAutoCommit();
        c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        c.setAutoCommit(false);
        try {
            String sql = "UPDATE " + MailboxRowPeer.TABLE_NAME + " set "
                    + MailboxRowPeer.LAST_UID + " = " + MailboxRowPeer.LAST_UID
                    + "+1 WHERE " + MailboxRowPeer.MAILBOX_ID + " = "
                    + getMailboxId();
            MailboxRowPeer.executeStatement(sql, c);
            MailboxRow mr = MailboxRowPeer.retrieveByPK(getMailboxId(), c);
            Transaction.commit(c);
            return mr;
        } catch (TorqueException e) {
            Transaction.safeRollback(c);
            throw e;
        } finally {
            try {
                c.setTransactionIsolation(ti);
                c.setAutoCommit(ac);
            } catch (Exception e) {
            }
        }

    }

    public int countMessages() throws TorqueException, DataSetException {
        return countMessages(new Flags(), true);
    }

    public int countMessages(Flags flags, boolean value)
            throws TorqueException, DataSetException {
        Criteria criteria = new Criteria();
        criteria.addSelectColumn(" COUNT(" + MessageFlagsPeer.UID + ") ");
        criteria.add(MessageFlagsPeer.MAILBOX_ID, getMailboxId());
        MessageFlagsPeer.addFlagsToCriteria(flags, value, criteria);
        List result = MessageFlagsPeer.doSelectVillageRecords(criteria);
        Record record = (Record) result.get(0);
        int numberOfRecords = record.getValue(1).asInt();
        return numberOfRecords;
    }

    public void resetRecent() throws TorqueException {
        String sql = "UPDATE " + MessageFlagsPeer.TABLE_NAME + " set "
                + MessageFlagsPeer.RECENT + " = 0 WHERE "
                + MessageFlagsPeer.MAILBOX_ID + " = " + getMailboxId()
                + " AND " + MessageFlagsPeer.RECENT + " = 1 ";
        MessageFlagsPeer.executeStatement(sql);
    }
}
