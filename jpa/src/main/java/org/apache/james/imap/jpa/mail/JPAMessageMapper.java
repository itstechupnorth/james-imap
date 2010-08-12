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

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jpa.JPATransactionalMapper;
import org.apache.james.imap.jpa.mail.model.JPAMailbox;
import org.apache.james.imap.jpa.mail.model.openjpa.AbstractJPAMailboxMembership;
import org.apache.james.imap.jpa.mail.model.openjpa.JPAMailboxMembership;
import org.apache.james.imap.jpa.mail.model.openjpa.JPAStreamingMailboxMembership;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.mailbox.MessageRange.Type;
import org.apache.james.imap.mailbox.SearchQuery.Criterion;
import org.apache.james.imap.mailbox.SearchQuery.NumericRange;
import org.apache.james.imap.store.SearchQueryIterator;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
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
    public List<MailboxMembership<Long>> findInMailbox(Mailbox<Long> mailbox, MessageRange set) throws StorageException {
        try {
            final List<MailboxMembership<Long>> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            final Type type = set.getType();
            switch (type) {
                default:
                case ALL:
                    results = findMessagesInMailbox(mailbox);
                    break;
                case FROM:
                    results = findMessagesInMailboxAfterUID(mailbox, from);
                    break;
                case ONE:
                    results = findMessagesInMailboxWithUID(mailbox, from);
                    break;
                case RANGE:
                    results = findMessagesInMailboxBetweenUIDs(mailbox, from, to);
                    break;       
            }
            return results;
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailboxAfterUID(Mailbox<Long> mailbox, long uid) {
        return getEntityManager().createNamedQuery("findMessagesInMailboxAfterUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailboxWithUID(Mailbox<Long> mailbox, long uid) {
        return getEntityManager().createNamedQuery("findMessagesInMailboxWithUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid).setMaxResults(1).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailboxBetweenUIDs(Mailbox<Long> mailbox, long from, long to) {
        return getEntityManager().createNamedQuery("findMessagesInMailboxBetweenUIDs")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("fromParam", from)
        .setParameter("toParam", to).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findMessagesInMailbox(Mailbox<Long> mailbox) {
        return getEntityManager().createNamedQuery("findMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId()).getResultList();
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#findMarkedForDeletionInMailbox(org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership<Long>> findMarkedForDeletionInMailbox(Mailbox<Long> mailbox, final MessageRange set) throws StorageException {
        try {
            final List<MailboxMembership<Long>> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            switch (set.getType()) {
                case ONE:
                    results = findDeletedMessagesInMailboxWithUID(mailbox, from);
                    break;
                case RANGE:
                    results = findDeletedMessagesInMailboxBetweenUIDs(mailbox, from, to);
                    break;
                case FROM:
                    results = findDeletedMessagesInMailboxAfterUID(mailbox, from);
                    break;
                default:
                case ALL:
                    results = findDeletedMessagesInMailbox(mailbox);
                    break;
            }
            return results;
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailbox(Mailbox<Long> mailbox) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId()).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailboxAfterUID(Mailbox<Long> mailbox, long uid) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailboxAfterUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailboxWithUID(Mailbox<Long> mailbox, long uid) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailboxWithUID")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("uidParam", uid).setMaxResults(1).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<MailboxMembership<Long>> findDeletedMessagesInMailboxBetweenUIDs(Mailbox<Long> mailbox, long from, long to) {
        return getEntityManager().createNamedQuery("findDeletedMessagesInMailboxBetweenUIDs")
        .setParameter("idParam", mailbox.getMailboxId())
        .setParameter("fromParam", from)
        .setParameter("toParam", to).getResultList();
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox(Mailbox<Long> mailbox) throws StorageException {
        try {
            return (Long) getEntityManager().createNamedQuery("countMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId()).getSingleResult();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
    }

    /**
     * @see org.apache.james.imap.store.mail.MessageMapper#countUnseenMessagesInMailbox()
     */
    public long countUnseenMessagesInMailbox(Mailbox<Long> mailbox) throws StorageException {
        try {
            return (Long) getEntityManager().createNamedQuery("countUnseenMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId()).getSingleResult();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
    }



    /*
     * 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.imap.mailbox.SearchQuery)
     */
    @SuppressWarnings("unchecked")
    public Iterator<Long> searchMailbox(Mailbox<Long> mailbox, SearchQuery query) throws StorageException {
        try {
            final StringBuilder queryBuilder = new StringBuilder(50);
            queryBuilder.append("SELECT membership FROM Membership membership WHERE membership.mailboxId = ").append(mailbox.getMailboxId());
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
            return new SearchQueryIterator(jQuery.getResultList().iterator(), query);
            
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#delete(java.lang.Object, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void delete(Mailbox<Long> mailbox, MailboxMembership<Long> message) throws StorageException {
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
    public List<MailboxMembership<Long>> findUnseenMessagesInMailbox(Mailbox<Long> mailbox, int limit)  throws StorageException {
        try {
            Query query = getEntityManager().createNamedQuery("findUnseenMessagesInMailboxOrderByUid").setParameter("idParam", mailbox.getMailboxId());
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
    public List<MailboxMembership<Long>> findRecentMessagesInMailbox(Mailbox<Long> mailbox, int limit) throws StorageException {
        try {
            Query query = getEntityManager().createNamedQuery("findRecentMessagesInMailbox").setParameter("idParam", mailbox.getMailboxId());
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
    public long save(Mailbox<Long> mailbox, MailboxMembership<Long> message) throws StorageException {
        try {
            
            if (message.getUid() == 0) {
                
                ((AbstractJPAMailboxMembership) message).setUid(reserveUid((JPAMailbox)mailbox));
            }
            
            
            getEntityManager().persist(message);
            return message.getUid();
        } catch (PersistenceException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#copy(java.lang.Object, long, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public MailboxMembership<Long> copy(Mailbox<Long> mailbox, MailboxMembership<Long> original) throws StorageException {
        try {
            MailboxMembership<Long> copy;
            if (original instanceof JPAStreamingMailboxMembership) {
                copy = new JPAStreamingMailboxMembership(mailbox.getMailboxId(), (AbstractJPAMailboxMembership) original);
            } else {
                copy = new JPAMailboxMembership(mailbox.getMailboxId(), (AbstractJPAMailboxMembership)original);
            }
            save(mailbox, copy);
            return copy;
        } catch (MailboxException e) {
            throw new StorageException(e.getKey(),e);
        }
    }
    
    /**
     * Reserve the uid for the next {@link MailboxMembership} in the given {@link JPAMailbox}.
     * This is done by using a row lock.
     * 
     * @param mailbox
     * @return uid
     */
    protected long reserveUid(JPAMailbox mailbox) {
        EntityManager manager = getEntityManager();

        // we need to set a pessimistic write lock to be sure we don't get any problems with dirty reads etc.
        JPAMailbox m = manager.find(JPAMailbox.class, mailbox.getMailboxId(), LockModeType.PESSIMISTIC_WRITE);
        manager.refresh(m);
        m.consumeUid();
        manager.persist(m);
        return m.getLastUid();
    }
}
