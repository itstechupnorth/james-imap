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

import java.io.IOException;
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
import org.apache.jackrabbit.JcrConstants;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.AbstractJCRMapper;
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

public class JCRMessageMapper extends AbstractJCRMapper implements MessageMapper<String> {

    private final static String PATH =  "mailboxes";
    private final Log logger;
    private final String uuid;

    public JCRMessageMapper(final Session session, final String uuid, final int scaling, final Log logger) {
        super(session,scaling);
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
            String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"']";
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            NodeIterator nodes = result.getNodes();
            long count = nodes.getSize();
            if (count == -1) {
                count = 0;
                while(nodes.hasNext()) {
                    nodes.nextNode();
                    count++;
                }
            } 
            return count;
        } catch (RepositoryException e) {
            e.printStackTrace();
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
            String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] AND [@" + JCRMailboxMembership.SEEN_PROPERTY +"='false']";
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            NodeIterator nodes = result.getNodes();
            long count = nodes.getSize();
            
            if (count == -1) {
                count = 0;
                while(nodes.hasNext()) {
                    nodes.nextNode();
                    
                    count++;
                }
            } 
            return count;
        } catch (RepositoryException e) {
            e.printStackTrace();
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
    public void delete(MailboxMembership<String> message) throws StorageException {
        JCRMailboxMembership membership = (JCRMailboxMembership) message;
        if (membership.isPersistent()) {
            try {

                getSession().getNodeByUUID(membership.getId()).remove();
            } catch (RepositoryException e) {
                e.printStackTrace();
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
    public List<MailboxMembership<String>> findInMailbox(MessageRange set) throws StorageException {
        try {
            final List<MailboxMembership<String>> results;
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
            e.printStackTrace();
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    private List<MailboxMembership<String>> findMessagesInMailboxAfterUID(String uuid, long uid) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMailboxMembership.UID_PROPERTY + ">=" + uid + "] order by @" + JCRMailboxMembership.UID_PROPERTY;

        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }

    private List<MailboxMembership<String>> findMessagesInMailboxWithUID(String uuid, long uid) throws RepositoryException  {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMailboxMembership.UID_PROPERTY + "=" + uid + "] order by @" + JCRMailboxMembership.UID_PROPERTY;

        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }

    private List<MailboxMembership<String>> findMessagesInMailboxBetweenUIDs(String uuid, long from, long to) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMailboxMembership.UID_PROPERTY + ">=" + from + "] AND [@" + JCRMailboxMembership.UID_PROPERTY + "<=" + to + "] order by @" + JCRMailboxMembership.UID_PROPERTY;
        
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }
    
    private List<MailboxMembership<String>> findMessagesInMailbox(String uuid) throws RepositoryException {        
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        
        String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] order by @" + JCRMailboxMembership.UID_PROPERTY;
        getSession().refresh(true);
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }

    
    
    private List<MailboxMembership<String>> findDeletedMessagesInMailboxAfterUID(String uuid, long uid) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMailboxMembership.UID_PROPERTY + ">=" + uid + "] AND [@" + JCRMailboxMembership.DELETED_PROPERTY+ "='true'] order by @" + JCRMailboxMembership.UID_PROPERTY;
 
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }

    private List<MailboxMembership<String>> findDeletedMessagesInMailboxWithUID(String uuid, long uid) throws RepositoryException  {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMailboxMembership.UID_PROPERTY + "=" + uid + "] AND [@" + JCRMailboxMembership.DELETED_PROPERTY+ "='true']  order by @" + JCRMailboxMembership.UID_PROPERTY;
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            JCRMailboxMembership member = new JCRMailboxMembership(iterator.nextNode(), logger);
            list.add(member);
        }
        return list;
    }

    private List<MailboxMembership<String>> findDeletedMessagesInMailboxBetweenUIDs(String uuid, long from, long to) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMailboxMembership.UID_PROPERTY + ">=" + from + "] AND [@" + JCRMailboxMembership.UID_PROPERTY + "<=" + to + "] AND [@" + JCRMailboxMembership.DELETED_PROPERTY+ "='true'] order by @" + JCRMailboxMembership.UID_PROPERTY;
       
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
        }
        return list;
    }
    
    private List<MailboxMembership<String>> findDeletedMessagesInMailbox(String uuid) throws RepositoryException {
        
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] AND [@" + JCRMailboxMembership.DELETED_PROPERTY+ "='true'] order by @" + JCRMailboxMembership.UID_PROPERTY;
        
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            JCRMailboxMembership member = new JCRMailboxMembership(iterator.nextNode(), logger);
            list.add(member);
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
    public List<MailboxMembership<String>> findMarkedForDeletionInMailbox(MessageRange set) throws StorageException {
        try {
            final List<MailboxMembership<String>> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            final Type type = set.getType();
            switch (type) {
                default:
                case ALL:
                    results = findDeletedMessagesInMailbox(uuid);
                    break;
                case FROM:
                    results = findDeletedMessagesInMailboxAfterUID(uuid, from);
                    break;
                case ONE:
                    results = findDeletedMessagesInMailboxWithUID(uuid, from);
                    break;
                case RANGE:
                    results = findDeletedMessagesInMailboxBetweenUIDs(uuid, from, to);
                    break;       
            }
            return results;
        } catch (RepositoryException e) {
            e.printStackTrace();
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
    public List<MailboxMembership<String>> findRecentMessagesInMailbox() throws StorageException {
        
        try {
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] AND [@" + JCRMailboxMembership.RECENT_PROPERTY +"='true'] order by @" + JCRMailboxMembership.UID_PROPERTY;
            
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
            }
            return list;
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findUnseenMessagesInMailbox()
     */
    public List<MailboxMembership<String>> findUnseenMessagesInMailbox() throws StorageException {
        try {
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            String queryString = "//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] AND [@" + JCRMailboxMembership.SEEN_PROPERTY +"='false'] order by @" + JCRMailboxMembership.UID_PROPERTY;
          
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMailboxMembership(iterator.nextNode(), logger));
            }
            return list;
        } catch (RepositoryException e) {
            e.printStackTrace();
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
    public void save(MailboxMembership<String> message) throws StorageException {
        JCRMailboxMembership membership = (JCRMailboxMembership) message;
        try {
            //JCRUtils.createNodeRecursive(getSession().getRootNode(), mailboxN);
            Node messageNode = null;
            
            if (membership.isPersistent()) {
                messageNode = getSession().getNodeByUUID(membership.getId());
            }

            if (messageNode == null) {
                Node mailboxNode = getSession().getNodeByUUID(uuid);
                Node membershipsNode;
                
                if (mailboxNode.hasNode("mailboxMemberships")) {
                    membershipsNode = mailboxNode.getNode("mailboxMemberships");
                } else {
                    membershipsNode = mailboxNode.addNode("mailboxMemberships","imap:mailboxMemberships");
                }
                
                // TODO: Maybe we should use some kind of hashes for scaling here
                String path = membershipsNode.getPath() + NODE_DELIMITER + JCRUtils.escapePath(String.valueOf(membership.getUid()));
                
                // strip leading /
                path = path.substring(1, path.length());
                
                messageNode = getSession().getRootNode().addNode(path,"imap:mailboxMembership");
                messageNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
            }
            membership.merge(messageNode);
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        } catch (IOException e) {
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
    public List<MailboxMembership<String>> searchMailbox(SearchQuery query) throws StorageException {
        try {
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            final String xpathQuery = formulateXPath(uuid, query);
            
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(xpathQuery, Query.XPATH).execute();
            
            NodeIterator it = result.getNodes();
            while (it.hasNext()) {
                list.add(new JCRMailboxMembership(it.nextNode(), logger));
            }
            return list;
        } catch (RepositoryException e) {
            e.printStackTrace();
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
        queryBuilder.append("//" + PATH + "//element(*,imap:mailboxMembership)[@" + JCRMailboxMembership.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] ");
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
                        queryBuilder.append(" AND [@" + JCRMailboxMembership.UID_PROPERTY +"<=").append(high).append("]");
                    } else if (low == high) {
                        queryBuilder.append(" AND [@" + JCRMailboxMembership.UID_PROPERTY +"=").append(low).append("]");
                    } else {
                        queryBuilder.append(" AND [@" + JCRMailboxMembership.UID_PROPERTY +"<=").append(high).append("] AND [@" + JCRMailboxMembership.UID_PROPERTY + ">=").append(low).append("]");
                    }
                }
            }
        }
        queryBuilder.append(" order by @" + JCRMailboxMembership.UID_PROPERTY);
        final String jql = queryBuilder.toString();
        return jql;
    }
}
