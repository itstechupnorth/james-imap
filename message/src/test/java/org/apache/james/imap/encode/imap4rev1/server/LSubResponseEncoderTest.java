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

package org.apache.james.imap.encode.imap4rev1.server;

import java.util.Arrays;
import java.util.List;

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.encode.imap4rev1.server.LSubResponseEncoder;
import org.apache.james.imap.message.response.imap4rev1.server.LSubResponse;
import org.apache.james.imap.message.response.imap4rev1.server.ListResponse;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class LSubResponseEncoderTest extends MockObjectTestCase {

    LSubResponseEncoder encoder;

    ImapEncoder mockNextEncoder;

    ImapResponseComposer composer;

    protected void setUp() throws Exception {
        super.setUp();
        mockNextEncoder = mock(ImapEncoder.class);
        composer = mock(ImapResponseComposer.class);
        encoder = new LSubResponseEncoder(mockNextEncoder);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsAcceptable() {
        assertFalse(encoder.isAcceptable(new ListResponse(true, true, true,
                true, ".", "name")));
        assertTrue(encoder.isAcceptable(new LSubResponse("name", ".", true)));
        assertFalse(encoder.isAcceptable(mock(ImapMessage.class)));
        assertFalse(encoder.isAcceptable(null));
    }

    public void testName() throws Exception {
        checking(new Expectations() {{
            oneOf(composer).listResponse(with(same("LSUB")),with(aNull(List.class)), with(same(".")), with(same("INBOX.name")));
        }});
        encoder.encode(new LSubResponse("INBOX.name", ".", false), composer);
    }

    public void testDelimiter() throws Exception {
        checking(new Expectations() {{
            oneOf(composer).listResponse(with(same("LSUB")),with(aNull(List.class)), with(same("@")), with(same("INBOX.name")));
        }});
        encoder.encode(new LSubResponse("INBOX.name", "@", false), composer);
    }

    public void testNoDelimiter() throws Exception {
        checking(new Expectations() {{
            oneOf(composer).listResponse(with(same("LSUB")),with(aNull(List.class)), with(aNull(String.class)), with(same("INBOX.name")));
        }});
        encoder.encode(new LSubResponse("INBOX.name", null, false), composer);
    }

    public void testNoSelect() throws Exception {
        final String[] values = { ImapConstants.NAME_ATTRIBUTE_NOSELECT };
        checking(new Expectations() {{
            oneOf(composer).listResponse(with(same("LSUB")),with(equal(Arrays.asList(values))), with(equal(".")), with(same("INBOX.name")));
        }});
        encoder.encode(new LSubResponse("INBOX.name", ".", true), composer);
    }
}
