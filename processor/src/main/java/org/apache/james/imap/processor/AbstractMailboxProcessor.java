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

import static org.apache.james.imap.api.ImapConstants.NAMESPACE_PREFIX;

import java.util.Collection;
import java.util.Iterator;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxExistsException;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.util.FetchGroupImpl;
import org.apache.james.imap.message.response.ExistsResponse;
import org.apache.james.imap.message.response.ExpungeResponse;
import org.apache.james.imap.message.response.FetchResponse;
import org.apache.james.imap.message.response.RecentResponse;
import org.apache.james.imap.processor.base.AbstractChainedProcessor;
import org.apache.james.imap.processor.base.ImapSessionUtils;

abstract public class AbstractMailboxProcessor extends AbstractChainedProcessor {

    private final MailboxManager mailboxManager;

    private final StatusResponseFactory factory;

    public AbstractMailboxProcessor(final ImapProcessor next,
            final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next);
        this.mailboxManager = mailboxManager;
        this.factory = factory;
    }

    protected final void doProcess(final ImapMessage acceptableMessage,
            final Responder responder, final ImapSession session) {
        final ImapRequest request = (ImapRequest) acceptableMessage;
        process(request, responder, session);
    }

    protected final void process(final ImapRequest message,
            final Responder responder, final ImapSession session) {
        final ImapCommand command = message.getCommand();
        final String tag = message.getTag();
        doProcess(message, command, tag, responder, session);
    }

    protected void no(final ImapCommand command, final String tag,
            final Responder responder, final MessagingException e, ImapSession session) {
        final Log logger = session.getLog();
        final ImapResponseMessage response;
        if (e instanceof MailboxExistsException) {
            response = factory.taggedNo(tag, command,
                    HumanReadableText.FAILURE_MAILBOX_EXISTS);
        } else if (e instanceof MailboxNotFoundException) {
            response = factory.taggedNo(tag, command,
                    HumanReadableText.FAILURE_NO_SUCH_MAILBOX);
        } else {
            if (logger != null) {
                logger.info(e.getMessage());
                logger.debug("Processing failed:", e);
            }
            final HumanReadableText key;
            if (e instanceof MailboxException) {
                final MailboxException mailboxException = (MailboxException) e;
                final HumanReadableText exceptionKey = mailboxException.getKey();
                if (exceptionKey == null) {
                    key = HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING;
                } else {
                    key = exceptionKey;
                }
            } else {
                key = HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING;
            }
            response = factory.taggedNo(tag, command, key);
        }
        responder.respond(response);
    }

    final void doProcess(final ImapRequest message, final ImapCommand command,
            final String tag, Responder responder, ImapSession session) {
        if (!command.validForState(session.getState())) {
            ImapResponseMessage response = factory.taggedNo(tag, command,
                    HumanReadableText.INVALID_COMMAND);
            responder.respond(response);

        } else {
            doProcess(message, session, tag, command, responder);
        }
    }

    protected void unsolicitedResponses(final ImapSession session,
            final ImapProcessor.Responder responder, boolean useUids) {
        unsolicitedResponses(session, responder, false, useUids);
    }

    /**
     * Sends any unsolicited responses to the client, such as EXISTS and FLAGS
     * responses when the selected mailbox is modified by another user.
     */
    protected void unsolicitedResponses(final ImapSession session, final ImapProcessor.Responder responder, 
            boolean omitExpunged, boolean useUid) {
        final SelectedMailbox selected = session.getSelected();
        if (selected == null) {
            session.getLog().debug("No mailbox selected");
        } else {
            unsolicitedResponses(session, responder, selected, omitExpunged, useUid);
        }
    }

    /**
     * @see org.apache.james.imap.api.process.SelectedMailbox#unsolicitedResponses(boolean,
     *      boolean)
     */
    public void unsolicitedResponses(final ImapSession session, final ImapProcessor.Responder responder, 
            final SelectedMailbox selected, boolean omitExpunged, boolean useUid) {
        final boolean sizeChanged = selected.isSizeChanged();
        // New message response
            if (sizeChanged) {
                addExistsResponses(session, selected, responder);
            }
            // Expunged messages
            if (!omitExpunged) {
                addExpungedResponses(selected, responder);
            }
            if (sizeChanged || (selected.isRecentUidRemoved() && !omitExpunged)) {
                addRecentResponses(selected, responder);
                selected.resetRecentUidRemoved();
            }
    
            // Message updates
            addFlagsResponses(session, selected, responder, useUid);
    
            selected.resetEvents();
    }

    private void addExpungedResponses(final SelectedMailbox selected, final ImapProcessor.Responder responder) {
        final Collection<Long> expungedUids = selected.expungedUids();
        for (final Long uid: expungedUids) {
            final long uidValue = uid.longValue();
            final int msn = selected.remove(uidValue);
            // TODO: use factory
            ExpungeResponse response = new ExpungeResponse(msn);
            responder.respond(response);
        }
    }

    private void addFlagsResponses(final ImapSession session, final SelectedMailbox selected, 
            final ImapProcessor.Responder responder, boolean useUid) {
        try {
            final Collection<Long> flagUpdateUids = selected.flagUpdateUids();
            if (!flagUpdateUids.isEmpty()) {
                final Mailbox mailbox = getMailbox(session, selected);
                final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
                for (final Long uid: flagUpdateUids) {
                    MessageRange messageSet = MessageRange.one(uid.longValue());
                    addFlagsResponses(session, selected, responder, useUid, messageSet, mailbox, mailboxSession);
                }
            }
        } catch (MessagingException e) {
            handleResponseException(responder, e, HumanReadableText.FAILURE_TO_LOAD_FLAGS, session);
        }
    }

    private void addFlagsResponses(final ImapSession session, final SelectedMailbox selected, 
            final ImapProcessor.Responder responder, boolean useUid, MessageRange messageSet, Mailbox mailbox, MailboxSession mailboxSession)
    throws MailboxException {
        final Iterator<MessageResult> it = mailbox.getMessages(messageSet, FetchGroupImpl.MINIMAL, mailboxSession);
        while (it.hasNext()) {
            MessageResult mr = it.next();
            final long uid = mr.getUid();
            int msn = selected.msn(uid);
            final Flags flags = mr.getFlags();
            final Long uidOut;
            if (useUid) {
                uidOut = new Long(uid);
            } else {
                uidOut = null;
            }
            if (selected.isRecent(uid)) {
                flags.add(Flags.Flag.RECENT);
            } else {
                flags.remove(Flags.Flag.RECENT);
            }
            final FetchResponse response = new FetchResponse(msn, flags, uidOut,
                    null, null, null, null, null, null);
            responder.respond(response);
        }
    }

    private Mailbox getMailbox(final ImapSession session, final SelectedMailbox selected) throws MailboxException {
        final String fullMailboxName = buildFullName(session, selected.getName());
        final MailboxManager mailboxManager = getMailboxManager();
        final Mailbox mailbox = mailboxManager.getMailbox(fullMailboxName, ImapSessionUtils.getMailboxSession(session));
        return mailbox;
    }

    private void addRecentResponses(final SelectedMailbox selected, final ImapProcessor.Responder responder) {
        final int recentCount = selected.recentCount();
//      TODO: use factory
        RecentResponse response = new RecentResponse(recentCount);
        responder.respond(response);
    }

    private void addExistsResponses(final ImapSession session, final SelectedMailbox selected, 
            final ImapProcessor.Responder responder) {
        try {
            final Mailbox mailbox = getMailbox(session, selected);
            final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
            final int messageCount = mailbox.getMessageCount(mailboxSession);
            // TODO: use factory
            final ExistsResponse response = new ExistsResponse(messageCount);
            responder.respond(response);
        } catch (MailboxException e) {
            handleResponseException(responder, e, HumanReadableText.FAILURE_EXISTS_COUNT, session);
        }
    }

    private void handleResponseException(final ImapProcessor.Responder responder,
            MessagingException e, final HumanReadableText message, ImapSession session) {
        session.getLog().info(message);
        session.getLog().debug(message, e);
        // TODO: consider whether error message should be passed to the user
        final StatusResponse response = factory.untaggedNo(message);;
        responder.respond(response);
    }

    protected void okComplete(final ImapCommand command, final String tag,
            final ImapProcessor.Responder responder) {
        final StatusResponse response = factory.taggedOk(tag, command,
                HumanReadableText.COMPLETED);
        responder.respond(response);
    }

    protected void no(final ImapCommand command, final String tag,
            final ImapProcessor.Responder responder,
            final HumanReadableText displayTextKey) {
        final StatusResponse response = factory.taggedNo(tag, command,
                displayTextKey);
        responder.respond(response);
    }

    protected void no(final ImapCommand command, final String tag,
            final ImapProcessor.Responder responder,
            final HumanReadableText displayTextKey,
            final StatusResponse.ResponseCode responseCode) {
        final StatusResponse response = factory.taggedNo(tag, command,
                displayTextKey, responseCode);
        responder.respond(response);
    }

    protected void bye(final ImapProcessor.Responder responder) {
        final StatusResponse response = factory.bye(HumanReadableText.BYE);
        responder.respond(response);
    }

    protected void bye(final ImapProcessor.Responder responder,
            final HumanReadableText key) {
        final StatusResponse response = factory.bye(key);
        responder.respond(response);
    }

    protected abstract void doProcess(final ImapRequest message,
            ImapSession session, String tag, ImapCommand command,
            Responder responder);

    public String buildFullName(final ImapSession session, String mailboxName)
            throws MailboxException {
        final String user = ImapSessionUtils.getUserName(session);
        if (!mailboxName.startsWith(NAMESPACE_PREFIX)) {
            mailboxName = getMailboxManager().resolve(user, mailboxName);
        }
        return mailboxName;
    }
    
    public MailboxManager getMailboxManager() throws MailboxException {
        return mailboxManager;
    }

    public Mailbox getSelectedMailbox(final ImapSession session) throws MailboxException {
        Mailbox result;
        final SelectedMailbox selectedMailbox = session.getSelected();
        if (selectedMailbox == null) {
            result = null;
        } else {
            final String mailboxName = selectedMailbox.getName();
            final MailboxManager mailboxManager = getMailboxManager();
            result = mailboxManager.getMailbox(mailboxName, ImapSessionUtils.getMailboxSession(session));
        }
        return result;
    }

}
