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
package org.apache.james.imap.jcr.mail.model;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.james.imap.jcr.JCRImapConstants;
import org.apache.james.imap.store.mail.model.Mailbox;


/**
 * JCR implementation of a Mailbox
 */
public class JCRMailbox implements Mailbox{

    public final static String ID_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "id";
    public final static String NAME_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "name";
    public final static String UIDVALIDITY_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "uidValidity";
    public final static String LASTUID_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "lastUid";

    private long id = -1;
    private String name;
    private final long uidValidity;
    private long lastUid = 0;
    
    public JCRMailbox(final long id, final String name, final long uidValidity, final long lastUid) {
        this.id = id;
        this.name = name;
        this.uidValidity = uidValidity;
        this.lastUid = lastUid;
    }
    
    public JCRMailbox( final String name, final long uidValidity) {
        this.name = name;
        this.uidValidity = uidValidity;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#consumeUid()
     */
    public void consumeUid() {
        lastUid++;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#getLastUid()
     */
    public long getLastUid() {
        return lastUid;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#getMailboxId()
     */
    public long getMailboxId() {
        return id;
    }

   
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#getUidValidity()
     */
    public long getUidValidity() {
        return uidValidity;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#setName(java.lang.String)
     */
    public void setName(String name) {     
        this.name = name;
    }
    
    /**
     * Create a JCRMailbox from the given Node
     * 
     * @param node
     * @return jcrMailbox
     * @throws ValueFormatException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public static JCRMailbox from(Node node) throws ValueFormatException, PathNotFoundException, RepositoryException {
        long id = node.getProperty(ID_PROPERTY).getLong();
        String name = node.getProperty(NAME_PROPERTY).getString();
        long uidValidity = node.getProperty(UIDVALIDITY_PROPERTY).getLong();
        long lastUid = node.getProperty(LASTUID_PROPERTY).getLong();

        return new JCRMailbox(id, name, uidValidity, lastUid);
    }

    /**
     * Copy the mailbox to the given Node
     * 
     * @param node
     * @param mailbox
     * @return node
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public static Node copy(Node node, Mailbox mailbox) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        node.setProperty(ID_PROPERTY, mailbox.getMailboxId());
        node.setProperty(NAME_PROPERTY, mailbox.getName());
        node.setProperty(UIDVALIDITY_PROPERTY, mailbox.getUidValidity());
        node.setProperty(LASTUID_PROPERTY, mailbox.getLastUid());
        return node;
    }
}
