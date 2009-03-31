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

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.decode.DelegatingImapCommandParser;
import org.apache.james.imap.decode.ImapCommandParser;
import org.apache.james.imap.decode.ImapCommandParserFactory;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.ProtocolException;
import org.apache.james.imap.decode.base.AbstractImapCommandParser;

class UidCommandParser extends AbstractImapCommandParser implements
        DelegatingImapCommandParser {
	
    private final ImapCommand uid = ImapCommand.selectedStateCommand(ImapConstants.UID_COMMAND_NAME);
	
	private ImapCommandParserFactory parserFactory;

    public UidCommandParser() {
    	setCommand(uid);
    }

    /**
     * @see org.apache.james.imap.decode.DelegatingImapCommandParser#getParserFactory()
     */
    public ImapCommandParserFactory getParserFactory() {
        return parserFactory;
    }

    /**
     * @see org.apache.james.imap.decode.DelegatingImapCommandParser#setParserFactory(org.apache.james.imap.decode.imap4rev1.Imap4Rev1CommandParserFactory)
     */
    public void setParserFactory(ImapCommandParserFactory imapCommandFactory) {
        this.parserFactory = imapCommandFactory;
    }

    protected ImapMessage decode(ImapCommand command,
            ImapRequestLineReader request, String tag, Log logger) throws ProtocolException {
        // TODO: check the logic against the specification:
        // TODO: suspect that it is now bust
        // TODO: the command written may be wrong
        // TODO: this will be easier to fix a little later
        // TODO: also not sure whether the old implementation shares this flaw
        String commandName = atom(request);
        ImapCommandParser helperCommand = parserFactory.getParser(commandName);
        // TODO: replace abstract class with interface
        if (helperCommand == null
                || !(helperCommand instanceof AbstractUidCommandParser)) {
            throw new ProtocolException("Invalid UID command: '" + commandName
                    + "'");
        }
        final AbstractUidCommandParser uidEnabled = (AbstractUidCommandParser) helperCommand;
        final ImapMessage result = uidEnabled.decode(request, tag, true, logger);
        return result;
    }

}
