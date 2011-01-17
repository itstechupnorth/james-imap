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

import java.util.Arrays;
import java.util.List;

import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.message.response.ImapResponseComposer;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ListResponseEncoder;
import org.apache.james.imap.message.response.LSubResponse;
import org.apache.james.imap.message.response.ListResponse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class SearchResponseEncoderTest {

    ListResponseEncoder encoder;

    ImapEncoder mockNextEncoder;

    ImapResponseComposer composer;

    private Mockery context = new JUnit4Mockery();
    
    @Before
    public void setUp() throws Exception {
        mockNextEncoder = context.mock(ImapEncoder.class);
        composer = context.mock(ImapResponseComposer.class);
        encoder = new ListResponseEncoder(mockNextEncoder);
    }

    @Test
    public void testIsAcceptable() {
        assertTrue(encoder.isAcceptable(new ListResponse(true, true, true,
                true, false, false, "name", '.')));
        assertFalse(encoder.isAcceptable(new LSubResponse("name", true, '.')));
        assertFalse(encoder.isAcceptable(context.mock(ImapMessage.class)));
        assertFalse(encoder.isAcceptable(null));
    }

    @Test
    @SuppressWarnings("unchecked")
	public void testName() throws Exception {
        context.checking(new Expectations() {{
            oneOf(composer).listResponse(
                            with(equal("LIST")), 
                            with(aNull(List.class)), 
                            with(equal('.')), 
                            with(equal("INBOX.name")));
        }});
        encoder.encode(new ListResponse(false, false, false, false, false, false, "INBOX.name", '.'), composer, new FakeImapSession());
    }

    @Test
    @SuppressWarnings("unchecked")
	public void testDelimiter() throws Exception {
        context.checking(new Expectations() {{
            oneOf(composer).listResponse(
                            with(equal("LIST")), 
                            with(aNull(List.class)), 
                            with(equal('.')), 
                            with(equal("INBOX.name")));
        }});
        encoder.encode(new ListResponse(false, false, false, false, false, false, "INBOX.name", '.'), composer, new FakeImapSession());
    }


    @Test
    public void testAllAttributes() throws Exception {
        final String[] all = { ImapConstants.NAME_ATTRIBUTE_NOINFERIORS,
                ImapConstants.NAME_ATTRIBUTE_NOSELECT,
                ImapConstants.NAME_ATTRIBUTE_MARKED,
                ImapConstants.NAME_ATTRIBUTE_UNMARKED };
        context.checking(new Expectations() {{
            oneOf(composer).listResponse(
                            with(equal("LIST")), 
                            with(equal(Arrays.asList(all))), 
                            with(equal('.')), 
                            with(equal("INBOX.name")));
        }});
        encoder.encode(new ListResponse(true, true, true, true, false, false, "INBOX.name", '.'), composer, new FakeImapSession());
    }

    @Test
    public void testNoInferiors() throws Exception {
        final String[] values = { ImapConstants.NAME_ATTRIBUTE_NOINFERIORS };
        context.checking(new Expectations() {{
            oneOf(composer).listResponse(
                            with(equal("LIST")), 
                            with(equal(Arrays.asList(values))), 
                            with(equal('.')), 
                            with(equal("INBOX.name")));
        }});
        encoder.encode(new ListResponse(true, false, false, false, false, false, "INBOX.name", '.'), composer, new FakeImapSession());
    }

    @Test
    public void testNoSelect() throws Exception {
        final String[] values = { ImapConstants.NAME_ATTRIBUTE_NOSELECT };
        context.checking(new Expectations() {{
            oneOf(composer).listResponse(
                            with(equal("LIST")), 
                            with(equal(Arrays.asList(values))), 
                            with(equal('.')), 
                            with(equal("INBOX.name")));
        }});
        encoder.encode(new ListResponse(false, true, false, false, false, false, "INBOX.name", '.'), composer, new FakeImapSession());
    }

    @Test
    public void testMarked() throws Exception {
        final String[] values = { ImapConstants.NAME_ATTRIBUTE_MARKED };
        context.checking(new Expectations() {{
            oneOf(composer).listResponse(
                            with(equal("LIST")), 
                            with(equal(Arrays.asList(values))), 
                            with(equal('.')), 
                            with(equal("INBOX.name")));
        }});
        encoder.encode(new ListResponse(false, false, true, false, false, false, "INBOX.name", '.'), composer, new FakeImapSession());
    }

    @Test
    public void testUnmarked() throws Exception {
        final String[] values = { ImapConstants.NAME_ATTRIBUTE_UNMARKED };
        context.checking(new Expectations() {{
            oneOf(composer).listResponse(
                            with(equal("LIST")), 
                            with(equal(Arrays.asList(values))), 
                            with(equal('.')), 
                            with(equal("INBOX.name")));
        }});
        encoder.encode(new ListResponse(false, false, false, true, false, false, "INBOX.name", '.'), composer, new FakeImapSession());
    }
}
