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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.request.ExpungeRequest;
import org.apache.james.imap.processor.base.MessageRangeException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageRange;

public class ExpungeProcessor extends AbstractMailboxProcessor implements CapabilityImplementingProcessor{

    private final static List<String> UIDPLUS = Arrays.asList("UIDPLUS");
    
    public ExpungeProcessor(final ImapProcessor next,
            final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof ExpungeRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        try {
            final MessageManager mailbox = getSelectedMailbox(session);
            final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
            final ExpungeRequest request = (ExpungeRequest) message;
            
            if (!mailbox.isWriteable(mailboxSession)) {
                no(command, tag, responder,
                        HumanReadableText.MAILBOX_IS_READ_ONLY);
            } else {
                IdRange[] ranges = request.getUidSet();
                if (ranges == null) {
                    expunge(mailbox, MessageRange.all(), session, mailboxSession);
                } else {
                    // Handle UID EXPUNGE which is part of UIDPLUS
                    // See http://tools.ietf.org/html/rfc4315
                    for (int i = 0; i <ranges.length; i++) {
                        MessageRange mRange = messageRange(session.getSelected(), ranges[i], true);
                        expunge(mailbox, mRange, session, mailboxSession);

                    }

                }
               
                unsolicitedResponses(session, responder, false);
                okComplete(command, tag, responder);
            }
        } catch (MessageRangeException e) {
            taggedBad(command, tag, responder, HumanReadableText.INVALID_MESSAGESET);
        } catch (MailboxException e) {
            no(command, tag, responder, HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING);
        }
    }
    
    private void expunge(MessageManager mailbox, MessageRange range, ImapSession session, MailboxSession mailboxSession) throws MailboxException {
        final Iterator<Long> it = mailbox.expunge(MessageRange.all(), mailboxSession);
        final SelectedMailbox selected = session.getSelected();
        if (mailboxSession != null) {
            while (it.hasNext()) {
                final long uid = it.next();
                selected.removeRecent(uid);
            }
        }
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.processor.CapabilityImplementingProcessor#getImplementedCapabilities(org.apache.james.imap.api.process.ImapSession)
     */
    public List<String> getImplementedCapabilities(ImapSession session) {
        return UIDPLUS;
    }
}
