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
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.AbstractJCRMapper;
import org.apache.james.imap.jcr.user.model.JCRSubscription;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * JCR implementation of a SubscriptionManager
 * 
 */
public class JCRSubscriptionMapper extends AbstractJCRMapper implements SubscriptionMapper {

    public JCRSubscriptionMapper(final Session session, final Log log) {
        super(session, log);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.user.SubscriptionMapper#delete(org.apache
     * .james.imap.store.user.model.Subscription)
     */
    public void delete(Subscription subscription) throws SubscriptionException {

        JCRSubscription sub = (JCRSubscription) subscription;
        try {

            Node node = sub.getNode();
            if (node != null) {
                node.remove();
            }
        } catch (PathNotFoundException e) {
            // do nothing
        } catch (RepositoryException e) {
            throw new SubscriptionException(HumanReadableText.DELETED_FAILED, e);
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
            String queryString = "//" + SUBSCRIPTIONS_PATH + "//element(*,imap:subscription)[@" + JCRSubscription.USERNAME_PROPERTY + "='" + user + "'] AND [@" + JCRSubscription.MAILBOX_PROPERTY +"='" + mailbox + "']";

            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator nodeIt = result.getNodes();
            if (nodeIt.hasNext()) {
                JCRSubscription sub = new JCRSubscription(nodeIt.nextNode(), getLogger());
                return sub;
            }
            
        } catch (PathNotFoundException e) {
            // nothing todo here
        } catch (RepositoryException e) {
            throw new SubscriptionException(HumanReadableText.SEARCH_FAILED, e);
        }
        return null;

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
            String queryString = "//" + SUBSCRIPTIONS_PATH + "//element(*,imap:subscription)[@" + JCRSubscription.USERNAME_PROPERTY + "='" + user + "']";

            QueryManager manager = getSession().getWorkspace().getQueryManager();
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            
            NodeIterator nodeIt = result.getNodes();
            while (nodeIt.hasNext()) {
                subList.add(new JCRSubscription(nodeIt.nextNode(), getLogger()));
            }
        } catch (PathNotFoundException e) {
            // Do nothing just return the empty list later
        } catch (RepositoryException e) {
            throw new SubscriptionException(HumanReadableText.SEARCH_FAILED, e);
        }
        return subList;

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
        try {

            Node node = null;
         
            JCRSubscription sub = (JCRSubscription) findFindMailboxSubscriptionForUser(username, mailbox);
            
            // its a new subscription
            if (sub == null) {
                Node subscriptionsNode = JcrUtils.getOrAddNode(getSession().getRootNode(), SUBSCRIPTIONS_PATH);
                
                // this loop will create a structure like:
                // /mailboxes/u/user/INBOX
                //
                // This is needed to minimize the child nodes a bit
                Node userNode = JcrUtils.getOrAddNode(subscriptionsNode, String.valueOf(username.charAt(0)));
                userNode = JcrUtils.getOrAddNode(userNode, String.valueOf(username));
                node = JcrUtils.getOrAddNode(userNode, mailbox, "imap:subscription");
            } else {
                node = sub.getNode();
            }
            // Copy new properties to the node
            ((JCRSubscription)subscription).merge(node);

        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new SubscriptionException(HumanReadableText.SAVE_FAILED, e);
        }
    }

}
