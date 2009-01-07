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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.james.api.imap.AbstractLogEnabled;
import org.apache.james.imap.mailbox.ListResult;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxExistsException;
import org.apache.james.imap.mailbox.MailboxExpression;
import org.apache.james.imap.mailbox.MailboxListener;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.mailbox.util.ListResultImpl;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;

public abstract class StoreMailboxManager extends AbstractLogEnabled implements MailboxManager {

    private static final char SQL_WILDCARD_CHAR = '%';

    protected final static Random random = new Random();

    private final Map<String, StoreMailbox> mailboxes;

    private final Authenticator authenticator;    
    private final Subscriber subscriber;    

    public StoreMailboxManager(final Authenticator authenticator, final Subscriber subscriber) {
        mailboxes = new HashMap<String, StoreMailbox>();
        this.authenticator = authenticator;
        this.subscriber = subscriber;
    }

    protected abstract StoreMailbox createMailbox(Mailbox mailboxRow);
    
    protected abstract MailboxMapper createMailboxMapper();
    
    protected abstract void doCreate(String namespaceName) throws MailboxException;
    
    public org.apache.james.imap.mailbox.Mailbox getMailbox(String mailboxName)
    throws MailboxException {
        return doGetMailbox(mailboxName);
    }

    private StoreMailbox doGetMailbox(String mailboxName) throws MailboxException {
        synchronized (mailboxes) {
            final MailboxMapper mapper = createMailboxMapper();
            Mailbox mailboxRow = mapper.findMailboxByName(mailboxName);

            if (mailboxRow == null) {
                getLog().info("Mailbox '" + mailboxName + "' not found.");
                throw new MailboxNotFoundException(mailboxName);

            } else {
                getLog().debug("Loaded mailbox " + mailboxName);

                StoreMailbox result = (StoreMailbox) mailboxes.get(mailboxName);
                if (result == null) {
                    result = createMailbox(mailboxRow);
                    mailboxes.put(mailboxName, result);
                }
                return result;
            }
        }
    }

    public void createMailbox(String namespaceName)
    throws MailboxException {
        getLog().debug("createMailbox " + namespaceName);
        final int length = namespaceName.length();
        if (length == 0) {
            getLog().warn("Ignoring mailbox with empty name");
        } else if (namespaceName.charAt(length - 1) == HIERARCHY_DELIMITER) {
            createMailbox(namespaceName.substring(0, length - 1));
        } else {
            synchronized (mailboxes) {
                // Create root first
                // If any creation fails then mailbox will not be created
                // TODO: transaction
                int index = namespaceName.indexOf(HIERARCHY_DELIMITER);
                int count = 0;
                while (index >= 0) {
                    // Until explicit namespace support is added,
                    // this workaround prevents the namespaced elements being
                    // created
                    // TODO: add explicit support for namespaces
                    if (index > 0 && count++ > 1) {
                        final String mailbox = namespaceName
                        .substring(0, index);
                        if (!mailboxExists(mailbox)) {
                            doCreate(mailbox);
                        }
                    }
                    index = namespaceName.indexOf(HIERARCHY_DELIMITER, ++index);
                }
                if (mailboxExists(namespaceName)) {
                    throw new MailboxExistsException(namespaceName); 
                } else {
                    doCreate(namespaceName);
                }
            }
        }
    }

    public void deleteMailbox(String mailboxName, MailboxSession session)
    throws MailboxException {
        getLog().info("deleteMailbox " + mailboxName);
        synchronized (mailboxes) {
            // TODO put this into a serilizable transaction
            final MailboxMapper mapper = createMailboxMapper();
            mapper.begin();
            Mailbox mailbox = mapper.findMailboxByName(mailboxName);
            if (mailbox == null) {
                throw new MailboxNotFoundException("Mailbox not found");
            }
            mapper.delete(mailbox);
            mapper.commit();
            final StoreMailbox storeMailbox = mailboxes.remove(mailboxName);
            if (storeMailbox != null) {
                storeMailbox.deleted(session);
            }
        }
    }

    public void renameMailbox(String from, String to)
    throws MailboxException {
        final Log log = getLog();
        if (log.isDebugEnabled()) log.debug("renameMailbox " + from + " to " + to);
        synchronized (mailboxes) {
            if (mailboxExists(to)) {
                throw new MailboxExistsException(to);
            }

            final MailboxMapper mapper = createMailboxMapper();                
            mapper.begin();
            // TODO put this into a serilizable transaction
            final Mailbox mailbox = mapper.findMailboxByName(from);

            if (mailbox == null) {
                throw new MailboxNotFoundException(from);
            }
            mailbox.setName(to);
            mapper.save(mailbox);

            changeMailboxName(from, to);

            // rename submailbox
            final List<Mailbox> subMailboxes = mapper.findMailboxWithNameLike(from + HIERARCHY_DELIMITER + "%");
            for (Mailbox sub:subMailboxes) {
                final String subOriginalName = sub.getName();
                final String subNewName = to + subOriginalName.substring(from.length());
                sub.setName(subNewName);
                mapper.save(sub);

                changeMailboxName(subOriginalName, subNewName);

                if (log.isDebugEnabled()) log.debug("Rename mailbox sub-mailbox " + subOriginalName + " to "
                        + subNewName);
            }
            mapper.commit();
        }
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

    public void copyMessages(MessageRange set, String from, String to,
            MailboxSession session) throws MailboxException {
        StoreMailbox toMailbox = doGetMailbox(to);
        StoreMailbox fromMailbox = doGetMailbox(from);
        fromMailbox.copyTo(set, toMailbox, session);
    }

    public ListResult[] list(final MailboxExpression mailboxExpression)
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
                HIERARCHY_DELIMITER).replace(freeWildcard, SQL_WILDCARD_CHAR)
                .replace(localWildcard, SQL_WILDCARD_CHAR);

        final MailboxMapper mapper = createMailboxMapper();
        final List<Mailbox> mailboxes = mapper.findMailboxWithNameLike(search);
        final List<ListResult> listResults = new ArrayList<ListResult>(mailboxes.size());
        for (Mailbox mailbox: mailboxes) {
            final String name = mailbox.getName();
            if (name.startsWith(base)) {
                final String match = name.substring(baseLength);
                if (mailboxExpression.isExpressionMatch(match,
                        HIERARCHY_DELIMITER)) {
                    listResults.add(new ListResultImpl(name, "."));
                }
            }
        }
        final ListResult[] results = (ListResult[]) listResults
        .toArray(ListResult.EMPTY_ARRAY);
        Arrays.sort(results);
        return results;

    }

    public boolean mailboxExists(String mailboxName) throws MailboxException {
        synchronized (mailboxes) {
            final MailboxMapper mapper = createMailboxMapper();
            final long count = mapper.countMailboxesWithName(mailboxName);
            if (count == 0) {
                mailboxes.remove(mailboxName);
                return false;
            } else {
                if (count == 1) {
                    return true;
                } else {
                    throw new MailboxException("Expected one mailbox but found " + count + " mailboxes");
                }
            }
        }
    }

    public void deleteEverything() throws MailboxException {
        final MailboxMapper mapper = createMailboxMapper();
        mapper.begin();
        mapper.deleteAll();
        mapper.commit();
        mailboxes.clear();
    }

    public MailboxSession createSession() {
        return new SimpleMailboxSession(random.nextLong());
    }

    public String resolve(final String userName, String mailboxPath) {
        if (mailboxPath.charAt(0) != HIERARCHY_DELIMITER) {
            mailboxPath = HIERARCHY_DELIMITER + mailboxPath;
        }
        final String result = USER_NAMESPACE + HIERARCHY_DELIMITER + userName
        + mailboxPath;
        return result;
    }

    public boolean isAuthentic(String userid, String passwd) {
        return authenticator.isAuthentic(userid, passwd);
    }

    public void subscribe(String user, String mailbox)
    throws SubscriptionException {
        subscriber.subscribe(user, mailbox);
    }

    public Collection<String> subscriptions(String user) throws SubscriptionException {
        return subscriber.subscriptions(user);
    }

    public void unsubscribe(String user, String mailbox)
    throws SubscriptionException {
        subscriber.unsubscribe(user, mailbox);
    }

    public void addListener(String mailboxName, MailboxListener listener) throws MailboxException {
        final StoreMailbox mailbox = doGetMailbox(mailboxName);
        mailbox.addListener(listener);
    }
}
