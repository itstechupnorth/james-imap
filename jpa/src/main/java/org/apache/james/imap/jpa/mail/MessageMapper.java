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
package org.apache.james.imap.jpa.mail;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.james.imap.jpa.mail.model.Message;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.SearchQuery.Criterion;
import org.apache.james.imap.mailbox.SearchQuery.NumericRange;

public class MessageMapper extends Mapper {
    
    public MessageMapper(EntityManager entityManager) {
        super(entityManager);
    }

    public List<Message> findInMailbox(MessageRange set, long mailboxId) {
        final List<Message> results;
        switch (set.getType()) {
            case MessageRange.TYPE_UID:
                final long from = set.getUidFrom();
                final long to = set.getUidTo();
                if (from == to) {
                    results = findMessagesInMailboxWithUID(mailboxId, from);
                } else if (to > 0) {
                    results = findMessagesInMailboxBetweenUIDs(mailboxId, from, to);
                } else {
                    results = findMessagesInMailboxAfterUID(mailboxId, from);
                }
                break;
            default:
                //TODO: Log?
            case MessageRange.TYPE_ALL:
                results = findMessagesInMailbox(mailboxId);
                break;
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Message> findMessagesInMailboxAfterUID(long mailboxId, long uid) {
        return entityManager.createNamedQuery("findMessagesInMailboxAfterUID")
            .setParameter("idParam", mailboxId)
            .setParameter("uidParam", uid).getResultList();
    }
    
    @SuppressWarnings("unchecked")
    private List<Message> findMessagesInMailboxWithUID(long mailboxId, long uid) {
        return entityManager.createNamedQuery("findMessagesInMailboxWithUID")
            .setParameter("idParam", mailboxId)
            .setParameter("uidParam", uid).getResultList();
    }
    
    @SuppressWarnings("unchecked")
    private List<Message> findMessagesInMailboxBetweenUIDs(long mailboxId, long from, long to) {
        return entityManager.createNamedQuery("findMessagesInMailboxBetweenUIDs")
            .setParameter("idParam", mailboxId)
            .setParameter("fromParam", from)
            .setParameter("toParam", to).getResultList();
    }
    
    @SuppressWarnings("unchecked")
    private List<Message> findMessagesInMailbox(long mailboxId) {
        return entityManager.createNamedQuery("findMessagesInMailbox").setParameter("idParam", mailboxId).getResultList();
    }

    public List<Message> findMarkedForDeletionInMailbox(final MessageRange set, final long mailboxId) {
        final List<Message> results;
        switch (set.getType()) {
            case MessageRange.TYPE_UID:
                final long from = set.getUidFrom();
                final long to = set.getUidTo();
                if (from == to) {
                    results = findDeletedMessagesInMailboxWithUID(mailboxId, from);
                } else if (to > 0) {
                    results = findDeletedMessagesInMailboxBetweenUIDs(mailboxId, from, to);
                } else {
                    results = findDeletedMessagesInMailboxAfterUID(mailboxId, from);
                }
                break;
            default:
                //TODO: Log?
            case MessageRange.TYPE_ALL:
                results = findDeletedMessagesInMailbox(mailboxId);
                break;
        }
        return results;
    }
    
    @SuppressWarnings("unchecked")
    private List<Message> findDeletedMessagesInMailbox(long mailboxId) {
        return entityManager.createNamedQuery("findDeletedMessagesInMailbox").setParameter("idParam", mailboxId).getResultList();
    }
    
    @SuppressWarnings("unchecked")
    private List<Message> findDeletedMessagesInMailboxAfterUID(long mailboxId, long uid) {
        return entityManager.createNamedQuery("findDeletedMessagesInMailboxBetweenUIDs")
            .setParameter("idParam", mailboxId)
            .setParameter("uidParam", uid).getResultList();
    }
    
    @SuppressWarnings("unchecked")
    private List<Message> findDeletedMessagesInMailboxWithUID(long mailboxId, long uid) {
        return entityManager.createNamedQuery("findDeletedMessagesInMailboxBetweenUIDs")
            .setParameter("idParam", mailboxId)
            .setParameter("uidParam", uid).getResultList();
    }
    
    @SuppressWarnings("unchecked")
    private List<Message> findDeletedMessagesInMailboxBetweenUIDs(long mailboxId, long from, long to) {
        return entityManager.createNamedQuery("findDeletedMessagesInMailboxBetweenUIDs")
            .setParameter("idParam", mailboxId)
            .setParameter("fromParam", from)
            .setParameter("toParam", to).getResultList();
    }

    public long countMessagesInMailbox(long mailboxId) {
        return (Long) entityManager.createNamedQuery("countMessagesInMailbox").setParameter("idParam", mailboxId).getSingleResult();
    }
    
    public long countUnseenMessagesInMailbox(long mailboxId){
        return (Long) entityManager.createNamedQuery("countUnseenMessagesInMailbox").setParameter("idParam", mailboxId).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Message> searchMailbox(long mailboxId, SearchQuery query) {
        final String jql = formulateJQL(mailboxId, query);
        return entityManager.createQuery(jql).getResultList();
    }

    private String formulateJQL(long mailboxId, SearchQuery query) {
        final StringBuilder queryBuilder = new StringBuilder(50);
        queryBuilder.append("SELECT message FROM Message message WHERE message.mailboxId = ").append(mailboxId);
        final List<Criterion> criteria = query.getCriterias();
        if (criteria.size() == 1) {
            final Criterion firstCriterion = criteria.get(0);
            if (firstCriterion instanceof SearchQuery.UidCriterion) {
                final SearchQuery.UidCriterion uidCriterion = (SearchQuery.UidCriterion) firstCriterion;
                final NumericRange[] ranges = uidCriterion.getOperator().getRange();
                for (int i = 0; i < ranges.length; i++) {
                    final long low = ranges[i].getLowValue();
                    final long high = ranges[i].getHighValue();
                    
                    if (low == Long.MAX_VALUE) {
                        queryBuilder.append(" AND message.uid<=").append(high);
                    } else if (low == high) {
                        queryBuilder.append(" AND message.uid=").append(low);
                    } else {
                        queryBuilder.append(" AND message.uid BETWEEN ").append(low).append(" AND ").append(high);
                    }
                }
            }
        }
        final String jql = queryBuilder.toString();
        return jql;
    }
    
    public void delete(Message message)  {
        entityManager.remove(message);
    }
    
    @SuppressWarnings("unchecked")
    public List<Message> findUnseenMessagesInMailboxOrderByUid(final long mailboxId)  {
        return entityManager.createNamedQuery("findUnseenMessagesInMailboxOrderByUid").setParameter("idParam", mailboxId).getResultList();
    }
    
    @SuppressWarnings("unchecked")
    public List<Message> findRecentMessagesInMailbox(final long mailboxId) {
        return entityManager.createNamedQuery("findRecentMessagesInMailbox").setParameter("idParam", mailboxId).getResultList();
    }

    public void save(Message message) {
        entityManager.persist(message);
    }
}
