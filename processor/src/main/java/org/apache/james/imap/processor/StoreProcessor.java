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

import java.util.Map;

import javax.mail.Flags;

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
import org.apache.james.imap.message.request.StoreRequest;
import org.apache.james.imap.message.response.FetchResponse;
import org.apache.james.imap.processor.base.MessageRangeException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageRange;

public class StoreProcessor extends AbstractMailboxProcessor {
    
    public StoreProcessor(final ImapProcessor next, final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof StoreRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final StoreRequest request = (StoreRequest) message;
        final IdRange[] idSet = request.getIdSet();
        final Flags flags = request.getFlags();
        final boolean useUids = request.isUseUids();
        final boolean silent = request.isSilent();
        final boolean isSignedPlus = request.isSignedPlus();
        final boolean isSignedMinus = request.isSignedMinus();

        final boolean replace;
        final boolean value;
        if (isSignedMinus) {
            value = false;
            replace = false;
        } else if (isSignedPlus) {
            value = true;
            replace = false;
        } else {
            replace = true;
            value = true;
        }
        try {
            final MessageManager mailbox = getSelectedMailbox(session);
            for (int i = 0; i < idSet.length; i++) {
                final SelectedMailbox selected = session.getSelected();
                MessageRange messageSet = messageRange(selected, idSet[i], useUids);

                final MailboxSession mailboxSession = ImapSessionUtils
                        .getMailboxSession(session);
                final Map<Long, Flags> flagsByUid = mailbox.setFlags(flags, value, replace,
                        messageSet, mailboxSession);
                if (!silent) {
                    for (Map.Entry<Long, Flags> entry: flagsByUid.entrySet()) {
                        final long uid = entry.getKey();
                        final int msn = selected.msn(uid);
                        
                        if (msn == SelectedMailbox.NO_SUCH_MESSAGE) throw new MailboxException("No message found with uid " + uid);
                        
                        final Flags resultFlags = entry.getValue();
                        final Long resultUid;
                        if (useUids) {
                            resultUid = new Long(uid);
                        } else {
                            resultUid = null;
                        }
                        if (selected.isRecent(uid)) {
                            resultFlags.add(Flags.Flag.RECENT);
                        }
                        final FetchResponse response = new FetchResponse(msn,
                                resultFlags, resultUid, null, null, null, null,
                                null, null);
                        responder.respond(response);
                    }
                }
            }
            final boolean omitExpunged = (!useUids);
            unsolicitedResponses(session, responder, omitExpunged, useUids);
            okComplete(command, tag, responder);
        } catch (MessageRangeException e) {
            taggedBad(command, tag, responder, HumanReadableText.INVALID_MESSAGESET);
        } catch (MailboxException e) {
            no(command, tag, responder, HumanReadableText.SAVE_FAILED);
        }
    }
}
