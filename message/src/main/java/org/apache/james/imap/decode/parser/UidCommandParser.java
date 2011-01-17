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

import org.apache.james.imap.api.DecodingException;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessageCallback;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.decode.DelegatingImapCommandParser;
import org.apache.james.imap.decode.ImapCommandParser;
import org.apache.james.imap.decode.ImapCommandParserFactory;
import org.apache.james.imap.decode.ImapRequestLine;
import org.apache.james.imap.decode.base.AbstractImapCommandParser;

/**
 * Parse UID commands
 *
 */
public class UidCommandParser extends AbstractImapCommandParser implements
        DelegatingImapCommandParser {

    private ImapCommandParserFactory parserFactory;

    public UidCommandParser() {
        super(ImapCommand.selectedStateCommand(ImapConstants.UID_COMMAND_NAME));
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

    

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.base.AbstractImapCommandParser#decode(org.apache.james.imap.api.ImapCommand, org.apache.james.imap.decode.ImapRequestLineReader, java.lang.String, org.apache.james.imap.api.process.ImapSession, org.apache.james.imap.api.ImapMessageCallback)
     */
    protected void decode(ImapCommand command, ImapRequestLine request, String tag, ImapSession session, ImapMessageCallback callback) {
        // TODO: check the logic against the specification:
        // TODO: suspect that it is now bust
        // TODO: the command written may be wrong
        // TODO: this will be easier to fix a little later
        // TODO: also not sure whether the old implementation shares this flaw
        try {
            String commandName = atom(request);
            ImapCommandParser helperCommand = parserFactory.getParser(commandName);
            // TODO: replace abstract class with interface
            if (helperCommand == null
                    || !(helperCommand instanceof AbstractUidCommandParser)) {
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, 
                        "Invalid UID command: '" + commandName + "'");
            }
            final AbstractUidCommandParser uidEnabled = (AbstractUidCommandParser) helperCommand;
            
            uidEnabled.decode(request, tag, true, session, callback);
        } catch (DecodingException e) {
            callback.onException(e);
        }
        
    }
    
    

}
