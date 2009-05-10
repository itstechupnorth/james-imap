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
import java.util.Collection;
import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.NamespaceRequest;
import org.apache.james.imap.message.response.NamespaceResponse;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MailboxSession.Namespace;
import org.apache.james.imap.processor.base.ImapSessionUtils;

/**
 * Processes a NAMESPACE command into a suitable set of responses.
 */
public class NamespaceProcessor extends AbstractMailboxProcessor {
    
    public NamespaceProcessor(ImapProcessor next,
            MailboxManagerProvider mailboxManagerProvider,
            StatusResponseFactory factory) {
        super(next, mailboxManagerProvider, factory);
    }

    @Override
    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
        final List<NamespaceResponse.Namespace> personalSpaces = buildPersonalNamespaces(mailboxSession);
        
        final MailboxSession.Namespace otherUsersSpace = mailboxSession.getOtherUsersSpace();
        final List<NamespaceResponse.Namespace> otherUsersSpaces = buildOtherUsersSpaces(otherUsersSpace);
        
        final Collection<Namespace> sharedSpaces = mailboxSession.getSharedSpaces();
        final List<NamespaceResponse.Namespace> sharedNamespaces;
        if (sharedSpaces.isEmpty()) {
            sharedNamespaces = null;
        } else {
            sharedNamespaces = new ArrayList<NamespaceResponse.Namespace>(sharedSpaces.size());
            for (MailboxSession.Namespace space: sharedSpaces) {
                sharedNamespaces.add(new NamespaceResponse.Namespace(space.getPrefix(), space.getDeliminator()));
            }    
        }
        
        final NamespaceResponse response = new NamespaceResponse(personalSpaces, otherUsersSpaces, sharedNamespaces);
        responder.respond(response);
        unsolicitedResponses(session, responder, false);
        okComplete(command, tag, responder);
    }

    private List<NamespaceResponse.Namespace> buildOtherUsersSpaces(final MailboxSession.Namespace otherUsersSpace) {
        final List<NamespaceResponse.Namespace> otherUsersSpaces;
        if (otherUsersSpace == null) {
            otherUsersSpaces = null;
        } else {
            otherUsersSpaces = new ArrayList<NamespaceResponse.Namespace>(1);
            otherUsersSpaces.add(new NamespaceResponse.Namespace(otherUsersSpace.getPrefix(), otherUsersSpace.getDeliminator()));
        }
        return otherUsersSpaces;
    }

    /**
     * Builds personal namespaces from the session.
     * @param mailboxSession not null
     * @return personal namespaces, not null
     */
    private List<NamespaceResponse.Namespace> buildPersonalNamespaces(final MailboxSession mailboxSession) {
        final MailboxSession.Namespace personalNamespace = mailboxSession.getPersonalSpace();
        final List<NamespaceResponse.Namespace> personalSpaces = new ArrayList<NamespaceResponse.Namespace>();
        personalSpaces.add(new NamespaceResponse.Namespace(personalNamespace.getPrefix(), personalNamespace.getDeliminator()));
        return personalSpaces;
    }

    @Override
    protected boolean isAcceptable(ImapMessage message) {
        return message instanceof NamespaceRequest;
    }

}
