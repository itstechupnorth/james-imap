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
package org.apache.james.imap.jcr;

import java.util.ArrayList;
import java.util.Locale;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.mail.JCRMailboxMapper;
import org.apache.james.imap.mailbox.BadCredentialsException;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.PasswordAwareMailboxSession;
import org.apache.james.imap.store.PasswordAwareUser;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.StoreMailboxManager;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.TransactionalMapper;

/**
 * JCR implementation of a MailboxManager
 * 
 *
 */
public class JCRMailboxManager extends StoreMailboxManager{

	private final Repository repository;

    public JCRMailboxManager(final Authenticator authenticator, final Subscriber subscriber, final Repository repository) {
        super(authenticator, subscriber);
        this.repository = repository;
    }

    @Override
    protected StoreMailbox createMailbox(Mailbox mailboxRow, MailboxSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected MailboxMapper createMailboxMapper(MailboxSession session) throws MailboxException{
    	PasswordAwareUser s = (PasswordAwareUser) session.getUser();
        try {
			return new JCRMailboxMapper(repository.login(new SimpleCredentials(s.getUserName(), s.getPassword().toCharArray())));
		} catch (LoginException e) {
			throw new MailboxException(HumanReadableText.INVALID_LOGIN, e);
		} catch (RepositoryException e) {
			throw new MailboxException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING, e);
		}
    }

    @Override
    protected void doCreate(String namespaceName, MailboxSession session) throws MailboxException {
        final Mailbox mailbox = new org.apache.james.imap.jcr.mail.model.JCRMailbox(namespaceName, randomUidValidity());
        final MailboxMapper mapper = createMailboxMapper(session);
        mapper.execute(new TransactionalMapper.Transaction(){

            public void run() throws MailboxException {
                mapper.save(mailbox);
            }
            
        });
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#login(java.lang.String, java.lang.String, org.apache.commons.logging.Log)
     */
    public MailboxSession login(String userid, String passwd, Log log) throws BadCredentialsException, MailboxException {
        if (login(userid, passwd)) {
        	
            return new PasswordAwareMailboxSession(randomId(), userid, passwd, log, getDelimiter(), new ArrayList<Locale>());
        } else {
            throw new BadCredentialsException();
        }
    }

}
