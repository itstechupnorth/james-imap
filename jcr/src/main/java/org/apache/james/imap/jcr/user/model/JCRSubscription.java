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

package org.apache.james.imap.jcr.user.model;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.james.imap.store.user.model.Subscription;


/**
 * JCR implementation of a Subscription
 *
 */
public class JCRSubscription implements Subscription{

	private final String mailbox;

	private final String username;

	public final static String USERNAME_PROPERTY = "username";
	public final static String MAILBOX_PROPERTY = "mailbox";

	public JCRSubscription(String username, String mailbox) {
		this.username = username;
		this.mailbox = mailbox;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.user.model.Subscription#getMailbox()
	 */
	public String getMailbox() {
		return mailbox;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.user.model.Subscription#getUser()
	 */
	public String getUser() {
		return username;
	}
	
	/**
	 * Return the JCRSubscription for the given node
	 * 
	 * @param node 
	 * @return subscription
	 * @throws ValueFormatException
	 * @throws PathNotFoundException
	 * @throws RepositoryException
	 */
	public static JCRSubscription from(Node node) throws ValueFormatException, PathNotFoundException, RepositoryException {
		String username = node.getProperty(USERNAME_PROPERTY).getString();;
		String mailbox = node.getProperty(MAILBOX_PROPERTY).getString();
		
		return new JCRSubscription(username, mailbox);
	}
	
	
	/**
	 * Copy all needed properties to the given node for the given subscription
	 * 
	 * @param subscription
	 * @param node
	 * @return node
	 * @throws ValueFormatException
	 * @throws VersionException
	 * @throws LockException
	 * @throws ConstraintViolationException
	 * @throws RepositoryException
	 */
	public static Node copy(Subscription subscription, Node node) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
		node.setProperty(USERNAME_PROPERTY, subscription.getUser());
		node.setProperty(MAILBOX_PROPERTY, subscription.getMailbox());
		return node;
		
	}
}
