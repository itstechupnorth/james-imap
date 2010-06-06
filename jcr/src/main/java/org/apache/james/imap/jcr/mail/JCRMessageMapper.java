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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.Locked;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.AbstractJCRMapper;
import org.apache.james.imap.jcr.MailboxSessionJCRRepository;
import org.apache.james.imap.jcr.NodeLocker;
import org.apache.james.imap.jcr.mail.model.JCRMessage;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.mailbox.MessageRange.Type;
import org.apache.james.imap.mailbox.SearchQuery.Criterion;
import org.apache.james.imap.mailbox.SearchQuery.NumericRange;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.MailboxMembership;

/**
 * JCR implementation of a {@link MessageMapper}
 *
 */
public class JCRMessageMapper extends AbstractJCRMapper implements MessageMapper<String> {

    public JCRMessageMapper(final MailboxSessionJCRRepository repos, MailboxSession session, NodeLocker locker, final Log logger) {
        super(repos, session, locker, logger);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox(String uuid) throws StorageException {
        try {
            // we use order by because without it count will always be 0 in jackrabbit
            String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] order by @" + JCRMessage.UID_PROPERTY;
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
    public long countUnseenMessagesInMailbox(String uuid) throws StorageException {
        
        try {
            // we use order by because without it count will always be 0 in jackrabbit
            String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] AND [@" + JCRMessage.SEEN_PROPERTY +"='false'] order by @" + JCRMessage.UID_PROPERTY;
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
     * @see org.apache.james.imap.store.mail.MessageMapper#delete(java.lang.Object, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void delete(String uuid, MailboxMembership<String> message) throws StorageException {
        JCRMessage membership = (JCRMessage) message;
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
    public List<MailboxMembership<String>> findInMailbox(String uuid, MessageRange set) throws StorageException {
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
        String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMessage.UID_PROPERTY + ">=" + uid + "] order by @" + JCRMessage.UID_PROPERTY;

        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    private List<MailboxMembership<String>> findMessagesInMailboxWithUID(String uuid, long uid) throws RepositoryException  {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMessage.UID_PROPERTY + "=" + uid + "] order by @" + JCRMessage.UID_PROPERTY;

        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    private List<MailboxMembership<String>> findMessagesInMailboxBetweenUIDs(String uuid, long from, long to) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMessage.UID_PROPERTY + ">=" + from + "] AND [@" + JCRMessage.UID_PROPERTY + "<=" + to + "] order by @" + JCRMessage.UID_PROPERTY;
        
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }
    
    private List<MailboxMembership<String>> findMessagesInMailbox(String uuid) throws RepositoryException {        
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        
        String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] order by @" + JCRMessage.UID_PROPERTY;
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    
    
    private List<MailboxMembership<String>> findDeletedMessagesInMailboxAfterUID(String uuid, long uid) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMessage.UID_PROPERTY + ">=" + uid + "] AND [@" + JCRMessage.DELETED_PROPERTY+ "='true'] order by @" + JCRMessage.UID_PROPERTY;
 
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    private List<MailboxMembership<String>> findDeletedMessagesInMailboxWithUID(String uuid, long uid) throws RepositoryException  {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMessage.UID_PROPERTY + "=" + uid + "] AND [@" + JCRMessage.DELETED_PROPERTY+ "='true']  order by @" + JCRMessage.UID_PROPERTY;
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            JCRMessage member = new JCRMessage(iterator.nextNode(), getLogger());
            list.add(member);
        }
        return list;
    }

    private List<MailboxMembership<String>> findDeletedMessagesInMailboxBetweenUIDs(String uuid, long from, long to) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY + "='" + uuid + "'] AND [@" + JCRMessage.UID_PROPERTY + ">=" + from + "] AND [@" + JCRMessage.UID_PROPERTY + "<=" + to + "] AND [@" + JCRMessage.DELETED_PROPERTY+ "='true'] order by @" + JCRMessage.UID_PROPERTY;
       
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }
    
    private List<MailboxMembership<String>> findDeletedMessagesInMailbox(String uuid) throws RepositoryException {
        
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] AND [@" + JCRMessage.DELETED_PROPERTY+ "='true'] order by @" + JCRMessage.UID_PROPERTY;
        
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            JCRMessage member = new JCRMessage(iterator.nextNode(), getLogger());
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
    public List<MailboxMembership<String>> findMarkedForDeletionInMailbox(String uuid, MessageRange set) throws StorageException {
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
    public List<MailboxMembership<String>> findRecentMessagesInMailbox(String uuid) throws StorageException {
        
        try {
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] AND [@" + JCRMessage.RECENT_PROPERTY +"='true'] order by @" + JCRMessage.UID_PROPERTY;
            
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMessage(iterator.nextNode(), getLogger()));
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
    public List<MailboxMembership<String>> findUnseenMessagesInMailbox(String uuid) throws StorageException {
        try {
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            String queryString = "//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] AND [@" + JCRMessage.SEEN_PROPERTY +"='false'] order by @" + JCRMessage.UID_PROPERTY;
          
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMessage(iterator.nextNode(), getLogger()));
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
    public void save(String uuid, MailboxMembership<String> message) throws StorageException {
        final JCRMessage membership = (JCRMessage) message;
        try {
            //JCRUtils.createNodeRecursive(getSession().getRootNode(), mailboxN);
            Node messageNode = null;
            
            if (membership.isPersistent()) {
                messageNode = getSession().getNodeByUUID(membership.getId());
            }

            if (messageNode == null) {
                Date date = message.getInternalDate();
                if (date == null) {
                    date = new Date();
                }
                
                // extracte the date from the message to create node structure later
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                final String year = String.valueOf(cal.get(Calendar.YEAR));
                final String month = String.valueOf(cal.get(Calendar.MONTH) +1);
                final String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
               
                Node dayNode = null;
                Node mailboxNode = getSession().getNodeByUUID(uuid);
                String dayNodePath = year + NODE_DELIMITER + month + NODE_DELIMITER + day;
                boolean found = mailboxNode.hasNode(dayNodePath);
                
                // check if the node for the day already exists. if not we need to create the structure
                if (found == false) {
                    // we lock the whole mailbox with all its childs while
                    // adding the folder structure for the date
                    // TODO: Maybe we should just lock the last child folder
                    dayNode = (Node) new Locked() {

                        @Override
                        protected Object run(Node mailboxNode) throws RepositoryException {

                            Node yearNode = JcrUtils.getOrAddFolder(mailboxNode, year);
                            yearNode.addMixin(JcrConstants.MIX_LOCKABLE);

                            Node monthNode = JcrUtils.getOrAddFolder(yearNode, month);
                            monthNode.addMixin(JcrConstants.MIX_LOCKABLE);

                            Node dayNode = JcrUtils.getOrAddFolder(monthNode, day);
                            dayNode.addMixin(JcrConstants.MIX_LOCKABLE);
                            // save the folders for now
                            getSession().save();
                            return dayNode;
                        }
                    }.with(mailboxNode, true);
                } else {
                    
                    dayNode = mailboxNode.getNode(dayNodePath);
                }
                
                
                // lock the day node and add the message
                new Locked() {

                    @Override
                    protected Object run(Node dayNode) throws RepositoryException {
                        Node messageNode = dayNode.addNode(String.valueOf(membership.getUid()),"jamesMailbox:message");
                        try {
                            membership.merge(messageNode);
                        } catch (IOException e) {
                            throw new RepositoryException("Unable to merge message in to tree", e);
                        }
                        // save the message 
                        getSession().save();

                        return null;
                    }
                    
                }.with(dayNode,false);
                
              
            } else {
                membership.merge(messageNode);
            }
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        } catch (IOException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        } catch (InterruptedException e) {
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
    public List<MailboxMembership<String>> searchMailbox(String uuid, SearchQuery query) throws StorageException {
        try {
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            final String xpathQuery = formulateXPath(uuid, query);
            
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(xpathQuery, Query.XPATH).execute();
            
            NodeIterator it = result.getNodes();
            while (it.hasNext()) {
                list.add(new JCRMessage(it.nextNode(), getLogger()));
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
        queryBuilder.append("//" + MAILBOXES_PATH + "//element(*,jamesMailbox:message)[@" + JCRMessage.MAILBOX_UUID_PROPERTY +"='" + uuid +"'] ");
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
                        queryBuilder.append(" AND [@" + JCRMessage.UID_PROPERTY +"<=").append(high).append("]");
                    } else if (low == high) {
                        queryBuilder.append(" AND [@" + JCRMessage.UID_PROPERTY +"=").append(low).append("]");
                    } else {
                        queryBuilder.append(" AND [@" + JCRMessage.UID_PROPERTY +"<=").append(high).append("] AND [@" + JCRMessage.UID_PROPERTY + ">=").append(low).append("]");
                    }
                }
            }
        }
        queryBuilder.append(" order by @" + JCRMessage.UID_PROPERTY);
        final String jql = queryBuilder.toString();
        return jql;
    }
}
