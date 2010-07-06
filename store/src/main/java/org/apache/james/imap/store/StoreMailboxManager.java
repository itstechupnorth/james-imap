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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.AbstractLogEnabled;
import org.apache.james.imap.mailbox.BadCredentialsException;
import org.apache.james.imap.mailbox.MailboxConstants;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxExistsException;
import org.apache.james.imap.mailbox.MailboxListener;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxMetaData;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.MailboxQuery;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.StandardMailboxMetaDataComparator;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.mailbox.MailboxMetaData.Selectability;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.mailbox.util.SimpleMailboxMetaData;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.TransactionalMapper;

/**
 * This abstract base class of an {@link MailboxManager} implementation provides a high-level api for writing your own
 * {@link MailboxManager} implementation. If you plan to write your own {@link MailboxManager} its most times so easiest 
 * to extend just this class.
 * 
 * If you need a more low-level api just implement {@link MailboxManager} directly
 *
 * @param <Id>
 */
public abstract class StoreMailboxManager<Id> extends AbstractLogEnabled implements MailboxManager {
    
    public static final char SQL_WILDCARD_CHAR = '%';

    private final Object mutex = new Object();
    
    private final static Random random = new Random();

    private final MailboxEventDispatcher dispatcher = new MailboxEventDispatcher();
    private final DelegatingMailboxListener delegatingListener = new DelegatingMailboxListener();
    private final Authenticator authenticator;    
    private final Subscriber subscriber;    
    protected final MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory;
    private UidConsumer<Id> consumer;
    
    
    public StoreMailboxManager(MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory, final Authenticator authenticator, final Subscriber subscriber, final UidConsumer<Id> consumer) {
        this.authenticator = authenticator;
        this.subscriber = subscriber;
        this.consumer = consumer;
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
    }
    /**
     * Create a {@link StoreMessageManager} for the given Mailbox
     * 
     * @param mailboxRow
     * @return storeMailbox
     */
    protected abstract StoreMessageManager<Id> createMessageManager(MailboxEventDispatcher dispatcher, UidConsumer<Id> consumer, Mailbox<Id> mailboxRow, MailboxSession session) throws MailboxException;

    /**
     * Create a Mailbox for the given namespace and store it to the underlying storage
     * 
     * @param namespaceName
     * @throws MailboxException
     */
    protected abstract void doCreateMailbox(String namespaceName, MailboxSession session) throws MailboxException;
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#getMailbox(java.lang.String, org.apache.james.imap.mailbox.MailboxSession)
     */
    public org.apache.james.imap.mailbox.Mailbox getMailbox(String mailboxName, MailboxSession session)
    throws MailboxException {
        return doGetMailbox(mailboxName, session);
    }

    /**
     * Get the Mailbox for the given name. If non is found a MailboxException will get thrown
     * 
     * @param mailboxName the name of the mailbox to return
     * @return mailbox the mailbox for the given name
     * @throws MailboxException get thrown if no Mailbox could be found for the given name
     */
    private StoreMessageManager<Id> doGetMailbox(String mailboxName, MailboxSession session) throws MailboxException {
        synchronized (mutex) {
            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
            Mailbox<Id> mailboxRow = mapper.findMailboxByName(mailboxName);
            
            if (mailboxRow == null) {
                getLog().info("Mailbox '" + mailboxName + "' not found.");
                throw new MailboxNotFoundException(mailboxName);

            } else {
                getLog().debug("Loaded mailbox " + mailboxName);

                StoreMessageManager<Id> result = createMessageManager(dispatcher, consumer, mailboxRow, session);
                result.addListener(delegatingListener);
                return result;
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#createMailbox(java.lang.String, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void createMailbox(String namespaceName, MailboxSession mailboxSession)
    throws MailboxException {
        getLog().debug("createMailbox " + namespaceName);
        final int length = namespaceName.length();
        if (length == 0) {
            getLog().warn("Ignoring mailbox with empty name");
        } else if (namespaceName.charAt(length - 1) == MailboxConstants.DEFAULT_DELIMITER) {
            createMailbox(namespaceName.substring(0, length - 1), mailboxSession);
        } else {
            synchronized (mutex) {
                // Create root first
                // If any creation fails then mailbox will not be created
                // TODO: transaction
                int index = namespaceName.indexOf(MailboxConstants.DEFAULT_DELIMITER);
                int count = 0;
                while (index >= 0) {
                    // Until explicit namespace support is added,
                    // this workaround prevents the namespaced elements being
                    // created
                    // TODO: add explicit support for namespaces
                    if (index > 0 && count++ > 1) {
                        final String mailbox = namespaceName
                        .substring(0, index);
                        if (!mailboxExists(mailbox, mailboxSession)) {
                            doCreateMailbox(mailbox, mailboxSession);
                        }
                    }
                    index = namespaceName.indexOf(MailboxConstants.DEFAULT_DELIMITER, ++index);
                }
                if (mailboxExists(namespaceName, mailboxSession)) {
                    throw new MailboxExistsException(namespaceName); 
                } else {
                	doCreateMailbox(namespaceName, mailboxSession);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#deleteMailbox(java.lang.String, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void deleteMailbox(final String mailboxName, final MailboxSession session)
    throws MailboxException {
        session.getLog().info("deleteMailbox " + mailboxName);
        synchronized (mutex) {
            // TODO put this into a serilizable transaction
            
            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
            
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    Mailbox<Id> mailbox = mapper.findMailboxByName(mailboxName);
                    if (mailbox == null) {
                        throw new MailboxNotFoundException("Mailbox not found");
                    }
                    mapper.delete(mailbox);
                }
                
            });
            
            dispatcher.mailboxDeleted(session.getSessionId(), mailboxName);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#renameMailbox(java.lang.String, java.lang.String, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void renameMailbox(final String from, final String to, final MailboxSession session)
    throws MailboxException {
        final Log log = getLog();
        if (log.isDebugEnabled()) log.debug("renameMailbox " + from + " to " + to);
        synchronized (mutex) {
            if (mailboxExists(to, session)) {
                throw new MailboxExistsException(to);
            }

            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);                
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    // TODO put this into a serilizable transaction
                    final Mailbox<Id> mailbox = mapper.findMailboxByName(from);

                    if (mailbox == null) {
                        throw new MailboxNotFoundException(from);
                    }
                    mailbox.setName(to);
                    mapper.save(mailbox);

                    changeMailboxName(from, to, session);

                    // rename submailbox
                    final List<Mailbox<Id>> subMailboxes = mapper.findMailboxWithNameLike(from + MailboxConstants.DEFAULT_DELIMITER + "%");
                    for (Mailbox<Id> sub:subMailboxes) {
                        final String subOriginalName = sub.getName();
                        final String subNewName = to + subOriginalName.substring(from.length());
                        sub.setName(subNewName);
                        mapper.save(sub);

                        changeMailboxName(subOriginalName, subNewName, session);

                        if (log.isDebugEnabled()) log.debug("Rename mailbox sub-mailbox " + subOriginalName + " to "
                                + subNewName);
                    }
                }
                
            });
        }
    }

    /**
     * Generate an return the next uid validity 
     * 
     * @return uidValidity
     */
    protected int randomUidValidity() {
        return Math.abs(random.nextInt());
    }
    
    /**
     * Changes the name of the mailbox instance in the cache.
     * @param from not null
     * @param to not null
     */
    private void changeMailboxName(String from, String to, MailboxSession session) {
        dispatcher.mailboxRenamed(from, to, session.getSessionId());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#copyMessages(org.apache.james.imap.mailbox.MessageRange, java.lang.String, java.lang.String, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void copyMessages(MessageRange set, String from, String to,
            MailboxSession session) throws MailboxException {
        synchronized (mutex) {

        StoreMessageManager<Id> toMailbox = doGetMailbox(to, session);
        StoreMessageManager<Id> fromMailbox = doGetMailbox(from, session);
            fromMailbox.copyTo(set, toMailbox, session);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#search(org.apache.james.imap.mailbox.MailboxQuery, org.apache.james.imap.mailbox.MailboxSession)
     */
    public List<MailboxMetaData> search(final MailboxQuery mailboxExpression, MailboxSession session)
    throws MailboxException {
        final char localWildcard = mailboxExpression.getLocalWildcard();
        final char freeWildcard = mailboxExpression.getFreeWildcard();
        final String base = mailboxExpression.getBase();
        final int baseLength;
        if (base == null) {
            baseLength = 0;
        } else {
            baseLength = base.length();
        }

        final String search = mailboxExpression.getCombinedName(
                MailboxConstants.DEFAULT_DELIMITER).replace(freeWildcard, SQL_WILDCARD_CHAR)
                .replace(localWildcard, SQL_WILDCARD_CHAR);

        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        final List<Mailbox<Id>> mailboxes = mapper.findMailboxWithNameLike(search);
        final List<MailboxMetaData> results = new ArrayList<MailboxMetaData>(mailboxes.size());
        for (Mailbox<Id> mailbox: mailboxes) {
            final String name = mailbox.getName();
            if (name.startsWith(base)) {
                final String match = name.substring(baseLength);
                if (mailboxExpression.isExpressionMatch(match, MailboxConstants.DEFAULT_DELIMITER)) {
                    final MailboxMetaData.Children inferiors; 
                    if (mapper.hasChildren(mailbox)) {
                        inferiors = MailboxMetaData.Children.HAS_CHILDREN;
                    } else {
                        inferiors = MailboxMetaData.Children.HAS_NO_CHILDREN;
                    }
                    results.add(new SimpleMailboxMetaData(name, ".", inferiors, Selectability.NONE));
                }
            }
        }
        Collections.sort(results, new StandardMailboxMetaDataComparator());
        return results;

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#mailboxExists(java.lang.String, org.apache.james.imap.mailbox.MailboxSession)
     */
    public boolean mailboxExists(String mailboxName, MailboxSession session) throws MailboxException {
        synchronized (mutex) {
            try {
                final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
                mapper.findMailboxByName(mailboxName);
                return true;
            }
            catch (MailboxNotFoundException e) {
                return false;
            }
        }
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
        return random.nextLong();
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
     * @see org.apache.james.imap.mailbox.MailboxManager#subscribe(org.apache.james.imap.mailbox.MailboxSession, java.lang.String)
     */
    public void subscribe(MailboxSession session, String mailbox)
    throws SubscriptionException {
        subscriber.subscribe(session, mailbox);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#subscriptions(org.apache.james.imap.mailbox.MailboxSession)
     */
    public Collection<String> subscriptions(MailboxSession session) throws SubscriptionException {
        return subscriber.subscriptions(session);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#unsubscribe(org.apache.james.imap.mailbox.MailboxSession, java.lang.String)
     */
    public void unsubscribe(MailboxSession session, String mailbox)
    throws SubscriptionException {
        subscriber.unsubscribe(session, mailbox);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#addListener(java.lang.String, org.apache.james.imap.mailbox.MailboxListener, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void addListener(String mailboxName, MailboxListener listener, MailboxSession session) throws MailboxException {
        delegatingListener.addListener(mailboxName, listener);
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
     * Default do nothing. Should be overriden by subclass if needed
     */
    public void logout(MailboxSession session, boolean force) throws MailboxException {
        // Do nothing by default
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#getDelimiter()
     */
    public final char getDelimiter() {
        return MailboxConstants.DEFAULT_DELIMITER;
    }

    /**
     * End processing of Request for session
     */
    public void endProcessingRequest(MailboxSession session) {
        mailboxSessionMapperFactory.endRequest(session);
    }

    /**
     * Start processing of Request for session. Default is to do nothing.
     * Implementations should override this if they need to do anything special
     */
    public void startProcessingRequest(MailboxSession session) {
        // Default do nothing
    }

}
