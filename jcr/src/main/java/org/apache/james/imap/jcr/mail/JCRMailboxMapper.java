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
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.jackrabbit.JcrConstants;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.AbstractJCRMapper;
import org.apache.james.imap.jcr.JCRUtils;
import org.apache.james.imap.jcr.mail.model.JCRMailbox;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;

/**
 * JCR implementation of a MailboxMapper
 * 
 * 
 */
public class JCRMailboxMapper extends AbstractJCRMapper implements MailboxMapper {

    public final String PATH = PROPERTY_PREFIX + "mailboxes";
    private final Log logger;

    public JCRMailboxMapper(final Session session, final int scaling,final Log logger) {
        super(session, scaling);
        this.logger = logger;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MailboxMapper#countMailboxesWithName
     * (java.lang.String)
     */
    public long countMailboxesWithName(String name) throws StorageException {
       
        try {
        	QueryManager manager = getSession().getWorkspace().getQueryManager();
        	String queryString =  "//" + PATH + "//element(*)[@" + JCRMailbox.NAME_PROPERTY + "='" + name + "']";
        	QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
        	NodeIterator it = result.getNodes();
        	long resultSize = it.getSize();
        	if (resultSize == -1) {
        	    resultSize = 0;
        	    while (it.hasNext()) {
        	        it.nextNode();
        	        resultSize++;
        	    }
        	}
        	return resultSize;
        } catch (PathNotFoundException e) {
            // not found
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.COUNT_FAILED, e);
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MailboxMapper#delete(org.apache.james
     * .imap.store.mail.model.Mailbox)
     */
    public void delete(Mailbox mailbox) throws StorageException {
        try {
        	getSession().getNodeByUUID(((JCRMailbox) mailbox).getUUID()).remove();
        } catch (PathNotFoundException e) {
            // mailbox does not exists..
        } catch (RepositoryException e) {
            e.printStackTrace();
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
            getSession().getRootNode().getNode(PATH).remove();

        } catch (PathNotFoundException e) {
            // nothing todo
        } catch (RepositoryException e) {
        	e.printStackTrace();
            throw new StorageException(HumanReadableText.DELETED_FAILED, e);
        }
    }

    /*
     * 
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.MailboxMapper#existsMailboxStartingWith
     * (java.lang.String)
     */
    public boolean existsMailboxStartingWith(String mailboxName) throws StorageException {
        try {
        	QueryManager manager = getSession().getWorkspace().getQueryManager();
        	String queryString = "//" + PATH + "//element(*)[jcr:like(@" + JCRMailbox.NAME_PROPERTY + ",'" +mailboxName+"%')]";
        	QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
        	NodeIterator it = result.getNodes();
        	return it.hasNext();
        } catch (RepositoryException e) {
            throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxById(long)
     */
    public Mailbox findMailboxById(long mailboxId) throws StorageException, MailboxNotFoundException {
        throw new StorageException(HumanReadableText.UNSUPPORTED,null);
    }

    public Mailbox findMailboxByUUID(String uuid) throws StorageException, MailboxNotFoundException {
    	try {
            return new JCRMailbox(getSession().getNodeByUUID(uuid),logger);
        } catch (PathNotFoundException e) {
            throw new MailboxNotFoundException(uuid);
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
    public Mailbox findMailboxByName(String name) throws StorageException, MailboxNotFoundException {
        try {
        	QueryManager manager = getSession().getWorkspace().getQueryManager();
        	String queryString = "//" + PATH + "//element(*)[@" + JCRMailbox.NAME_PROPERTY + "='" + name + "']";
        	QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
        	NodeIterator it = result.getNodes();
        	if (it.hasNext()) {
                return new JCRMailbox(it.nextNode(), logger);
        	}
            throw new MailboxNotFoundException(name);
        } catch (PathNotFoundException e) {
        	e.printStackTrace();
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
    public List<Mailbox> findMailboxWithNameLike(String name) throws StorageException {
        List<Mailbox> mailboxList = new ArrayList<Mailbox>();
        try {       
        	QueryManager manager = getSession().getWorkspace().getQueryManager();
        	String queryString = "//" + PATH + "//element(*)[jcr:like(@" + JCRMailbox.NAME_PROPERTY + ",'%" + name + "%')]";
        	QueryResult result = manager.createQuery(queryString, Query.XPATH).execute();
        	NodeIterator it = result.getNodes();
        	while (it.hasNext()) {
                mailboxList.add(new JCRMailbox(it.nextNode(), logger));
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
    public void save(Mailbox mailbox) throws StorageException {
        
        try {
            JCRMailbox jcrMailbox = (JCRMailbox)mailbox;
            Node node = null;

            if (jcrMailbox.isPersistent()) {
                node = getSession().getNodeByUUID(jcrMailbox.getUUID());
            }
            if (node == null) {
                String nodePath = JCRUtils.escapePath(PATH,JCRUtils.createScaledPath(mailbox.getName(), getScaling()));

                node = JCRUtils.createNodeRecursive(getSession().getRootNode(), nodePath);
                node.addMixin(JcrConstants.MIX_REFERENCEABLE);
           } 
            
            jcrMailbox.merge(node);

        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new StorageException(HumanReadableText.SAVE_FAILED, e);
        }
    }
    
    /**
     * Consume the next uid for the {@link Mailbox} with the given uuid
     * 
     * @param uuid
     * @return mailbox
     * @throws StorageException
     * @throws MailboxNotFoundException
     */
    public Mailbox consumeNextUid(String uuid) throws StorageException, MailboxNotFoundException {

        final JCRMailbox mailbox = (JCRMailbox) findMailboxByUUID(uuid);
        try {
            execute(new Transaction() {
                
                public void run() throws MailboxException {
                    mailbox.consumeUid();
                }
            });
        } catch (MailboxException e) {
            throw (StorageException)e;
        }
        return mailbox;

    }
}
