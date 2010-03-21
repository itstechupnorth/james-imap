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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.AbstractLogEnabled;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.BadCredentialsException;
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
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.mailbox.MailboxMetaData.Selectability;
import org.apache.james.imap.mailbox.util.SimpleMailboxMetaData;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.TransactionalMapper;

public abstract class StoreMailboxManager extends AbstractLogEnabled implements MailboxManager {
    public static final String USER_NAMESPACE_PREFIX = "#mail";
    
    public static final char SQL_WILDCARD_CHAR = '%';

    private final static Random random = new Random();

    protected final Map<String, StoreMailbox> mailboxes;

    private final Authenticator authenticator;    
    private final Subscriber subscriber;    
    
    private final char delimiter;

    public final static String MAILBOX = "MAILBOX";
    
    public StoreMailboxManager(final Authenticator authenticator, final Subscriber subscriber) {
        this(authenticator, subscriber, '.');
    }

    
    public StoreMailboxManager(final Authenticator authenticator, final Subscriber subscriber, final char delimiter) {
        mailboxes = new HashMap<String, StoreMailbox>();
        this.authenticator = authenticator;
        this.subscriber = subscriber;
        this.delimiter = delimiter;
    }

    /**
     * Create a StoreMailbox for the given Mailbox
     * 
     * @param mailboxRow
     * @return storeMailbox
     */
    protected abstract StoreMailbox createMailbox(Mailbox mailboxRow, MailboxSession session);
    
    /**
     * Create the MailboxMapper which should get used 
     * 
     * @return mailboxMapper
     */
    protected abstract MailboxMapper createMailboxMapper(MailboxSession session) throws MailboxException;
    
    /**
     * Create a Mailbox for the given namespace and store it to the underlying storage
     * 
     * @param namespaceName
     * @throws MailboxException
     */
    protected abstract void doCreate(String namespaceName, MailboxSession session) throws MailboxException;
    
    
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
    private StoreMailbox doGetMailbox(String mailboxName, MailboxSession session) throws MailboxException {
        synchronized (mailboxes) {
            final MailboxMapper mapper = createMailboxMapper(session);
            Mailbox mailboxRow = mapper.findMailboxByName(mailboxName);
            
            if (mailboxRow == null) {
                getLog().info("Mailbox '" + mailboxName + "' not found.");
                throw new MailboxNotFoundException(mailboxName);

            } else {
                getLog().debug("Loaded mailbox " + mailboxName);

                StoreMailbox result = (StoreMailbox) mailboxes.get(mailboxName);
                if (result == null) {
                    result = createMailbox(mailboxRow, session);
                    mailboxes.put(mailboxName, result);
                    
                    // store the mailbox in the session so we can cleanup things later
                    //session.getAttributes().put(MAILBOX, result);
                }
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
        } else if (namespaceName.charAt(length - 1) == delimiter) {
            createMailbox(namespaceName.substring(0, length - 1), mailboxSession);
        } else {
            synchronized (mailboxes) {
                // Create root first
                // If any creation fails then mailbox will not be created
                // TODO: transaction
                int index = namespaceName.indexOf(delimiter);
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
                            doCreate(mailbox, mailboxSession);
                        }
                    }
                    index = namespaceName.indexOf(delimiter, ++index);
                }
                if (mailboxExists(namespaceName, mailboxSession)) {
                    throw new MailboxExistsException(namespaceName); 
                } else {
                    doCreate(namespaceName, mailboxSession);
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
        synchronized (mailboxes) {
            // TODO put this into a serilizable transaction
            
            final MailboxMapper mapper = createMailboxMapper(session);
            
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    Mailbox mailbox = mapper.findMailboxByName(mailboxName);
                    if (mailbox == null) {
                        throw new MailboxNotFoundException("Mailbox not found");
                    }
                    mapper.delete(mailbox);
                }
                
            });
            
            final StoreMailbox storeMailbox = mailboxes.remove(mailboxName);
            if (storeMailbox != null) {
                storeMailbox.deleted(session);
            }
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
        synchronized (mailboxes) {
            if (mailboxExists(to, session)) {
                throw new MailboxExistsException(to);
            }

            final MailboxMapper mapper = createMailboxMapper(session);                
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    // TODO put this into a serilizable transaction
                    final Mailbox mailbox = mapper.findMailboxByName(from);

                    if (mailbox == null) {
                        throw new MailboxNotFoundException(from);
                    }
                    mailbox.setName(to);
                    mapper.save(mailbox);

                    changeMailboxName(from, to);

                    // rename submailbox
                    final List<Mailbox> subMailboxes = mapper.findMailboxWithNameLike(from + delimiter + "%");
                    for (Mailbox sub:subMailboxes) {
                        final String subOriginalName = sub.getName();
                        final String subNewName = to + subOriginalName.substring(from.length());
                        sub.setName(subNewName);
                        mapper.save(sub);

                        changeMailboxName(subOriginalName, subNewName);

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
    private void changeMailboxName(String from, String to) {
        final StoreMailbox jpaMailbox = mailboxes.remove(from);
        if (jpaMailbox != null) {
            jpaMailbox.reportRenamed(to);
            mailboxes.put(to, jpaMailbox);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#copyMessages(org.apache.james.imap.mailbox.MessageRange, java.lang.String, java.lang.String, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void copyMessages(MessageRange set, String from, String to,
            MailboxSession session) throws MailboxException {
        StoreMailbox toMailbox = doGetMailbox(to, session);
        StoreMailbox fromMailbox = doGetMailbox(from, session);
        fromMailbox.copyTo(set, toMailbox, session);
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
                delimiter).replace(freeWildcard, SQL_WILDCARD_CHAR)
                .replace(localWildcard, SQL_WILDCARD_CHAR);

        final MailboxMapper mapper = createMailboxMapper(session);
        final List<Mailbox> mailboxes = mapper.findMailboxWithNameLike(search);
        final List<MailboxMetaData> results = new ArrayList<MailboxMetaData>(mailboxes.size());
        for (Mailbox mailbox: mailboxes) {
            final String name = mailbox.getName();
            if (name.startsWith(base)) {
                final String match = name.substring(baseLength);
                if (mailboxExpression.isExpressionMatch(match, delimiter)) {
                    final MailboxMetaData.Children inferiors; 
                    if (hasChildren(name, mapper)) {
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

    /**
     * Does the mailbox with the given name have inferior child mailboxes?
     * @param name not null
     * @return true when the mailbox has children, false otherwise
     * @throws StorageException 
     * @throws TorqueException
     */
    private boolean hasChildren(String name, final MailboxMapper mapper) throws StorageException {
        return mapper.existsMailboxStartingWith(name + delimiter);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#mailboxExists(java.lang.String, org.apache.james.imap.mailbox.MailboxSession)
     */
    public boolean mailboxExists(String mailboxName, MailboxSession session) throws MailboxException {
        synchronized (mailboxes) {
            final MailboxMapper mapper = createMailboxMapper(session);
            final long count = mapper.countMailboxesWithName(mailboxName);
            if (count == 0) {
                mailboxes.remove(mailboxName);
                return false;
            } else {
                if (count == 1) {
                    return true;
                } else {
                    throw new MailboxException(HumanReadableText.DUPLICATE_MAILBOXES, 
                            "Expected one mailbox but found " + count + " mailboxes");
                }
            }
        }
    }

  
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#createSystemSession(java.lang.String, org.apache.commons.logging.Log)
     */
    public MailboxSession createSystemSession(String userName, Log log) {
        return createSession(userName, log);
    }


    /**
     * Create Session 
     * 
     * @param userName
     * @param log
     * @return session
     */
    private SimpleMailboxSession createSession(String userName, Log log) {
        return new SimpleMailboxSession(randomId(), userName, log, delimiter, new ArrayList<Locale>());
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
        if (mailboxPath.charAt(0) != delimiter) {
            mailboxPath = delimiter + mailboxPath;
        }
        final String result = USER_NAMESPACE_PREFIX + delimiter + userName
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
        final StoreMailbox mailbox = doGetMailbox(mailboxName,session);
        mailbox.addListener(listener);
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#login(java.lang.String, java.lang.String, org.apache.commons.logging.Log)
     */
    public MailboxSession login(String userid, String passwd, Log log) throws BadCredentialsException, MailboxException {
        if (login(userid, passwd)) {
            return createSession(userid, log);
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

    /**
     * Return the delemiter to use
     * 
     * @return delemiter
     */
    protected char getDelimiter() {
    	return delimiter;
    }


    /**
     * End processing of Request for session. Default is to do nothing.
     * 
     * Implementations should override this if they need todo anything special
     */
    public void endProcessingRequest(MailboxSession session) {
        // Default do nothing
        
    }


    /**
     * Start processing of Request for session. Default is to do nothing.
     * Implementations should override this if they need todo anything special
     */
    public void startProcessingRequest(MailboxSession session) {
        // Default do nothing
    }
    
    
}
