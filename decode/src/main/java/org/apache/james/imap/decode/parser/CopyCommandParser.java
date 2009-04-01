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
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.ProtocolException;

class CopyCommandParser extends AbstractUidCommandParser {
	
    public CopyCommandParser() {
    	super(ImapCommand.selectedStateCommand(ImapConstants.COPY_COMMAND_NAME));
    }

    protected ImapMessage decode(ImapCommand command,
            ImapRequestLineReader request, String tag, boolean useUids, Log logger)
            throws ProtocolException {
        IdRange[] idSet = parseIdRange(request);
        String mailboxName = mailbox(request);
        endLine(request);
        final ImapMessage result = getMessageFactory().createCopyMessage(
                command, idSet, mailboxName, useUids, tag);
        return result;
    }

}