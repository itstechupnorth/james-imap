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
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.api.process.ImapProcessor.Responder;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.encode.base.ImapResponseComposerImpl;
import org.apache.james.imap.message.request.SystemMessage;

/**
 * @version $Revision: 109034 $
 */
public final class ImapRequestHandler  {

    private static final byte[] ABANDON_SIGNOFF = { '*', ' ', 'B', 'Y', 'E',
            ' ', 'A', 'b', 'a', 'n', 'd', 'o', 'n', 'e', 'd', '\r', '\n' };

    private static final byte[] MAILBOX_DELETED_SIGNOFF = { '*', ' ', 'B', 'Y',
            'E', ' ', 'S', 'e', 'l', 'e', 'c', 't', 'e', 'd', ' ', 'm', 'a',
            'i', 'l', 'b', 'o', 'x', ' ', 'h', 'a', 's', ' ', 'b', 'e', 'e',
            'n', ' ', 'd', 'e', 'l', 'e', 't', 'e', 'd', '\r', '\n' };
    
    private final ImapDecoder decoder;

    private final ImapProcessor processor;

    private final ImapEncoder encoder;

    public ImapRequestHandler(final ImapDecoder decoder,
            final ImapProcessor processor, final ImapEncoder encoder) {
        this.decoder = decoder;
        this.processor = processor;
        this.encoder = encoder;
    }

    /**
     * This method parses IMAP commands read off the wire in handleConnection.
     * Actual processing of the command (possibly including additional back and
     * forth communication with the client) is delegated to one of a number of
     * command specific handler methods. The primary purpose of this method is
     * to parse the raw command string to determine exactly which handler should
     * be called. It returns true if expecting additional commands, false
     * otherwise.
     * 
     * @return whether additional commands are expected.
     */
    public boolean handleRequest(InputStream input, OutputStream output,
            ImapSession session) {
        final boolean result;
        if (isSelectedMailboxDeleted(session)) {
            writeSignoff(output, session);
            result = false;
        } else {
            ImapRequestLineReader request = new ImapRequestLineReader(input,
                    output);

            final Log logger = session.getLog();
            try {
                request.nextChar();
            } catch (DecodingException e) {
                logger.debug("Unexpected end of line. Cannot handle request: ",
                        e);
                abandon(output, session);
                return false;
            }

            ImapResponseComposerImpl response = new ImapResponseComposerImpl(
                    new OutputStreamImapResponseWriter(output));

            if (doProcessRequest(request, response, session)) {

                try {
                    // Consume the rest of the line, throwing away any extras.
                    // This allows us
                    // to clean up after a protocol error.
                    request.consumeLine();
                } catch (DecodingException e) {
                    // Cannot clean up. No recovery is therefore possible.
                    // Abandon connection.
                    if (logger.isInfoEnabled()) {
                        logger.info("Fault during clean up: " + e.getMessage());
                    }
                    logger.debug("Abandoning after fault in clean up", e);
                    abandon(output, session);
                    return false;
                }

                result = !(ImapSessionState.LOGOUT == session.getState());
            } else {
                logger.debug("Connection was abandoned after request processing failed.");
                result = false;
                abandon(output, session);
            }
        }
        return result;
    }

    private void writeSignoff(OutputStream output, ImapSession session) {
        try {
            output.write(MAILBOX_DELETED_SIGNOFF);
        } catch (IOException e) {
            session.getLog().warn("Failed to write signoff");
            session.getLog().debug("Failed to write signoff:", e);
        }
    }

    private boolean isSelectedMailboxDeleted(ImapSession session) {
        final boolean selectedMailboxIsDeleted;
        final SelectedMailbox mailbox = session.getSelected();
        if (mailbox != null) {
            selectedMailboxIsDeleted = mailbox.isDeletedByOtherSession();
        } else {
            selectedMailboxIsDeleted = false;
        }
        return selectedMailboxIsDeleted;
    }

    private void abandon(OutputStream out, ImapSession session) {
        if (session != null){
            try {
                session.logout();
            } catch (Throwable t) {
                session.getLog().warn("Session logout failed. Resources may not be correctly recycled.");
            }
        }
        try {
            out.write(ABANDON_SIGNOFF);
        } catch (Throwable t) {
            session.getLog().debug("Failed to write ABANDON_SIGNOFF", t);
        }
        processor.process(SystemMessage.FORCE_LOGOUT, new SilentResponder(), session);
    }

    private boolean doProcessRequest(ImapRequestLineReader request,
            ImapResponseComposer response, ImapSession session) {
        ImapMessage message = decoder.decode(request, session);
        final ResponseEncoder responseEncoder = new ResponseEncoder(encoder,
                response, session);
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

    /**
     * Silents swallows all responses.
     */
    private static final class SilentResponder implements Responder {

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
