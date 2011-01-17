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
package org.apache.james.imap.decode.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.mail.Flags;

import org.apache.james.imap.api.DecodingException;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapLineHandler;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapMessageCallback;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.decode.ImapRequestLine;
import org.apache.james.imap.decode.base.AbstractImapCommandParser;
import org.apache.james.imap.message.request.AppendRequest;

/**
 * Parses APPEND command
 *
 */
public class AppendCommandParser extends AbstractImapCommandParser {
    
    public final String BYTES_WRITTEN = AppendCommandParser.class.getName() + "_BYTES_WRITTEN";
    
    public AppendCommandParser() {
    	super(ImapCommand.authenticatedStateCommand(ImapConstants.APPEND_COMMAND_NAME));
    }

    /**
     * If the next character in the request is a '(', tries to read a
     * "flag_list" argument from the request. If not, returns a MessageFlags
     * with no flags set.
     */
    public Flags optionalAppendFlags(ImapRequestLine request)
            throws DecodingException {
        char next = request.nextWordChar();
        if (next == '(') {
            return flagList(request);
        } else {
            return null;
        }
    }

    /**
     * If the next character in the request is a '"', tries to read a DateTime
     * argument. If not, returns null.
     */
    public Date optionalDateTime(ImapRequestLine request)
            throws DecodingException {
        char next = request.nextWordChar();
        if (next == '"') {
            return dateTime(request);
        } else {
            return null;
        }
    }



    
    private class DeleteOnCloseInputStream extends FilterInputStream {

        private File f;

        protected DeleteOnCloseInputStream(InputStream in, File f) {
            super(in);
            this.f = f;
        }

        @Override
        public void close() throws IOException {
            super.close();
            f.delete();
        }
        
    }

    @Override
    protected void decode(final ImapCommand command, ImapRequestLine request, final String tag, final ImapSession session, final ImapMessageCallback callback) {
        try {
            final String mailboxName = mailbox(request);
            Flags flags = optionalAppendFlags(request);
            if (flags == null) {
                flags = new Flags();
            }
            final Flags f = flags;
            
            Date datetime = optionalDateTime(request);
            if (datetime == null) {
                datetime = new Date();
            }
            final Date dt = datetime;
            request.nextWordChar();
            
            try {
                final File file = File.createTempFile("imap-append", ".m64");
                final FileOutputStream out = new FileOutputStream(file);
                final int size = consumeLiteralSize(request);
                session.setAttribute(BYTES_WRITTEN, 0);
                session.pushImapLineHandler( new ImapLineHandler() {
                    
                    public void onLine(ImapSession session, byte[] data, ImapMessageCallback callback) {
                        int bytes = (Integer)session.getAttribute(BYTES_WRITTEN);
                        try {
                            int left = size - bytes;
                            if (left -2 > data.length) {
                                out.write(data);
                                bytes += data.length;
                             
                                // as we push data without delimiters we need to put them in back
                                if (bytes != size) {
                                    out.write("\r\n".getBytes());
                                    bytes += 2;
                                }
                            } else {
                                out.write(data, 0, left);
                                bytes = size;
                            }
                            

                            
                            
                            if (bytes == size) {
                                session.popImapLineHandler();
                                session.setAttribute(BYTES_WRITTEN, null);
                                ImapMessage message =  new AppendRequest(command, mailboxName, f, dt, new DeleteOnCloseInputStream(new FileInputStream(file), file) , tag);
                                callback.onMessage(message);
                            } else {
                                
                                session.setAttribute(BYTES_WRITTEN, bytes);
                            }
                            
                        } catch (DecodingException e) {
                            session.popImapLineHandler();
                            callback.onException(e);
                        } catch (IOException e) {
                            session.popImapLineHandler();
                            callback.onException(new DecodingException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING,e.getMessage()));
                        }
                    }
                });
                            
            } catch (IOException e1) {            
                callback.onException(new DecodingException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING,e1.getMessage()));
            }
            
        } catch (DecodingException e2) {
            callback.onException(e2);

        }
        
    }
}
