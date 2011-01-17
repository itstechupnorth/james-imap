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
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.request.CopyRequest;
import org.apache.james.imap.processor.base.MessageRangeException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;

public class CopyProcessor extends AbstractMailboxProcessor {

    public CopyProcessor(final ImapProcessor next,
            final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof CopyRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final CopyRequest request = (CopyRequest) message;
        final MailboxPath targetMailbox = buildFullPath(session, request.getMailboxName());
        final IdRange[] idSet = request.getIdSet();
        final boolean useUids = request.isUseUids();
        final SelectedMailbox currentMailbox = session.getSelected();
        try {
            final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
            final MailboxManager mailboxManager = getMailboxManager();
            final boolean mailboxExists = mailboxManager.mailboxExists(targetMailbox, mailboxSession);
            if (!mailboxExists) {
                no(command, tag, responder, HumanReadableText.FAILURE_NO_SUCH_MAILBOX,
                        ResponseCode.tryCreate());
            } else {
                for (int i = 0; i < idSet.length; i++) {
                    
                    MessageRange messageSet = messageRange(currentMailbox, idSet[i], useUids);

                    mailboxManager.copyMessages(messageSet, currentMailbox.getPath(),
                                                targetMailbox, mailboxSession);
                }
                unsolicitedResponses(session, responder, useUids);
                okComplete(command, tag, responder);
            }
        } catch (MessageRangeException e) {
            taggedBad(command, tag, responder, HumanReadableText.INVALID_MESSAGESET);
        } catch (MailboxException e) {
            no(command, tag, responder, HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING);
        }
    }
}
