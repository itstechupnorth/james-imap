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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapLineHandler;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.request.AuthenticateRequest;
import org.apache.james.imap.message.response.AuthenticateResponse;
import org.apache.james.mailbox.MailboxManager;

public class AuthenticateProcessor extends AbstractAuthProcessor<AuthenticateRequest> implements CapabilityImplementingProcessor{
    private final static String PLAIN = "PLAIN";
    
    public AuthenticateProcessor(final ImapProcessor next, final MailboxManager mailboxManager, final StatusResponseFactory factory) {
        super(AuthenticateRequest.class, next, mailboxManager, factory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.processor.AbstractMailboxProcessor#doProcess(org
     * .apache.james.imap.api.message.request.ImapRequest,
     * org.apache.james.imap.api.process.ImapSession, java.lang.String,
     * org.apache.james.imap.api.ImapCommand,
     * org.apache.james.imap.api.process.ImapProcessor.Responder)
     */
    protected void doProcess(AuthenticateRequest request, ImapSession session, final String tag, final ImapCommand command, final Responder responder) {
        final String authType = request.getAuthType();
        if (authType.equalsIgnoreCase(PLAIN)) {
            responder.respond(new AuthenticateResponse());
            session.pushLineHandler(new ImapLineHandler() {
                
                public void onLine(ImapSession session, byte[] data) {
                    String user = null, pass = null;
                    try {
                        // strip of the newline
                        String userpass = new String(data, 0, data.length - 2, Charset.forName("US-ASCII"));

                        userpass = new String(Base64.decodeBase64(userpass));
                        StringTokenizer authTokenizer = new StringTokenizer(userpass, "\0");
                        String authorize_id = authTokenizer.nextToken();  // Authorization Identity
                        user = authTokenizer.nextToken();                 // Authentication Identity
                        try {
                            pass = authTokenizer.nextToken();             // Password
                        } catch (java.util.NoSuchElementException _) {
                            // If we got here, this is what happened.  RFC 2595
                            // says that "the client may leave the authorization
                            // identity empty to indicate that it is the same as
                            // the authentication identity."  As noted above,
                            // that would be represented as a decoded string of
                            // the form: "\0authenticate-id\0password".  The
                            // first call to nextToken will skip the empty
                            // authorize-id, and give us the authenticate-id,
                            // which we would store as the authorize-id.  The
                            // second call will give us the password, which we
                            // think is the authenticate-id (user).  Then when
                            // we ask for the password, there are no more
                            // elements, leading to the exception we just
                            // caught.  So we need to move the user to the
                            // password, and the authorize_id to the user.
                            pass = user;
                            user = authorize_id;
                        }

                        authTokenizer = null;
                    } catch (Exception e) {
                        // Ignored - this exception in parsing will be dealt
                        // with in the if clause below
                    }
                    // Authenticate user
                    doAuth(user, pass, session, tag, command, responder, HumanReadableText.AUTHENTICATION_FAILED);

                    // remove the handler now
                    session.popLineHandler();
                    
                }
            });
        } else {
            session.getLog().info("Unsupported authentication mechanism '" + authType + "'");
            no(command, tag, responder, HumanReadableText.UNSUPPORTED_AUTHENTICATION_MECHANISM);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.processor.CapabilityImplementingProcessor#getImplementedCapabilities(org.apache.james.imap.api.process.ImapSession)
     */
    public List<String> getImplementedCapabilities(ImapSession session) {
        return Arrays.asList("AUTH=PLAIN");
    }

}
