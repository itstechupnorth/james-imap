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

package org.apache.james.imap.decode.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.james.imap.api.DecodingException;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapMessageCallback;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.request.SearchKey;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.decode.ImapRequestLine;
import org.apache.james.imap.decode.MockImapMessageCallback;
import org.apache.james.imap.encode.MockImapResponseComposer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class SearchCommandParserQuotedCharsetTest {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final Charset ASCII = Charset.forName("US-ASCII");

    private static final String TAG = "A1";

    private static final String ASCII_SEARCH_TERM = "A Search Term";

    private static final String NON_ASCII_SEARCH_TERM = "\u043A\u0430\u043A \u0414\u0435\u043B\u0430?";

    private static final String LENGTHY_NON_ASCII_SEARCH_TERM = NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM
            + NON_ASCII_SEARCH_TERM;

    private static final byte[] BYTES_LENGTHY_NON_ASCII_SEARCH_TERM = NioUtils
            .toBytes(LENGTHY_NON_ASCII_SEARCH_TERM, UTF8);

    private static final byte[] BYTES_NON_ASCII_SEARCH_TERM = NioUtils.toBytes(
            NON_ASCII_SEARCH_TERM, UTF8);

    private static final byte[] BYTES_QUOTED_UTF8_LENGTHY_NON_ASCII_SEARCH_TERM = add(
            add(NioUtils.toBytes(" \"", ASCII),
                    BYTES_LENGTHY_NON_ASCII_SEARCH_TERM), NioUtils.toBytes(
                    "\"", ASCII));

    private static final byte[] BYTES_QUOTED_UTF8_NON_ASCII_SEARCH_TERM = add(
            add(NioUtils.toBytes(" \"", ASCII), BYTES_NON_ASCII_SEARCH_TERM),
            NioUtils.toBytes("\"", ASCII));

    private static final byte[] BYTES_UTF8_NON_ASCII_SEARCH_TERM = add(NioUtils
            .toBytes(" {16}", ASCII), BYTES_NON_ASCII_SEARCH_TERM);

    private static final byte[] CHARSET = NioUtils.toBytes("CHARSET UTF-8 ",
            ASCII);

    private static final byte[] add(byte[] one, byte[] two) {
        byte[] results = new byte[one.length + two.length];
        System.arraycopy(one, 0, results, 0, one.length);
        System.arraycopy(two, 0, results, one.length, two.length);
        return results;
    }

    SearchCommandParser parser;

    StatusResponseFactory mockStatusResponseFactory;

    ImapCommand command;

    ImapMessage message;

    private Mockery mockery = new JUnit4Mockery();

	private ImapSession session;
    
    @Before
    public void setUp() throws Exception {
        parser = new SearchCommandParser();
        command = ImapCommand.anyStateCommand("Command");
        message = mockery.mock(ImapMessage.class);
        mockStatusResponseFactory = mockery.mock(StatusResponseFactory.class);
        session = mockery.mock(ImapSession.class);

        parser.setStatusResponseFactory(mockStatusResponseFactory);
    }

    @Test
    public void testShouldDecoderLengthyQuotedCharset() throws Exception {
        SearchKey key = SearchKey.buildBcc(LENGTHY_NON_ASCII_SEARCH_TERM);
        ImapRequestLine reader = new ImapRequestLine(
                (add(add(CHARSET, "BCC"
                        .getBytes("US-ASCII")),
                        BYTES_QUOTED_UTF8_LENGTHY_NON_ASCII_SEARCH_TERM)),
                new MockImapResponseComposer());
        final SearchKey searchKey = parser.searchKey(reader, null, true);
        assertEquals(key, searchKey);
    }

    @Test
    public void testShouldDecoderQuotedCharset() throws Exception {
        SearchKey key = SearchKey.buildBcc(NON_ASCII_SEARCH_TERM);
        ImapRequestLine reader = new ImapRequestLine(
                (add(add(CHARSET, "BCC"
                        .getBytes("US-ASCII")),
                        BYTES_QUOTED_UTF8_NON_ASCII_SEARCH_TERM)),
                new MockImapResponseComposer());
        final SearchKey searchKey = parser.searchKey(reader, null, true);
        assertEquals(key, searchKey);
    }

    @Test
    public void testBadCharset() throws Exception {
        final Collection<String> charsetNames = new HashSet<String>();
        for (final Iterator<Charset> it = Charset.availableCharsets().values()
                .iterator(); it.hasNext();) {
            final Charset charset = it.next();
            final Set<String> aliases = charset.aliases();
            charsetNames.addAll(aliases);
        }
        mockery.checking(new Expectations() {{
            oneOf (mockStatusResponseFactory).taggedNo(
                    with(equal(TAG)), 
                    with(same(command)), 
                    with(equal(HumanReadableText.BAD_CHARSET)),
                    with(equal(StatusResponse.ResponseCode.badCharset(charsetNames))));
            oneOf(session).getLog(); returnValue(new MockLogger());
        }});
        ImapRequestLine reader = new ImapRequestLine("CHARSET BOGUS ".getBytes("US-ASCII"),
                new MockImapResponseComposer());
        parser.decode(command, reader, TAG, false, session, new ImapMessageCallback() {
            
            public void onMessage(ImapMessage message) {                
            }
            
            public void onException(DecodingException ex) {
                fail();
            }
        });
    }

    @Test
    public void testShouldThrowProtocolExceptionWhenBytesAreNotEncodedByCharset() throws Exception {
        MockImapMessageCallback callback = new MockImapMessageCallback();
        ImapRequestLine reader = new ImapRequestLine(add("CHARSET US-ASCII BCC ".getBytes("US-ASCII"), BYTES_NON_ASCII_SEARCH_TERM), new MockImapResponseComposer());

        parser.decode(command, reader, TAG, false, session, callback);
        assertNull("A protocol exception should be thrown when charset is incompatible with input", callback.getMessage());
    }

    @Test
    public void testBCCShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildBcc(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("BCC".getBytes("US-ASCII"), key);
    }

    @Test
    public void testBODYShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildBody(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("BODY".getBytes("US-ASCII"), key);
    }

    @Test
    public void testCCShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildCc(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("CC".getBytes("US-ASCII"), key);
    }

    @Test
    public void testFROMShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildFrom(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("FROM".getBytes("US-ASCII"), key);
    }

    @Test
    public void testHEADERShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey
                .buildHeader("whatever", NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("HEADER whatever".getBytes("US-ASCII"), key);
    }

    @Test
    public void testSUBJECTShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildSubject(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("SUBJECT".getBytes("US-ASCII"), key);
    }

    @Test
    public void testTEXTShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildText(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("TEXT".getBytes("US-ASCII"), key);
    }

    @Test
    public void testTOShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildTo(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("TO".getBytes("US-ASCII"), key);
    }

    @Test
    public void testASCIICharset() throws Exception {
        SearchKey key = SearchKey.buildBcc(ASCII_SEARCH_TERM);
        checkValid("CHARSET US-ASCII BCC \"" + ASCII_SEARCH_TERM + "\"", key,
                true, "US-ASCII");
    }

    @Test
    public void testSimpleUTF8Charset() throws Exception {
        SearchKey key = SearchKey.buildBcc(ASCII_SEARCH_TERM);
        checkValid("CHARSET UTF-8 BCC \"" + ASCII_SEARCH_TERM + "\"", key,
                true, "US-ASCII");
    }

    private void checkUTF8Valid(byte[] term, final SearchKey key)
            throws Exception {
        ImapRequestLine reader = new ImapRequestLine(add(add(CHARSET, term),
                        BYTES_UTF8_NON_ASCII_SEARCH_TERM),
                new MockImapResponseComposer());
        final SearchKey searchKey = parser.searchKey(reader, null, true);
        assertEquals(key, searchKey);
    }

    private void checkValid(String input, final SearchKey key, boolean isFirst,
            String charset) throws Exception {
        ImapRequestLine reader = new ImapRequestLine(input.getBytes(charset),
                new MockImapResponseComposer());

        final SearchKey searchKey = parser.searchKey(reader, null, isFirst);
        assertEquals(key, searchKey);
    }

}
