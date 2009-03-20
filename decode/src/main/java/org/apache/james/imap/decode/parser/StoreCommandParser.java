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

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.Imap4Rev1CommandFactory;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.InitialisableCommandFactory;
import org.apache.james.imap.decode.ProtocolException;

class StoreCommandParser extends AbstractUidCommandParser implements
        InitialisableCommandFactory {
    public StoreCommandParser() {
    }

    /**
     * @see org.apache.james.imap.decode.InitialisableCommandFactory#init(org.apache.james.imap.api.Imap4Rev1CommandFactory)
     */
    public void init(Imap4Rev1CommandFactory factory) {
        final ImapCommand command = factory.getStore();
        setCommand(command);
    }

    protected ImapMessage decode(ImapCommand command,
            ImapRequestLineReader request, String tag, boolean useUids, Log logger)
            throws ProtocolException {
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
            throw new ProtocolException("Invalid Store Directive: '"
                    + directive + "'");
        }

        final Flags flags = flagList(request);
        endLine(request);
        final ImapMessage result = getMessageFactory().createStoreMessage(
                command, idSet, silent, sign, flags, useUids, tag);
        return result;
    }
}
