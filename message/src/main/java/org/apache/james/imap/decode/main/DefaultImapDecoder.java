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
package org.apache.james.imap.decode.main;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapMessageFactory;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.decode.ImapCommandParser;
import org.apache.james.imap.decode.ImapCommandParserFactory;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.decode.base.AbstractImapCommandParser;

/**
 * {@link ImapDecoder} implementation which parse the data via lookup the right {@link ImapCommandParser} via an {@link ImapCommandParserFactory}. The 
 * response will get generated via the {@link ImapMessageFactory}.
 *
 */
public class DefaultImapDecoder implements ImapDecoder {

    private final ImapMessageFactory messageFactory;

    private final ImapCommandParserFactory imapCommands;

    public DefaultImapDecoder(final ImapMessageFactory messageFactory,
            final ImapCommandParserFactory imapCommands) {
        this.messageFactory = messageFactory;
        this.imapCommands = imapCommands;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.ImapDecoder#decode(org.apache.james.imap.decode.ImapRequestLineReader, org.apache.james.imap.api.process.ImapSession)
     */
    public ImapMessage decode(ImapRequestLineReader request, ImapSession session) {
        ImapMessage message;
        final Log logger = session.getLog();

        try {
            final String tag = AbstractImapCommandParser.tag(request);
            message = decodeCommandTagged(request, tag, session);
        } catch (DecodingException e) {
            logger.debug("Cannot parse tag", e);

            // When the tag cannot be read, there is something seriously wrong.
            // It is probably not possible to recover
            // and (since this may indicate an attack) wiser not to try
            message = messageFactory.bye(HumanReadableText.ILLEGAL_TAG);
            session.logout();
        }
        return message;
    }

    
    private ImapMessage decodeCommandTagged(
            final ImapRequestLineReader request, 
            final String tag, final ImapSession session) {
        ImapMessage message;
        if (session.getLog().isDebugEnabled()) {
            session.getLog().debug("Got <tag>: " + tag);
        }
        try {
            final String commandName = AbstractImapCommandParser.atom(request);
            message = decodeCommandNamed(request, tag, commandName,
                    session);
        } catch (DecodingException e) {
            session.getLog().debug("Error during initial request parsing", e);
            message = unknownCommand(tag, session);
        }
        return message;
    }

    private ImapMessage unknownCommand(final String tag,
            final ImapSession session) {
        ImapMessage message;
        if (session.getState() == ImapSessionState.NON_AUTHENTICATED) {
            message = messageFactory
                    .bye(HumanReadableText.BYE_UNKNOWN_COMMAND);
            session.logout();
        } else {
            message = messageFactory.taggedBad(tag, null,
                    HumanReadableText.UNKNOWN_COMMAND);
        }
        return message;
    }

    private ImapMessage decodeCommandNamed(final ImapRequestLineReader request,
            final String tag, String commandName,
            final ImapSession session) {
        ImapMessage message;
        if (session.getLog().isDebugEnabled()) {
            session.getLog().debug("Got <command>: " + commandName);
        }
        final ImapCommandParser command = imapCommands.getParser(commandName);
        if (command == null) {
            session.getLog().info("Missing command implementation.");
            message = unknownCommand(tag, session);
        } else {
            message = command.parse(request, tag, session);
        }
        return message;
    }
}
