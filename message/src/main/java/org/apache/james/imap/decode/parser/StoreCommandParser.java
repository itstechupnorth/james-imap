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

import javax.mail.Flags;

import org.apache.james.imap.api.DecodingException;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapMessageCallback;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.decode.ImapRequestLine;
import org.apache.james.imap.message.request.StoreRequest;

/**
 * Parse STORE commands
 *
 */
public class StoreCommandParser extends AbstractUidCommandParser  {

    public StoreCommandParser() {
        super(ImapCommand.selectedStateCommand(ImapConstants.STORE_COMMAND_NAME));
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.parser.AbstractUidCommandParser#decode(org.apache.james.imap.api.ImapCommand, org.apache.james.imap.decode.ImapRequestLineReader, java.lang.String, boolean, org.apache.james.imap.api.process.ImapSession, org.apache.james.imap.api.ImapMessageCallback)
     */
    protected void decode(ImapCommand command,
            ImapRequestLine request, String tag, boolean useUids, ImapSession session, ImapMessageCallback callback) {
        try {
            final IdRange[] idSet = parseIdRange(request);
            final Boolean sign;
            boolean silent = false;

            char next = request.nextWordChar();
            if (next == '+') {
                sign = Boolean.TRUE;
                request.consume();
            } else if (next == '-') {
                sign = Boolean.FALSE;
                request.consume();
            } else {
                sign = null;
            }

            String directive = consumeWord(request, new NoopCharValidator());
            if ("FLAGS".equalsIgnoreCase(directive)) {
                silent = false;
            } else if ("FLAGS.SILENT".equalsIgnoreCase(directive)) {
                silent = true;
            } else {
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Invalid Store Directive: '" + directive + "'");
            }

            final Flags flags = flagList(request);
            endLine(request);
            final ImapMessage result = new StoreRequest(command, idSet, silent, flags, useUids, tag, sign);
            callback.onMessage(result);
        } catch (DecodingException e) {
            callback.onException(e);
        }
    }
}
