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

package org.apache.james.imap.mailbox.util;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.mailbox.MailboxListener;

public class MailboxListenerCollector implements MailboxListener {

    protected List<Long> addedList = new ArrayList<Long>();

    protected List<Long> expungedList = new ArrayList<Long>();

    protected List<MessageFlags> flaggedList = new ArrayList<MessageFlags>();

    public void added(final long uid) {
        addedList.add(new Long(uid));
    }

    public void expunged(final long uid) {
        expungedList.add(new Long(uid));
    }

    public void flagsUpdated(final long uid, final Flags flags) {
        flaggedList.add(new MessageFlags(uid, flags));
    }

    public synchronized List<Long> getAddedList(boolean reset) {
        final List<Long> list = addedList;
        if (reset) {
            addedList = new ArrayList<Long>();
        }
        return list;
    }

    public synchronized List<Long> getExpungedList(boolean reset) {
        final List<Long> list = expungedList;
        if (reset) {
            expungedList = new ArrayList<Long>();
        }
        return list;
    }

    public synchronized List<MessageFlags> getFlaggedList(boolean reset) {
        final List<MessageFlags> list = flaggedList;
        if (reset) {
            flaggedList = new ArrayList<MessageFlags>();
        }
        return list;
    }

    public void mailboxDeleted() {
    }

    public void mailboxRenamed(String origName, String newName) {
    }

    public void mailboxRenamed(String newName) {
    }

    public void event(Event event) {
        if (event instanceof MessageEvent) {
            final MessageEvent messageEvent = (MessageEvent) event;
            final long uid = messageEvent.getSubjectUid();
            if (event instanceof Added) {
                added(uid);
            } else if (event instanceof Expunged) {
                expunged(uid);
            } else if (event instanceof FlagsUpdated) {
                final FlagsUpdated flagsUpdated = (FlagsUpdated) messageEvent;
                final Flags newFlags = flagsUpdated.getNewFlags();
                flagsUpdated(uid, newFlags);
            }
        }
    }

    public boolean isClosed() {
        return false;
    }

}
