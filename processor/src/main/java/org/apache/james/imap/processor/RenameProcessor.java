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

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.RenameRequest;
import org.apache.james.imap.processor.base.ImapSessionUtils;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxExistsException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;

public class RenameProcessor extends AbstractMailboxProcessor {

    public RenameProcessor(final ImapProcessor next,
            final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof RenameRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final RenameRequest request = (RenameRequest) message;
        final MailboxPath existingPath = buildFullPath(session, request.getExistingName());
        final MailboxPath newPath = buildFullPath(session, request.getNewName());
        try {
            final MailboxManager mailboxManager = getMailboxManager();
            MailboxSession mailboxsession = ImapSessionUtils.getMailboxSession(session);
            if (existingPath.getName().equalsIgnoreCase(ImapConstants.INBOX_NAME)) {

                // if the mailbox is INBOX we need to move move the messages
                // https://issues.apache.org/jira/browse/IMAP-188                           
                MessageRange range = MessageRange.all();
                // create the mailbox if it not exist yet
                if (mailboxManager.mailboxExists(newPath, mailboxsession) == false) {
                    mailboxManager.createMailbox(newPath, mailboxsession);
                }
                mailboxManager.copyMessages(range, existingPath, newPath, mailboxsession);
                
                org.apache.james.mailbox.MessageManager inbox = mailboxManager.getMailbox(existingPath, mailboxsession);
                inbox.setFlags(new Flags(Flags.Flag.DELETED), true, false, range, mailboxsession);
                inbox.expunge(range, mailboxsession);
            } else {
                mailboxManager.renameMailbox(existingPath, newPath, mailboxsession);
            }
            okComplete(command, tag, responder);
            unsolicitedResponses(session, responder, false);
        } catch (MailboxExistsException e) {
            no(command, tag, responder, HumanReadableText.FAILURE_MAILBOX_EXISTS);
        } catch (MailboxNotFoundException e) {
            no(command, tag, responder, HumanReadableText.MAILBOX_NOT_FOUND);
        } catch (MailboxException e) {
            no(command, tag, responder, HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING);
        }
    }
}
