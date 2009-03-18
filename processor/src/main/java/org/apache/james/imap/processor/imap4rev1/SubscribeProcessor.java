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

package org.apache.james.imap.processor.imap4rev1;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.display.HumanReadableTextKey;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.message.request.imap4rev1.SubscribeRequest;
import org.apache.james.imap.processor.base.ImapSessionUtils;

public class SubscribeProcessor extends AbstractMailboxProcessor {

    public SubscribeProcessor(final ImapProcessor next,
            final MailboxManagerProvider mailboxManagerProvider,
            final StatusResponseFactory factory) {
        super(next, mailboxManagerProvider, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof SubscribeRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final SubscribeRequest request = (SubscribeRequest) message;
        final String mailboxName = request.getMailboxName();
        final String userName = ImapSessionUtils.getUserName(session);
        try {
            final MailboxManager manager = getMailboxManager();
            manager.subscribe(userName, mailboxName);

            unsolicitedResponses(session, responder, false);
            okComplete(command, tag, responder);

        } catch (SubscriptionException e) {
            getLog().debug("Subscription failed", e);
            unsolicitedResponses(session, responder, false);

            final HumanReadableTextKey exceptionKey = e.getKey();
            final HumanReadableTextKey displayTextKey;
            if (exceptionKey == null) {
                displayTextKey = HumanReadableTextKey.GENERIC_SUBSCRIPTION_FAILURE;
            } else {
                displayTextKey = exceptionKey;
            }
            no(command, tag, responder, displayTextKey);
        } catch (MailboxException e) {
            getLog().debug("Subscription failed", e);
            unsolicitedResponses(session, responder, false);

            final HumanReadableTextKey displayTextKey = HumanReadableTextKey.GENERIC_SUBSCRIPTION_FAILURE;
            no(command, tag, responder, displayTextKey);
        }
    }
}
