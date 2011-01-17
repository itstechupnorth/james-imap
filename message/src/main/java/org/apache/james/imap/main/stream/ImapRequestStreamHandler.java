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

package org.apache.james.imap.main.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.DecodingException;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapMessageCallback;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.message.response.ImapResponseComposer;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.decode.ImapRequestLine;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.main.AbstractImapRequestHandler;
import org.apache.james.imap.message.request.SystemMessage;

/**
 * @version $Revision: 109034 $
 */
public final class ImapRequestStreamHandler extends AbstractImapRequestHandler {

    public ImapRequestStreamHandler(final ImapDecoder decoder, final ImapProcessor processor, final ImapEncoder encoder) {
        super(decoder, processor, encoder);
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
    public boolean handleRequest(final InputStream input, final OutputStream output, final ImapSession session) {
        final boolean result;
        final ImapResponseComposer composer = new OutputStreamImapResponseComposer(output);

        if (isSelectedMailboxDeleted(session)) {
            writeSignoff(composer, session);
            result = false;
            return result;
        } else {
            final Log logger = session.getLog();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            ImapRequestLine request;
            try {
                request = new ImapRequestLine((reader.readLine()).getBytes(), composer);
                BlockingImapMessageCallback callback = new BlockingImapMessageCallback(composer, session);
                doProcessRequest(request, composer, session, callback);
                return callback.hasMore();
            } catch (IOException e) {
                e.printStackTrace();

                logger.debug("Unexpected end of line. Cannot handle request: ", e);
                abandon(composer, session);
            }

        }
        return false;
        
        
    }

    private void writeSignoff(ImapResponseComposer output, ImapSession session) {
        try {
            output.tag(new String(MAILBOX_DELETED_SIGNOFF));
        } catch (IOException e) {
            session.getLog().warn("Failed to write signoff");
            session.getLog().debug("Failed to write signoff:", e);
        }
    }

    private void abandon(ImapResponseComposer out, ImapSession session) {
        if (session != null) {
            try {
                session.logout();
            } catch (Throwable t) {
                session.getLog().warn("Session logout failed. Resources may not be correctly recycled.");
            }
        }
        try {
            out.tag(new String(ABANDON_SIGNOFF));
        } catch (Throwable t) {
            session.getLog().debug("Failed to write ABANDON_SIGNOFF", t);
        }
        processor.process(SystemMessage.FORCE_LOGOUT, new SilentResponder(), session);
    }
    
    private final class BlockingImapMessageCallback implements ImapMessageCallback {

        private ImapResponseComposer composer;
        private ImapSession session;
        private boolean result;
        private CountDownLatch latch = new CountDownLatch(1);
        
        public BlockingImapMessageCallback(ImapResponseComposer composer, ImapSession session) {
            this.composer = composer;
            this.session = session;
        }
        public void onMessage(ImapMessage message) {

            final ResponseEncoder responseEncoder = new ResponseEncoder(encoder, composer, session);
            processor.process(message, responseEncoder, session);

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
            result = !(ImapSessionState.LOGOUT == session.getState());

            if (result == false) {
                session.getLog().debug("Connection was abandoned after request processing failed.");
                result = false;
                abandon(composer, session);
            }
            latch.countDown();

        }

        public void onException(DecodingException ex) {
            ex.printStackTrace();

            session.getLog().debug("Unexpected end of line. Cannot handle request: ", ex);
            abandon(composer, session);
        }
        
        public boolean hasMore() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return result;
        }
    }

}
