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

package org.apache.james.imap.processor;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.message.request.SystemMessage;
import org.apache.james.imap.processor.base.AbstractChainedProcessor;
import org.apache.james.imap.processor.base.ImapSessionUtils;

/**
 * Processes system messages unrelated to IMAP.
 */
public class SystemMessageProcessor extends AbstractChainedProcessor {

    private final MailboxManagerProvider mailboxManagerProvider;
    
    public SystemMessageProcessor(ImapProcessor next, final MailboxManagerProvider mailboxManagerProvider) {
        super(next);
        this.mailboxManagerProvider = mailboxManagerProvider;
    }

    @Override
    protected void doProcess(ImapMessage acceptableMessage, Responder responder, ImapSession session) {
        try {
            final SystemMessage message = (SystemMessage) acceptableMessage;
            switch (message) {
                case FORCE_LOGOUT:
                    forceLogout(session);
                    break;
            } 
        } catch (MailboxException e) {
            final Log log = session.getLog();
            log.warn("Force logout failed " + e.getMessage());
            log.debug("Cannot force logout", e);
        }
    }

    /**
     * Forces a logout of any mailbox session.
     * @param imapSession not null
     * @throws MailboxException when forced logout fails
     */
    private void forceLogout(ImapSession imapSession) throws MailboxException {
        final MailboxSession session = ImapSessionUtils.getMailboxSession(imapSession);
        if (session == null) {
            imapSession.getLog().trace("No mailbox session so no force logout needed");
        } else {
            session.close();
            final MailboxManager manager = mailboxManagerProvider.getMailboxManager();
            manager.logout(session, true);
        }
    }

    @Override
    protected boolean isAcceptable(ImapMessage message) {
        return message instanceof SystemMessage;
    }

}
