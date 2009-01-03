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

import java.util.List;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.message.request.imap4rev1.CapabilityRequest;
import org.apache.james.imap.message.response.imap4rev1.server.CapabilityResponse;
import org.apache.james.imap.processor.base.AbstractMailboxAwareProcessor;

public class CapabilityProcessor extends AbstractMailboxAwareProcessor {

    private final List<String> capabilities;

    public CapabilityProcessor(final ImapProcessor next, final MailboxManagerProvider mailboxManagerProvider,
            final StatusResponseFactory factory, final List<String> capabilities) {
        super(next, mailboxManagerProvider, factory);
        this.capabilities = capabilities;
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof CapabilityRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final CapabilityRequest request = (CapabilityRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag,
                command);
        responder.respond(result);
        unsolicitedResponses(session, responder, false);
        okComplete(command, tag, responder);
    }

    private ImapResponseMessage doProcess(CapabilityRequest request,
            ImapSession session, String tag, ImapCommand command) {
        final CapabilityResponse result = new CapabilityResponse(capabilities);
        return result;
    }
}
