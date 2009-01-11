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

package org.apache.james.imap.decode.imap4rev1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.BodyFetchElement;
import org.apache.james.api.imap.message.FetchData;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.decode.ProtocolException;
import org.apache.james.imap.decode.imap4rev1.FetchCommandParser;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class FetchCommandParserPartialFetchTest extends MockObjectTestCase {

    FetchCommandParser parser;

    Imap4Rev1CommandFactory mockCommandFactory;

    Imap4Rev1MessageFactory mockMessageFactory;
    ImapCommand command;

    ImapMessage message;

    protected void setUp() throws Exception {
        super.setUp();
        parser = new FetchCommandParser();
        mockCommandFactory = mock(Imap4Rev1CommandFactory.class);
        checking(new Expectations() {{
            oneOf (mockCommandFactory).getFetch();
        }});
        mockMessageFactory = mock(Imap4Rev1MessageFactory.class);
        command = mock(ImapCommand.class);
        message = mock(ImapMessage.class);
        parser.init(mockCommandFactory);
        parser.setMessageFactory(mockMessageFactory);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldParseZeroAndLength() throws Exception {
        IdRange[] ranges = { new IdRange(1) };
        FetchData data = new FetchData();
        data.add(new BodyFetchElement("BODY[]", BodyFetchElement.CONTENT, null,
                null, new Long(0), new Long(100)), false);
        check("1 (BODY[]<0.100>)\r\n", ranges, false, data, "A01");
    }

    public void testShouldParseNonZeroAndLength() throws Exception {
        IdRange[] ranges = { new IdRange(1) };
        FetchData data = new FetchData();
        data.add(new BodyFetchElement("BODY[]", BodyFetchElement.CONTENT, null,
                null, new Long(20), new Long(12342348)), false);
        check("1 (BODY[]<20.12342348>)\r\n", ranges, false, data, "A01");
    }

    public void testShouldNotParseZeroLength() throws Exception {
        try {
            ImapRequestLineReader reader = new ImapRequestLineReader(
                    new ByteArrayInputStream("1 (BODY[]<20.0>)\r\n"
                            .getBytes("US-ASCII")), new ByteArrayOutputStream());
            parser.decode(command, reader, "A01", false);
            fail("Number of octets must be non-zero");
        } catch (ProtocolException e) {
            // expected
        }
    }

    private void check(String input, final IdRange[] idSet,
            final boolean useUids, final FetchData data, final String tag) throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(
                new ByteArrayInputStream(input.getBytes("US-ASCII")),
                new ByteArrayOutputStream());
        checking(new Expectations() {{
            oneOf (mockMessageFactory).createFetchMessage( with(equal(command)), with(equal(useUids)), 
                    with(equal(idSet)),with(equal(data)), with(same(tag)));will(returnValue(message));
        }});
        parser.decode(command, reader, tag, useUids);
    }
}
