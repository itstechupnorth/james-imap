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

package org.apache.james.imap.mailbox;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;


/**
 * <p>
 * Central MailboxManager which creates, lists, provides, renames and deletes
 * Mailboxes
 * </p>
 * <p>
 * An important goal is to be JavaMail feature compatible. That means JavaMail
 * could be used in both directions: As a backend for e.g. accessing a Maildir
 * JavaMail store or as a frontend to access a JDBC MailboxManager through
 * JavaMail. This should be possible by not too complicated wrapper classes. Due
 * to the complexity of JavaMail it might be impossible to avoid some
 * limitations.
 * </p>
 * <p>
 * Internally MailboxManager deals with named repositories that could have
 * different implementations. E.g. JDBC connections to different hosts or
 * Maildir / Mbox like stores. This repositories are identified by its names and
 * maybe are configured in config.xml. The names of the mailboxes have to be
 * mapped to the corresponding repository name. For user mailboxes this could be
 * done by a "User.getRepositoryName()" property. It is imaginable that
 * repositories lookup further properties from the user object like a path name
 * for a file based storage method. Until Milestone 6 there is only one named
 * repository: "default".
 * </p>
 * <p>
 * The only operation that requires dealing with the named repositories directly
 * is the quota management. It is probably really difficult to implement a quota
 * system that spans multiple repository implementations. That is why quotas are
 * created for a specific repository. To be able to administer, repositories and
 * theier belonging mailboxes can be listet.
 * </p>
 */

public interface MailboxManager {

    public static final char HIERARCHY_DELIMITER = '.';

    public static final String USER_NAMESPACE = "#mail";

    public static final String INBOX = "INBOX";

    /**
     * <p>
     * Resolves a path for the given user.
     * </p>
     * TODO: Think about replacing this operation TODO: More elegant to pass in
     * the username TODO: Or switch to URLs
     */
    String resolve(String userName, String mailboxPath);

    /**
     * Gets an session suitable for IMAP.
     * 
     * @param mailboxName
     *            the name of the mailbox, not null
     * @return <code>ImapMailboxSession</code>, not null
     * @throws MailboxException
     *             when the mailbox cannot be opened
     * @throws MailboxNotFoundException
     *             when the given mailbox does not exist
     */
    Mailbox getMailbox(String mailboxName) throws MailboxException;

    /**
     * Creates a new mailbox. Any intermediary mailboxes missing from the
     * hierarchy should be created.
     * 
     * @param mailboxName
     *            name, not null
     * @throws MailboxException
     */
    void createMailbox(String mailboxName) throws MailboxException;

    void deleteMailbox(String mailboxName, MailboxSession session) throws MailboxException;

    /**
     * Renames a mailbox.
     * 
     * @param from
     *            original name for the mailbox
     * @param to
     *            new name for the mailbox
     * @throws MailboxException
     * @throws MailboxExistsException
     *             when the <code>to</code> mailbox exists
     * @throws MailboxNotFound
     *             when the <code>from</code> mailbox does not exist
     */
    void renameMailbox(String from, String to) throws MailboxException;

    /**
     * this is done by the MailboxRepository because maybe this operation could
     * be optimized in the corresponding store.
     * 
     * @param set
     *            messages to copy
     * @param from
     *            name of the source mailbox
     * @param to
     *            name of the destination mailbox
     * @param session
     *            <code>MailboxSession</code>, not null
     */
    void copyMessages(MessageRange set, String from, String to,
            MailboxSession session) throws MailboxException;

    /**
     * Searches for mailboxes matching the given query.
     * @param expression not null
     * @throws MailboxException
     */
    List<MailboxMetaData> search(MailboxQuery expression) throws MailboxException;

    boolean mailboxExists(String mailboxName) throws MailboxException;

    /**
     * Creates a new session.
     * @param userName the name of the user whose session is being created
     * @param log context sensitive log
     * @return <code>MailboxSession</code>, not null
     */
    public MailboxSession createSession(String userName, Log log);

    /**
     * Autenticates the given user against the given password.
     * 
     * @param userid
     *            user name
     * @param passwd
     *            password supplied
     * @return true if the user is authenticated
     */
    boolean isAuthentic(String userid, String passwd);

    /**
     * Subscribes the user to the given mailbox.
     * 
     * @param session
     *            the user name, not null
     * @param mailbox
     *            the mailbox name, not null
     */
    public void subscribe(MailboxSession session, String mailbox)
            throws SubscriptionException;

    /**
     * Unsubscribes the user from the given mailbox.
     * 
     * @param session
     *            the user name, not null
     * @param mailbox
     *            the mailbox name, not null
     */
    public void unsubscribe(MailboxSession session, String mailbox)
            throws SubscriptionException;

    /**
     * Lists current subscriptions for the given user.
     * 
     * @param session
     *            the user name, not null
     * @return a <code>Collection<String></code> of mailbox names
     */
    public Collection<String> subscriptions(MailboxSession session) throws SubscriptionException;
    
    /**
     * <p>Implementations of Mailbox may interpret the fact that someone is
     * listening and do some caching and even postpone persistence until
     * everyone has removed itself.
     * </p><p>
     * Listeners should return true from {@link MailboxListener#isClosed()}
     * when they are ready to be removed.
     * </p>
     * @param mailboxName not null
     * @param listener not null
     * @throws MailboxException
     */
    void addListener(String mailboxName, MailboxListener listener) throws MailboxException;
}
