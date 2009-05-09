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

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.display.HumanReadableTextKey;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.util.FetchGroupImpl;
import org.apache.james.imap.mailbox.util.MessageRangeImpl;
import org.apache.james.imap.message.request.AbstractMailboxSelectionRequest;
import org.apache.james.imap.message.response.ExistsResponse;
import org.apache.james.imap.message.response.FlagsResponse;
import org.apache.james.imap.message.response.RecentResponse;
import org.apache.james.imap.processor.base.ImapSessionUtils;
import org.apache.james.imap.processor.base.SelectedMailboxImpl;

abstract class AbstractSelectionProcessor extends AbstractMailboxProcessor {

    private final FlagsResponse standardFlags;

    final StatusResponseFactory statusResponseFactory;

    private final boolean openReadOnly;

    public AbstractSelectionProcessor(final ImapProcessor next,
            final MailboxManagerProvider mailboxManagerProvider,
            final StatusResponseFactory statusResponseFactory,
            final boolean openReadOnly) {
        super(next, mailboxManagerProvider, statusResponseFactory);
        this.statusResponseFactory = statusResponseFactory;
        this.openReadOnly = openReadOnly;
        final Flags flags = new Flags();
        flags.add(Flags.Flag.ANSWERED);
        flags.add(Flags.Flag.DELETED);
        flags.add(Flags.Flag.DRAFT);
        flags.add(Flags.Flag.FLAGGED);
        flags.add(Flags.Flag.SEEN);
        standardFlags = new FlagsResponse(flags);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final AbstractMailboxSelectionRequest request = (AbstractMailboxSelectionRequest) message;
        final String mailboxName = request.getMailboxName();
        try {
            final String fullMailboxName = buildFullName(session, mailboxName);
            final Mailbox.MetaData metaData = selectMailbox(fullMailboxName, session);
            respond(tag, command, session, metaData, responder);
        } catch (MailboxNotFoundException e) {
            responder.respond(statusResponseFactory.taggedNo(tag, command,
                    HumanReadableTextKey.FAILURE_NO_SUCH_MAILBOX));
        } catch (MailboxException e) {
            no(command, tag, responder, e, session);
        }
    }

    private void respond(String tag, ImapCommand command, ImapSession session,
            final Mailbox.MetaData metaData, Responder responder) throws MailboxException {

        Mailbox mailbox = getSelectedMailbox(session);
        final MailboxSession mailboxSession = ImapSessionUtils
                .getMailboxSession(session);
        final SelectedMailbox selected = session.getSelected();

        // TODO: compact this into a single API call for meta-data about the
        // repository
        flags(responder);
        exists(responder, metaData);
        recent(responder, selected);
        uidValidity(responder, metaData);
        unseen(responder, mailbox, mailboxSession, selected);
        permanentFlags(responder, metaData);
        uidNext(responder, metaData);
        taggedOk(responder, tag, command, mailbox);
    }

    private void uidNext(final Responder responder, final Mailbox.MetaData metaData)
    throws MailboxException {
        final long uid = metaData.getUidNext();
        final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                HumanReadableTextKey.UNSEEN, ResponseCode.uidNext(uid));
        responder.respond(untaggedOk);
    }
    
    private void taggedOk(final Responder responder, final String tag,
            final ImapCommand command, final Mailbox mailbox) {
        final boolean writeable = mailbox.isWriteable() && !openReadOnly;
        final ResponseCode code;
        if (writeable) {
            code = ResponseCode.readWrite();
        } else {
            code = ResponseCode.readOnly();
        }
        final StatusResponse taggedOk = statusResponseFactory.taggedOk(tag,
                command, HumanReadableTextKey.SELECT, code);
        responder.respond(taggedOk);
    }

    private void flags(Responder responder) {
        responder.respond(standardFlags);
    }

    private void permanentFlags(Responder responder, Mailbox.MetaData metaData) {
        final Flags permanentFlags = metaData.getPermanentFlags();
        final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                HumanReadableTextKey.PERMANENT_FLAGS, ResponseCode
                        .permanentFlags(permanentFlags));
        responder.respond(untaggedOk);
    }

    private void unseen(Responder responder, Mailbox mailbox,
            final MailboxSession mailboxSession,
            final SelectedMailbox selected) throws MailboxException {
        final Long firstUnseen = mailbox.getFirstUnseen(mailboxSession);
        if (firstUnseen != null) {
            final long unseenUid = firstUnseen;
            int msn = selected.msn(unseenUid);
            final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                    HumanReadableTextKey.UNSEEN, ResponseCode.unseen(msn));
            responder.respond(untaggedOk);
        }

    }

    private void uidValidity(Responder responder, Mailbox.MetaData metaData) throws MailboxException {
        final long uidValidity = metaData.getUidValidity();
        final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                HumanReadableTextKey.UID_VALIDITY, ResponseCode
                        .uidValidity(uidValidity));
        responder.respond(untaggedOk);
    }

    private void recent(Responder responder, final SelectedMailbox selected) {
        final int recentCount = selected.recentCount();
        final RecentResponse recentResponse = new RecentResponse(recentCount);
        responder.respond(recentResponse);
    }

    private void exists(Responder responder, Mailbox.MetaData metaData) throws MailboxException {
        final int messageCount = metaData.getMessageCount();
        final ExistsResponse existsResponse = new ExistsResponse(messageCount);
        responder.respond(existsResponse);
    }

    private Mailbox.MetaData  selectMailbox(String mailboxName, ImapSession session)
            throws MailboxException {
        final MailboxManager mailboxManager = getMailboxManager();
        final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
        final Mailbox mailbox = mailboxManager.getMailbox(mailboxName, mailboxSession);

        final SelectedMailbox sessionMailbox;
        final SelectedMailbox currentMailbox = session.getSelected();
        if (currentMailbox == null
                || !currentMailbox.getName().equals(mailboxName)) {
            sessionMailbox = createNewSelectedMailbox(mailbox, mailboxSession,
                    session, mailboxName);
        } else {
            sessionMailbox = currentMailbox;
        }
        final Mailbox.MetaData metaData = mailbox.getMetaData(!openReadOnly, mailboxSession);
        addRecent(metaData, sessionMailbox);
        return metaData;
    }

    private SelectedMailbox createNewSelectedMailbox(final Mailbox mailbox,
            final MailboxSession mailboxSession, ImapSession session, String name)
            throws MailboxException {
        final SelectedMailbox sessionMailbox;
        final Iterator<MessageResult> it = mailbox.getMessages(MessageRangeImpl.all(),
                FetchGroupImpl.MINIMAL, mailboxSession);
        final List<Long> uids = new ArrayList<Long>();
        while (it.hasNext()) {
            final MessageResult result = (MessageResult) it.next();
            uids.add(new Long(result.getUid()));
        }
        sessionMailbox = new SelectedMailboxImpl(getMailboxManager(), uids,
                mailboxSession, name);
        session.selected(sessionMailbox);
        return sessionMailbox;
    }

    private void addRecent(final Mailbox.MetaData metaData,
            SelectedMailbox sessionMailbox) throws MailboxException {
        final long[] recentUids = metaData.getRecent();
        if (recentUids != null) {
            for (int i = 0; i < recentUids.length; i++) {
                long uid = recentUids[i];
                sessionMailbox.addRecent(uid);
            }
        }
    }
}
