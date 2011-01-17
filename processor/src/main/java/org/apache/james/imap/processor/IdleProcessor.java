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

import static org.apache.james.imap.api.ImapConstants.SUPPORTS_IDLE;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapLineHandler;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapMessageCallback;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.request.IdleRequest;
import org.apache.james.imap.message.response.ContinuationResponse;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;

public class IdleProcessor extends AbstractMailboxProcessor implements CapabilityImplementingProcessor {

    private final StatusResponseFactory factory;
    
    public IdleProcessor(final ImapProcessor next, final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
        
        this.factory = factory;
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof IdleRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session, final String tag, final ImapCommand command, Responder responder) {

        try {
            responder.respond(new ContinuationResponse(HumanReadableText.IDLING));
            unsolicitedResponses(session, responder, false);
            final AtomicBoolean closed = new AtomicBoolean(false);

            session.pushImapLineHandler(new ImapLineHandler() {
                
                public void onLine(ImapSession session, byte[] data, ImapMessageCallback callback) {
                    try {
                        String line = new String(data);
                        session.popImapLineHandler();

                        if (!"DONE".equals(line.toUpperCase())) {
                            
                            StatusResponse response = factory.taggedBad(tag, command, HumanReadableText.INVALID_COMMAND);
                            session.setAttribute(ImapConstants.NEXT_DECODER, null);
                            callback.onMessage(response);
                        } else {
                            final StatusResponse response = factory.taggedOk(tag, command, HumanReadableText.COMPLETED);
                            callback.onMessage(response);
                        }
                    } finally {
                        synchronized (session) {
                            closed.set(true);

                        }
                    }

                                        
                }
            });
            MailboxManager mailboxManager = getMailboxManager();
            MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
            
            SelectedMailbox sm = session.getSelected();
            if (sm != null) {
                mailboxManager.addListener(sm.getPath(), new IdleMailboxListener(closed, session, responder), mailboxSession);
            }
        } catch (MailboxException e) {
            // TODO: What should we do here?
            no(command, tag, responder, HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING);
        }
    }

    public List<String> getImplementedCapabilities(ImapSession session) {
        return Arrays.asList(SUPPORTS_IDLE);
    }
    
    private class IdleMailboxListener implements MailboxListener {
        
        private final AtomicBoolean closed;
        private final ImapSession session;
        private final Responder responder;
        
        public IdleMailboxListener(AtomicBoolean closed, ImapSession session, Responder responder) {
            this.closed = closed;
            this.session = session;
            this.responder = responder;
        }

        public void event(Event event) {
            synchronized (session) {
                if (isClosed()) {
                    return;
                }
                if (event instanceof Added || event instanceof Expunged || event instanceof FlagsUpdated) {
                    unsolicitedResponses(session, responder, false);
                }
            }
        }

        public boolean isClosed() {
            return closed.get();
        }
        
    }
}
