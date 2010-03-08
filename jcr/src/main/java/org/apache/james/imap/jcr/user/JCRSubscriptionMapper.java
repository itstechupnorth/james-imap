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

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.user.model.JCRSubscription;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.transaction.NonTransactionalMapper;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * JCR implementation of a SubscriptionManager. Just be aware, this SubscriptionManager doesn't
 * support transactions. So very call on a method ends in a "real" action
  *
 */
public class JCRSubscriptionMapper extends NonTransactionalMapper implements SubscriptionMapper{

	private final Session session;
	private final static String PATH = "subscriptions";
	
	public JCRSubscriptionMapper(Session session) {
		this.session = session;
	}
	

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.user.SubscriptionMapper#delete(org.apache.james.imap.store.user.model.Subscription)
	 */
	public void delete(Subscription subscription) throws SubscriptionException {
		try {
			Node node = session.getRootNode().getNode(PATH + "/" + subscription.getUser() + "/" + subscription.getMailbox());
			node.remove();
			session.save();
		} catch (PathNotFoundException e) {
			// do nothing
		} catch (RepositoryException e) {
			throw new SubscriptionException(HumanReadableText.DELETED_FAILED,e);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.user.SubscriptionMapper#findFindMailboxSubscriptionForUser(java.lang.String, java.lang.String)
	 */
	public Subscription findFindMailboxSubscriptionForUser(String user,
			String mailbox) throws SubscriptionException {
		try {
			Node node = session.getRootNode().getNode(PATH + "/" + user + "/" + mailbox);
			return JCRSubscription.from(node);
		} catch (PathNotFoundException e) {
			return null;
		} catch (RepositoryException e) {
            throw new SubscriptionException(HumanReadableText.SEARCH_FAILED, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.user.SubscriptionMapper#findSubscriptionsForUser(java.lang.String)
	 */
	public List<Subscription> findSubscriptionsForUser(String user)
			throws SubscriptionException {
		List<Subscription> subList = new ArrayList<Subscription>();
		try {
			Node node = session.getRootNode().getNode(PATH + "/" + user);
			NodeIterator nodeIt = node.getNodes("*");
			while(nodeIt.hasNext()) {
				subList.add(JCRSubscription.from(nodeIt.nextNode()));
			}
		} catch (PathNotFoundException e) {
			// Do nothing just return the empty list later
		} catch (RepositoryException e) {
			throw new SubscriptionException(HumanReadableText.SEARCH_FAILED,e);
		}
		return subList;

	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.user.SubscriptionMapper#save(org.apache.james.imap.store.user.model.Subscription)
	 */
	public void save(Subscription subscription) throws SubscriptionException {
		String username = subscription.getUser();
		String mailbox = subscription.getMailbox();
		String nodename = username + "/" + mailbox;
		try {
			Node node = session.getRootNode().getNode(PATH);
			Node subNode;
			if (node.hasNode(nodename )) {
				subNode = node.getNode(nodename);
			} else {
				subNode = node.addNode(nodename);
			}
			JCRSubscription.copy(subscription, subNode);
			session.save();
		} catch (RepositoryException e) {
            throw new SubscriptionException(HumanReadableText.SAVE_FAILED, e);
		}		
	}


}