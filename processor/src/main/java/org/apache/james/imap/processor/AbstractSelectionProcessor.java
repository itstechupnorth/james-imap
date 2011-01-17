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
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.request.AbstractMailboxSelectionRequest;
import org.apache.james.imap.message.response.ExistsResponse;
import org.apache.james.imap.message.response.FlagsResponse;
import org.apache.james.imap.message.response.RecentResponse;
import org.apache.james.imap.processor.base.SelectedMailboxImpl;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.MessageManager.MetaData;

abstract class AbstractSelectionProcessor extends AbstractMailboxProcessor {

    private final FlagsResponse standardFlags;

    final StatusResponseFactory statusResponseFactory;

    private final boolean openReadOnly;

    public AbstractSelectionProcessor(final ImapProcessor next,
            final MailboxManager mailboxManager,
            final StatusResponseFactory statusResponseFactory,
            final boolean openReadOnly) {
        super(next, mailboxManager, statusResponseFactory);
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
            final MailboxPath fullMailboxPath = buildFullPath(session, mailboxName);
            final MessageManager.MetaData metaData = selectMailbox(fullMailboxPath, session);
            respond(tag, command, session, metaData, responder);
        } catch (MailboxNotFoundException e) {
            responder.respond(statusResponseFactory.taggedNo(tag, command,
                    HumanReadableText.FAILURE_NO_SUCH_MAILBOX));
        } catch (MailboxException e) {
            no(command, tag, responder, HumanReadableText.SELECT);
        }
    }

    private void respond(String tag, ImapCommand command, ImapSession session,
            final MessageManager.MetaData metaData, Responder responder) throws MailboxException {

        final SelectedMailbox selected = session.getSelected();
        
        flags(responder);
        exists(responder, metaData);
        recent(responder, selected);
        uidValidity(responder, metaData);
        unseen(responder, metaData, selected);
        permanentFlags(responder, metaData);
        uidNext(responder, metaData);
        taggedOk(responder, tag, command, metaData);
    }

    private void uidNext(final Responder responder, final MessageManager.MetaData metaData)
    throws MailboxException {
        final long uid = metaData.getUidNext();
        final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                HumanReadableText.UNSEEN, ResponseCode.uidNext(uid));
        responder.respond(untaggedOk);
    }
    
    private void taggedOk(final Responder responder, final String tag,
            final ImapCommand command, final MetaData metaData) {
        final boolean writeable = metaData.isWriteable() && !openReadOnly;
        final ResponseCode code;
        if (writeable) {
            code = ResponseCode.readWrite();
        } else {
            code = ResponseCode.readOnly();
        }
        final StatusResponse taggedOk = statusResponseFactory.taggedOk(tag,
                command, HumanReadableText.SELECT, code);
        responder.respond(taggedOk);
    }

    private void flags(Responder responder) {
        responder.respond(standardFlags);
    }

    private void permanentFlags(Responder responder, MessageManager.MetaData metaData) {
        final Flags permanentFlags = metaData.getPermanentFlags();
        final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                HumanReadableText.PERMANENT_FLAGS, ResponseCode
                        .permanentFlags(permanentFlags));
        responder.respond(untaggedOk);
    }

    private void unseen(Responder responder, MessageManager.MetaData metaData,
            final SelectedMailbox selected) throws MailboxException {
        final Long firstUnseen = metaData.getFirstUnseen();
        if (firstUnseen != null) {
            final long unseenUid = firstUnseen;
            int msn = selected.msn(unseenUid);

            if (msn == SelectedMailbox.NO_SUCH_MESSAGE) throw new MailboxException("No message found with uid " + unseenUid);

            final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                    HumanReadableText.UNSEEN, ResponseCode.unseen(msn));
            responder.respond(untaggedOk);
        }

    }

    private void uidValidity(Responder responder, MessageManager.MetaData metaData) throws MailboxException {
        final long uidValidity = metaData.getUidValidity();
        final StatusResponse untaggedOk = statusResponseFactory.untaggedOk(
                HumanReadableText.UID_VALIDITY, ResponseCode
                        .uidValidity(uidValidity));
        responder.respond(untaggedOk);
    }

    private void recent(Responder responder, final SelectedMailbox selected) {
        final int recentCount = selected.recentCount();
        final RecentResponse recentResponse = new RecentResponse(recentCount);
        responder.respond(recentResponse);
    }

    private void exists(Responder responder, MessageManager.MetaData metaData) throws MailboxException {
        final long messageCount = metaData.getMessageCount();
        final ExistsResponse existsResponse = new ExistsResponse(messageCount);
        responder.respond(existsResponse);
    }

    private MessageManager.MetaData  selectMailbox(MailboxPath mailboxPath, ImapSession session)
            throws MailboxException {
        final MailboxManager mailboxManager = getMailboxManager();
        final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
        final MessageManager mailbox = mailboxManager.getMailbox(mailboxPath, mailboxSession);

        final SelectedMailbox sessionMailbox;
        final SelectedMailbox currentMailbox = session.getSelected();
        if (currentMailbox == null
                || !currentMailbox.getPath().equals(mailboxPath)) {
            sessionMailbox = createNewSelectedMailbox(mailbox, mailboxSession, session, mailboxPath);
        } else {
            sessionMailbox = currentMailbox;
        }
        final MessageManager.MetaData metaData = mailbox.getMetaData(!openReadOnly, mailboxSession, MessageManager.MetaData.FetchGroup.FIRST_UNSEEN);
        addRecent(metaData, sessionMailbox);
        return metaData;
    }

    private SelectedMailbox createNewSelectedMailbox(final MessageManager mailbox,
            final MailboxSession mailboxSession, ImapSession session, MailboxPath path)
            throws MailboxException {
        SearchQuery query = new SearchQuery();
        query.andCriteria(SearchQuery.all());
        
        // use search here to allow implementation a better way to improve selects on mailboxes. 
        // See https://issues.apache.org/jira/browse/IMAP-192
        final Iterator<Long> it = mailbox.search(query, mailboxSession);

        final SelectedMailbox sessionMailbox = new SelectedMailboxImpl(getMailboxManager(), it, mailboxSession, path);
        session.selected(sessionMailbox);
        return sessionMailbox;
    }

    private void addRecent(final MessageManager.MetaData metaData,
            SelectedMailbox sessionMailbox) throws MailboxException {
        final List<Long> recentUids = metaData.getRecent();
        for (int i = 0; i < recentUids.size(); i++) {
            long uid = recentUids.get(i);
            sessionMailbox.addRecent(uid);
        }
    }
}
