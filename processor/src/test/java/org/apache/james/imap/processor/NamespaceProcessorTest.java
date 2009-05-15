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
import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.ImapProcessor.Responder;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.message.request.NamespaceRequest;
import org.apache.james.imap.message.response.NamespaceResponse;
import org.apache.james.imap.processor.base.ImapSessionUtils;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class NamespaceProcessorTest extends MockObjectTestCase {

    private static final char SHARED_SPACE_DELIMINATOR = '&';
    private static final String SHARED_PREFIX = "SharedPrefix";
    private static final String USERS_PREFIX = "UsersPrefix";
    private static final String PERSONAL_PREFIX = "PersonalPrefix";
    private static final char USERS_DELIMINATOR = '%';
    private static final char PERSONAL_DELIMINATOR = '$';
    
    
    NamespaceProcessor subject;
    StatusResponseFactory statusResponseStub;
    ImapSession imapSessionStub;
    MailboxSession mailboxSessionStub;
    MailboxSession.Namespace personalSpaceStub;
    MailboxSession.Namespace usersSpaceStub;
    MailboxSession.Namespace sharedSpaceStub;
    NamespaceRequest namespaceRequest;
    Collection<MailboxSession.Namespace> sharedSpaces;
    
    protected void setUp() throws Exception {
        super.setUp();
        sharedSpaces = new ArrayList<MailboxSession.Namespace>();
        statusResponseStub = mock(StatusResponseFactory.class);
        final MailboxManagerProvider mailboxManagerStub = mock(MailboxManagerProvider.class);
        subject = new NamespaceProcessor(mock(ImapProcessor.class), mailboxManagerStub, statusResponseStub);
        imapSessionStub = mock(ImapSession.class);
        mailboxSessionStub = mock(MailboxSession.class);
        personalSpaceStub = mock(MailboxSession.Namespace.class, "PersonalNamespace");
        usersSpaceStub = mock(MailboxSession.Namespace.class, "UsersNamespace");
        sharedSpaceStub = mock(MailboxSession.Namespace.class, "SharedNamespace");
     
        namespaceRequest = new NamespaceRequest(ImapCommand.anyStateCommand("Name"), "TAG");
        
        checking (new Expectations() {{
            allowing(imapSessionStub).getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY); will(returnValue(mailboxSessionStub));
            allowing(personalSpaceStub).getDeliminator(); will(returnValue(PERSONAL_DELIMINATOR));
            allowing(personalSpaceStub).getPrefix(); will(returnValue(PERSONAL_PREFIX));
            allowing(usersSpaceStub).getDeliminator(); will(returnValue(USERS_DELIMINATOR));
            allowing(usersSpaceStub).getPrefix(); will(returnValue(USERS_PREFIX));
            allowing(sharedSpaceStub).getDeliminator(); will(returnValue(SHARED_SPACE_DELIMINATOR));
            allowing(sharedSpaceStub).getPrefix(); will(returnValue(SHARED_PREFIX));
            allowing(mailboxSessionStub).getPersonalSpace(); will(returnValue(personalSpaceStub));
            allowing(mailboxSessionStub).getOtherUsersSpace(); will(returnValue(usersSpaceStub));
            allowing(mailboxSessionStub).getSharedSpaces();will(returnValue(sharedSpaces));
            allowing(imapSessionStub).getState();will(returnValue(ImapSessionState.AUTHENTICATED));
            allowing(statusResponseStub).taggedOk(
                    with(any(String.class)), with(any(ImapCommand.class)), 
                    with(any(HumanReadableText.class)), with(any(ResponseCode.class))); will(returnValue(mock(StatusResponse.class)));
            ignoring(imapSessionStub);
            ignoring(mailboxSessionStub);
            ignoring(mailboxManagerStub);
            ignoring(statusResponseStub);
        }});
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testShouldAcceptNamespaceRequests() throws Exception {
        assertFalse(subject.isAcceptable(mock(ImapMessage.class)));
        assertTrue(subject.isAcceptable(namespaceRequest));
    }
    
    public void testNamespaceResponseShouldContainPersonalAndUserSpaces() throws Exception {
        final NamespaceResponse response = buildResponse(null);
        
        final Responder responderMock = expectResponse(response);
        
        subject.doProcess(namespaceRequest, responderMock, imapSessionStub);
    }
    
    public void testNamespaceResponseShouldContainSharedSpaces() throws Exception {
        this.sharedSpaces.add(sharedSpaceStub);
        
        
        final List<NamespaceResponse.Namespace> sharedSpaces = new ArrayList<NamespaceResponse.Namespace>();
        sharedSpaces.add(new NamespaceResponse.Namespace(SHARED_PREFIX, SHARED_SPACE_DELIMINATOR));
        final NamespaceResponse response = buildResponse(sharedSpaces);
        
        final Responder responderMock = expectResponse(response);
        
        subject.doProcess(namespaceRequest, responderMock, imapSessionStub);
    }

    private NamespaceResponse buildResponse(final List<NamespaceResponse.Namespace> sharedSpaces) {
        final List<NamespaceResponse.Namespace> personalSpaces = new ArrayList<NamespaceResponse.Namespace>();
        personalSpaces.add(new NamespaceResponse.Namespace(PERSONAL_PREFIX, PERSONAL_DELIMINATOR));
        final List<NamespaceResponse.Namespace> otherUsersSpaces = new ArrayList<NamespaceResponse.Namespace>();
        otherUsersSpaces.add(new NamespaceResponse.Namespace(USERS_PREFIX, USERS_DELIMINATOR)); 
        
        final NamespaceResponse response = new NamespaceResponse(personalSpaces, otherUsersSpaces, sharedSpaces);
        return response;
    }

    private Responder expectResponse(final NamespaceResponse response) {
        final Responder responderMock = mock(Responder.class);
        checking(new Expectations(){{
            oneOf(responderMock).respond(with(equal(response)));
            oneOf(responderMock).respond(with(any(StatusResponse.class)));
        }});
        return responderMock;
    }
}

