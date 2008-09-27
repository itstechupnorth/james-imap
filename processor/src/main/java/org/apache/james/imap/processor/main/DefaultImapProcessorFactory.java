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

package org.apache.james.imap.processor.main;

import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapProcessorFactory;
import org.apache.james.imap.message.response.imap4rev1.status.UnpooledStatusResponseFactory;
import org.apache.james.imap.processor.base.ImapResponseMessageProcessor;
import org.apache.james.imap.processor.base.UnknownRequestImapProcessor;
import org.apache.james.imap.processor.imap4rev1.Imap4Rev1ProcessorFactory;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

/**
 * 
 */
public class DefaultImapProcessorFactory implements ImapProcessorFactory {

    public static final ImapProcessor createDefaultProcessor(
            final MailboxManagerProvider mailboxManagerProvider) {
        final StatusResponseFactory statusResponseFactory = new UnpooledStatusResponseFactory();
        final UnknownRequestImapProcessor unknownRequestImapProcessor = new UnknownRequestImapProcessor(
                statusResponseFactory);
        final ImapProcessor imap4rev1Chain = Imap4Rev1ProcessorFactory
                .createDefaultChain(unknownRequestImapProcessor,
                        mailboxManagerProvider, statusResponseFactory);
        final ImapProcessor result = new ImapResponseMessageProcessor(
                imap4rev1Chain);
        return result;
    }

    private MailboxManagerProvider mailboxManagerProvider;

    public final void configure(MailboxManagerProvider mailboxManagerProvider) {
        setMailboxManagerProvider(mailboxManagerProvider);
    }

    public final MailboxManagerProvider getMailboxManagerProvider() {
        return mailboxManagerProvider;
    }

    public final void setMailboxManagerProvider(
            MailboxManagerProvider mailboxManagerProvider) {
        this.mailboxManagerProvider = mailboxManagerProvider;
    }

    public ImapProcessor buildImapProcessor() {
        return createDefaultProcessor(mailboxManagerProvider);
    }
}