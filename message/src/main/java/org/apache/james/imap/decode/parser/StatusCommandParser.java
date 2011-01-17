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
import org.apache.james.imap.api.message.StatusDataItems;
import org.apache.james.imap.decode.ImapRequestLine;
import org.apache.james.imap.decode.base.AbstractImapCommandParser;
import org.apache.james.imap.message.request.StatusRequest;

/**
 * Parse STATUS commands
 *
 */
public class StatusCommandParser extends AbstractImapCommandParser {
    public StatusCommandParser() {
        super(ImapCommand.authenticatedStateCommand(ImapConstants.STATUS_COMMAND_NAME));
    }

    StatusDataItems statusDataItems(ImapRequestLine request)
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

    @Override
    protected void decode(ImapCommand command, ImapRequestLine request, String tag, ImapSession session, ImapMessageCallback callback) {
        try {
            final String mailboxName = mailbox(request);
            final StatusDataItems statusDataItems = statusDataItems(request);
            endLine(request);
            callback.onMessage(new StatusRequest(command, mailboxName, statusDataItems, tag));        
        } catch (DecodingException e) {
            callback.onException(e);
        }
        
    }

}
