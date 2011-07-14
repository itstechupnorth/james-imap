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

import java.util.Iterator;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.CloseRequest;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageManager.MetaData;
import org.apache.james.mailbox.MessageManager.MetaData.FetchGroup;

public class CloseProcessor extends AbstractMailboxProcessor<CloseRequest> {

    public CloseProcessor(final ImapProcessor next, final MailboxManager mailboxManager, final StatusResponseFactory factory) {
        super(CloseRequest.class, next, mailboxManager, factory);
    }

    protected void doProcess(CloseRequest message, ImapSession session, String tag, ImapCommand command, Responder responder) {
        try {
            MessageManager mailbox = getSelectedMailbox(session);
            final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
            if (mailbox.isWriteable(mailboxSession)) {

                Iterator<Long> expunged = mailbox.expunge(MessageRange.all(), mailboxSession);
                session.deselect();
                
                // Check if some messages was expunged and if QRESYNC was enabled. If so we need to respond with an OK response that contain the HIGHESTMODSEQ
                //
                // See RFC5162 3.4. CLOSE Command
                if (expunged.hasNext() && EnableProcessor.getEnabledCapabilities(session).contains(ImapConstants.SUPPORTS_QRESYNC)) {
                    MetaData mdata = mailbox.getMetaData(false, mailboxSession, FetchGroup.NO_COUNT);
                    okComplete(command, tag, ResponseCode.highestModSeq(mdata.getHighestModSeq()), responder);
                } else {
                    okComplete(command, tag, responder);
                }
            }

        } catch (MailboxException e) {
            session.getLog().debug("Close failed", e);
            no(command, tag, responder, HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING);
        }
    }
}
