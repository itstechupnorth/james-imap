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
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.MailboxPath;
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
public abstract class StoreMailboxManager<Id> extends DelegatingMailboxManager {
    
    public static final char SQL_WILDCARD_CHAR = '%';

    private final Object mutex = new Object();
    
    private final MailboxEventDispatcher dispatcher = new MailboxEventDispatcher();
    private final DelegatingMailboxListener delegatingListener = new DelegatingMailboxListener();   
    
    protected final MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory;
    private UidConsumer<Id> consumer;
    
    
    public StoreMailboxManager(MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory, final Authenticator authenticator, final Subscriber subscriber, final UidConsumer<Id> consumer) {
        super(authenticator, subscriber);
        this.consumer = consumer;
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
        
        // The dispatcher need to have the delegating listener added
        dispatcher.addMailboxListener(delegatingListener);
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
    protected abstract void doCreateMailbox(MailboxPath mailboxPath, MailboxSession session) throws MailboxException;
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#getMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.imap.mailbox.MailboxSession)
     */
    public org.apache.james.imap.mailbox.Mailbox getMailbox(MailboxPath mailboxPath, MailboxSession session)
    throws MailboxException {
        return doGetMailbox(mailboxPath, session);
    }

    /**
     * Get the Mailbox for the given name. If none is found a MailboxException will get thrown
     * 
     * @param mailboxPath the name of the mailbox to return
     * @return mailbox the mailbox for the given name
     * @throws MailboxException get thrown if no Mailbox could be found for the given name
     */
    private StoreMessageManager<Id> doGetMailbox(MailboxPath mailboxPath, MailboxSession session) throws MailboxException {
        synchronized (mutex) {
            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
            Mailbox<Id> mailboxRow = mapper.findMailboxByPath(mailboxPath);
            
            if (mailboxRow == null) {
                getLog().info("Mailbox '" + mailboxPath + "' not found.");
                throw new MailboxNotFoundException(mailboxPath);

            } else {
                getLog().debug("Loaded mailbox " + mailboxPath);

                return createMessageManager(dispatcher, consumer, mailboxRow, session);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#createMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void createMailbox(MailboxPath mailboxPath, MailboxSession mailboxSession)
    throws MailboxException {
        getLog().debug("createMailbox " + mailboxPath);
        final int length = mailboxPath.getName().length();
        if (length == 0) {
            getLog().warn("Ignoring mailbox with empty name");
        }
        else {
            if (mailboxPath.getName().charAt(length - 1) == MailboxConstants.DEFAULT_DELIMITER)
                mailboxPath.setName(mailboxPath.getName().substring(0, length - 1));
            if (mailboxExists(mailboxPath, mailboxSession))
                throw new MailboxExistsException(mailboxPath.toString()); 
            synchronized (mutex) {
                // Create parents first
                // If any creation fails then the mailbox will not be created
                // TODO: transaction
                for (MailboxPath mailbox : mailboxPath.getHierarchyLevels(MailboxConstants.DEFAULT_DELIMITER))
                    if (!mailboxExists(mailbox, mailboxSession))
                        doCreateMailbox(mailbox, mailboxSession);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#deleteMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void deleteMailbox(final MailboxPath mailboxPath, final MailboxSession session)
    throws MailboxException {
        session.getLog().info("deleteMailbox " + mailboxPath);
        synchronized (mutex) {
            // TODO put this into a serilizable transaction
            
            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
            
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    Mailbox<Id> mailbox = mapper.findMailboxByPath(mailboxPath);
                    if (mailbox == null) {
                        throw new MailboxNotFoundException("Mailbox not found");
                    }
                    mapper.delete(mailbox);
                }
                
            });
            
            dispatcher.mailboxDeleted(session.getSessionId(), mailboxPath);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#renameMailbox(org.apache.james.imap.api.MailboxPath, org.apache.james.imap.api.MailboxPath, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void renameMailbox(final MailboxPath from, final MailboxPath to, final MailboxSession session)
    throws MailboxException {
        final Log log = getLog();
        if (log.isDebugEnabled()) log.debug("renameMailbox " + from + " to " + to);
        synchronized (mutex) {
            if (mailboxExists(to, session)) {
                throw new MailboxExistsException(to.toString());
            }

            final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);                
            mapper.execute(new TransactionalMapper.Transaction() {

                public void run() throws MailboxException {
                    // TODO put this into a serilizable transaction
                    final Mailbox<Id> mailbox = mapper.findMailboxByPath(from);
                    if (mailbox == null) {
                        throw new MailboxNotFoundException(from);
                    }
                    mailbox.setNamespace(to.getNamespace());
                    mailbox.setUser(to.getUser());
                    mailbox.setName(to.getName());
                    mapper.save(mailbox);

                    changeMailboxName(from, to, session);

                    // rename submailboxes
                    MailboxPath children = new MailboxPath(MailboxConstants.USER_NAMESPACE, from.getUser(), from.getName() + MailboxConstants.DEFAULT_DELIMITER + "%");
                    final List<Mailbox<Id>> subMailboxes = mapper.findMailboxWithPathLike(children);
                    for (Mailbox<Id> sub:subMailboxes) {
                        final String subOriginalName = sub.getName();
                        final String subNewName = to.getName() + subOriginalName.substring(from.getName().length());
                        sub.setName(subNewName);
                        mapper.save(sub);

                        changeMailboxName(new MailboxPath(children, subOriginalName),
                                new MailboxPath(children, subNewName), session);

                        if (log.isDebugEnabled()) log.debug("Rename mailbox sub-mailbox " + subOriginalName + " to "
                                + subNewName);
                    }
                }
                
            });
        }
    }

 
    /**
     * Changes the name of the mailbox instance in the cache.
     * @param from not null
     * @param to not null
     */
    private void changeMailboxName(MailboxPath from, MailboxPath to, MailboxSession session) {
        dispatcher.mailboxRenamed(from, to, session.getSessionId());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#copyMessages(org.apache.james.imap.mailbox.MessageRange, org.apache.james.imap.api.MailboxPath, org.apache.james.imap.api.MailboxPath, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void copyMessages(MessageRange set, MailboxPath from, MailboxPath to, MailboxSession session)
    throws MailboxException {
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
     * @see org.apache.james.imap.mailbox.MailboxManager#mailboxExists(org.apache.james.imap.api.MailboxPath, org.apache.james.imap.mailbox.MailboxSession)
     */
    public boolean mailboxExists(MailboxPath mailboxPath, MailboxSession session) throws MailboxException {
        synchronized (mutex) {
            try {
                final MailboxMapper<Id> mapper = mailboxSessionMapperFactory.getMailboxMapper(session);
                mapper.findMailboxByPath(mailboxPath);
                return true;
            } catch (MailboxNotFoundException e) {
                return false;
            }
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#addListener(org.apache.james.imap.api.MailboxPath, org.apache.james.imap.mailbox.MailboxListener, org.apache.james.imap.mailbox.MailboxSession)
     */
    public void addListener(MailboxPath path, MailboxListener listener, MailboxSession session) throws MailboxException {
        delegatingListener.addListener(path, listener);
    }

     /**
     * End processing of Request for session
     */
    public void endProcessingRequest(MailboxSession session) {
        mailboxSessionMapperFactory.endRequest(session);
    }

}
