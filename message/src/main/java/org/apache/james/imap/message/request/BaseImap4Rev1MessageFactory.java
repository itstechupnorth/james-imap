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
package org.apache.james.imap.message.request;

import java.util.Date;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapMessageFactory;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.FetchData;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.StatusDataItems;
import org.apache.james.imap.api.message.request.SearchKey;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.decode.base.EolInputStream;

/**
 * Naive, factory creates unpooled instances.
 */
public class BaseImap4Rev1MessageFactory implements ImapMessageFactory {

    private StatusResponseFactory statusResponseFactory;

    public BaseImap4Rev1MessageFactory(
            StatusResponseFactory statusResponseFactory) {
        super();
        this.statusResponseFactory = statusResponseFactory;
    }

    public ImapMessage createAppendMessage(ImapCommand command,
            String mailboxName, Flags flags, Date datetime,
            EolInputStream message, String tag) {
        return new AppendRequest(command, mailboxName, flags, datetime,
                message, tag);
    }

    public ImapMessage createAuthenticateMessage(ImapCommand command,
            String authType, String tag) {
        return new AuthenticateRequest(command, authType, tag);
    }

    public ImapMessage createCapabilityMessage(ImapCommand command, String tag) {
        return new CapabilityRequest(command, tag);
    }

    public ImapMessage createNoopMessage(ImapCommand command, String tag) {
        return new NoopRequest(command, tag);
    }

    public ImapMessage createCloseMessage(ImapCommand command, String tag) {
        return new CloseRequest(command, tag);
    }

    public ImapMessage createCopyMessage(ImapCommand command, IdRange[] idSet,
            String mailboxName, boolean useUids, String tag) {
        return new CopyRequest(command, idSet, mailboxName, useUids, tag);
    }

    public ImapMessage createCreateMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new CreateRequest(command, mailboxName, tag);
    }

    public ImapMessage createDeleteMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new DeleteRequest(command, mailboxName, tag);
    }

    public ImapMessage createExamineMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new ExamineRequest(command, mailboxName, tag);
    }

    public ImapMessage createExpungeMessage(ImapCommand command, String tag) {
        return new ExpungeRequest(command, tag);
    }

    public ImapMessage createFetchMessage(ImapCommand command, boolean useUids,
            IdRange[] idSet, FetchData fetch, String tag) {
        return new FetchRequest(command, useUids, idSet, fetch, tag);
    }

    public ImapMessage createListMessage(ImapCommand command,
            String referenceName, String mailboxPattern, String tag) {
        return new ListRequest(command, referenceName, mailboxPattern, tag);
    }

    public ImapMessage createLoginMessage(ImapCommand command, String userid,
            String password, String tag) {
        return new LoginRequest(command, userid, password, tag);
    }

    public ImapMessage createLogoutMessage(ImapCommand command, String tag) {
        return new LogoutRequest(command, tag);
    }

    public ImapMessage createLsubMessage(ImapCommand command,
            String referenceName, String mailboxPattern, String tag) {
        return new LsubRequest(command, referenceName, mailboxPattern, tag);
    }

    public ImapMessage createRenameMessage(ImapCommand command,
            String existingName, String newName, String tag) {
        return new RenameRequest(command, existingName, newName, tag);
    }

    public ImapMessage createSearchMessage(ImapCommand command, SearchKey key,
            boolean useUids, String tag) {
        return new SearchRequest(command, key, useUids, tag);
    }

    public ImapMessage createSelectMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new SelectRequest(command, mailboxName, tag);
    }

    public ImapMessage createStatusMessage(ImapCommand command,
            String mailboxName, StatusDataItems statusDataItems, String tag) {
        return new StatusRequest(command, mailboxName, statusDataItems, tag);
    }

    public ImapMessage createStoreMessage(ImapCommand command, IdRange[] idSet,
            boolean silent, Boolean sign, Flags flags, boolean useUids,
            String tag) {
        return new StoreRequest(command, idSet, silent, flags, useUids, tag,
                sign);
    }

    public ImapMessage createSubscribeMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new SubscribeRequest(command, mailboxName, tag);
    }

    public ImapMessage createUnsubscribeMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new UnsubscribeRequest(command, mailboxName, tag);
    }

    public ImapMessage createCheckMessage(ImapCommand command, String tag) {
        return new CheckRequest(command, tag);
    }

    public StatusResponse taggedBad(String tag, ImapCommand command,
            HumanReadableText displayTextKey) {
        return statusResponseFactory.taggedBad(tag, command, displayTextKey);
    }

    public StatusResponse bye(HumanReadableText displayTextKey) {
        return statusResponseFactory.bye(displayTextKey);
    }

	public ImapMessage createNamespaceMessage(ImapCommand command, String tag) {
		return new NamespaceRequest(command, tag);
	}
}
