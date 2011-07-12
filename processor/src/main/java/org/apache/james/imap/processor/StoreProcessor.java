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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.request.StoreRequest;
import org.apache.james.imap.message.response.FetchResponse;
import org.apache.james.imap.processor.base.FetchGroupImpl;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageManager.MetaData.FetchGroup;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageRangeException;
import org.apache.james.mailbox.MessageResult;

public class StoreProcessor extends AbstractMailboxProcessor<StoreRequest> {

    public StoreProcessor(final ImapProcessor next, final MailboxManager mailboxManager, final StatusResponseFactory factory) {
        super(StoreRequest.class, next, mailboxManager, factory);
    }

    protected void doProcess(StoreRequest request, ImapSession session, String tag, ImapCommand command, Responder responder) {
        final IdRange[] idSet = request.getIdSet();
        final boolean useUids = request.isUseUids();
        final long unchangedSince = request.getUnchangedSince();

        try {
            final MessageManager mailbox = getSelectedMailbox(session);
            final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);

            if (unchangedSince != -1 && mailbox.isModSeqPermanent(mailboxSession) == false ) {
                // Check if the mailbox did not support modsequences. If so return a tagged bad response.
                // See RFC4551 3.1.2. NOMODSEQ Response Code 
                taggedBad(command, tag, responder, HumanReadableText.NO_MOD_SEQ);
                return;
            }
            List<Long> failedUids = new ArrayList<Long>();

            for (int i = 0; i < idSet.length; i++) {
                final SelectedMailbox selected = session.getSelected();
                MessageRange messageSet = messageRange(selected, idSet[i], useUids);
                if (messageSet != null) {
                    
                    if (unchangedSince != -1) {
                        List<Long> uids = new ArrayList<Long>();

                        Iterator<MessageResult> results = mailbox.getMessages(messageSet, FetchGroupImpl.MINIMAL, mailboxSession);
                        while(results.hasNext()) {
                            MessageResult r = results.next();
                            long uid = r.getUid();
                            if (r.getModSeq() <= unchangedSince) {
                                uids.add(uid);
                            } else {
                                failedUids.add(uid);
                            }
                        }
                        List<MessageRange> mRanges = MessageRange.toRanges(uids);
                        for (int a = 0 ; a < mRanges.size(); a++) {
                            setFlags(request, mailboxSession, mailbox, mRanges.get(a), selected, tag, command, responder);
                        }
                    } else {
                        setFlags(request, mailboxSession, mailbox, messageSet, selected, tag, command, responder);
                    }
                    
                }

                
            }
            final boolean omitExpunged = (!useUids);
            unsolicitedResponses(session, responder, omitExpunged, useUids);
            if (failedUids.isEmpty()) {
                okComplete(command, tag, responder);
            } else {
                // TODO: Fix me!
                final StatusResponse response = getStatusResponseFactory().taggedOk(tag, command, HumanReadableText.COMPLETED);
                responder.respond(response);
            }
        } catch (MessageRangeException e) {
            session.getLog().debug("Store failed", e); 
            taggedBad(command, tag, responder, HumanReadableText.INVALID_MESSAGESET);
        } catch (MailboxException e) {
            session.getLog().debug("Store failed", e);
            no(command, tag, responder, HumanReadableText.SAVE_FAILED);
        }
    }
    
    private void setFlags(StoreRequest request, MailboxSession mailboxSession, MessageManager mailbox, MessageRange messageSet, SelectedMailbox selected, String tag, ImapCommand command, Responder responder) throws MailboxException {
        
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
        
        final Map<Long, Flags> flagsByUid = mailbox.setFlags(flags, value, replace, messageSet, mailboxSession);
        // As the STORE command is allowed to create a new "flag/keyword", we need to send a FLAGS and PERMANENTFLAGS response before the FETCH response
        // if some new flag/keyword was used
        // See IMAP-303
        if (selected.hasNewApplicableFlags()) {
            flags(responder, selected);
            permanentFlags(responder, mailbox.getMetaData(false, mailboxSession, FetchGroup.NO_COUNT), selected);
            selected.resetNewApplicableFlags();
        }
        
        if (!silent) {

            for (Map.Entry<Long, Flags> entry : flagsByUid.entrySet()) {
                final long uid = entry.getKey();
                final int msn = selected.msn(uid);

                if (msn == SelectedMailbox.NO_SUCH_MESSAGE)
                    throw new MailboxException("No message found with uid " + uid);

                final Flags resultFlags = entry.getValue();
                final Long resultUid;
                if (useUids) {
                    resultUid = uid;
                } else {
                    resultUid = null;
                }
                
                
                if (selected.isRecent(uid)) {
                    resultFlags.add(Flags.Flag.RECENT);
                }
               
                final FetchResponse response = new FetchResponse(msn, resultFlags, resultUid, null, null, null, null, null, null);
                responder.respond(response);
            }
        }
    }
}
