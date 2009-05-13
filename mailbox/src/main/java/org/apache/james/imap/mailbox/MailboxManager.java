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
     * @param session the context for this call, not null
     * @return <code>ImapMailboxSession</code>, not null
     * @throws MailboxException
     *             when the mailbox cannot be opened
     * @throws MailboxNotFoundException
     *             when the given mailbox does not exist
     */
    Mailbox getMailbox(String mailboxName, MailboxSession session) throws MailboxException;

    /**
     * Creates a new mailbox. Any intermediary mailboxes missing from the
     * hierarchy should be created.
     * 
     * @param mailboxName
     *            name, not null
     * @param mailboxSession the context for this call, not null
     * @throws MailboxException when creation fails
     */
    void createMailbox(String mailboxName, MailboxSession mailboxSession) throws MailboxException;

    void deleteMailbox(String mailboxName, MailboxSession session) throws MailboxException;

    /**
     * Renames a mailbox.
     * 
     * @param from
     *            original name for the mailbox
     * @param to
     *            new name for the mailbox
     * @param session the context for this call, not nul
     * @throws MailboxException otherwise
     * @throws MailboxExistsException
     *             when the <code>to</code> mailbox exists
     * @throws MailboxNotFound
     *             when the <code>from</code> mailbox does not exist
     */
    void renameMailbox(String from, String to, MailboxSession session) throws MailboxException;

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
     * @param session the context for this call, not null
     * @throws MailboxException
     */
    List<MailboxMetaData> search(MailboxQuery expression, MailboxSession session) throws MailboxException;

    /**
     * Does the given mailbox exist?
     * @param mailboxName not null
     * @param session the context for this call, not null
     * @return true when the mailbox exists and is accessible for the given user, false otherwise
     * @throws MailboxException
     */
    boolean mailboxExists(String mailboxName, MailboxSession session) throws MailboxException;

    /**
     * Creates a new system session.
     * A system session is intended to be used for programmatic access.
     * Use {@link #login(String, String, Log)} when accessing this API
     * from a protocol.
     * @param userName the name of the user whose session is being created
     * @param log context sensitive log
     * @return <code>MailboxSession</code>, not null
     * @throws BadCredentialsException when system access is not allowed for the given user
     * @throws MailboxException when the creation fails for other reasons
     */
    public MailboxSession createSystemSession(String userName, Log log) throws BadCredentialsException, MailboxException;

    /**
     * Autenticates the given user against the given password.
     * When authentic and authorized, a session will be supplied
     * @param userid
     *            user name
     * @param passwd
     *            password supplied
     * @param log context sensitive log    
     * @return a <code>MailboxSession</code> when the user is authentic and authorized to access
     * @throws BadCredentialsException when system access is denighed for the given user
     * @throws MailboxException when the creation fails for other reasons
     */
    MailboxSession login(String userid, String passwd, Log log) throws BadCredentialsException, MailboxException;
    
    /**
     * <p>Logs the session out, freeing any resources.
     * Clients who open session should make best efforts to call this
     * when the session is closed.
     * </p>
     * <p>
     * Note that clients may not always be able to call logout (whether forced or not).
     * Mailboxes that create sessions which are expensive to maintain
     * <code>MUST</code> retain a reference and periodically check {@link MailboxSession#isOpen()}.
     * </p>
     * <p>
     * Note that implementations much be aware that it is possible that this method 
     * may be called more than once with the same session.
     * </p>
     * @param session not null
     * @param force true when the session logout is forced by premature connection termination
     * @throws MailboxException when logout fails
     */
    void logout(MailboxSession session, boolean force) throws MailboxException;

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
