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

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.decode.base.AbstractImapCommandParser;
import org.apache.james.imap.message.request.AppendRequest;
import org.apache.james.imap.message.request.ContinuationRequest;

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
    public Flags optionalAppendFlags(ImapRequestLineReader request)
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
    public Date optionalDateTime(ImapRequestLineReader request)
            throws DecodingException {
        char next = request.nextWordChar();
        if (next == '"') {
            return dateTime(request);
        } else {
            return null;
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.base.AbstractImapCommandParser#decode(org.apache.james.imap.api.ImapCommand, org.apache.james.imap.decode.ImapRequestLineReader, java.lang.String, org.apache.commons.logging.Log, org.apache.james.imap.api.process.ImapSession)
     */
    protected ImapMessage decode(final ImapCommand command,
            ImapRequestLineReader request, final String tag, ImapSession session) throws DecodingException {
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
            
            ImapDecoder nextDecoder = new ImapDecoder() {
                
                public ImapMessage decode(ImapRequestLineReader request, ImapSession session) {
                    int bytes = (Integer)session.getAttribute(BYTES_WRITTEN);
                    try {
                        while(request.isConsumed() == false && bytes < size) {
                            out.write((byte)request.consume());
                            bytes++;
                        }
                        if (bytes == size) {
                            request.eol();
                            session.setAttribute(ImapConstants.NEXT_DECODER, null);
                            session.setAttribute(BYTES_WRITTEN, null);
                            return new AppendRequest(command, mailboxName, f, dt, new DeleteOnCloseInputStream(new FileInputStream(file), file) , tag);
                        } else {
                            session.setAttribute(BYTES_WRITTEN, bytes);
                        }
                        
                    } catch (DecodingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            session.setAttribute(ImapConstants.NEXT_DECODER, nextDecoder);

            
            return new ContinuationRequest(command, tag, nextDecoder); 
            
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new DecodingException(HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING,e1.getMessage());
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
}
