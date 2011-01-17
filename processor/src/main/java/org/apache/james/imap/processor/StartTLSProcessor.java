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
import java.util.Arrays;
import java.util.List;

import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.message.request.StartTLSRequest;
import org.apache.james.imap.processor.base.AbstractChainedProcessor;

/**
 *
 * Processing STARTLS commands
 *
 */
public class StartTLSProcessor extends AbstractChainedProcessor implements CapabilityImplementingProcessor{

    private StatusResponseFactory factory;

    public StartTLSProcessor(final ImapProcessor next, final StatusResponseFactory factory) {
        super(next);
        this.factory = factory;
    }

    @Override
    protected void doProcess(ImapMessage acceptableMessage,
            Responder responder, ImapSession session) {
        ImapRequest request = (ImapRequest) acceptableMessage;     
        if (session.supportStartTLS()) {
            session.startTLS();
            responder.respond(factory.taggedOk(request.getTag(), request.getCommand(), HumanReadableText.STARTTLS));
        } else {
            responder.respond(factory.taggedBad(request.getTag(), request.getCommand(), HumanReadableText.INVALID_COMMAND));
        }

    }

    @Override
    protected boolean isAcceptable(ImapMessage message) {
        return message instanceof StartTLSRequest;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.processor.CapabilityImplementingProcessor#getImplementedCapabilities(org.apache.james.imap.api.process.ImapSession)
     */
    public List<String> getImplementedCapabilities(ImapSession session) {
        if (session.supportStartTLS()) {
            return Arrays.asList(ImapConstants.SUPPORTS_STARTTLS);
        } else {
            return new ArrayList<String>();
        }
    }

}
