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

package org.apache.james.imap.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.AbstractLogEnabled;
import org.apache.james.imap.mailbox.BadCredentialsException;
import org.apache.james.imap.mailbox.MailboxConstants;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.SubscriptionException;

/**
 * Abstract {@link MailboxManager} which delegates various stuff to the {@link Authenticator} and {@link Subscriber}
 *
 */
public abstract class DelegatingMailboxManager extends AbstractLogEnabled implements MailboxManager{

    private final Authenticator authenticator;
    private final Subscriber subscriper;
    private final static Random RANDOM = new Random();

    public DelegatingMailboxManager(final Authenticator authenticator, final Subscriber subscriper) {
        this.subscriper = subscriper;
        this.authenticator = authenticator;
    }

    /**
     * Generate an return the next uid validity 
     * 
     * @return uidValidity
     */
    protected int randomUidValidity() {
        return Math.abs(RANDOM.nextInt());
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#createSystemSession(java.lang.String, org.apache.commons.logging.Log)
     */
    public MailboxSession createSystemSession(String userName, Log log) {
        return createSession(userName, null, log);
    }

    /**
     * Create Session 
     * 
     * @param userName
     * @param log
     * @return session
     */
    private SimpleMailboxSession createSession(String userName, String password, Log log) {
        return new SimpleMailboxSession(randomId(), userName, password, log, new ArrayList<Locale>());
    }

    /**
     * Generate and return the next id to use
     * 
     * @return id
     */
    protected long randomId() {
        return RANDOM.nextLong();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#getDelimiter()
     */
    public final char getDelimiter() {
        return MailboxConstants.DEFAULT_DELIMITER;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#resolve(java.lang.String, java.lang.String)
     */
    public String resolve(final String userName, String mailboxPath) {
        if (mailboxPath.length() > 0 && mailboxPath.charAt(0) != MailboxConstants.DEFAULT_DELIMITER) {
            mailboxPath = MailboxConstants.DEFAULT_DELIMITER + mailboxPath;
        }
        final String result = MailboxConstants.USER_NAMESPACE + MailboxConstants.DEFAULT_DELIMITER + userName
        + mailboxPath;
        return result;
    }

    /**
     * Log in the user with the given userid and password
     * 
     * @param userid the username
     * @param passwd the password
     * @return success true if login success false otherwise
     */
    public boolean login(String userid, String passwd) {
        return authenticator.isAuthentic(userid, passwd);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#login(java.lang.String, java.lang.String, org.apache.commons.logging.Log)
     */
    public MailboxSession login(String userid, String passwd, Log log) throws BadCredentialsException, MailboxException {
        if (login(userid, passwd)) {
            return createSession(userid, passwd, log);
        } else {
            throw new BadCredentialsException();
        }
    }
    
    /**
     * Default do nothing. Should be overridden by subclass if needed
     */
    public void logout(MailboxSession session, boolean force) throws MailboxException {
        // Do nothing by default
    }
  

    /**
     * Start processing of Request for session. Default is to do nothing.
     * Implementations should override this if they need to do anything special
     */
    public void startProcessingRequest(MailboxSession session) {
        // Default do nothing
    }

    /**
     * End processing of a Request for the given session. Default is to do nothing
     */
    public void endProcessingRequest(MailboxSession session) {
        // Default do nothing
        
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#subscribe(org.apache.james.imap.mailbox.MailboxSession, java.lang.String)
     */
    public void subscribe(MailboxSession session, String mailbox) throws SubscriptionException {
        subscriper.subscribe(session, mailbox);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#subscriptions(org.apache.james.imap.mailbox.MailboxSession)
     */
    public Collection<String> subscriptions(MailboxSession session) throws SubscriptionException {
        return subscriper.subscriptions(session);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#unsubscribe(org.apache.james.imap.mailbox.MailboxSession, java.lang.String)
     */
    public void unsubscribe(MailboxSession session, String mailbox) throws SubscriptionException {
        subscriper.unsubscribe(session, mailbox);
    }

}
