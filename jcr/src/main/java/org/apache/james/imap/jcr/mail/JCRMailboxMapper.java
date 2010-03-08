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

import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.NonTransactionalMapper;

/**
 * JCR implementation of a MailboxMapper
 * 
 *
 */
public class JCRMailboxMapper extends NonTransactionalMapper implements MailboxMapper{

	private final static String WILDCARD = "*";
	private final Session session;
	private final String PATH = "mailboxes";

	public JCRMailboxMapper(Session session) {
		this.session = session;
	}
	
	
	public long countMailboxesWithName(String name) throws StorageException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.mail.MailboxMapper#delete(org.apache.james.imap.store.mail.model.Mailbox)
	 */
	public void delete(Mailbox mailbox) throws StorageException {
		try {
			session.getRootNode().getNode(PATH + "/" +mailbox.getName()).remove();
		} catch (PathNotFoundException e) {
			// mailbox does not exists.. 
		} catch (RepositoryException e) {
			throw new StorageException(HumanReadableText.DELETED_FAILED, e);
		}		
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.mail.MailboxMapper#deleteAll()
	 */
	public void deleteAll() throws StorageException {
		try {
			session.getRootNode().getNode(PATH).remove();
			session.save();

		} catch (PathNotFoundException e) {
			// nothing todo
		} catch (RepositoryException e) {
			throw new StorageException(HumanReadableText.DELETED_FAILED, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.mail.MailboxMapper#existsMailboxStartingWith(java.lang.String)
	 */
	public boolean existsMailboxStartingWith(String mailboxName)
			throws StorageException {
		try {
			return session.getRootNode().getNodes(PATH + "/" + mailboxName + WILDCARD).hasNext();
		} catch (RepositoryException e) {
			throw new StorageException(HumanReadableText.SEARCH_FAILED, e);
		}
	}

	public Mailbox findMailboxById(long mailboxId) throws StorageException,
			MailboxNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	public Mailbox findMailboxByName(String name) throws StorageException,
			MailboxNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Mailbox> findMailboxWithNameLike(String name)
			throws StorageException {
		// TODO Auto-generated method stub
		return null;
	}

	public void save(Mailbox mailbox) throws StorageException {
		// TODO Auto-generated method stub
		
	}

}
