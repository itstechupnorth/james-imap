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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.display.HumanReadableTextKey;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.mailbox.MailboxMetaData;
import org.apache.james.imap.message.request.LsubRequest;
import org.apache.james.imap.message.response.LSubResponse;
import org.apache.james.imap.processor.LSubProcessor;
import org.apache.james.imap.processor.base.ImapSessionUtils;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class LSubProcessorTest extends MockObjectTestCase {

    private static final String ROOT = "ROOT";

    private static final String PARENT = ROOT
            + ImapConstants.HIERARCHY_DELIMITER + "PARENT";

    private static final String CHILD_ONE = PARENT
            + ImapConstants.HIERARCHY_DELIMITER + "CHILD_ONE";

    private static final String CHILD_TWO = PARENT
            + ImapConstants.HIERARCHY_DELIMITER + "CHILD_TWO";

    private static final String MAILBOX_C = "C.MAILBOX";

    private static final String MAILBOX_B = "B.MAILBOX";

    private static final String MAILBOX_A = "A.MAILBOX";

    private static final String USER = "A User";

    private static final String TAG = "TAG";

    LSubProcessor processor;

    ImapProcessor next;

    MailboxManagerProvider provider;

    MailboxManager manager;

    ImapProcessor.Responder responder;

    MailboxMetaData result;

    ImapSession session;

    StatusResponseFactory serverResponseFactory;

    StatusResponse statusResponse;

    Collection<String> subscriptions;

    ImapCommand command;

    private ImapProcessor.Responder responderImpl;

    protected void setUp() throws Exception {
        subscriptions = new ArrayList<String>();
        serverResponseFactory = mock(StatusResponseFactory.class);
        session = mock(ImapSession.class);
        command = mock(ImapCommand.class);
        next = mock(ImapProcessor.class);
        responder = mock(ImapProcessor.Responder.class);
        result = mock(MailboxMetaData.class);
        provider = mock(MailboxManagerProvider.class);
        statusResponse = mock(StatusResponse.class);
        responderImpl = responder;
        manager = mock(MailboxManager.class);
        processor = new LSubProcessor(next, provider, serverResponseFactory);
        checking(new Expectations() {{
            atMost(1).of(provider).getMailboxManager(); will(returnValue(manager));
        }});
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHierarchy() throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        checking(new Expectations() {{
            oneOf(responder).respond(with(
                    equal(new LSubResponse("",
                            ImapConstants.HIERARCHY_DELIMITER, true))));
        }});

        expectOk();

        LsubRequest request = new LsubRequest(command, "", "", TAG);
        processor.doProcess(request, session, TAG, command, responderImpl);

    }

    public void testShouldRespondToRegexWithSubscribedMailboxes()
            throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        subscriptions.add(CHILD_ONE);
        subscriptions.add(CHILD_TWO);

        checking(new Expectations() {{
            oneOf(responder).respond(with(
                    equal(new LSubResponse(CHILD_ONE,
                            ImapConstants.HIERARCHY_DELIMITER, false))));
            oneOf(responder).respond(with(
                    equal(new LSubResponse(CHILD_TWO,
                                    ImapConstants.HIERARCHY_DELIMITER, false))));
        }});

        expectSubscriptions();
        expectOk();

        LsubRequest request = new LsubRequest(command, "", PARENT
                + ImapConstants.HIERARCHY_DELIMITER + "%", TAG);
        processor.doProcess(request, session, TAG, command, responderImpl);

    }

    public void testShouldRespondNoSelectToRegexWithParentsOfSubscribedMailboxes()
            throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        subscriptions.add(CHILD_ONE);
        subscriptions.add(CHILD_TWO);

        checking(new Expectations() {{
            oneOf(responder).respond(with(
                    equal(new LSubResponse(PARENT, ImapConstants.HIERARCHY_DELIMITER, true))));
        }});

        expectSubscriptions();
        expectOk();

        LsubRequest request = new LsubRequest(command, "", ROOT
                + ImapConstants.HIERARCHY_DELIMITER + "%", TAG);
        processor.doProcess(request, session, TAG, command, responderImpl);

    }

    public void testShouldRespondSelectToRegexWithParentOfSubscribedMailboxesWhenParentSubscribed()
            throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        subscriptions.add(PARENT);
        subscriptions.add(CHILD_ONE);
        subscriptions.add(CHILD_TWO);

        checking(new Expectations() {{
            oneOf(responder).respond(with(
                    equal(new LSubResponse(PARENT, ImapConstants.HIERARCHY_DELIMITER,
                            false))));
        }});

        expectSubscriptions();
        expectOk();

        LsubRequest request = new LsubRequest(command, "", ROOT
                + ImapConstants.HIERARCHY_DELIMITER + "%", TAG);
        processor.doProcess(request, session, TAG, command, responderImpl);

    }

    public void testSelectAll() throws Exception {
        checking(new Expectations() {{
            oneOf(responder).respond(with(equal(
                    new LSubResponse(MAILBOX_A, ImapConstants.HIERARCHY_DELIMITER, false))));
            oneOf(responder).respond(with(equal(
                    new LSubResponse(MAILBOX_B,
                            ImapConstants.HIERARCHY_DELIMITER, false))));
            oneOf(responder).respond(with(equal(
                    new LSubResponse(MAILBOX_C,
                            ImapConstants.HIERARCHY_DELIMITER, false))));
        }});
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);

        expectSubscriptions();
        expectOk();

        LsubRequest request = new LsubRequest(command, "", "*", TAG);
        processor.doProcess(request, session, TAG, command, responderImpl);

    }

    private void expectOk() {
        checking(new Expectations() {{
            oneOf(serverResponseFactory).taggedOk(
                    with(equal(TAG)),
                    with(same(command)),
                    with(equal(HumanReadableTextKey.COMPLETED)));will(returnValue(statusResponse));
            oneOf(responder).respond(with(same(statusResponse)));          
        }});
    }

    private void expectSubscriptions() throws Exception {
        checking(new Expectations() {{
            oneOf(session).getAttribute(
                    with(equal(ImapSessionUtils.MAILBOX_USER_ATTRIBUTE_SESSION_KEY)));
                    will(returnValue(USER));
            oneOf(manager).subscriptions(with(same(USER)));will(returnValue(subscriptions));     
        }});
    }
}
