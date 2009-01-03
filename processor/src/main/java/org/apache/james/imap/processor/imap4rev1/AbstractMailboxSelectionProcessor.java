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
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse.ResponseCode;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.api.imap.process.SelectedImapMailbox;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.util.FetchGroupImpl;
import org.apache.james.imap.mailbox.util.MessageRangeImpl;
import org.apache.james.imap.message.request.imap4rev1.AbstractMailboxSelectionRequest;
import org.apache.james.imap.message.response.imap4rev1.ExistsResponse;
import org.apache.james.imap.message.response.imap4rev1.FlagsResponse;
import org.apache.james.imap.message.response.imap4rev1.RecentResponse;
import org.apache.james.imap.processor.base.AbstractMailboxAwareProcessor;
import org.apache.james.imap.processor.base.ImapSessionUtils;
import org.apache.james.imap.processor.base.SelectedMailboxSessionImpl;

abstract public class AbstractMailboxSelectionProcessor extends
        AbstractMailboxAwareProcessor {

    private final FlagsResponse standardFlags;

    final StatusResponseFactory statusResponseFactory;

    private final boolean openReadOnly;

    public AbstractMailboxSelectionProcessor(final ImapProcessor next,
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
            selectMailbox(fullMailboxName, session);
            respond(tag, command, session, responder);
        } catch (MailboxNotFoundException e) {
            responder.respond(statusResponseFactory.taggedNo(tag, command,
                    HumanReadableTextKey.FAILURE_NO_SUCH_MAILBOX));
        } catch (MailboxException e) {
            no(command, tag, responder, e);
        }
    }

    private void respond(String tag, ImapCommand command, ImapSession session,
            Responder responder) throws MailboxException {

        Mailbox mailbox = getSelectedMailbox(session);
        final MailboxSession mailboxSession = ImapSessionUtils
                .getMailboxSession(session);
        final SelectedImapMailbox selected = session.getSelected();

        // TODO: compact this into a single API call for meta-data about the
        // repository
        flags(responder);
        exists(responder, mailbox, mailboxSession);
        recent(responder, selected);
        uidValidity(responder, mailbox, mailboxSession);
        unseen(responder, mailbox, mailboxSession, selected);
        permanentFlags(responder, mailbox);
        taggedOk(responder, tag, command, mailbox);
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

    private void permanentFlags(Responder responder, Mailbox mailbox) {
        final Flags permanentFlags = mailbox.getPermanentFlags();
        final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                HumanReadableTextKey.PERMANENT_FLAGS, ResponseCode
                        .permanentFlags(permanentFlags));
        responder.respond(untaggedOk);
    }

    private void unseen(Responder responder, Mailbox mailbox,
            final MailboxSession mailboxSession,
            final SelectedImapMailbox selected) throws MailboxException {
        final MessageResult firstUnseen = mailbox.getFirstUnseen(
                FetchGroupImpl.MINIMAL, mailboxSession);
        if (firstUnseen != null) {
            final long unseenUid = firstUnseen.getUid();
            int msn = selected.msn(unseenUid);
            final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                    HumanReadableTextKey.UNSEEN, ResponseCode.unseen(msn));
            responder.respond(untaggedOk);
        }

    }

    private void uidValidity(Responder responder, Mailbox mailbox,
            final MailboxSession mailboxSession) throws MailboxException {
        final long uidValidity = mailbox.getUidValidity(mailboxSession);
        final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                HumanReadableTextKey.UID_VALIDITY, ResponseCode
                        .uidValidity(uidValidity));
        responder.respond(untaggedOk);
    }

    private void recent(Responder responder, final SelectedImapMailbox selected) {
        final int recentCount = selected.recentCount();
        final RecentResponse recentResponse = new RecentResponse(recentCount);
        responder.respond(recentResponse);
    }

    private void exists(Responder responder, Mailbox mailbox,
            final MailboxSession mailboxSession) throws MailboxException {
        final int messageCount = mailbox.getMessageCount(mailboxSession);
        final ExistsResponse existsResponse = new ExistsResponse(messageCount);
        responder.respond(existsResponse);
    }

    private void selectMailbox(String mailboxName, ImapSession session)
            throws MailboxException {
        final MailboxManager mailboxManager = getMailboxManager();
        final Mailbox mailbox = mailboxManager.getMailbox(mailboxName);
        final MailboxSession mailboxSession = ImapSessionUtils
                .getMailboxSession(session);

        final SelectedImapMailbox sessionMailbox;
        final SelectedImapMailbox currentMailbox = session.getSelected();
        if (currentMailbox == null
                || !currentMailbox.getName().equals(mailboxName)) {
            sessionMailbox = createNewSelectedMailbox(mailbox, mailboxSession,
                    session, mailboxName);
        } else {
            sessionMailbox = currentMailbox;
        }
        addRecent(mailbox, mailboxSession, sessionMailbox);
    }

    private SelectedImapMailbox createNewSelectedMailbox(final Mailbox mailbox,
            final MailboxSession mailboxSession, ImapSession session, String name)
            throws MailboxException {
        final SelectedImapMailbox sessionMailbox;
        final Iterator it = mailbox.getMessages(MessageRangeImpl.all(),
                FetchGroupImpl.MINIMAL, mailboxSession);
        final List uids = new ArrayList();
        while (it.hasNext()) {
            final MessageResult result = (MessageResult) it.next();
            uids.add(new Long(result.getUid()));
        }
        sessionMailbox = new SelectedMailboxSessionImpl(mailbox, uids,
                mailboxSession, name);
        session.selected(sessionMailbox);
        session.setAttribute(
                ImapSessionUtils.SELECTED_MAILBOX_ATTRIBUTE_SESSION_KEY,
                mailbox);
        return sessionMailbox;
    }

    private void addRecent(final Mailbox mailbox,
            final MailboxSession mailboxSession,
            SelectedImapMailbox sessionMailbox) throws MailboxException {
        final long[] recentUids = mailbox.recent(!openReadOnly, mailboxSession);
        for (int i = 0; i < recentUids.length; i++) {
            long uid = recentUids[i];
            sessionMailbox.addRecent(uid);
        }
    }
}
