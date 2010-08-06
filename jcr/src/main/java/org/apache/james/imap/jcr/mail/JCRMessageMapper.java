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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.AbstractJCRMapper;
import org.apache.james.imap.jcr.MailboxSessionJCRRepository;
import org.apache.james.imap.jcr.NodeLocker;
import org.apache.james.imap.jcr.NodeLocker.NodeLockedExecution;
import org.apache.james.imap.jcr.mail.model.JCRMailbox;
import org.apache.james.imap.jcr.mail.model.JCRMessage;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.mailbox.MessageRange.Type;
import org.apache.james.imap.mailbox.SearchQuery.Criterion;
import org.apache.james.imap.mailbox.SearchQuery.NumericRange;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;

/**
 * JCR implementation of a {@link MessageMapper}
 *
 */
public class JCRMessageMapper extends AbstractJCRMapper implements MessageMapper<String> {

    /**
     * Store the messages directly in the mailbox:
     * .../mailbox/
     */
    public final static int MESSAGE_SCALE_NONE = 0;
    
    /**
     * Store the messages under a year directory in the mailbox:
     * .../mailbox/2010/
     */
    public final static int MESSAGE_SCALE_YEAR = 1;
    
    /**
     * Store the messages under a year/month directory in the mailbox:
     * .../mailbox/2010/05/
     */
    public final static int MESSAGE_SCALE_MONTH = 2;
    
    /**
     * Store the messages under a year/month/day directory in the mailbox:
     * .../mailbox/2010/05/01/
     */
    public final static int MESSAGE_SCALE_DAY = 3;
    
    /**
     * Store the messages under a year/month/day/hour directory in the mailbox:
     * .../mailbox/2010/05/02/11
     */
    public final static int MESSAGE_SCALE_HOUR = 4;
    
    
    /**
     * Store the messages under a year/month/day/hour/min directory in the mailbox:
     * .../mailbox/2010/05/02/11/59
     */
    public final static int MESSAGE_SCALE_MINUTE = 5;

    private final int scaleType;
    

    /**
     * 
     * @see #JCRMessageMapper(MailboxSessionJCRRepository, MailboxSession, NodeLocker, Log, int)
     * 
     * In this case {@link #MESSAGE_SCALE_DAY} is used
     */
    public JCRMessageMapper(final MailboxSessionJCRRepository repos, MailboxSession session, NodeLocker locker, final Log logger) {
        this(repos, session, locker, logger, MESSAGE_SCALE_DAY);
    }

    /**
     * Construct a new {@link JCRMessageMapper} instance
     * 
     * @param repos {@link MailboxSessionJCRRepository} to use
     * @param session {@link MailboxSession} to which the mapper is bound
     * @param locker {@link NodeLocker} for locking Nodes
     * @param logger Lo
     * @param scaleType the scale type to use when storing messages. See {@link #MESSAGE_SCALE_NONE}, {@link #MESSAGE_SCALE_YEAR}, {@link #MESSAGE_SCALE_MONTH}, {@link #MESSAGE_SCALE_DAY},
     *                  {@link #MESSAGE_SCALE_HOUR}, {@link #MESSAGE_SCALE_MINUTE}  
     */
    public JCRMessageMapper(final MailboxSessionJCRRepository repos, MailboxSession session, NodeLocker locker, final Log logger, int scaleType) {
        super(repos, session, locker, logger);
        this.scaleType = scaleType;
    }
    
    /**
     * Return the path to the mailbox. This path is escaped to be able to use it in xpath queries
     * 
     * See http://wiki.apache.org/jackrabbit/EncodingAndEscaping
     * 
     * @param mailbox
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private String getMailboxPath(Mailbox<String> mailbox) throws ItemNotFoundException, RepositoryException {
        return ISO9075.encodePath(getSession().getNodeByIdentifier(mailbox.getMailboxId()).getPath());
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox(Mailbox<String> mailbox) throws StorageException {
        try {
            // we use order by because without it count will always be 0 in jackrabbit
            String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message) order by @" + JCRMessage.UID_PROPERTY;
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
    public long countUnseenMessagesInMailbox(Mailbox<String> mailbox) throws StorageException {
        
        try {
            // we use order by because without it count will always be 0 in jackrabbit
            String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.SEEN_PROPERTY +"='false'] order by @" + JCRMessage.UID_PROPERTY;
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
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#delete(java.lang.Object, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void delete(Mailbox<String> mailbox, MailboxMembership<String> message) throws StorageException {
        JCRMessage membership = (JCRMessage) message;
        if (membership.isPersistent()) {
            try {

                getSession().getNodeByIdentifier(membership.getId()).remove();
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
    public List<MailboxMembership<String>> findInMailbox(Mailbox<String> mailbox, MessageRange set) throws StorageException {
        try {
            final List<MailboxMembership<String>> results;
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
                    results = findMessageInMailboxWithUID(mailbox, from);
                    break;
                case RANGE:
                    results = findMessagesInMailboxBetweenUIDs(mailbox, from, to);
                    break;       
            }
            return results;
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    private List<MailboxMembership<String>> findMessagesInMailboxAfterUID(Mailbox<String> mailbox, long uid) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + ">=" + uid + "] order by @" + JCRMessage.UID_PROPERTY;

        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    private List<MailboxMembership<String>> findMessageInMailboxWithUID(Mailbox<String> mailbox, long uid) throws RepositoryException  {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + "=" + uid + "]";

        QueryManager manager = getSession().getWorkspace().getQueryManager();
        Query query = manager.createQuery(queryString, Query.XPATH);
        query.setLimit(1);
        QueryResult result = query.execute();
        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    private List<MailboxMembership<String>> findMessagesInMailboxBetweenUIDs(Mailbox<String> mailbox, long from, long to) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + ">=" + from + " and @" + JCRMessage.UID_PROPERTY + "<=" + to + "] order by @" + JCRMessage.UID_PROPERTY;
        
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }
    
    private List<MailboxMembership<String>> findMessagesInMailbox(Mailbox<String> mailbox) throws RepositoryException {        
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message) order by @" + JCRMessage.UID_PROPERTY;
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    
    
    private List<MailboxMembership<String>> findDeletedMessagesInMailboxAfterUID(Mailbox<String> mailbox, long uid) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + ">=" + uid + " and @" + JCRMessage.DELETED_PROPERTY+ "='true'] order by @" + JCRMessage.UID_PROPERTY;
 
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }

    private List<MailboxMembership<String>> findDeletedMessageInMailboxWithUID(Mailbox<String> mailbox, long uid) throws RepositoryException  {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + "=" + uid + " and @" + JCRMessage.DELETED_PROPERTY+ "='true']";
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        Query query = manager.createQuery(queryString, Query.XPATH);
        query.setLimit(1);
        QueryResult result = query.execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            JCRMessage member = new JCRMessage(iterator.nextNode(), getLogger());
            list.add(member);
        }
        return list;
    }

    private List<MailboxMembership<String>> findDeletedMessagesInMailboxBetweenUIDs(Mailbox<String> mailbox, long from, long to) throws RepositoryException {
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.UID_PROPERTY + ">=" + from + " and @" + JCRMessage.UID_PROPERTY + "<=" + to + " and @" + JCRMessage.DELETED_PROPERTY+ "='true'] order by @" + JCRMessage.UID_PROPERTY;
       
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();

        NodeIterator iterator = result.getNodes();
        while (iterator.hasNext()) {
            list.add(new JCRMessage(iterator.nextNode(), getLogger()));
        }
        return list;
    }
    
    private List<MailboxMembership<String>> findDeletedMessagesInMailbox(Mailbox<String> mailbox) throws RepositoryException {
        
        List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
        String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.DELETED_PROPERTY+ "='true'] order by @" + JCRMessage.UID_PROPERTY;
        
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
    public List<MailboxMembership<String>> findMarkedForDeletionInMailbox(Mailbox<String> mailbox, MessageRange set) throws StorageException {
        try {
            final List<MailboxMembership<String>> results;
            final long from = set.getUidFrom();
            final long to = set.getUidTo();
            final Type type = set.getType();
            switch (type) {
                default:
                case ALL:
                    results = findDeletedMessagesInMailbox(mailbox);
                    break;
                case FROM:
                    results = findDeletedMessagesInMailboxAfterUID(mailbox, from);
                    break;
                case ONE:
                    results = findDeletedMessageInMailboxWithUID(mailbox, from);
                    break;
                case RANGE:
                    results = findDeletedMessagesInMailboxBetweenUIDs(mailbox, from, to);
                    break;       
            }
            return results;
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /*
     * 
     * TODO: Maybe we should better use an ItemVisitor and just traverse through the child nodes. This could be a way faster
     * 
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#findRecentMessagesInMailbox
     * ()
     */
    public List<MailboxMembership<String>> findRecentMessagesInMailbox(Mailbox<String> mailbox, int limit) throws StorageException {
        
        try {
 
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.RECENT_PROPERTY +"='true'] order by @" + JCRMessage.UID_PROPERTY;
            
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            Query query = manager.createQuery(queryString, Query.XPATH);
            if (limit > 0) {
                query.setLimit(limit);
            }
            QueryResult result = query.execute();
            
            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMessage(iterator.nextNode(), getLogger()));
            }
            return list;

        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }


    /*
     * TODO: Maybe we should better use an ItemVisitor and just traverse through the child nodes. This could be a way faster
     * 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findUnseenMessagesInMailbox()
     */
    public List<MailboxMembership<String>> findUnseenMessagesInMailbox(Mailbox<String> mailbox, int limit) throws StorageException {
        try {
  
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            String queryString = "/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)[@" + JCRMessage.SEEN_PROPERTY +"='false'] order by @" + JCRMessage.UID_PROPERTY;

            QueryManager manager = getSession().getWorkspace().getQueryManager();
            
            Query query = manager.createQuery(queryString, Query.XPATH);
            if (limit > 0) {
                query.setLimit(limit);
            }
            QueryResult result = query.execute();

            NodeIterator iterator = result.getNodes();
            while(iterator.hasNext()) {
                list.add(new JCRMessage(iterator.nextNode(), getLogger()));
            }
            return list;
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#save(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public long save(Mailbox<String> mailbox, MailboxMembership<String> message) throws StorageException {
        final JCRMessage membership = (JCRMessage) message;
        try {

            Node messageNode = null;

            if (membership.isPersistent()) {
                messageNode = getSession().getNodeByIdentifier(membership.getId());
            }

            if (messageNode == null) {
                Date date = message.getInternalDate();
                if (date == null) {
                    date = new Date();
                }

                // extracte the date from the message to create node structure
                // later
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                final String year = convertIntToString(cal.get(Calendar.YEAR));
                final String month = convertIntToString(cal.get(Calendar.MONTH) + 1);
                final String day = convertIntToString(cal.get(Calendar.DAY_OF_MONTH));
                final String hour = convertIntToString(cal.get(Calendar.HOUR_OF_DAY));
                final String min = convertIntToString(cal.get(Calendar.MINUTE));

                Node node = null;
                Node mailboxNode = getSession().getNodeByIdentifier(mailbox.getMailboxId());

                final long nextUid = reserveNextUid((JCRMailbox) mailbox);
                
                NodeLocker locker = getNodeLocker();

                if (scaleType > MESSAGE_SCALE_NONE) {
                    // we lock the whole mailbox with all its childs while
                    // adding the folder structure for the date
                    
                    // TODO: Maybe we should just lock the last child folder to
                    // improve performance
                    node = locker.execute(new NodeLocker.NodeLockedExecution<Node>() {

                        public Node execute(Node node) throws RepositoryException {

                            if (scaleType >= MESSAGE_SCALE_YEAR) {
                                node = JcrUtils.getOrAddFolder(node, year);
                                node.addMixin(JcrConstants.MIX_LOCKABLE);

                                if (scaleType >= MESSAGE_SCALE_MONTH) {
                                    node = JcrUtils.getOrAddFolder(node, month);
                                    node.addMixin(JcrConstants.MIX_LOCKABLE);

                                    if (scaleType >= MESSAGE_SCALE_DAY) {
                                        node = JcrUtils.getOrAddFolder(node, day);
                                        node.addMixin(JcrConstants.MIX_LOCKABLE);

                                        if (scaleType >= MESSAGE_SCALE_HOUR) {
                                            node = JcrUtils.getOrAddFolder(node, hour);
                                            node.addMixin(JcrConstants.MIX_LOCKABLE);

                                            if (scaleType >= MESSAGE_SCALE_MINUTE) {
                                                node = JcrUtils.getOrAddFolder(node, min);
                                                node.addMixin(JcrConstants.MIX_LOCKABLE);
                                            }
                                        }
                                    }
                                }
                            }

                            // save the folders for now
                            getSession().save();
                            return node;
                        }

                        public boolean isDeepLocked() {
                            return true;
                        }
                    }, mailboxNode, Node.class);
                } else {
                    node = mailboxNode;
                }

                // lock the day node and add the message
                locker.execute(new NodeLockedExecution<Void>() {

                    public Void execute(Node node) throws RepositoryException {
                        Node messageNode = node.addNode(String.valueOf(nextUid), "nt:file");
                        messageNode.addMixin("jamesMailbox:message");
                        try {
                            membership.merge(messageNode);
                            messageNode.setProperty(JCRMessage.UID_PROPERTY, nextUid);

                        } catch (IOException e) {
                            throw new RepositoryException("Unable to merge message in to tree", e);
                        }
                        // save the message
                        getSession().save();

                        return null;
                    }

                    public boolean isDeepLocked() {
                        return true;
                    }
                }, node, Void.class);
                return nextUid;

            } else {
                membership.merge(messageNode);
                return membership.getUid();
            }
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        } catch (IOException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        } catch (InterruptedException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        }

    }

    /**
     * Convert the given int value to a String. If the int value is smaller then 9 it will prefix the String with 0.
     * 
     * @param value
     * @return stringValue
     */
    private String convertIntToString(int value) {
        if (value <= 9) {
            return "0" +String.valueOf(value);
        } else {
            return String.valueOf(value);
        }
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache
     * .james.imap.mailbox.SearchQuery)
     */
    public List<MailboxMembership<String>> searchMailbox(Mailbox<String> mailbox, SearchQuery query) throws StorageException {
        try {
            List<MailboxMembership<String>> list = new ArrayList<MailboxMembership<String>>();
            final Query xQuery = formulateXPath(mailbox, query);
            
            QueryResult result = xQuery.execute();
            
            NodeIterator it = result.getNodes();
            while (it.hasNext()) {
                list.add(new JCRMessage(it.nextNode(), getLogger()));
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
     * @throws RepositoryException 
     * @throws ItemNotFoundException 
     */
    private Query formulateXPath(Mailbox<String> mailbox, SearchQuery query) throws ItemNotFoundException, RepositoryException {
        final StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("/jcr:root" + getMailboxPath(mailbox) + "//element(*,jamesMailbox:message)");
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
                    if (i > 0) {
                        queryBuilder.append(" and ");
                    } else {
                        queryBuilder.append("[");
                    }
                    if (low == Long.MAX_VALUE) {
                        range = true;
                        queryBuilder.append("@" + JCRMessage.UID_PROPERTY +"<=").append(high);
                    } else if (low == high) {
                        range = false;
                        queryBuilder.append("@" + JCRMessage.UID_PROPERTY +"=").append(low);
                    } else {
                        range = true;
                        queryBuilder.append("@" + JCRMessage.UID_PROPERTY +"<=").append(high).append(" and @" + JCRMessage.UID_PROPERTY + ">=").append(low);
                    }
                }
            }
        }
        if (rangeLength > 0) queryBuilder.append("]");
        
        if (rangeLength != 0 || range) {
            queryBuilder.append(" order by @" + JCRMessage.UID_PROPERTY);
        }
        
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        Query xQuery = manager.createQuery(queryBuilder.toString(), Query.XPATH);
        
        // Check if we only need to fetch 1 message, if so we can set a limit to speed up things
        if (rangeLength == 1 && range == false) {
            xQuery.setLimit(1);
        }
        return xQuery;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#copy(java.lang.Object, long, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public MailboxMembership<String> copy(Mailbox<String> mailbox, MailboxMembership<String> oldmessage) throws StorageException{
        try {
            long uid = reserveNextUid((JCRMailbox) mailbox);
            String newMessagePath = getSession().getNodeByIdentifier(mailbox.getMailboxId()).getPath() + NODE_DELIMITER + String.valueOf(uid);
            getSession().getWorkspace().copy(((JCRMessage)oldmessage).getNode().getPath(), getSession().getNodeByIdentifier(mailbox.getMailboxId()).getPath() + NODE_DELIMITER + String.valueOf(uid));
            Node node = getSession().getNode(newMessagePath);
            node.setProperty(JCRMessage.MAILBOX_UUID_PROPERTY, mailbox.getMailboxId());
            node.setProperty(JCRMessage.UID_PROPERTY, uid);
            return new JCRMessage(node,getLogger());
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        } catch (InterruptedException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        }
    }
    
    protected long reserveNextUid(Mailbox<String> mailbox) throws RepositoryException, InterruptedException {
        long result = getNodeLocker().execute(new NodeLocker.NodeLockedExecution<Long>() {

            public Long execute(Node node) throws RepositoryException {
                Property uidProp = node.getProperty(JCRMailbox.LASTUID_PROPERTY);
                long uid = uidProp.getLong();
                uid++;
                uidProp.setValue(uid);
                uidProp.getSession().save();
                return uid;
            }

            public boolean isDeepLocked() {
                return true;
            }
            
        }, getSession().getNodeByIdentifier(mailbox.getMailboxId()), Long.class);
        return result;
        
    }

}
