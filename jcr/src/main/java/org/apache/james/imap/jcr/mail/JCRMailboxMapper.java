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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.AbstractJCRMapper;
import org.apache.james.imap.jcr.MailboxSessionJCRRepository;
import org.apache.james.imap.jcr.NodeLocker;
import org.apache.james.imap.jcr.NodeLocker.NodeLockedExecution;
import org.apache.james.imap.jcr.mail.model.JCRMailbox;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;

/**
 * JCR implementation of a MailboxMapper
 * 
 * 
 */
public class JCRMailboxMapper extends AbstractJCRMapper implements MailboxMapper<String> {

    private char delimiter;

    public JCRMailboxMapper(final MailboxSessionJCRRepository repos, MailboxSession session, final NodeLocker locker, final Log logger, char delimiter) {
        super(repos, session, locker, logger);
        this.delimiter = delimiter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MailboxMapper#delete(org.apache.james
     * .imap.store.mail.model.Mailbox)
     */
    public void delete(Mailbox<String> mailbox) throws StorageException {
        try {
            Node node = getSession().getNodeByIdentifier(((JCRMailbox) mailbox).getMailboxId());
                   
            node.remove();
            
        } catch (PathNotFoundException e) {
            // mailbox does not exists..
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.DELETED_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.store.mail.MailboxMapper#deleteAll()
     */
    public void deleteAll() throws StorageException {
        try {
            getSession().getRootNode().getNode(MAILBOXES_PATH).remove();

        } catch (PathNotFoundException e) {
            // nothing todo
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.DELETED_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxById(java.lang.Object)
     */
    public Mailbox<String> findMailboxById(String mailboxId) throws StorageException, MailboxNotFoundException {
        try {
            return new JCRMailbox(getSession().getNodeByIdentifier(mailboxId), getLogger());
        } catch (PathNotFoundException e) {
            throw new MailboxNotFoundException(mailboxId);
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MailboxMapper#findMailboxByName(java
     * .lang.String)
     */
    public Mailbox<String> findMailboxByName(String name) throws StorageException, MailboxNotFoundException {
        try {
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            String queryString = "/jcr:root/" + MAILBOXES_PATH + "//element(*,jamesMailbox:mailbox)[@" + JCRMailbox.NAME_PROPERTY + "='" + name + "']";
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            NodeIterator it = result.getNodes();
            if (it.hasNext()) {
                return new JCRMailbox(it.nextNode(), getLogger());
            }
            throw new MailboxNotFoundException(name);
        } catch (PathNotFoundException e) {
            throw new MailboxNotFoundException(name);
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MailboxMapper#findMailboxWithNameLike
     * (java.lang.String)
     */
    public List<Mailbox<String>> findMailboxWithNameLike(String name) throws StorageException {
        List<Mailbox<String>> mailboxList = new ArrayList<Mailbox<String>>();
        try {
            QueryManager manager = getSession().getWorkspace().getQueryManager();
            String queryString = "/jcr:root/" + MAILBOXES_PATH + "//element(*,jamesMailbox:mailbox)[jcr:like(@" + JCRMailbox.NAME_PROPERTY + ",'%" + name + "%')]";
            QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
            NodeIterator it = result.getNodes();
            while (it.hasNext()) {
                mailboxList.add(new JCRMailbox(it.nextNode(), getLogger()));
            }
        } catch (PathNotFoundException e) {
            // nothing todo
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
        return mailboxList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MailboxMapper#save(org.apache.james.
     * imap.store.mail.model.Mailbox)
     */
    public void save(Mailbox<String> mailbox) throws StorageException {
        
        try {
            final JCRMailbox jcrMailbox = (JCRMailbox)mailbox;
            Node node = null;

            if (jcrMailbox.isPersistent()) {
                node = getSession().getNodeByIdentifier(jcrMailbox.getMailboxId());
            }
            if (node == null) {
                Node rootNode = getSession().getRootNode();
                Node mailboxNode;
                if (rootNode.hasNode(MAILBOXES_PATH) == false) {
                    mailboxNode = rootNode.addNode(MAILBOXES_PATH);
                    mailboxNode.addMixin(JcrConstants.MIX_LOCKABLE);
                    getSession().save();
                } else {
                    mailboxNode = rootNode.getNode(MAILBOXES_PATH);
                }
                NodeLocker locker = getNodeLocker();
                locker.execute(new NodeLockedExecution<Void>() {

                    public Void execute(Node node) throws RepositoryException {
                        final String name = jcrMailbox.getName();
                        
                        //split the name so we can construct a nice node tree
                        final String nameParts[] = name.split("\\" + String.valueOf(delimiter), 3);
                        
                        // this loop will create a structure like:
                        // /mailboxes/u/user/INBOX
                        //
                        // This is needed to minimize the child nodes a bit
                        for (int i = 0; i < nameParts.length; i++) {
                           String part = nameParts[i];
                           if (i == 1) {
                               node = JcrUtils.getOrAddNode(node, String.valueOf(part.charAt(0)), "nt:unstructured");   

                           } 
                           node = JcrUtils.getOrAddNode(node, part, "nt:unstructured");
                           node.addMixin("jamesMailbox:mailbox");
                           
                        }
                        jcrMailbox.merge(node);

                        getSession().save();
                        return null;
                    }

                    public boolean isDeepLocked() {
                        return true;
                    }
                    
                }, mailboxNode, Void.class);
                
           } else {
               jcrMailbox.merge(node);
           }
            
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        } catch (InterruptedException e) {
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.store.mail.MailboxMapper#hasChildren(org.apache.james.
     * imap.store.mail.model.Mailbox)
     */
    public boolean hasChildren(Mailbox<String> mailbox)
            throws StorageException, MailboxNotFoundException {
        try {
            QueryManager manager = getSession().getWorkspace()
                    .getQueryManager();
            String queryString = "/jcr:root/" + MAILBOXES_PATH
                    + "//element(*,jamesMailbox:mailbox)[jcr:like(@"
                    + JCRMailbox.NAME_PROPERTY + ",'" + mailbox.getName() + delimiter + "%')]";
            QueryResult result = manager.createQuery(queryString, Query.XPATH)
                    .execute();
            NodeIterator it = result.getNodes();
            return it.hasNext();
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }
 
}
