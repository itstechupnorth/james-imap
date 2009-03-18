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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.display.HumanReadableTextKey;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.mailbox.MailboxQuery;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.message.request.imap4rev1.LsubRequest;
import org.apache.james.imap.message.response.imap4rev1.server.LSubResponse;
import org.apache.james.imap.processor.base.ImapSessionUtils;

public class LSubProcessor extends AbstractMailboxProcessor {

    public LSubProcessor(final ImapProcessor next,
            final MailboxManagerProvider mailboxManagerProvider,
            final StatusResponseFactory factory) {
        super(next, mailboxManagerProvider, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof LsubRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final LsubRequest request = (LsubRequest) message;
        final String referenceName = request.getBaseReferenceName();
        final String mailboxPattern = request.getMailboxPattern();

        try {
            if (mailboxPattern.length() == 0) {
                respondWithHierarchyDelimiter(responder);

            } else {
                listSubscriptions(session, responder, referenceName,
                        mailboxPattern);
            }

            okComplete(command, tag, responder);

        } catch (SubscriptionException e) {
            getLog().debug("Subscription failed", e);
            final HumanReadableTextKey exceptionKey = e.getKey();
            final HumanReadableTextKey displayTextKey;
            if (exceptionKey == null) {
                displayTextKey = HumanReadableTextKey.GENERIC_LSUB_FAILURE;
            } else {
                displayTextKey = exceptionKey;
            }
            no(command, tag, responder, displayTextKey);
        } catch (MailboxException e) {
            getLog().debug("Subscription failed", e);
            final HumanReadableTextKey displayTextKey = HumanReadableTextKey.GENERIC_LSUB_FAILURE;
            no(command, tag, responder, displayTextKey);
        }
    }

    private void listSubscriptions(ImapSession session, Responder responder,
            final String referenceName, final String mailboxPattern)
            throws SubscriptionException, MailboxException {
        final String userName = ImapSessionUtils.getUserName(session);
        final MailboxManager manager = getMailboxManager();
        final Collection<String> mailboxes = manager.subscriptions(userName);
        final MailboxQuery expression = new MailboxQuery(
                referenceName, mailboxPattern, '*', '%');
        final Collection<String> mailboxResponses = new ArrayList<String>();
        for (final String mailboxName: mailboxes) {
            respond(responder, expression, mailboxName, true, mailboxes,
                    mailboxResponses);
        }
    }

    private void respond(Responder responder,
            final MailboxQuery expression, final String mailboxName,
            final boolean originalSubscription, final Collection<String> mailboxes,
            final Collection<String> mailboxResponses) {
        if (expression.isExpressionMatch(mailboxName,
                ImapConstants.HIERARCHY_DELIMITER_CHAR)) {
            if (!mailboxResponses.contains(mailboxName)) {
                final LSubResponse response = new LSubResponse(mailboxName,
                        ImapConstants.HIERARCHY_DELIMITER,
                        !originalSubscription);
                responder.respond(response);
                mailboxResponses.add(mailboxName);
            }
        } else {
            final int lastDelimiter = mailboxName
                    .lastIndexOf(ImapConstants.HIERARCHY_DELIMITER_CHAR);
            if (lastDelimiter > 0) {
                final String parentMailbox = mailboxName.substring(0,
                        lastDelimiter);
                if (!mailboxes.contains(parentMailbox)) {
                    respond(responder, expression, parentMailbox, false,
                            mailboxes, mailboxResponses);
                }
            }
        }
    }

    /**
     * An empty mailboxPattern signifies a request for the hierarchy delimiter
     * and root name of the referenceName argument
     * 
     * @param referenceName
     *            IMAP reference name, possibly null
     */
    private void respondWithHierarchyDelimiter(final Responder responder) {
        final LSubResponse response = new LSubResponse("",
                ImapConstants.HIERARCHY_DELIMITER, true);
        responder.respond(response);
    }
}
