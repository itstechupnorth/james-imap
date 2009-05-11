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

import java.util.Date;

import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapMessageFactory;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.decode.base.AbstractImapCommandParser;

class AppendCommandParser extends AbstractImapCommandParser {

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

    protected ImapMessage decode(ImapCommand command,
            ImapRequestLineReader request, String tag, Log logger) throws DecodingException {
        String mailboxName = mailbox(request);
        Flags flags = optionalAppendFlags(request);
        if (flags == null) {
            flags = new Flags();
        }
        Date datetime = optionalDateTime(request);
        if (datetime == null) {
            datetime = new Date();
        }
        request.nextWordChar();
        final byte[] message = consumeLiteral(request);
        endLine(request);
        final ImapMessageFactory factory = getMessageFactory();
        final ImapMessage result = factory.createAppendMessage(command,
                mailboxName, flags, datetime, message, tag);
        return result;
    }
}
