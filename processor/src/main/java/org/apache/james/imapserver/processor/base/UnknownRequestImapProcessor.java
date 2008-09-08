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

package org.apache.james.imapserver.processor.base;

import org.apache.commons.logging.Log;
import org.apache.james.api.imap.AbstractLogEnabled;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;

public class UnknownRequestImapProcessor extends AbstractLogEnabled implements ImapProcessor {

    private final StatusResponseFactory factory;
    
    public UnknownRequestImapProcessor(StatusResponseFactory factory) {
        super();
        this.factory = factory;
    }

    public ImapResponseMessage process(ImapMessage message, ImapSession session) {
        Log logger = getLog();
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug("Unknown message: " + message);
        }
        final ImapResponseMessage result;
        if (message instanceof ImapRequest) {
            ImapRequest request = (ImapRequest) message;
            final String tag = request.getTag();
            final ImapCommand command = request.getCommand();
            result = factory.taggedBad(tag, command, HumanReadableTextKey.UNKNOWN_COMMAND);
        } else {
            result = factory.untaggedBad(HumanReadableTextKey.UNKNOWN_COMMAND);
        }
        return result;
    }

    public void process(ImapMessage message, Responder responder, ImapSession session) {
        final ImapResponseMessage response = process(message, session);
        responder.respond(response);
    }

}