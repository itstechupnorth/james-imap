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
import org.apache.james.imap.encode.SearchResponseEncoder;
import org.apache.james.imap.message.response.SearchResponse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class ListResponseEncoderTest {

    private static final long[] IDS = { 1, 4, 9, 16 };

    SearchResponse response;

    SearchResponseEncoder encoder;

    ImapEncoder mockNextEncoder;

    ImapResponseComposer composer;

    private Mockery context = new JUnit4Mockery();
    
    @Before
    public void setUp() throws Exception {
        mockNextEncoder = context.mock(ImapEncoder.class);
        composer = context.mock(ImapResponseComposer.class);
        response = new SearchResponse(IDS);
        encoder = new SearchResponseEncoder(mockNextEncoder);
    }

    @Test
    public void testIsAcceptable() {
        assertTrue(encoder.isAcceptable(response));
        assertFalse(encoder.isAcceptable(context.mock(ImapMessage.class)));
        assertFalse(encoder.isAcceptable(null));
    }

    @Test
    public void testEncode() throws Exception {
        context.checking(new Expectations() {{
            oneOf(composer).searchResponse(with(same(IDS)));
        }});
        encoder.encode(response, composer, new FakeImapSession());
    }
}
