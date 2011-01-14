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

package org.apache.james.imap.main;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.api.process.ImapProcessor.Responder;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.parser.AppendCommandParser;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ImapResponseComposer;

public abstract class AbstractImapRequestHandler {


    protected static final byte[] ABANDON_SIGNOFF = { '*', ' ', 'B', 'Y', 'E',
            ' ', 'A', 'b', 'a', 'n', 'd', 'o', 'n', 'e', 'd', '\r', '\n' };

    protected static final byte[] MAILBOX_DELETED_SIGNOFF = { '*', ' ', 'B', 'Y',
            'E', ' ', 'S', 'e', 'l', 'e', 'c', 't', 'e', 'd', ' ', 'm', 'a',
            'i', 'l', 'b', 'o', 'x', ' ', 'h', 'a', 's', ' ', 'b', 'e', 'e',
            'n', ' ', 'd', 'e', 'l', 'e', 't', 'e', 'd', '\r', '\n' };
    

    
    private final ImapDecoder decoder;
    protected final ImapProcessor processor;
    private final ImapEncoder encoder;

    public AbstractImapRequestHandler(final ImapDecoder decoder,
            final ImapProcessor processor, final ImapEncoder encoder) {
        this.decoder = decoder;
        this.processor = processor;
        this.encoder = encoder;
    }
    
    protected boolean doProcessRequest(ImapRequestLineReader request,
            ImapResponseComposer response, ImapSession session) {
        ImapDecoder nextDecoder = getDecoder(session);
        ImapMessage message = nextDecoder.decode(request, session);
        if (message != null) {
            final ResponseEncoder responseEncoder = new ResponseEncoder(encoder, response, session);
            processor.process(message, responseEncoder, session);

            final boolean result;
            final IOException failure = responseEncoder.getFailure();
            if (failure == null) {
                result = true;
            } else {
                result = false;
                final Log logger = session.getLog();
                logger.info(failure.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to write " + message, failure);
                }
            }
            return result;
        }
        return true;
        
    }

    private ImapDecoder getDecoder(ImapSession session) {
        ImapDecoder nextDecoder = (ImapDecoder) session.getAttribute(AppendCommandParser.NEXT_DECODER);
        if (nextDecoder == null) {
            return this.decoder;
        } else {
            return nextDecoder;
        }
    }

    protected boolean isSelectedMailboxDeleted(ImapSession session) {
        final boolean selectedMailboxIsDeleted;
        final SelectedMailbox mailbox = session.getSelected();
        if (mailbox != null) {
            selectedMailboxIsDeleted = mailbox.isDeletedByOtherSession();
        } else {
            selectedMailboxIsDeleted = false;
        }
        return selectedMailboxIsDeleted;
    }

    
    /**
     * Silents swallows all responses.
     */
    public static final class SilentResponder implements Responder {

        public void respond(ImapResponseMessage message) {
            // Swallow
        }
    }
    
    private static final class ResponseEncoder implements Responder {
        private final ImapEncoder encoder;
        private final ImapSession session;
        private final ImapResponseComposer composer;

        private IOException failure;
        

        public ResponseEncoder(final ImapEncoder encoder,
                final ImapResponseComposer composer, final ImapSession session) {
            super();
            this.encoder = encoder;
            this.composer = composer;
            this.session = session;
        }

        public void respond(final ImapResponseMessage message) {
            try {
                encoder.encode(message, composer, session);
            } catch (IOException failure) {
                this.failure = failure;
            }
        }

        /**
         * Gets the recorded failure.
         * 
         * @return the failure, or null when no failure has occurred
         */
        public final IOException getFailure() {
            return failure;
        }

    }
}
