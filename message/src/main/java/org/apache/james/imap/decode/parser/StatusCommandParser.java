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
import org.apache.james.imap.api.ImapMessageFactory;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.StatusDataItems;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.decode.base.AbstractImapCommandParser;

/**
 * Parse STATUS commands
 *
 */
public class StatusCommandParser extends AbstractImapCommandParser {
    public StatusCommandParser() {
        super(ImapCommand.authenticatedStateCommand(ImapConstants.STATUS_COMMAND_NAME));
    }

    StatusDataItems statusDataItems(ImapRequestLineReader request)
            throws DecodingException {
        StatusDataItems items = new StatusDataItems();

        request.nextWordChar();
        consumeChar(request, '(');
        CharacterValidator validator = new NoopCharValidator();
        String nextWord = consumeWord(request, validator);

        while (!nextWord.endsWith(")")) {
            addItem(nextWord, items);
            nextWord = consumeWord(request, validator);
        }
        // Got the closing ")", may be attached to a word.
        if (nextWord.length() > 1) {
            addItem(nextWord.substring(0, nextWord.length() - 1), items);
        }

        return items;
    }

    private void addItem(String nextWord, StatusDataItems items)
            throws DecodingException {

        if (nextWord.equals(ImapConstants.STATUS_MESSAGES)) {
            items.setMessages(true);
        } else if (nextWord.equals(ImapConstants.STATUS_RECENT)) {
            items.setRecent(true);
        } else if (nextWord.equals(ImapConstants.STATUS_UIDNEXT)) {
            items.setUidNext(true);
        } else if (nextWord.equals(ImapConstants.STATUS_UIDVALIDITY)) {
            items.setUidValidity(true);
        } else if (nextWord.equals(ImapConstants.STATUS_UNSEEN)) {
            items.setUnseen(true);
        } else {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, 
                    "Unknown status item: '" + nextWord + "'");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.base.AbstractImapCommandParser#decode(org.apache.james.imap.api.ImapCommand, org.apache.james.imap.decode.ImapRequestLineReader, java.lang.String, org.apache.commons.logging.Log)
     */
    protected ImapMessage decode(ImapCommand command,
            ImapRequestLineReader request, String tag, Log logger) throws DecodingException {
        final String mailboxName = mailbox(request);
        final StatusDataItems statusDataItems = statusDataItems(request);
        endLine(request);
        final ImapMessageFactory factory = getMessageFactory();
        final ImapMessage result = factory.createStatusMessage(command,
                mailboxName, statusDataItems, tag);
        
        return result;
    }
}