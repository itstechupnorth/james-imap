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
import org.apache.james.imap.message.request.LoginRequest;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxExistsException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;

/**
 * Processes a <code>LOGIN</code> command.
 */
public class LoginProcessor extends AbstractMailboxProcessor {

    public static final String INBOX = "INBOX";
    
    private static final String ATTRIBUTE_NUMBER_OF_FAILURES = "org.apache.james.imap.processor.imap4rev1.LoginProcessor.NUMBER_OF_FAILURES";

    // TODO: this should be configurable
    private static final int MAX_FAILURES = 3;

    public LoginProcessor(final ImapProcessor next,
            final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof LoginRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        try {
            final LoginRequest request = (LoginRequest) message;
            final String userid = request.getUserid();
            final String passwd = request.getPassword();
            final MailboxManager mailboxManager = getMailboxManager();
            try {
                final MailboxSession mailboxSession = mailboxManager.login(userid, passwd, session.getLog());
                session.authenticated();
                session.setAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY,
                                        mailboxSession);
                final MailboxPath inboxPath = buildFullPath(session, INBOX);
                if (mailboxManager.mailboxExists(inboxPath, mailboxSession)) {
                    session.getLog().debug("INBOX exists. No need to create it.");
                } else {
                    try {
                        session.getLog().debug("INBOX does not exist. Creating it.");
                        mailboxManager.createMailbox(inboxPath, mailboxSession);
                    } catch (MailboxExistsException e) {
                        session.getLog().debug("Mailbox created by concurrent call. Safe to ignore this exception.");
                    }
                }
                okComplete(command, tag, responder);
            } catch (BadCredentialsException e) {
                final Integer currentNumberOfFailures = (Integer) session
                        .getAttribute(ATTRIBUTE_NUMBER_OF_FAILURES);
                final int failures;
                if (currentNumberOfFailures == null) {
                    failures = 1;
                } else {
                    failures = currentNumberOfFailures.intValue() + 1;
                }
                if (failures < MAX_FAILURES) {
                    session.setAttribute(ATTRIBUTE_NUMBER_OF_FAILURES,
                            new Integer(failures));
                    no(command, tag, responder,
                            HumanReadableText.INVALID_LOGIN);
                } else {
                    session.getLog().info("Too many authentication failures. Closing connection.");
                    bye(responder, HumanReadableText.TOO_MANY_FAILURES);
                    session.logout();
                }
            }
        } catch (MailboxException e) {
            session.getLog().debug("Login failed", e);
            no(command, tag, responder, HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING);
        }
    }
}
