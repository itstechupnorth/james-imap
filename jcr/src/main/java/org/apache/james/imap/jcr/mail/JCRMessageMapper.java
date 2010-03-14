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
package org.apache.james.imap.jcr.mail;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.JCRMapper;
import org.apache.james.imap.jcr.JCRUtils;
import org.apache.james.imap.jcr.mail.model.JCRMailboxMembership;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.mailbox.MessageRange.Type;
import org.apache.james.imap.mailbox.SearchQuery.Criterion;
import org.apache.james.imap.mailbox.SearchQuery.NumericRange;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.MailboxMembership;

public class JCRMessageMapper extends JCRMapper implements MessageMapper {

    private final static String PATH = PROPERTY_PREFIX + "mailboxMemberships";
    private final Log logger;
    private String uuid;

    public JCRMessageMapper(final Session session, final String uuid, final Log logger) {
        super(session);
        this.logger = logger;
        this.uuid = uuid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox() throws StorageException {
       
        try {
            String queryString = "//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"']";
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            return result.getNodes().getSize();
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
       
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#countUnseenMessagesInMailbox
     * ()
     */
    public long countUnseenMessagesInMailbox() throws StorageException {
        
        try {
            String queryString = "//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] && [@" + JCRMailboxMembership.SEEN_PROPERTY +"='" + false +"']";
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            return result.getNodes().getSize();
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#delete(org.apache.james
     * .imap.store.mail.model.MailboxMembership)
     */
    public void delete(MailboxMembership message) throws StorageException {
        JCRMailboxMembership membership = (JCRMailboxMembership) message;
        if (membership.isPersistent()) {
            try {
                getSession().getNodeByUUID(membership.getUUID()).remove();
            } catch (RepositoryException e) {
                throw new StorageException(HumanReadableText.DELETED_FAILED, e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#findInMailbox(org.apache
     * .james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership> findInMailbox(MessageRange set) throws StorageException {
        try {
            final List<MailboxMembership> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            final Type type = set.getType();
            switch (type) {
                default:
                case ALL:
                    results = findMessagesInMailbox(uuid);
                    break;
                case FROM:
                    results = findMessagesInMailboxAfterUID(uuid, from);
                    break;
                case ONE:
                    results = findMessagesInMailboxWithUID(uuid, from);
                    break;
                case RANGE:
                    results = findMessagesInMailboxBetweenUIDs(uuid, from, to);
                    break;       
            }
            return results;
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    private List<MailboxMembership> findMessagesInMailboxAfterUID(String uuid, long uid) throws RepositoryException {
        List<MailboxMembership> list = new ArrayList<MailboxMembership>();
        String queryString = "//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] && [@" + JCRMailboxMembership.UID_PROPERTY + ">" + uid + "]";
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }

    private List<MailboxMembership> findMessagesInMailboxWithUID(String uuid, long uid) throws RepositoryException  {
        List<MailboxMembership> list = new ArrayList<MailboxMembership>();
        String queryString = "//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] && [@" + JCRMailboxMembership.UID_PROPERTY + "=" + uid + "]";
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }

    private List<MailboxMembership> findMessagesInMailboxBetweenUIDs(String uuid, long from, long to) throws RepositoryException {
        List<MailboxMembership> list = new ArrayList<MailboxMembership>();
        String queryString = "//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] && [@" + JCRMailboxMembership.UID_PROPERTY + ">" + from + "] && [@" + JCRMailboxMembership.UID_PROPERTY + "<" + to + "]";
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }

    private List<MailboxMembership> findMessagesInMailbox(String uuid) throws RepositoryException {
        List<MailboxMembership> list = new ArrayList<MailboxMembership>();
        String queryString = "//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "']";
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#findMarkedForDeletionInMailbox
     * (org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership> findMarkedForDeletionInMailbox(MessageRange set) throws StorageException {
        try {
            List<MailboxMembership> list = new ArrayList<MailboxMembership>();
            String queryString = "//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] && [@" + JCRMailboxMembership.DELETED_PROPERTY +"=" + true +"]";
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
            }
            return list;
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#findRecentMessagesInMailbox
     * ()
     */
    public List<MailboxMembership> findRecentMessagesInMailbox() throws StorageException {
        
        try {
            List<MailboxMembership> list = new ArrayList<MailboxMembership>();
            String queryString = "//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] && [@" + JCRMailboxMembership.RECENT_PROPERTY +"=" + true +"]";
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
            }
            return list;
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.apache.james.imap.store.mail.MessageMapper#
     * findUnseenMessagesInMailboxOrderByUid()
     */
    public List<MailboxMembership> findUnseenMessagesInMailboxOrderByUid() throws StorageException {
        try {
            List<MailboxMembership> list = new ArrayList<MailboxMembership>();
            String queryString = "//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] && [@" + JCRMailboxMembership.SEEN_PROPERTY +"=" + false +"] order by @" + JCRMailboxMembership.UID_PROPERTY;
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
            }
            return list;
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#save(org.apache.james.
     * imap.store.mail.model.MailboxMembership)
     */
    public void save(MailboxMembership message) throws StorageException {
        Node messageNode;
        JCRMailboxMembership membership = (JCRMailboxMembership) message;
        try {
            if (membership.isPersistent()) {

                messageNode = getSession().getNodeByUUID(membership.getUUID());

            } else {
                messageNode = getSession().getRootNode().addNode(JCRUtils.createPath(PATH, String.valueOf(membership.getUid())));
            }
            membership.merge(messageNode);
            getSession().save();

       
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache
     * .james.imap.mailbox.SearchQuery)
     */
    public List<MailboxMembership> searchMailbox(SearchQuery query) throws StorageException {
        try {
            List<MailboxMembership> list = new ArrayList<MailboxMembership>();
            final String xpathQuery = formulateXPath(uuid, query);
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(xpathQuery, Query.XPATH).execute();
            
            NodeIterator it = result.getNodes();
            while (it.hasNext()) {
                list.add(new JCRMailboxMembership(it.nextNode(), logger));
            }
            return list;
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /**
     * Generate the XPath query for the SearchQuery
     * 
     * @param uuid
     * @param query
     * @return xpathQuery
     */
    private String formulateXPath(String uuid, SearchQuery query) {
        final StringBuilder queryBuilder = new StringBuilder(50);
        queryBuilder.append("//" + PATH + "//element(*)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] ");
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
                        queryBuilder.append(" and [@" + JCRMailboxMembership.UID_PROPERTY +"<=").append(high).append("]");
                    } else if (low == high) {
                        queryBuilder.append(" and [@" + JCRMailboxMembership.UID_PROPERTY +"=").append(low).append("]");
                    } else {
                        queryBuilder.append(" and [@" + JCRMailboxMembership.UID_PROPERTY +"<").append(low).append("] and [@" + JCRMailboxMembership.UID_PROPERTY + ">").append(high).append("]");
                    }
                }
            }
        }
        final String jql = queryBuilder.toString();
        return jql;
    }
}
