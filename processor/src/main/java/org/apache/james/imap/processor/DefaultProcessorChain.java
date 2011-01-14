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


import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.processor.fetch.FetchProcessor;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.SubscriptionManager;

/**
 * TODO: perhaps this should be a POJO
 */
public class DefaultProcessorChain {

    public static final ImapProcessor createDefaultChain(
            final ImapProcessor chainEndProcessor,
            final MailboxManager mailboxManager,
            final SubscriptionManager subscriptionManager,
            final StatusResponseFactory statusResponseFactory) {
        return createDefaultChain(chainEndProcessor, mailboxManager, subscriptionManager, statusResponseFactory, 100);
        
    }  
    
    public static final ImapProcessor createDefaultChain(
            final ImapProcessor chainEndProcessor,
            final MailboxManager mailboxManager,
            final SubscriptionManager subscriptionManager,
            final StatusResponseFactory statusResponseFactory, int batchSize) {
        final SystemMessageProcessor systemProcessor = new SystemMessageProcessor(chainEndProcessor, mailboxManager);
        final LogoutProcessor logoutProcessor = new LogoutProcessor(
                systemProcessor, mailboxManager, statusResponseFactory);
        
        final CapabilityProcessor capabilityProcessor = new CapabilityProcessor(
                logoutProcessor, mailboxManager, statusResponseFactory);
        final CheckProcessor checkProcessor = new CheckProcessor(
                capabilityProcessor, mailboxManager, statusResponseFactory);
        final LoginProcessor loginProcessor = new LoginProcessor(
                checkProcessor, mailboxManager, statusResponseFactory);
        final RenameProcessor renameProcessor = new RenameProcessor(
                loginProcessor, mailboxManager, statusResponseFactory);
        final DeleteProcessor deleteProcessor = new DeleteProcessor(
                renameProcessor, mailboxManager, statusResponseFactory);
        final CreateProcessor createProcessor = new CreateProcessor(
                deleteProcessor, mailboxManager, statusResponseFactory);
        final CloseProcessor closeProcessor = new CloseProcessor(
                createProcessor, mailboxManager, statusResponseFactory);
        final UnsubscribeProcessor unsubscribeProcessor = new UnsubscribeProcessor(
                closeProcessor, mailboxManager, subscriptionManager, statusResponseFactory);
        final SubscribeProcessor subscribeProcessor = new SubscribeProcessor(
                unsubscribeProcessor, mailboxManager,
                subscriptionManager, statusResponseFactory);
        final CopyProcessor copyProcessor = new CopyProcessor(
                subscribeProcessor, mailboxManager,
                statusResponseFactory);
        final AuthenticateProcessor authenticateProcessor = new AuthenticateProcessor(
                copyProcessor, mailboxManager, statusResponseFactory);
        final ExpungeProcessor expungeProcessor = new ExpungeProcessor(
                authenticateProcessor, mailboxManager,
                statusResponseFactory);
        final ExamineProcessor examineProcessor = new ExamineProcessor(
                expungeProcessor, mailboxManager, statusResponseFactory);
        final AppendProcessor appendProcessor = new AppendProcessor(
                examineProcessor, mailboxManager, statusResponseFactory);
        final StoreProcessor storeProcessor = new StoreProcessor(
                appendProcessor, mailboxManager, statusResponseFactory);
        final NoopProcessor noopProcessor = new NoopProcessor(storeProcessor,
                mailboxManager, statusResponseFactory);
        final IdleProcessor idleProcessor = new IdleProcessor(noopProcessor,
                mailboxManager, statusResponseFactory);
        final StatusProcessor statusProcessor = new StatusProcessor(
                idleProcessor, mailboxManager, statusResponseFactory);
        final LSubProcessor lsubProcessor = new LSubProcessor(statusProcessor,
                mailboxManager, subscriptionManager, statusResponseFactory);
        final ListProcessor listProcessor = new ListProcessor(lsubProcessor,
                mailboxManager, statusResponseFactory);
        final SearchProcessor searchProcessor = new SearchProcessor(
                listProcessor, mailboxManager, statusResponseFactory);
        final SelectProcessor selectProcessor = new SelectProcessor(
                searchProcessor, mailboxManager, statusResponseFactory);
        final NamespaceProcessor namespaceProcessor = new NamespaceProcessor(
                selectProcessor, mailboxManager, statusResponseFactory);
        
        
        final ImapProcessor fetchProcessor = new FetchProcessor(namespaceProcessor,
                mailboxManager, statusResponseFactory, batchSize);
        final StartTLSProcessor startTLSProcessor = new StartTLSProcessor(fetchProcessor, statusResponseFactory);
        
        final UnselectProcessor unselectProcessor = new UnselectProcessor(startTLSProcessor, mailboxManager, statusResponseFactory);
        
        final ContinuationProcessor contProcessor = new ContinuationProcessor(unselectProcessor);
        
        capabilityProcessor.addProcessor(startTLSProcessor);
        capabilityProcessor.addProcessor(idleProcessor);
        capabilityProcessor.addProcessor(namespaceProcessor);
        // added to announce UIDPLUS support
        capabilityProcessor.addProcessor(expungeProcessor);
        
        // announce the UNSELECT extension. See RFC3691
        capabilityProcessor.addProcessor(unselectProcessor);

        return contProcessor;
        
    }  
        
}
