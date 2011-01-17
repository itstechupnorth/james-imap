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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.request.AppendRequest;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;

public class AppendProcessor extends AbstractMailboxProcessor {

    final StatusResponseFactory statusResponseFactory;

    public AppendProcessor(final ImapProcessor next,
            final MailboxManager mailboxManager,
            final StatusResponseFactory statusResponseFactory) {
        super(next, mailboxManager, statusResponseFactory);
        this.statusResponseFactory = statusResponseFactory;
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof AppendRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final AppendRequest request = (AppendRequest) message;
        final String mailboxName = request.getMailboxName();
        final InputStream messageIn = request.getMessage();
        final Date datetime = request.getDatetime();
        final Flags flags = request.getFlags();
        try {

            final MailboxPath mailboxPath = buildFullPath(session, mailboxName);
            final MailboxManager mailboxManager = getMailboxManager();
            final MessageManager mailbox = mailboxManager.getMailbox(mailboxPath, ImapSessionUtils.getMailboxSession(session));
            appendToMailbox(messageIn, datetime, flags, session, tag,
                    command, mailbox, responder, mailboxPath);
        } catch (MailboxNotFoundException e) {
            // consume message on exception
            cosume(messageIn);
            
//          Indicates that the mailbox does not exist
//          So TRY CREATE
            tryCreate(session, tag, command, responder, e);
            
        } catch (MailboxException e) {
            // consume message on exception
            cosume(messageIn);
            
//          Some other issue
            no(command, tag, responder, HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING);
            
        }

    }
    
    private void cosume(InputStream in) {
        try {
            while(in.read() != -1);
        } catch (IOException e1) {
            // just consume
        } 
    }

    /**
     * Issues a TRY CREATE response.
     * @param session not null
     * @param tag not null
     * @param command not null
     * @param responder not null
     * @param e not null
     */
    private void tryCreate(ImapSession session, String tag, ImapCommand command, 
            Responder responder, MailboxNotFoundException e) {
        
        final Log logger = session.getLog();
        if (logger.isInfoEnabled()) {
            logger.info(e.getMessage());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Cannot open mailbox: ", e);
        }
        
        no(command, tag, responder,
                HumanReadableText.FAILURE_NO_SUCH_MAILBOX,
                StatusResponse.ResponseCode.tryCreate());
    }

    private void appendToMailbox(final InputStream message, final Date datetime,
            final Flags flagsToBeSet, final ImapSession session, final String tag,
            final ImapCommand command, final MessageManager mailbox, Responder responder, final MailboxPath mailboxPath) {
        try {
            final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
            final SelectedMailbox selectedMailbox = session.getSelected();
            final boolean isSelectedMailbox = selectedMailbox != null
                    && selectedMailbox.getPath().equals(mailboxPath);
            final long uid  = mailbox.appendMessage(message, datetime, mailboxSession, 
                    !isSelectedMailbox, flagsToBeSet);
            if (isSelectedMailbox) {
                selectedMailbox.addRecent(uid);
            }
            unsolicitedResponses(session, responder, false);
            okComplete(command, tag, responder);
        } catch (MailboxNotFoundException e) {
//          Indicates that the mailbox does not exist
//          So TRY CREATE
            tryCreate(session, tag, command, responder, e);
        /*} catch (StorageException e) {
            taggedBad(command, tag, responder, e.getKey());
        */   
        } catch (MailboxException e) {
//          Some other issue
            no(command, tag, responder, HumanReadableText.SAVE_FAILED);
        }
    }
    
    
}
