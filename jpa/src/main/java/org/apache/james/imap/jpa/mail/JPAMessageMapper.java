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

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jpa.JPATransactionalMapper;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.mailbox.MessageRange.Type;
import org.apache.james.imap.mailbox.SearchQuery.Criterion;
import org.apache.james.imap.mailbox.SearchQuery.NumericRange;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.MailboxMembership;


/**
 * JPA implementation of a {@link MessageMapper}. This class is not thread-safe!
 *
 */
public class JPAMessageMapper extends JPATransactionalMapper implements MessageMapper<Long> {

    private final long mailboxId;
    
    public JPAMessageMapper(final EntityManagerFactory factory, final long mailboxId) {
        super(factory);
        this.mailboxId = mailboxId;
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#findInMailbox(org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership<Long>> findInMailbox(MessageRange set) throws StorageException {
        try {
            final List<MailboxMembership<Long>> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            final Type type = set.getType();
            switch (type) {
                default:
                case ALL:
                    results = findMessagesInMailbox(mailboxId);
                    break;
                case FROM:
                    results = findMessagesInMailboxAfterUID(mailboxId, from);
                    break;
                case ONE:
                    results = findMessagesInMailboxWithUID(mailboxId, from);
                    break;
                case RANGE:
                    results = findMessagesInMailboxBetweenUIDs(mailboxId, from, to);
                    break;       
            }
            return results;
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailboxAfterUID(long mailboxId, long uid) {
        return getManager().createNamedQuery("findMessagesInMailboxAfterUID")
        .setParameter("idParam", mailboxId)
        .setParameter("uidParam", uid).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailboxWithUID(long mailboxId, long uid) {
        return getManager().createNamedQuery("findMessagesInMailboxWithUID")
        .setParameter("idParam", mailboxId)
        .setParameter("uidParam", uid).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailboxBetweenUIDs(long mailboxId, long from, long to) {
        return getManager().createNamedQuery("findMessagesInMailboxBetweenUIDs")
        .setParameter("idParam", mailboxId)
        .setParameter("fromParam", from)
        .setParameter("toParam", to).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailbox(long mailboxId) {
        return getManager().createNamedQuery("findMessagesInMailbox").setParameter("idParam", mailboxId).getResultList();
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#findMarkedForDeletionInMailbox(org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership<Long>> findMarkedForDeletionInMailbox(final MessageRange set) throws StorageException {
        try {
            final List<MailboxMembership<Long>> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            switch (set.getType()) {
                case ONE:
                    results = findDeletedMessagesInMailboxWithUID(mailboxId, from);
                    break;
                case RANGE:
                    results = findDeletedMessagesInMailboxBetweenUIDs(mailboxId, from, to);
                    break;
                case FROM:
                    results = findDeletedMessagesInMailboxAfterUID(mailboxId, from);
                    break;
                default:
                case ALL:
                    results = findDeletedMessagesInMailbox(mailboxId);
                    break;
            }
            return results;
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailbox(long mailboxId) {
        return getManager().createNamedQuery("findDeletedMessagesInMailbox").setParameter("idParam", mailboxId).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailboxAfterUID(long mailboxId, long uid) {
        return getManager().createNamedQuery("findDeletedMessagesInMailboxAfterUID")
        .setParameter("idParam", mailboxId)
        .setParameter("uidParam", uid).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailboxWithUID(long mailboxId, long uid) {
        return getManager().createNamedQuery("findDeletedMessagesInMailboxWithUID")
        .setParameter("idParam", mailboxId)
        .setParameter("uidParam", uid).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailboxBetweenUIDs(long mailboxId, long from, long to) {
        return getManager().createNamedQuery("findDeletedMessagesInMailboxBetweenUIDs")
        .setParameter("idParam", mailboxId)
        .setParameter("fromParam", from)
        .setParameter("toParam", to).getResultList();
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox() throws StorageException {
        try {
            return (Long) getManager().createNamedQuery("countMessagesInMailbox").setParameter("idParam", mailboxId).getSingleResult();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#countUnseenMessagesInMailbox()
     */
    public long countUnseenMessagesInMailbox() throws StorageException {
        try {
            return (Long) getManager().createNamedQuery("countUnseenMessagesInMailbox").setParameter("idParam", mailboxId).getSingleResult();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache.james.imap.mailbox.SearchQuery)
     */
    @SuppressWarnings("unchecked")
    public List<MailboxMembership<Long>> searchMailbox(SearchQuery query) throws StorageException {
        try {
            final String jql = formulateJQL(mailboxId, query);
            return getManager().createQuery(jql).getResultList();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    private String formulateJQL(long mailboxId, SearchQuery query) {
        final StringBuilder queryBuilder = new StringBuilder(50);
        queryBuilder.append("SELECT membership FROM Membership membership WHERE membership.mailboxId = ").append(mailboxId);
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
                        queryBuilder.append(" AND membership.uid<=").append(high);
                    } else if (low == high) {
                        queryBuilder.append(" AND membership.uid=").append(low);
                    } else {
                        queryBuilder.append(" AND membership.uid BETWEEN ").append(low).append(" AND ").append(high);
                    }
                }
            }
        }
        final String jql = queryBuilder.toString();
        return jql;
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#delete(MailboxMembership)
     */
    public void delete(MailboxMembership<Long> message) throws StorageException {
        try {
            getManager().remove(message);
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.DELETED_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findUnseenMessagesInMailbox()
     */
    @SuppressWarnings("unchecked")
    public List<MailboxMembership<Long>> findUnseenMessagesInMailbox()  throws StorageException {
        try {
            return getManager().createNamedQuery("findUnseenMessagesInMailboxOrderByUid").setParameter("idParam", mailboxId).getResultList();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#findRecentMessagesInMailbox()
     */
    @SuppressWarnings("unchecked")
    public List<MailboxMembership<Long>> findRecentMessagesInMailbox() throws StorageException {
        try {
            return getManager().createNamedQuery("findRecentMessagesInMailbox").setParameter("idParam", mailboxId).getResultList();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#save(MailboxMembership)
     */
    public void save(MailboxMembership<Long> message) throws StorageException {
        try {
            getManager().persist(message);
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        }
    }
}
