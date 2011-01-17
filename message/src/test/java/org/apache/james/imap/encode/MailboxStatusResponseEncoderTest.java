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

package org.apache.james.imap.encode;

import static org.junit.Assert.*;

import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.message.response.ImapResponseComposer;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.MailboxStatusResponseEncoder;
import org.apache.james.imap.message.response.MailboxStatusResponse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class MailboxStatusResponseEncoderTest  {

    MailboxStatusResponseEncoder encoder;

    ImapEncoder mockNextEncoder;

    ImapResponseComposer composer;

    private Mockery context = new JUnit4Mockery();
    
    @Before
    public void setUp() throws Exception {
        mockNextEncoder = context.mock(ImapEncoder.class);
        composer = context.mock(ImapResponseComposer.class);
        encoder = new MailboxStatusResponseEncoder(mockNextEncoder);
    }
    

    @Test
    public void testIsAcceptable() throws Exception {
        assertTrue(encoder.isAcceptable(new MailboxStatusResponse(null, null, null,
                null, null, "mailbox")));
        assertFalse(encoder.isAcceptable(context.mock(ImapMessage.class)));
        assertFalse(encoder.isAcceptable(null));
    }

    @Test
    public void testDoEncode() throws Exception {
        final Long messages = new Long(2);
        final Long recent = new Long(3);
        final Long uidNext = new Long(5);
        final Long uidValidity = new Long(7);
        final Long unseen = new Long(11);
        final String mailbox = "A mailbox named desire";
        context.checking(new Expectations() {{
            oneOf(composer).statusResponse(
                    with(same(messages)), 
                    with(same(recent)), 
                    with(same(uidNext)),
                    with(same(uidValidity)), 
                    with(same(unseen)), 
                    with(same(mailbox))
                    );
        }});

        encoder.encode(new MailboxStatusResponse(messages, recent, uidNext,
                uidValidity, unseen, mailbox), composer, new FakeImapSession());
    }
}
