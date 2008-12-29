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
import java.util.ArrayList;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.SearchQuery.Criterion;
import org.apache.james.mailboxmanager.SearchQuery.NumericRange;
import org.apache.torque.TorqueException;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;

import com.workingdogs.village.DataSetException;
import com.workingdogs.village.Record;

public class MessageMapper {
    
    public MailboxRow consumeNextUid(long mailboxId) throws SQLException, TorqueException {
        Connection c = Transaction.begin(MailboxRowPeer.DATABASE_NAME);
        int ti = c.getTransactionIsolation();
        boolean ac = c.getAutoCommit();
        c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        c.setAutoCommit(false);
        try {
            String sql = "UPDATE " + MailboxRowPeer.TABLE_NAME + " set "
                    + MailboxRowPeer.LAST_UID + " = " + MailboxRowPeer.LAST_UID
                    + "+1 WHERE " + MailboxRowPeer.MAILBOX_ID + " = "
                    + mailboxId;
            MailboxRowPeer.executeStatement(sql, c);
            MailboxRow mr = MailboxRowPeer.retrieveByPK(mailboxId, c);
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
                // TODO: Log?
            }
        }
    }

    public void resetRecent(long mailboxId) throws TorqueException {
        String sql = "UPDATE " + MessageFlagsPeer.TABLE_NAME + " set "
                + MessageFlagsPeer.RECENT + " = 0 WHERE "
                + MessageFlagsPeer.MAILBOX_ID + " = " + mailboxId
                + " AND " + MessageFlagsPeer.RECENT + " = 1 ";
        MessageFlagsPeer.executeStatement(sql);
    }
    
    public int countMessages(long mailboxId) throws TorqueException, DataSetException {
        return countMessages(new Flags(), true, mailboxId);
    }
    
    public int countMessages(Flags flags, boolean value, long mailboxId)
            throws TorqueException, DataSetException {
        Criteria criteria = new Criteria();
        criteria.addSelectColumn(" COUNT(" + MessageFlagsPeer.UID + ") ");
        criteria.add(MessageFlagsPeer.MAILBOX_ID, mailboxId);
        MessageMapper.addFlagsToCriteria(flags, value, criteria);
        List result = MessageFlagsPeer.doSelectVillageRecords(criteria);
        Record record = (Record) result.get(0);
        int numberOfRecords = record.getValue(1).asInt();
        return numberOfRecords;
    }

    public List find(SearchQuery query) throws TorqueException {
        final Criteria criterion = preSelect(query);
        final List rows = MessageMapper
                .doSelectJoinMessageFlags(criterion);
        return rows;
    }
    
    private Criteria preSelect(SearchQuery query) {
        final Criteria results = new Criteria();
        final List criteria = query.getCriterias();
        if (criteria.size() == 1) {
            final Criterion criterion = (Criterion) criteria.get(0);
            if (criterion instanceof SearchQuery.UidCriterion) {
                final SearchQuery.UidCriterion uidCriterion = (SearchQuery.UidCriterion) criterion;
                preSelectUid(results, uidCriterion);
            }
        }
        return results;
    }

    
    private void preSelectUid(final Criteria results,
            final SearchQuery.UidCriterion uidCriterion) {
        final NumericRange[] ranges = uidCriterion.getOperator().getRange();
        for (int i = 0; i < ranges.length; i++) {
            final long low = ranges[i].getLowValue();
            final long high = ranges[i].getHighValue();
            if (low == Long.MAX_VALUE) {
                results.add(MessageRowPeer.UID, high, Criteria.LESS_EQUAL);
            } else if (low == high) {
                results.add(MessageRowPeer.UID, low);
            } else {
                final Criteria.Criterion fromCriterion = results
                        .getNewCriterion(MessageRowPeer.UID, new Long(low),
                                Criteria.GREATER_EQUAL);
                if (high > 0 && high < Long.MAX_VALUE) {
                    final Criteria.Criterion toCriterion = results
                            .getNewCriterion(MessageRowPeer.UID,
                                    new Long(high), Criteria.LESS_EQUAL);
                    fromCriterion.and(toCriterion);
                }
                results.add(fromCriterion);
            }
        }
    }
    
    public void delete(MessageRow messageRow) throws TorqueException {
        Criteria todelc = new Criteria();
        todelc
                .add(MessageRowPeer.MAILBOX_ID, messageRow
                        .getMailboxId());
        todelc.add(MessageRowPeer.UID, messageRow.getUid());
        MessageRowPeer.doDelete(todelc);
    }
    
    public List findUnseen(final long mailboxId) throws TorqueException {
        Criteria criteria = new Criteria();
        criteria.addAscendingOrderByColumn(MessageRowPeer.UID);
        criteria.setLimit(1);
        criteria.setSingleRecord(true);

        criteria.addJoin(MessageFlagsPeer.MAILBOX_ID,
                MessageRowPeer.MAILBOX_ID);
        criteria.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);

        MessageMapper.addFlagsToCriteria(new Flags(Flags.Flag.SEEN), false, criteria);
        criteria.add(MessageRowPeer.MAILBOX_ID, mailboxId);
        
        final List messageRows = MessageRowPeer.doSelect(criteria);
        return messageRows;
    }
    
    public List findRecent(final long mailboxId) throws TorqueException {
        final Criteria criteria = new Criteria();
        criteria.addJoin(MessageFlagsPeer.MAILBOX_ID,
                MessageRowPeer.MAILBOX_ID);
        criteria.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);
        
        MessageMapper.addFlagsToCriteria(new Flags(Flags.Flag.RECENT), true,
                criteria);
        criteria.add(MessageRowPeer.MAILBOX_ID, mailboxId);
        
        final List messageRows = MessageRowPeer.doSelect(criteria);
        return messageRows;
    }
    
    static List doSelectJoinMessageFlags(Criteria criteria)
            throws TorqueException {
    
        MessageRowPeer.addSelectColumns(criteria);
        int offset = MessageRowPeer.numColumns + 1;
        MessageFlagsPeer.addSelectColumns(criteria);
    
        criteria
                .addJoin(MessageRowPeer.MAILBOX_ID, MessageFlagsPeer.MAILBOX_ID);
        criteria.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);
    
        List rows = BasePeer.doSelect(criteria);
        List result = new ArrayList(rows.size());
    
        for (int i = 0; i < rows.size(); i++) {
            Record row = (Record) rows.get(i);
    
            Class omClass = MessageRowPeer.getOMClass();
            MessageRow messageRow = (MessageRow) MessageRowPeer.row2Object(row,
                    1, omClass);
    
            omClass = MessageFlagsPeer.getOMClass();
            MessageFlags messageFlags = (MessageFlags) MessageFlagsPeer
                    .row2Object(row, offset, omClass);
            messageRow.setMessageFlags(messageFlags);
    
            result.add(messageRow);
        }
        return result;
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

    public void save(MessageFlags messageFlags) throws TorqueException {
        messageFlags.save();
    }

    public void save(MessageRow messageRow) throws TorqueException {
        messageRow.save();
    }
}
