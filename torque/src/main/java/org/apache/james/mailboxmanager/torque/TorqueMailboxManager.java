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

package org.apache.james.mailboxmanager.torque;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.BadCredentialsException;
import org.apache.james.imap.mailbox.Mailbox;
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
import org.apache.james.imap.mailbox.util.SimpleMailboxMetaData;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imap.store.SimpleMailboxSession;
import org.apache.james.imap.store.Subscriber;
import org.apache.james.mailboxmanager.torque.om.MailboxRow;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.torque.TorqueException;
import org.apache.torque.util.CountHelper;
import org.apache.torque.util.Criteria;

public class TorqueMailboxManager implements MailboxManager {

    public static final String USER_NAMESPACE_PREFIX = "#mail";
    
    private static final char SQL_WILDCARD_CHAR = '%';

    private final static Random random = new Random();

    protected Log log = LogFactory.getLog(TorqueMailboxManager.class);

    private final ReentrantReadWriteLock lock;

    private final Map<String, TorqueMailbox> mailboxes;

    private final char delimiter;

	private final Authenticator authenticator;

	private final Subscriber subscriper;
    
    public TorqueMailboxManager(final Authenticator authenticator, final Subscriber subscriper) {
        this(authenticator, subscriper, '.');
    }
    
    public TorqueMailboxManager(final Authenticator authenticator, final Subscriber subscriper, final char delimiter) {
        this.lock = new ReentrantReadWriteLock();
        mailboxes = new HashMap<String, TorqueMailbox>();
        this.authenticator = authenticator;
        this.subscriper = subscriper;
        this.delimiter = delimiter;
    }

    public Mailbox getMailbox(String mailboxName, MailboxSession session)
            throws MailboxException {
        return doGetMailbox(mailboxName);
    }

    private TorqueMailbox doGetMailbox(String mailboxName)
            throws MailboxException {
        try {
            synchronized (mailboxes) {
                MailboxRow mailboxRow = MailboxRowPeer
                        .retrieveByName(mailboxName);

                if (mailboxRow != null) {
                    getLog().debug("Loaded mailbox " + mailboxName);

                    TorqueMailbox torqueMailbox = (TorqueMailbox) mailboxes
                            .get(mailboxName);
                    if (torqueMailbox == null) {
                        torqueMailbox = new TorqueMailbox(mailboxRow, lock);
                        mailboxes.put(mailboxName, torqueMailbox);
                    }

                    return torqueMailbox;
                } else {
                    getLog().info("Mailbox '" + mailboxName + "' not found.");
                    throw new MailboxNotFoundException(mailboxName);
                }
            }
        } catch (TorqueException e) {
            throw new MailboxException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

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
                            doCreate(mailbox);
                        }
                    }
                    index = namespaceName.indexOf(delimiter, ++index);
                }
                if (mailboxExists(namespaceName, mailboxSession)) {
                    throw new MailboxExistsException(namespaceName); 
                } else {
                    doCreate(namespaceName);
                }
            }
        }
    }

    private void doCreate(String namespaceName) throws MailboxException {
        MailboxRow mr = new MailboxRow();
        mr.setName(namespaceName);
        mr.setLastUid(0);
        mr.setUidValidity(Math.abs(random.nextInt()));
        try {
            mr.save();
        } catch (TorqueException e) {
            throw new MailboxException(HumanReadableText.SAVE_FAILED, e);
        }
    }

    public void deleteMailbox(String mailboxName, MailboxSession session)
            throws MailboxException {
        getLog().info("deleteMailbox " + mailboxName);
        synchronized (mailboxes) {
            try {
                // TODO put this into a serilizable transaction
                MailboxRow mr = MailboxRowPeer.retrieveByName(mailboxName);
                if (mr == null) {
                    throw new MailboxNotFoundException("Mailbox not found");
                }
                MailboxRowPeer.doDelete(mr);
                TorqueMailbox mailbox = (TorqueMailbox) mailboxes
                        .remove(mailboxName);
                if (mailbox != null) {
                    mailbox.deleted(session);
                }
            } catch (TorqueException e) {
                throw new MailboxException(HumanReadableText.DELETED_FAILED, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void renameMailbox(String from, String to, MailboxSession session)
            throws MailboxException {
        getLog().debug("renameMailbox " + from + " to " + to);
        try {
            synchronized (mailboxes) {
                if (mailboxExists(to, session)) {
                    throw new MailboxExistsException(to);
                }
                // TODO put this into a serilizable transaction
                final MailboxRow mr;

                mr = MailboxRowPeer.retrieveByName(from);

                if (mr == null) {
                    throw new MailboxNotFoundException(from);
                }
                mr.setName(to);
                mr.save();

                changeMailboxName(from, to, mr);

                // rename submailbox
                Criteria c = new Criteria();
                c.add(MailboxRowPeer.NAME,
                        (Object) (from + delimiter + "%"),
                        Criteria.LIKE);
                List l = MailboxRowPeer.doSelect(c);
                for (Iterator iter = l.iterator(); iter.hasNext();) {
                    MailboxRow sub = (MailboxRow) iter.next();
                    String subOrigName = sub.getName();
                    String subNewName = to + subOrigName.substring(from.length());
                    sub.setName(subNewName);
                    sub.save();
                    changeMailboxName(subOrigName, subNewName, sub);
                    getLog().info(
                            "renameMailbox sub-mailbox " + subOrigName + " to "
                                    + subNewName);
                }
            }
        } catch (TorqueException e) {
            throw new MailboxException(HumanReadableText.SAVE_FAILED, e);
        }
    }

    private void changeMailboxName(String from, String to, final MailboxRow mr) {
        TorqueMailbox torqueMailbox = (TorqueMailbox) mailboxes.remove(from);
        if (torqueMailbox != null) {
            torqueMailbox.reportRenamed(mr);
            mailboxes.put(to, torqueMailbox);
        }
    }

    public void copyMessages(MessageRange set, String from, String to,
            MailboxSession session) throws MailboxException {
        TorqueMailbox toMailbox = doGetMailbox(to);
        TorqueMailbox fromMailbox = doGetMailbox(from);
        fromMailbox.copyTo(set, toMailbox, session);
    }

    @SuppressWarnings("unchecked")
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

        Criteria criteria = new Criteria();
        criteria.add(MailboxRowPeer.NAME, (Object) (search), Criteria.LIKE);
        try {
            List mailboxRows = MailboxRowPeer.doSelect(criteria);
            List<MailboxMetaData> results = new ArrayList<MailboxMetaData>(mailboxRows.size());
            for (Iterator iter = mailboxRows.iterator(); iter.hasNext();) {
                final MailboxRow mailboxRow = (MailboxRow) iter.next();
                final String name = mailboxRow.getName();
                if (name.startsWith(base)) {
                    final String match = name.substring(baseLength);
                    if (mailboxExpression.isExpressionMatch(match, delimiter)) {
                        final MailboxMetaData.Children inferiors; 
                        if (hasChildren(name)) {
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
        } catch (TorqueException e) {
            throw new MailboxException(HumanReadableText.SEARCH_FAILED, e);
        }

    }

    /**
     * Does the mailbox with the given name have inferior child mailboxes?
     * @param name not null
     * @return true when the mailbox has children, false otherwise
     * @throws TorqueException
     */
    @SuppressWarnings("unchecked")
    private boolean hasChildren(String name) throws TorqueException {
        final Criteria criteria = new Criteria();
        criteria.add(MailboxRowPeer.NAME, (Object)(name + delimiter + SQL_WILDCARD_CHAR), Criteria.LIKE);
        final List mailboxes = MailboxRowPeer.doSelect(criteria);
        return !mailboxes.isEmpty();
    }
    
    public boolean mailboxExists(String mailboxName, MailboxSession session)
            throws MailboxException {
        Criteria c = new Criteria();
        c.add(MailboxRowPeer.NAME, mailboxName);
        CountHelper countHelper = new CountHelper();
        int count;
        try {
            synchronized (mailboxes) {
                count = countHelper.count(c);
                if (count == 0) {
                    mailboxes.remove(mailboxName);
                    return false;
                } else {
                    if (count == 1) {
                        return true;
                    } else {
                        throw new MailboxException(HumanReadableText.DUPLICATE_MAILBOXES, 
                                "found " + count + " mailboxes");
                    }
                }
            }
        } catch (TorqueException e) {
            throw new MailboxException(HumanReadableText.SEARCH_FAILED, e);
        }
    }

    public void deleteEverything() throws MailboxException {
        try {
            MailboxRowPeer.doDelete(new Criteria().and(
                    MailboxRowPeer.MAILBOX_ID, new Integer(-1),
                    Criteria.GREATER_THAN));
            mailboxes.clear();
        } catch (TorqueException e) {
            throw new MailboxException(HumanReadableText.DELETED_FAILED, e);
        }
    }

    protected Log getLog() {
        if (log == null) {
            log = new SimpleLog("TorqueMailboxManager");
        }
        return log;
    }

    public MailboxSession createSystemSession(String userName, Log log) {
        return createSession(userName, log);
    }

    private MailboxSession createSession(String userName, Log log) {
        return new SimpleMailboxSession(random.nextLong(), userName, log, delimiter, new ArrayList<Locale>());
    }

    public String resolve(final String userName, String mailboxPath) {
        if (mailboxPath.charAt(0) != delimiter) {
            mailboxPath = delimiter + mailboxPath;
        }
        final String result = USER_NAMESPACE_PREFIX + delimiter + userName
                + mailboxPath;
        return result;
    }

    public boolean login(String userid, String passwd) {
        return authenticator.isAuthentic(userid, passwd);
    }

    public void subscribe(MailboxSession session, String mailbox)
            throws SubscriptionException {
        subscriper.subscribe(session, mailbox);
    }

    public Collection<String> subscriptions(MailboxSession session) throws SubscriptionException {
        return subscriper.subscriptions(session);
    }

    public void unsubscribe(MailboxSession session, String mailbox)
            throws SubscriptionException {
        subscriper.unsubscribe(session, mailbox);
    }

    public void addListener(String mailboxName, MailboxListener listener, MailboxSession session) throws MailboxException {
        final TorqueMailbox mailbox = doGetMailbox(mailboxName);
        mailbox.addListener(listener);
    }

    public MailboxSession login(String userid, String passwd, Log log) throws BadCredentialsException, MailboxException {
        if (login(userid, passwd)) {
            return createSession(userid, log);
        } else {
            throw new BadCredentialsException();
        }
    }

    public void logout(MailboxSession session, boolean force) throws MailboxException {
        // fine
    }

    /**
     * Do nothing
     */
    public void endProcessingRequest(MailboxSession session) {
        // Do nothing 
    }

    /**
     * Do nothing
     */
    public void startProcessingRequest(MailboxSession session) {
        // Do nothing        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#getDelimiter()
     */
    public char getDelimiter() {
        return delimiter;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.MailboxManager#getUserNameSpacePrefix()
     */
    public String getUserNameSpacePrefix() {
        return USER_NAMESPACE_PREFIX;
    }

}
