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
import javax.persistence.Query;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jpa.JPATransactionalMapper;
import org.apache.james.imap.jpa.mail.model.JPAMailboxMembership;
import org.apache.james.imap.jpa.mail.model.openjpa.AbstractJPAMailboxMembership;
import org.apache.james.imap.jpa.mail.model.openjpa.JPAStreamingMailboxMembership;
import org.apache.james.imap.mailbox.MailboxException;
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
    
    public JPAMessageMapper(final EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#findInMailbox(org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership<Long>> findInMailbox(Long mailboxId, MessageRange set) throws StorageException {
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
    private List<MailboxMembership<Long>> findMessagesInMailboxAfterUID(Long mailboxId, long uid) {
        return getEntityManager().createNamedQuery("findMessagesInMailboxAfterUID")
        .setParameter("idParam", mailboxId)
        .setParameter("uidParam", uid).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailboxWithUID(Long mailboxId, long uid) {
        return getEntityManager().createNamedQuery("findMessagesInMailboxWithUID")
        .setParameter("idParam", mailboxId)
        .setParameter("uidParam", uid).setMaxResults(1).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailboxBetweenUIDs(Long mailboxId, long from, long to) {
        return getEntityManager().createNamedQuery("findMessagesInMailboxBetweenUIDs")
        .setParameter("idParam", mailboxId)
        .setParameter("fromParam", from)
        .setParameter("toParam", to).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailbox(Long mailboxId) {
        return getEntityManager().createNamedQuery("findMessagesInMailbox").setParameter("idParam", mailboxId).getResultList();
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#findMarkedForDeletionInMailbox(org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership<Long>> findMarkedForDeletionInMailbox(Long mailboxId, final MessageRange set) throws StorageException {
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
    private List<MailboxMembership<Long>> findDeletedMessagesInMailbox(Long mailboxId) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailbox").setParameter("idParam", mailboxId).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailboxAfterUID(Long mailboxId, long uid) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailboxAfterUID")
        .setParameter("idParam", mailboxId)
        .setParameter("uidParam", uid).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailboxWithUID(Long mailboxId, long uid) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailboxWithUID")
        .setParameter("idParam", mailboxId)
        .setParameter("uidParam", uid).setMaxResults(1).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailboxBetweenUIDs(Long mailboxId, long from, long to) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailboxBetweenUIDs")
        .setParameter("idParam", mailboxId)
        .setParameter("fromParam", from)
        .setParameter("toParam", to).getResultList();
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox(Long mailboxId) throws StorageException {
        try {
            return (Long) getEntityManager().createNamedQuery("countMessagesInMailbox").setParameter("idParam", mailboxId).getSingleResult();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#countUnseenMessagesInMailbox()
     */
    public long countUnseenMessagesInMailbox(Long mailboxId) throws StorageException {
        try {
            return (Long) getEntityManager().createNamedQuery("countUnseenMessagesInMailbox").setParameter("idParam", mailboxId).getSingleResult();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache.james.imap.mailbox.SearchQuery)
     */
    @SuppressWarnings("unchecked")
    public List<MailboxMembership<Long>> searchMailbox(Long mailboxId, SearchQuery query) throws StorageException {
        try {
            final Query jQuery = formulateJQL(mailboxId, query);
            return jQuery.getResultList();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    private Query formulateJQL(Long mailboxId, SearchQuery query) {
        final StringBuilder queryBuilder = new StringBuilder(50);
        queryBuilder.append("SELECT membership FROM Membership membership WHERE membership.mailboxId = ").append(mailboxId);
        final List<Criterion> criteria = query.getCriterias();
        boolean range = false;
        int rangeLength = -1;
        
        if (criteria.size() == 1) {
            final Criterion firstCriterion = criteria.get(0);
            if (firstCriterion instanceof SearchQuery.UidCriterion) {
                final SearchQuery.UidCriterion uidCriterion = (SearchQuery.UidCriterion) firstCriterion;
                final NumericRange[] ranges = uidCriterion.getOperator().getRange();
                rangeLength = ranges.length;

                for (int i = 0; i < ranges.length; i++) {
                    final long low = ranges[i].getLowValue();
                    final long high = ranges[i].getHighValue();

                    if (low == Long.MAX_VALUE) {
                        queryBuilder.append(" AND membership.uid<=").append(high);
                        range = true;
                    } else if (low == high) {
                        queryBuilder.append(" AND membership.uid=").append(low);
                        range = false;
                    } else {
                        queryBuilder.append(" AND membership.uid BETWEEN ").append(low).append(" AND ").append(high);
                        range = true;
                    }
                }
            }
        }        
        if (rangeLength != 0 || range) {
            queryBuilder.append(" order by membership.uid");
        }
        
        Query jQuery = getEntityManager().createQuery(queryBuilder.toString());

        // Check if we only need to fetch 1 message, if so we can set a limit to speed up things
        if (rangeLength == 1 && range == false) {
            jQuery.setMaxResults(1);
        }
        return jQuery;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#delete(java.lang.Object, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void delete(Long uid, MailboxMembership<Long> message) throws StorageException {
        try {
            getEntityManager().remove(message);
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.DELETED_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findUnseenMessagesInMailbox()
     */
    @SuppressWarnings("unchecked")
    public List<MailboxMembership<Long>> findUnseenMessagesInMailbox(Long mailboxId, int limit)  throws StorageException {
        try {
            Query query = getEntityManager().createNamedQuery("findUnseenMessagesInMailboxOrderByUid").setParameter("idParam", mailboxId);
            if (limit > 0) {
                query = query.setMaxResults(limit);
            }
            return query.getResultList();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#findRecentMessagesInMailbox()
     */
    @SuppressWarnings("unchecked")
    public List<MailboxMembership<Long>> findRecentMessagesInMailbox(Long mailboxId, int limit) throws StorageException {
        try {
            Query query = getEntityManager().createNamedQuery("findRecentMessagesInMailbox").setParameter("idParam", mailboxId);
            if (limit > 0) {
                query = query.setMaxResults(limit);
            }
            return query.getResultList();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#save(MailboxMembership)
     */
    public void save(Long mailboxId, MailboxMembership<Long> message) throws StorageException {
        try {
            getEntityManager().persist(message);
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#copy(java.lang.Object, long, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public MailboxMembership<Long> copy(Long mailboxId, long uid, MailboxMembership<Long> original) throws StorageException {
        try {
            MailboxMembership<Long> copy;
            if (original instanceof JPAStreamingMailboxMembership) {
                copy = new JPAStreamingMailboxMembership(mailboxId, uid, (AbstractJPAMailboxMembership) original);
            } else {
                copy = new JPAMailboxMembership(mailboxId, uid, (AbstractJPAMailboxMembership)original);
            }
            save(mailboxId, copy);
            return copy;
        } catch (MailboxException e) {
            throw new StorageException(e.getKey(),e);
        }
    }
}
