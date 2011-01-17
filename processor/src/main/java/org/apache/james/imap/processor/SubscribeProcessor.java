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

package org.apache.james.imap.processor;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.message.request.SubscribeRequest;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SubscriptionException;
import org.apache.james.mailbox.SubscriptionManager;

public class SubscribeProcessor extends AbstractSubscriptionProcessor {

    public SubscribeProcessor(ImapProcessor next, MailboxManager mailboxManager, SubscriptionManager subscriptionManager, StatusResponseFactory factory) {
        super(next, mailboxManager, subscriptionManager, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof SubscribeRequest);
    }

    @Override
    protected void doProcessRequest(ImapRequest message, ImapSession session, String tag, ImapCommand command, Responder responder) {
        final SubscribeRequest request = (SubscribeRequest) message;
        final String mailboxName = request.getMailboxName();
        final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
        try {
            getSubscriptionManager().subscribe(mailboxSession, mailboxName);

            unsolicitedResponses(session, responder, false);
            okComplete(command, tag, responder);

        } catch (SubscriptionException e) {
            session.getLog().debug("Subscription failed", e);
            unsolicitedResponses(session, responder, false);
            no(command, tag, responder,  HumanReadableText.GENERIC_SUBSCRIPTION_FAILURE);
        }        
    }

}
