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

package org.apache.james.imap.processor.imap4rev1;

import static org.apache.james.imap.api.ImapConstants.SUPPORTS_LITERAL_PLUS;
import static org.apache.james.imap.api.ImapConstants.VERSION;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.imap.api.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.processor.imap4rev1.fetch.FetchProcessor;

/**
 * TODO: perhaps this should be a POJO
 */
public class Imap4Rev1ProcessorFactory {

    public static final ImapProcessor createDefaultChain(
            final ImapProcessor chainEndProcessor,
            final MailboxManagerProvider mailboxManagerProvider,
            final StatusResponseFactory statusResponseFactory) {
        
        final LogoutProcessor logoutProcessor = new LogoutProcessor(
                chainEndProcessor, mailboxManagerProvider, statusResponseFactory);
        final List<String> capabilities = new ArrayList<String>();
        capabilities.add(VERSION);
        capabilities.add(SUPPORTS_LITERAL_PLUS);
        final CapabilityProcessor capabilityProcessor = new CapabilityProcessor(
                logoutProcessor, mailboxManagerProvider, statusResponseFactory, capabilities);
        final CheckProcessor checkProcessor = new CheckProcessor(
                capabilityProcessor, mailboxManagerProvider, statusResponseFactory);
        final LoginProcessor loginProcessor = new LoginProcessor(
                checkProcessor, mailboxManagerProvider, statusResponseFactory);
        final RenameProcessor renameProcessor = new RenameProcessor(
                loginProcessor, mailboxManagerProvider, statusResponseFactory);
        final DeleteProcessor deleteProcessor = new DeleteProcessor(
                renameProcessor, mailboxManagerProvider, statusResponseFactory);
        final CreateProcessor createProcessor = new CreateProcessor(
                deleteProcessor, mailboxManagerProvider, statusResponseFactory);
        final CloseProcessor closeProcessor = new CloseProcessor(
                createProcessor, mailboxManagerProvider, statusResponseFactory);
        final UnsubscribeProcessor unsubscribeProcessor = new UnsubscribeProcessor(
                closeProcessor, mailboxManagerProvider, statusResponseFactory);
        final SubscribeProcessor subscribeProcessor = new SubscribeProcessor(
                unsubscribeProcessor, mailboxManagerProvider,
                statusResponseFactory);
        final CopyProcessor copyProcessor = new CopyProcessor(
                subscribeProcessor, mailboxManagerProvider,
                statusResponseFactory);
        final AuthenticateProcessor authenticateProcessor = new AuthenticateProcessor(
                copyProcessor, mailboxManagerProvider, statusResponseFactory);
        final ExpungeProcessor expungeProcessor = new ExpungeProcessor(
                authenticateProcessor, mailboxManagerProvider,
                statusResponseFactory);
        final ExamineProcessor examineProcessor = new ExamineProcessor(
                expungeProcessor, mailboxManagerProvider, statusResponseFactory);
        final AppendProcessor appendProcessor = new AppendProcessor(
                examineProcessor, mailboxManagerProvider, statusResponseFactory);
        final StoreProcessor storeProcessor = new StoreProcessor(
                appendProcessor, mailboxManagerProvider, statusResponseFactory);
        final NoopProcessor noopProcessor = new NoopProcessor(storeProcessor,
                mailboxManagerProvider, statusResponseFactory);
        final StatusProcessor statusProcessor = new StatusProcessor(
                noopProcessor, mailboxManagerProvider, statusResponseFactory);
        final LSubProcessor lsubProcessor = new LSubProcessor(statusProcessor,
                mailboxManagerProvider, statusResponseFactory);
        final ListProcessor listProcessor = new ListProcessor(lsubProcessor,
                mailboxManagerProvider, statusResponseFactory);
        final SearchProcessor searchProcessor = new SearchProcessor(
                listProcessor, mailboxManagerProvider, statusResponseFactory);
        final SelectProcessor selectProcessor = new SelectProcessor(
                searchProcessor, mailboxManagerProvider, statusResponseFactory);
        final ImapProcessor result = new FetchProcessor(selectProcessor,
                mailboxManagerProvider, statusResponseFactory);
        return result;
    }
}
