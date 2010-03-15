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
package org.apache.james.imap.jcr.user;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.JCRMapper;
import org.apache.james.imap.jcr.JCRUtils;
import org.apache.james.imap.jcr.Persistent;
import org.apache.james.imap.jcr.user.model.JCRSubscription;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * JCR implementation of a SubscriptionManager. Just be aware, this
 * SubscriptionManager doesn't support transactions. So very call on a method
 * ends in a "real" action
 * 
 */
public class JCRSubscriptionMapper extends JCRMapper implements SubscriptionMapper {

    private final Log log;
    private final static String PATH = PROPERTY_PREFIX + "subscriptions";

    public JCRSubscriptionMapper(final Session session, final Log log) {
        super(session);
        this.log = log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.user.SubscriptionMapper#delete(org.apache
     * .james.imap.store.user.model.Subscription)
     */
    public void delete(Subscription subscription) throws SubscriptionException {
        // Check if the subscription was persistent in JCR if not don't do
        // anything
        if (subscription instanceof Persistent) {
            try {

                Node node = ((Persistent) subscription).getNode();
                node.remove();
                getSession().save();
            } catch (PathNotFoundException e) {
                // do nothing
            } catch (RepositoryException e) {
                throw new SubscriptionException(HumanReadableText.DELETED_FAILED, e);
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.store.user.SubscriptionMapper#
     * findFindMailboxSubscriptionForUser(java.lang.String, java.lang.String)
     */
    public Subscription findFindMailboxSubscriptionForUser(String user, String mailbox) throws SubscriptionException {
        try {
            Node node = getSession().getRootNode().getNode(JCRUtils.createPath(PATH, user, mailbox));
            return new JCRSubscription(node, log);
        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new SubscriptionException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.user.SubscriptionMapper#findSubscriptionsForUser
     * (java.lang.String)
     */
    public List<Subscription> findSubscriptionsForUser(String user) throws SubscriptionException {
        List<Subscription> subList = new ArrayList<Subscription>();
        try {
            String queryString = "//" + PATH + "//element(*)[@" + JCRSubscription.USERNAME_PROPERTY + "='" + user + "']";

            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator nodeIt = result.getNodes();
            while (nodeIt.hasNext()) {
                subList.add(new JCRSubscription(nodeIt.nextNode(), log));
            }
        } catch (PathNotFoundException e) {
            // Do nothing just return the empty list later
        } catch (RepositoryException e) {
            throw new SubscriptionException(HumanReadableText.SEARCH_FAILED, e);
        }
        return subList;

    }

    protected void createPathIfNotExists(String path) throws RepositoryException, PathNotFoundException {
        if (getSession().getRootNode().hasNode(JCRUtils.createPath(path)) == false) {
            getSession().getRootNode().addNode(JCRUtils.createPath(path));
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.user.SubscriptionMapper#save(org.apache.james
     * .imap.store.user.model.Subscription)
     */
    public void save(Subscription subscription) throws SubscriptionException {
        String username = subscription.getUser();
        String mailbox = subscription.getMailbox();
        String nodename = JCRUtils.createPath(PATH, username, mailbox);
        try {
            createPathIfNotExists(PATH);
            Node node;
            JCRSubscription sub = (JCRSubscription) subscription;
            if (sub.isPersistent() == false) {
                 node = getSession().getRootNode().getNode(nodename);
               
            } else {
                node = sub.getNode();
            }
            // Copy new properties to the node
            sub.merge(node);

            getSession().save();
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new SubscriptionException(HumanReadableText.SAVE_FAILED, e);
        }
    }

}
