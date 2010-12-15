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

import java.io.InputStream;
import java.util.Date;

import javax.mail.Flags;

import org.apache.james.imap.api.ContinuationReader;
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

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createAppendMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, javax.mail.Flags, java.util.Date, java.io.InputStream, java.lang.String)
     */
    public ImapMessage createAppendMessage(ImapCommand command,
            String mailboxName, Flags flags, Date datetime,
            InputStream message, String tag) {
        return new AppendRequest(command, mailboxName, flags, datetime,
                message, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createAuthenticateMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String)
     */
    public ImapMessage createAuthenticateMessage(ImapCommand command,
            String authType, String tag) {
        return new AuthenticateRequest(command, authType, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createCapabilityMessage(org.apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapMessage createCapabilityMessage(ImapCommand command, String tag) {
        return new CapabilityRequest(command, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createNoopMessage(org.apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapMessage createNoopMessage(ImapCommand command, String tag) {
        return new NoopRequest(command, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createNoopMessage(org.apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapMessage createIdleMessage(ImapCommand command, ContinuationReader reader, String tag) {
        return new IdleRequest(command, reader, tag);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createCloseMessage(org.apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapMessage createCloseMessage(ImapCommand command, String tag) {
        return new CloseRequest(command, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createCopyMessage(org.apache.james.imap.api.ImapCommand, org.apache.james.imap.api.message.IdRange[], java.lang.String, boolean, java.lang.String)
     */
    public ImapMessage createCopyMessage(ImapCommand command, IdRange[] idSet,
            String mailboxName, boolean useUids, String tag) {
        return new CopyRequest(command, idSet, mailboxName, useUids, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createCreateMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String)
     */
    public ImapMessage createCreateMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new CreateRequest(command, mailboxName, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createDeleteMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String)
     */
    public ImapMessage createDeleteMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new DeleteRequest(command, mailboxName, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createExamineMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String)
     */
    public ImapMessage createExamineMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new ExamineRequest(command, mailboxName, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createExpungeMessage(org.apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapMessage createExpungeMessage(ImapCommand command, String tag) {
        return new ExpungeRequest(command, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createFetchMessage(org.apache.james.imap.api.ImapCommand, boolean, org.apache.james.imap.api.message.IdRange[], org.apache.james.imap.api.message.FetchData, java.lang.String)
     */
    public ImapMessage createFetchMessage(ImapCommand command, boolean useUids,
            IdRange[] idSet, FetchData fetch, String tag) {
        return new FetchRequest(command, useUids, idSet, fetch, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createListMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String, java.lang.String)
     */
    public ImapMessage createListMessage(ImapCommand command,
            String referenceName, String mailboxPattern, String tag) {
        return new ListRequest(command, referenceName, mailboxPattern, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createLoginMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String, java.lang.String)
     */
    public ImapMessage createLoginMessage(ImapCommand command, String userid,
            String password, String tag) {
        return new LoginRequest(command, userid, password, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createLogoutMessage(org.apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapMessage createLogoutMessage(ImapCommand command, String tag) {
        return new LogoutRequest(command, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createLsubMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String, java.lang.String)
     */
    public ImapMessage createLsubMessage(ImapCommand command,
            String referenceName, String mailboxPattern, String tag) {
        return new LsubRequest(command, referenceName, mailboxPattern, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createRenameMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String, java.lang.String)
     */
    public ImapMessage createRenameMessage(ImapCommand command,
            String existingName, String newName, String tag) {
        return new RenameRequest(command, existingName, newName, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createSearchMessage(org.apache.james.imap.api.ImapCommand, org.apache.james.imap.api.message.request.SearchKey, boolean, java.lang.String)
     */
    public ImapMessage createSearchMessage(ImapCommand command, SearchKey key,
            boolean useUids, String tag) {
        return new SearchRequest(command, key, useUids, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createSelectMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String)
     */
    public ImapMessage createSelectMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new SelectRequest(command, mailboxName, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createStatusMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, org.apache.james.imap.api.message.StatusDataItems, java.lang.String)
     */
    public ImapMessage createStatusMessage(ImapCommand command,
            String mailboxName, StatusDataItems statusDataItems, String tag) {
        return new StatusRequest(command, mailboxName, statusDataItems, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createStoreMessage(org.apache.james.imap.api.ImapCommand, org.apache.james.imap.api.message.IdRange[], boolean, java.lang.Boolean, javax.mail.Flags, boolean, java.lang.String)
     */
    public ImapMessage createStoreMessage(ImapCommand command, IdRange[] idSet,
            boolean silent, Boolean sign, Flags flags, boolean useUids,
            String tag) {
        return new StoreRequest(command, idSet, silent, flags, useUids, tag,
                sign);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createSubscribeMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String)
     */
    public ImapMessage createSubscribeMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new SubscribeRequest(command, mailboxName, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createUnsubscribeMessage(org.apache.james.imap.api.ImapCommand, java.lang.String, java.lang.String)
     */
    public ImapMessage createUnsubscribeMessage(ImapCommand command,
            String mailboxName, String tag) {
        return new UnsubscribeRequest(command, mailboxName, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createCheckMessage(org.apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapMessage createCheckMessage(ImapCommand command, String tag) {
        return new CheckRequest(command, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#taggedBad(java.lang.String, org.apache.james.imap.api.ImapCommand, org.apache.james.imap.api.display.HumanReadableText)
     */
    public StatusResponse taggedBad(String tag, ImapCommand command,
            HumanReadableText displayTextKey) {
        return statusResponseFactory.taggedBad(tag, command, displayTextKey);
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#bye(org.apache.james.imap.api.display.HumanReadableText)
     */
    public StatusResponse bye(HumanReadableText displayTextKey) {
        return statusResponseFactory.bye(displayTextKey);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createNamespaceMessage(org.apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapMessage createNamespaceMessage(ImapCommand command, String tag) {
        return new NamespaceRequest(command, tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageFactory#createStartTLSMessage(org.apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapMessage createStartTLSMessage(ImapCommand command, String tag) {
        return new StartTLSRequest(tag, command);
    }
}
