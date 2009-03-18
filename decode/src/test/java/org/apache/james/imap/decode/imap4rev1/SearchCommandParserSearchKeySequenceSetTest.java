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

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.imap.api.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.request.SearchKey;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class SearchCommandParserSearchKeySequenceSetTest extends MockObjectTestCase {

    SearchCommandParser parser;

    Imap4Rev1CommandFactory mockCommandFactory;

    Imap4Rev1MessageFactory mockMessageFactory;

    ImapCommand command;

    ImapMessage message;

    protected void setUp() throws Exception {
        super.setUp();
        parser = new SearchCommandParser();
        mockCommandFactory = mock(Imap4Rev1CommandFactory.class);
        checking(new Expectations() {{
            oneOf (mockCommandFactory).getSearch();
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

    public void testAllNumbers() throws Exception {

        IdRange[] range = { new IdRange(2), new IdRange(4), new IdRange(9),
                new IdRange(16), new IdRange(25), new IdRange(36),
                new IdRange(49), new IdRange(64), new IdRange(81),
                new IdRange(100) };
        check("2,4,9,16,25,36,49,64,81,100", range);
    }

    public void testEndStar() throws Exception {
        IdRange[] range = { new IdRange(8), new IdRange(9, 10),
                new IdRange(17), new IdRange(100, Long.MAX_VALUE) };
        check("8,9:10,17,100:*", range);
    }

    public void testStartStar() throws Exception {
        IdRange[] range = { new IdRange(Long.MAX_VALUE, 9), new IdRange(15),
                new IdRange(799, 820) };
        check("*:9,15,799:820", range);
    }

    private void check(String sequence, IdRange[] range) throws Exception {
        checkUid(sequence, range);
        checkSequence(sequence, range);
    }

    private void checkUid(String sequence, IdRange[] range) throws Exception {
        SearchKey key = SearchKey.buildUidSet(range);
        checkValid("UID " + sequence, key);
        checkValid("uid " + sequence, key);
        checkValid("Uid " + sequence, key);
    }

    private void checkSequence(String sequence, IdRange[] range)
            throws Exception {
        SearchKey key = SearchKey.buildSequenceSet(range);
        checkValid(sequence, key);
        checkValid(sequence, key);
        checkValid(sequence, key);
    }

    private void checkValid(String input, final SearchKey key) throws Exception {
        input = input + "\r\n";
        ImapRequestLineReader reader = new ImapRequestLineReader(
                new ByteArrayInputStream(input.getBytes("US-ASCII")),
                new ByteArrayOutputStream());

        final SearchKey searchKey = parser.searchKey(reader, null, false);
        assertEquals(key, searchKey);
    }
}
