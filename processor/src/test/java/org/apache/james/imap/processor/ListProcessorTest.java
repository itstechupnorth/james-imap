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

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxMetaData;
import org.apache.james.imap.message.response.ListResponse;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class ListProcessorTest extends MockObjectTestCase {

    ListProcessor processor;

    ImapProcessor next;

    MailboxManager manager;

    ImapProcessor.Responder responder;

    MailboxMetaData result;

    ImapSession session;

    ImapCommand command;

    StatusResponseFactory serverResponseFactory;

    protected void setUp() throws Exception {
        serverResponseFactory = mock(StatusResponseFactory.class);
        session = mock(ImapSession.class);
        command = ImapCommand.anyStateCommand("Command");
        next = mock(ImapProcessor.class);
        responder = mock(ImapProcessor.Responder.class);
        result = mock(MailboxMetaData.class);
        manager = mock(MailboxManager.class);
        processor = createProcessor(next, manager, serverResponseFactory);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    ListProcessor createProcessor(ImapProcessor next,
            MailboxManager manager, StatusResponseFactory factory) {
        return new ListProcessor(next, manager, factory);
    }

    ListResponse createResponse(boolean noinferior, boolean noselect,
            boolean marked, boolean unmarked, boolean hasChildren,
            boolean hasNoChildren, String hierarchyDelimiter, String mailboxName) {
        return new ListResponse(noinferior, noselect, marked, unmarked,
                hasChildren, hasNoChildren, hierarchyDelimiter, mailboxName);
    }

    void setUpResult(final MailboxMetaData.Children children, final MailboxMetaData.Selectability selectability,
            final String hierarchyDelimiter, final String name) {
        checking(new Expectations() {{
            oneOf(result).inferiors();will(returnValue(children));
            oneOf(result).getSelectability();will(returnValue(selectability));
            oneOf(result).getHierarchyDelimiter();will(returnValue(hierarchyDelimiter));
            oneOf(result).getName();will(returnValue(name));
        }});
    }
    
    public void testHasChildren() throws Exception {
        setUpResult(MailboxMetaData.Children.HAS_CHILDREN, MailboxMetaData.Selectability.NONE, ".", "#INBOX");
        checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, false, false, false, true, false, ".", "#INBOX"))));
        }});
        processor.processResult(responder, false, 0, result);
    }

    public void testHasNoChildren() throws Exception {
        setUpResult(MailboxMetaData.Children.HAS_NO_CHILDREN, MailboxMetaData.Selectability.NONE, ".", "#INBOX");
        checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, false, false, false, false, true, ".", "#INBOX"))));
        }});
        processor.processResult(responder, false, 0, result);
    }
    
    public void testNoInferiors() throws Exception {
        setUpResult(MailboxMetaData.Children.NO_INFERIORS, MailboxMetaData.Selectability.NONE, ".", "#INBOX");
        checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(true, false, false, false, false, false, ".", "#INBOX"))));
        }});
        processor.processResult(responder, false, 0, result);
    }

    public void testNoSelect() throws Exception {
        setUpResult(MailboxMetaData.Children.CHILDREN_ALLOWED_BUT_UNKNOWN, MailboxMetaData.Selectability.NOSELECT, ".", "#INBOX");
        checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, true, false, false, false, false, ".", "#INBOX"))));
        }});
        processor.processResult(responder, false, 0, result);
    }

    public void testUnMarked() throws Exception {
        setUpResult(MailboxMetaData.Children.CHILDREN_ALLOWED_BUT_UNKNOWN, MailboxMetaData.Selectability.UNMARKED, ".",
                "#INBOX");
        checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, false, false, true, false, false, ".", "#INBOX"))));
        }});
        processor.processResult(responder, false, 0, result);
    }

    public void testMarked() throws Exception {
        setUpResult(MailboxMetaData.Children.CHILDREN_ALLOWED_BUT_UNKNOWN, MailboxMetaData.Selectability.MARKED, ".", "#INBOX");
        checking(new Expectations() {{
            oneOf(responder).respond(with(equal(createResponse(false, false, true, false, false, false, ".", "#INBOX"))));
        }});
        processor.processResult(responder, false, 0, result);
    }
}
