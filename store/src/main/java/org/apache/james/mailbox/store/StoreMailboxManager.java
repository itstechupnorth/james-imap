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

package org.apache.james.mailbox.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxExistsException;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxMetaData;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxQuery;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.StandardMailboxMetaDataComparator;
import org.apache.james.mailbox.MailboxMetaData.Selectability;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.transaction.Mapper;
import org.apache.james.mailbox.store.transaction.TransactionalMapper;
import org.apache.james.mailbox.util.MailboxEventDispatcher;
import org.apache.james.mailbox.util.SimpleMailboxMetaData;

/**
 * This abstract base class of an {@link MailboxManager} implementation provides a high-level api for writing your own
 * {@link MailboxManager} implementation. If you plan to write your own {@link MailboxManager} its most times so easiest 
 * to extend just this class.
 * 
 * If you need a more low-level api just implement {@link MailboxManager} directly
 *
 * @param <Id>
 */
public abstract class StoreMailboxManager<Id> implements MailboxManager {
    
    public static final char SQL_WILDCARD_CHAR = '%';
    
    private final MailboxEventDispatcher dispatcher = new MailboxEventDispatcher();
    private final DelegatingMailboxListener delegatingListener = new DelegatingMailboxListener();   
    private final MailboxPathLock lock = new MailboxPathLock();
    protected final MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory;    
    
    private final Authenticator authenticator;
    private final static Random RANDOM = new Random();
    
    private Log log = LogFactory.getLog("org.apache.james.imap");
    private ConcurrentMap<Id, AtomicLong> lastUids = new ConcurrentHashMap<Id, AtomicLong>();
    
    public StoreMailboxManager(MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory, final Authenticator authenticator) {
        this.authenticator = authenticator;
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
        
        // The dispatcher need to have the delegating listener added
        dispatcher.addMailboxListener(delegatingListener);
    }
   
    protected Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
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
     * @see org.apache.james.mailbox.MailboxManager#createSystemSession(java.lang.String, org.apache.commons.logging.Log)
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
     * @see org.apache.james.mailbox.MailboxManager#getDelimiter()
     */
    public final char getDelimiter() {
        return MailboxConstants.DEFAULT_DELIMITER;
    }

    /**
     * Log in the user with the given userid and password
     * 
     * @param userid the username
     * @param passwd the password
     * @return success true if login success false otherwise
     */
    private boolean login(String userid, String passwd) {
        return authenticator.isAuthentic(userid, passwd);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#login(java.lang.String, java.lang.String, org.apache.commons.logging.Log)
     */
    public MailboxSession login(String userid, String passwd, Log log) throws BadCredentialsException, MailboxException {
        if (login(userid, passwd)) {
            return createSession(userid, passwd, log);
        } else {
            throw new BadCredentialsException();
        }
    }
    
    /**
     * Close the {@link MailboxSession} if not null
     */
    public void logout(MailboxSession session, boolean force) throws MailboxException {
        session.close();
    }
  
    /**
     * Create a {@link MapperStoreMessageManager} for the given Mailbox
     * 
     * @param mailboxRow
     * @return storeMailbox
     */
    protected abstract StoreMessageManager<Id> createMessageManager(AtomicLong lastUid,MailboxEventDispatcher dispatcher, Mailbox<Id> mailboxRow, MailboxSession session) throws MailboxException;

    /**
     * Create a Mailbox for the given namespace
     * 
     * @param namespaceName
     * @throws MailboxException
     */
    protected abstract  org.apache.james.mailbox.store.mail.model.Mailbox<Id> doCreateMailbox(MailboxPath mailboxPath, final MailboxSession session) throws MailboxException;
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#getMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public org.apache.james.mailbox.MessageManager getMailbox(MailboxPath mailboxPath, MailboxSession session)
    throws MailboxException {
    	final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        Mailbox<Id> mailboxRow = mapper.findMailboxByPath(mailboxPath);

        if (mailboxRow == null) {
            getLog().info("Mailbox '" + mailboxPath + "' not found.");
            throw new MailboxNotFoundException(mailboxPath);

        } else {
            getLog().debug("Loaded mailbox " + mailboxPath);
            
            // Get the lastuid for the mailbox and register the LastUidTracker to update it when messages 
            // are added to the mailbox
            final AtomicLong lastUid = getLastUid(mailboxRow.getMailboxId());
            StoreMessageManager<Id>  m = createMessageManager(lastUid, dispatcher, mailboxRow, session);
            addListener(mailboxPath, new LastUidTracker(lastUid, session), session);
            return m;
        }
    }

    
    /**
     * Return the lastUid for the mailbox. 
     * 
     * 
     * @param mailboxId
     * @return lastUid
     * 
     * TODO: Maybe we should do something smart here and remove it from the {@link ConcurrentHashMap} once its not needed anymore
     */
    private AtomicLong getLastUid(Id mailboxId) {
        lastUids.putIfAbsent(mailboxId, new AtomicLong(0));
        return lastUids.get(mailboxId); 
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#createMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public void createMailbox(MailboxPath mailboxPath, MailboxSession mailboxSession)
    throws MailboxException {
        getLog().debug("createMailbox " + mailboxPath);
        final int length = mailboxPath.getName().length();
        if (length == 0) {
            getLog().warn("Ignoring mailbox with empty name");
        } else {
            if (mailboxPath.getName().charAt(length - 1) == MailboxConstants.DEFAULT_DELIMITER)
                mailboxPath.setName(mailboxPath.getName().substring(0, length - 1));
            if (mailboxExists(mailboxPath, mailboxSession))
                throw new MailboxExistsException(mailboxPath.toString());
            // Create parents first
            // If any creation fails then the mailbox will not be created
            // TODO: transaction
            for (MailboxPath mailbox : mailboxPath.getHierarchyLevels(MailboxConstants.DEFAULT_DELIMITER))
                if (!mailboxExists(mailbox, mailboxSession)) {
                    try {
                        lock.lock(mailbox);
                        final org.apache.james.mailbox.store.mail.model.Mailbox<Id> m = doCreateMailbox(mailbox, mailboxSession);
                        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(mailboxSession);
                        mapper.execute(new TransactionalMapper.VoidTransaction(){

                            public void runVoid() throws MailboxException {
                                mapper.save(m);
                            }
                            
                        });
                    } finally {
                        lock.unlock(mailbox);
                    }
                }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#deleteMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public void deleteMailbox(final MailboxPath mailboxPath, final MailboxSession session) throws MailboxException {
        session.getLog().info("deleteMailbox " + mailboxPath);
        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);

        mapper.execute(new Mapper.VoidTransaction() {

            public void runVoid() throws MailboxException {
                Mailbox<Id> mailbox = mapper.findMailboxByPath(mailboxPath);
                if (mailbox == null) {
                    throw new MailboxNotFoundException("Mailbox not found");
                }
                mapper.delete(mailbox);
            }

        });

        dispatcher.mailboxDeleted(session.getSessionId(), mailboxPath);

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#renameMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public void renameMailbox(final MailboxPath from, final MailboxPath to, final MailboxSession session) throws MailboxException {
        final Log log = getLog();
        if (log.isDebugEnabled())
            log.debug("renameMailbox " + from + " to " + to);
        if (mailboxExists(to, session)) {
            throw new MailboxExistsException(to.toString());
        }

        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        mapper.execute(new Mapper.VoidTransaction() {

            public void runVoid() throws MailboxException {
                // TODO put this into a serilizable transaction
                final Mailbox<Id> mailbox = mapper.findMailboxByPath(from);
                if (mailbox == null) {
                    throw new MailboxNotFoundException(from);
                }
                mailbox.setNamespace(to.getNamespace());
                mailbox.setUser(to.getUser());
                mailbox.setName(to.getName());
                mapper.save(mailbox);

                dispatcher.mailboxRenamed(from, to, session.getSessionId());

                // rename submailboxes
                MailboxPath children = new MailboxPath(MailboxConstants.USER_NAMESPACE, from.getUser(), from.getName() + MailboxConstants.DEFAULT_DELIMITER + "%");
                try {
                    lock.lock(children);
                    final List<Mailbox<Id>> subMailboxes = mapper.findMailboxWithPathLike(children);
                    for (Mailbox<Id> sub : subMailboxes) {
                        final String subOriginalName = sub.getName();
                        final String subNewName = to.getName() + subOriginalName.substring(from.getName().length());
                        final MailboxPath fromPath = new MailboxPath(children, subOriginalName);
                        final MailboxPath toPath = new MailboxPath(children, subNewName);

                        sub.setName(subNewName);
                        mapper.save(sub);
                        dispatcher.mailboxRenamed(fromPath, toPath, session.getSessionId());

                        if (log.isDebugEnabled())
                            log.debug("Rename mailbox sub-mailbox " + subOriginalName + " to " + subNewName);
                            
                      
                    }
                } finally {
                    lock.unlock(children);
                }
                
            }

        });

    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#copyMessages(org.apache.james.mailbox.MessageRange, org.apache.james.imap.api.MailboxPath, org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    @SuppressWarnings("unchecked")
	public void copyMessages(MessageRange set, MailboxPath from, MailboxPath to, MailboxSession session) throws MailboxException {
        StoreMessageManager<Id> toMailbox = (StoreMessageManager<Id>) getMailbox(to, session);
        StoreMessageManager<Id> fromMailbox = (StoreMessageManager<Id>) getMailbox(from, session);
        fromMailbox.copyTo(set, toMailbox, session);

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#search(org.apache.james.mailbox.MailboxQuery, org.apache.james.mailbox.MailboxSession)
     */
    public List<MailboxMetaData> search(final MailboxQuery mailboxExpression, MailboxSession session)
    throws MailboxException {
        final char localWildcard = mailboxExpression.getLocalWildcard();
        final char freeWildcard = mailboxExpression.getFreeWildcard();
        final String baseName = mailboxExpression.getBase().getName();
        final int baseLength;
        if (baseName == null) {
            baseLength = 0;
        } else {
            baseLength = baseName.length();
        }
        final String combinedName = mailboxExpression.getCombinedName()
                                    .replace(freeWildcard, SQL_WILDCARD_CHAR)
                                    .replace(localWildcard, SQL_WILDCARD_CHAR);
        final MailboxPath search = new MailboxPath(mailboxExpression.getBase(), combinedName);

        final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
        final List<Mailbox<Id>> mailboxes = mapper.findMailboxWithPathLike(search);
        final List<MailboxMetaData> results = new ArrayList<MailboxMetaData>(mailboxes.size());
        for (Mailbox<Id> mailbox: mailboxes) {
            final String name = mailbox.getName();
            if (name.startsWith(baseName)) {
                final String match = name.substring(baseLength);
                if (mailboxExpression.isExpressionMatch(match)) {
                    final MailboxMetaData.Children inferiors; 
                    if (mapper.hasChildren(mailbox)) {
                        inferiors = MailboxMetaData.Children.HAS_CHILDREN;
                    } else {
                        inferiors = MailboxMetaData.Children.HAS_NO_CHILDREN;
                    }
                    MailboxPath mailboxPath = new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), name);
                    results.add(new SimpleMailboxMetaData(mailboxPath, MailboxConstants.DEFAULT_DELIMITER_STRING, inferiors, Selectability.NONE));
                }
            }
        }
        Collections.sort(results, new StandardMailboxMetaDataComparator());
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#mailboxExists(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxSession)
     */
    public boolean mailboxExists(MailboxPath mailboxPath, MailboxSession session) throws MailboxException {
        try {
            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
            mapper.findMailboxByPath(mailboxPath);
            return true;
        } catch (MailboxNotFoundException e) {
            return false;
        }

    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxManager#addListener(org.apache.james.imap.api.MailboxPath, org.apache.james.mailbox.MailboxListener, org.apache.james.mailbox.MailboxSession)
     */
    public void addListener(MailboxPath path, MailboxListener listener, MailboxSession session) throws MailboxException {
        delegatingListener.addListener(path, listener);
    }

     /**
     * End processing of Request for session
     */
    public void endProcessingRequest(MailboxSession session) {
        mailboxSessionMapperFactory.endProcessingRequest(session);
    }

    /**
     * Do nothing. Sub classes should override this if needed
     */
    public void startProcessingRequest(MailboxSession session) {
        // do nothing
        
    }
    
    /**
     * {@link MailboxListener} which takes care of update the lastUid for a Mailbox.
     * 
     * 
     *
     */
    private final class LastUidTracker implements MailboxListener {

        private final AtomicLong lastUid;
        private final MailboxSession session;

        public LastUidTracker(AtomicLong lastUid, MailboxSession session) {
            this.lastUid = lastUid;
            this.session = session;
        }
        
        public void event(Event event) {
            if (event instanceof Added) {
                long uid = ((Added) event).getSubjectUid();
                if (uid > lastUid.get()) {
                    lastUid.set(uid);
                }
            }
        }

        public boolean isClosed() {
            if (session == null || session.isOpen() == false) {
                return true;
            }
            return false;
        }
        
    }

}
