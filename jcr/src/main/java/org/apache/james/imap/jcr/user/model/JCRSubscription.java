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
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.james.imap.jcr.IsPersistent;
import org.apache.james.imap.jcr.JCRImapConstants;
import org.apache.james.imap.store.user.model.Subscription;

/**
 * JCR implementation of a {@link Subscription}
 * 
 */
public class JCRSubscription implements Subscription, IsPersistent, JCRImapConstants {
    public final static String USERNAME_PROPERTY = PROPERTY_PREFIX + "subscriptionUsername";
    public final static String MAILBOX_PROPERTY = PROPERTY_PREFIX  + "subscriptionMailbox";
    
    private Node node;
    private final Log log;
    private String mailbox;
    private String username;

    
    public JCRSubscription(Node node, Log log) {
        this.node = node;
        this.log = log;
    }

    public JCRSubscription(String username, String mailbox, Log log) {
        this.username = username;
        this.mailbox = mailbox;
        this.log = log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.store.user.model.Subscription#getMailbox()
     */
    public String getMailbox() {
        if (isPersistent()) {
            try {
                return node.getProperty(MAILBOX_PROPERTY).getString();
            } catch (RepositoryException e) {
                log.error("Unable to access Property " + MAILBOX_PROPERTY, e);
            }
            return null;
        }
        return mailbox;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.store.user.model.Subscription#getUser()
     */
    public String getUser() {
        if (isPersistent()) {
            try {
                return node.getProperty(USERNAME_PROPERTY).getString();
            } catch (RepositoryException e) {
                log.error("Unable to access Property " + USERNAME_PROPERTY, e);
            }
            return null;
        }
        return username;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.jcr.NodeAware#getNode()
     */
    public Node getNode() {
        return node;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.jcr.IsPersistent#isPersistent()
     */
    public boolean isPersistent() {
        return node != null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.jcr.IsPersistent#merge(javax.jcr.Node)
     */
    public void merge(Node node) throws RepositoryException{
        this.node = node;
        node.setProperty(USERNAME_PROPERTY, username);
        node.setProperty(MAILBOX_PROPERTY, mailbox);

        mailbox = null;
        username = null;
    }

}
