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

import javax.mail.Flags;

import org.apache.james.imap.mailbox.Mailbox;

/**
 * Describes the current state of a mailbox.
 */
public class MailboxMetaData implements Mailbox.MetaData {

    private final int recentCount;
    private final long[] recent;
    private final Flags premanentFlags;
    private final long uidValidity;
    private final long nextUid;
    private final int messageCount;
    private final int unseenCount;
    private final Long firstUnseen;
    private final boolean writeable;
    
    public MailboxMetaData(final long[] recent, final Flags premanentFlags, final long uidValidity, final long nextUid,
            final int messageCount, final int unseenCount, final Long firstUnseen, final boolean writeable) {
        super();
        if (recent == null) {
            recentCount = 0;
        } else {
            recentCount = recent.length;
        }
        this.recent = recent;
        this.premanentFlags = premanentFlags;
        this.uidValidity = uidValidity;
        this.nextUid = nextUid;
        this.messageCount = messageCount;
        this.unseenCount = unseenCount;
        this.firstUnseen = firstUnseen;
        this.writeable = writeable;
    }

    /**
     * @see {@link Mailbox.MetaData#countRecent()()}
     */
    public int countRecent() {
        return recentCount;
    }

    /**
     * @see {@link Mailbox.MetaData#getPermanentFlags()()}
     */
    public Flags getPermanentFlags() {
        return premanentFlags;
    }

    /**
     * @see {@link Mailbox.MetaData#getRecent()}
     */
    public long[] getRecent() {
        return recent;
    }

    /**
     * @see {@link Mailbox.MetaData#getUidValidity()}
     */
    public long getUidValidity() {
        return uidValidity;
    }

    /**
     * @see {@link Mailbox.MetaData#getUidNext()}
     */
    public long getUidNext() {
        return nextUid;
    }

    /**
     * @see {@link Mailbox.MetaData#getMessageCount()}
     */
    public int getMessageCount() {
        return messageCount;
    }

    /**
     * @see {@link Mailbox.MetaData#getUnseenCount()}
     */
    public int getUnseenCount() {
        return unseenCount;
    }

    public Long getFirstUnseen() {
        return firstUnseen;
    }

    public boolean isWriteable() {
        return writeable;
    }
}