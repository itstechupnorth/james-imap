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

import static org.apache.james.imap.api.ImapConstants.SUPPORTS_LITERAL_PLUS;
import static org.apache.james.imap.api.ImapConstants.SUPPORTS_RFC3348;
import static org.apache.james.imap.api.ImapConstants.VERSION;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.message.request.CapabilityRequest;
import org.apache.james.imap.message.response.CapabilityResponse;
import org.apache.james.mailbox.MailboxManager;

public class CapabilityProcessor extends AbstractMailboxProcessor implements CapabilityImplementingProcessor {

    private final List<CapabilityImplementingProcessor> capabilities = new ArrayList<CapabilityImplementingProcessor>();

    public CapabilityProcessor(final ImapProcessor next, final MailboxManager mailboxManager,
            final StatusResponseFactory factory, final List<CapabilityImplementingProcessor> capabilities) {
        this(next, mailboxManager, factory);
        this.capabilities.addAll(capabilities);

    }

    public CapabilityProcessor(final ImapProcessor next, final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
        this.capabilities.add(this);

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
        List<String> caps = new ArrayList<String>();
        for (int i = 0; i < capabilities.size(); i++) {
            caps.addAll(capabilities.get(i).getImplementedCapabilities(session));
        }
        final CapabilityResponse result = new CapabilityResponse(caps);
        return result;
    }
    
    /**
     * Add a {@link CapabilityImplementor} which will get queried for implemented capabilities
     * 
     * @param implementor
     */
    public void addProcessor(CapabilityImplementingProcessor implementor) {
        capabilities.add(implementor);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.processor.CapabilityImplementingProcessor#getImplementedCapabilities(org.apache.james.imap.api.process.ImapSession)
     */
    public List<String> getImplementedCapabilities(ImapSession session) {
        final List<String> capabilities = new ArrayList<String>();
        capabilities.add(VERSION);
        capabilities.add(SUPPORTS_LITERAL_PLUS);
        capabilities.add(SUPPORTS_RFC3348);
        return capabilities;
    }
    
    
}
