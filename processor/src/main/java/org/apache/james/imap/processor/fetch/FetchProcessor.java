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

package org.apache.james.imap.processor.fetch;

import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.BodyFetchElement;
import org.apache.james.imap.api.message.FetchData;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.UnsupportedCriteriaException;
import org.apache.james.imap.mailbox.MessageResult.FetchGroup;
import org.apache.james.imap.mailbox.MessageResult.MimePath;
import org.apache.james.imap.mailbox.util.FetchGroupImpl;
import org.apache.james.imap.message.request.FetchRequest;
import org.apache.james.imap.message.response.FetchResponse;
import org.apache.james.imap.processor.AbstractMailboxProcessor;
import org.apache.james.imap.processor.base.ImapSessionUtils;
import org.apache.james.mime4j.field.address.parser.ParseException;

public class FetchProcessor extends AbstractMailboxProcessor {

    public FetchProcessor(final ImapProcessor next, final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof FetchRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final FetchRequest request = (FetchRequest) message;
        final boolean useUids = request.isUseUids();
        final IdRange[] idSet = request.getIdSet();
        final FetchData fetch = request.getFetch();
        try {
            FetchGroup resultToFetch = getFetchGroup(fetch);
            final Mailbox mailbox = getSelectedMailbox(session);
            for (int i = 0; i < idSet.length; i++) {
                final FetchResponseBuilder builder = new FetchResponseBuilder(
                        new EnvelopeBuilder(session.getLog()));
                final long highVal;
                final long lowVal;
                if (useUids) {
                    highVal = idSet[i].getHighVal();
                    lowVal = idSet[i].getLowVal();
                } else {
                    highVal = session.getSelected().uid(
                            (int) idSet[i].getHighVal());
                    lowVal = session.getSelected().uid(
                            (int) idSet[i].getLowVal());
                }
                MessageRange messageSet = MessageRange.range(lowVal, highVal);
                final MailboxSession mailboxSession = ImapSessionUtils
                        .getMailboxSession(session);
                final Iterator<MessageResult> it = mailbox.getMessages(messageSet, resultToFetch, mailboxSession);
                while (it.hasNext()) {
                    final MessageResult result = (MessageResult) it.next();
                    final FetchResponse response = builder.build(fetch, result, mailbox, 
                            session, useUids);
                    responder.respond(response);
                }
            }
            unsolicitedResponses(session, responder, useUids);
            okComplete(command, tag, responder);
        } catch (UnsupportedCriteriaException e) {
            no(command, tag, responder,
                    HumanReadableText.UNSUPPORTED_SEARCH_CRITERIA);
        } catch (MessagingException e) {
            no(command, tag, responder, e, session);
        } catch (ParseException e) {
            no(command, tag, responder, HumanReadableText.FAILURE_MAIL_PARSE);
        }
    }

    private FetchGroup getFetchGroup(FetchData fetch) {
        FetchGroupImpl result = new FetchGroupImpl();

        if (fetch.isEnvelope()) {
            result.or(FetchGroup.HEADERS);
        }
        if (fetch.isBody() || fetch.isBodyStructure()) {
            result.or(FetchGroup.MIME_DESCRIPTOR);
        }

        Collection<BodyFetchElement> bodyElements = fetch.getBodyElements();
        if (bodyElements != null) {
            for (final Iterator<BodyFetchElement> it = bodyElements.iterator(); it.hasNext();) {
                final BodyFetchElement element = it.next();
                final int sectionType = element.getSectionType();
                final int[] path = element.getPath();
                final boolean isBase = (path == null || path.length == 0);
                switch (sectionType) {
                    case BodyFetchElement.CONTENT:
                        if (isBase) {
                            addContent(result, path, isBase,
                                    MessageResult.FetchGroup.FULL_CONTENT);
                        } else {
                            addContent(result, path, isBase,
                                    MessageResult.FetchGroup.MIME_CONTENT);
                        }
                        break;
                    case BodyFetchElement.HEADER:
                    case BodyFetchElement.HEADER_NOT_FIELDS:
                    case BodyFetchElement.HEADER_FIELDS:
                        addContent(result, path, isBase,
                                MessageResult.FetchGroup.HEADERS);
                        break;
                    case BodyFetchElement.MIME:
                        addContent(result, path, isBase,
                                MessageResult.FetchGroup.MIME_HEADERS);
                        break;
                    case BodyFetchElement.TEXT:
                        addContent(result, path, isBase,
                                MessageResult.FetchGroup.BODY_CONTENT);
                        break;
                }

            }
        }
        return result;
    }

    private void addContent(FetchGroupImpl result, final int[] path,
            final boolean isBase, final int content) {
        if (isBase) {
            result.or(content);
        } else {
            MimePath mimePath = new MimePathImpl(path);
            result.addPartContent(mimePath, content);
        }
    }
}
