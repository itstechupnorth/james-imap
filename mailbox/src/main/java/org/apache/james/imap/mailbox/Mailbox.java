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

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.mail.Flags;

import org.apache.james.imap.mailbox.MessageResult.FetchGroup;


public interface Mailbox {

    public static final long ANONYMOUS_SESSION = 0;

    int getMessageCount(MailboxSession mailboxSession) throws MailboxException;

    boolean isWriteable();

    /**
     * Searches for messages matching the given query.
     * @param mailboxSession not null
     * @return uid iterator
     * @throws UnsupportedCriteriaException
     *             when any of the search parameters are not supported by this
     *             mailbox
     * @throws MailboxException when search fails for other reasons
     */
    Iterator<Long> search(SearchQuery searchQuery, MailboxSession mailboxSession) throws MailboxException;

    long getUidValidity(MailboxSession mailboxSession)
            throws MailboxException;

    /**
     * 
     * @param mailboxSession not null
     * @return the uid that will be assigned to the next appended message
     * @throws MailboxException
     */

    long getUidNext(MailboxSession mailboxSession) throws MailboxException;

    /**
     * @return Flags that can be stored
     */
    Flags getPermanentFlags();

    long[] recent(boolean reset, MailboxSession mailboxSession) throws MailboxException;

    int getUnseenCount(MailboxSession mailboxSession) throws MailboxException;

    /**
     * Gets the UID of the first unseen message.
     * @param mailboxSession not null
     * @return uid of the first unseen message,
     * or null when there are no unseen messages
     * @throws MailboxException 
     */
    Long getFirstUnseen(MailboxSession mailboxSession) throws MailboxException;

    /**
     * Expunges messages in the given range from this mailbox.
     * @param set not null
     * @param mailboxSession not null
     * @return 
     * @throws MailboxException
     *             if anything went wrong
     */
    Iterator<Long> expunge(MessageRange set, MailboxSession mailboxSession) throws MailboxException;

    /**
     * Sets flags on messages within the given range.
     * The new flags are returned for each message altered.
     * 
     * @param flags
     *            Flags to be set
     * @param value
     *            true = set, false = unset
     * @param replace
     *            replace all Flags with this flags, value has to be true
     * @param set
     *            the range of messages
     * @param mailboxSession not null
     * @return {@link MessageResult} <code>Iterator</code> containing messages
     *         whose flags have been updated, not null
     * @throws MailboxException
     */
    Map<Long, Flags> setFlags(Flags flags, boolean value, boolean replace,
            MessageRange set, MailboxSession mailboxSession) throws MailboxException;

    /**
     * Appends a message to this mailbox.
     * @param internalDate the time of addition to be set, not null
     * @param mailboxSession not null
     * @param isRecent true when the message should be marked recent,
     * false otherwise
     * @return uid for the newly added message
     * @throws MailboxException when message cannot be appended
     */
    long appendMessage(byte[] message, Date internalDate, MailboxSession mailboxSession, 
            boolean isRecent) throws MailboxException;

    /**
     * TODO: consolidate search and getMessages into a single method
     * 
     * @param set
     * @param mailboxSession
     *            TODO
     * @return MessageResult with the fields defined by <b>result</b>
     *         <ul>
     *         <li> IMAP: a set of msn, uid, Flags, header lines, content, mime
     *         parts...</li>
     *         <li> Javamail Folder: Message[]</li>
     *         </ul>
     * @throws MailboxException
     */

    Iterator getMessages(MessageRange set, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxException;

}
