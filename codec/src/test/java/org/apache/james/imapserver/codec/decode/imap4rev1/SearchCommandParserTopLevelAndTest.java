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

package org.apache.james.imapserver.codec.decode.imap4rev1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.request.DayMonthYear;
import org.apache.james.api.imap.message.request.SearchKey;
import org.apache.james.imapserver.codec.ProtocolException;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class SearchCommandParserTopLevelAndTest extends MockObjectTestCase {

    Input[] one = {sequence()};
    
    Input[] base = {sequence(), uid(), fromHeader(), since(), stringQuoted(), stringUnquoted(), draft()};
    
    Input[] variety = {sequence(), uid(), fromHeader(), since(), stringQuoted(), stringUnquoted(), draft(),
            mailingListHeader(), on(),  unanswered(), };
    
    public static Input sequence() {
        IdRange[] range = {new IdRange(Long.MAX_VALUE, 100), new IdRange(110), new IdRange(200, 201),
                new IdRange(400, Long.MAX_VALUE)};
        SearchKey key = SearchKey.buildSequenceSet(range);
        return new Input("*:100,110,200:201,400:*", key);
    }
    
    public static Input uid() {
        IdRange[] range = {new IdRange(Long.MAX_VALUE, 100), new IdRange(110), new IdRange(200, 201),
                new IdRange(400, Long.MAX_VALUE)};
        SearchKey key = SearchKey.buildUidSet(range);
        return new Input("UID *:100,110,200:201,400:*", key);
    }
    
    public static Input fromHeader() {
        SearchKey key = SearchKey.buildHeader("FROM", "Smith");
        return new Input("HEADER FROM Smith", key);
    }
    
    public static Input to() {
        SearchKey key = SearchKey.buildTo("JAMES Server Development <server-dev@james.apache.org>");
        return new Input("To \"JAMES Server Development <server-dev@james.apache.org>\"", key);
    }
    
    public static Input mailingListHeader() {
        SearchKey key = SearchKey.buildHeader("Mailing-List", "contact server-dev-help@james.apache.org; run by ezmlm");
        return new Input("HEADER Mailing-List \"contact server-dev-help@james.apache.org; run by ezmlm\"", key);
    }
    
    public static Input since() {
        SearchKey key = SearchKey.buildSince(new DayMonthYear(11,1, 2001));
        return new Input("since 11-Jan-2001", key);
    }
    
    public static Input on() {
        SearchKey key = SearchKey.buildOn(new DayMonthYear(1,2, 2001));
        return new Input("on 1-Feb-2001", key);
    }
    
    public static Input stringUnquoted() {
        SearchKey key = SearchKey.buildFrom("Smith");
        return new Input("FROM Smith", key);
    }
    
    public static Input stringQuoted() {
        SearchKey key = SearchKey.buildFrom("Smith And Jones");
        return new Input("FROM \"Smith And Jones\"", key);
    }
    
    public static Input draft() {
        SearchKey key = SearchKey.buildDraft();
        return new Input("DRAFT", key);
    }
    
    public static Input unanswered() {
        SearchKey key = SearchKey.buildUnanswered();
        return new Input("unanswered", key);
    }
    
    public static final class Input {
        public String input;
        public SearchKey key;
        
        public Input(String input, SearchKey key) {
            super();
            this.input = input;
            this.key = key;
        }
    }
    
    SearchCommandParser parser;
    Mock mockCommandFactory;
    Mock mockMessageFactory;
    Mock mockCommand;
    Mock mockMessage;
    ImapCommand command;
    ImapMessage message;
    
    protected void setUp() throws Exception {
        super.setUp();
        parser = new SearchCommandParser();
        mockCommandFactory = mock(Imap4Rev1CommandFactory.class);
        mockCommandFactory.expects(once()).method("getSearch");
        mockMessageFactory = mock(Imap4Rev1MessageFactory.class);
        mockCommand = mock(ImapCommand.class);
        command = (ImapCommand) mockCommand.proxy();
        mockMessage = mock(ImapMessage.class);
        message = (ImapMessage) mockMessage.proxy();
        parser.init((Imap4Rev1CommandFactory) mockCommandFactory.proxy());
        parser.setMessageFactory((Imap4Rev1MessageFactory) mockMessageFactory.proxy());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testLargePermutations() throws Exception {
        permute(16, one);
        permute(32, one);
    }
    
    public void testBasePermutations() throws Exception {
        permute(2, base);
        permute(3, base);
        permute(4, base);
        permute(5, base);
    }
    
    public void testVarietyPermutations() throws Exception {
        permute(5, variety);
    }
    
    private void permute(int mutations, Input[] inputs) throws Exception {
        permute(mutations, new ArrayList(), new StringBuffer(), inputs);
    }
    
    private void permute(int mutations, List keys, StringBuffer buffer, Input[] inputs) throws Exception {
        if (mutations == 0) {
            check(keys, buffer);
        } else {
            mutations -= 1;
            for (int i = 0; i < inputs.length; i++) {
                StringBuffer nextBuffer = new StringBuffer(buffer.toString());
                if (nextBuffer.length() > 0) {
                    nextBuffer.append(' ');
                }
                nextBuffer.append(inputs[i].input);
                List nextKeys = new ArrayList(keys);
                nextKeys.add(inputs[i].key);
                permute(mutations, nextKeys, nextBuffer, inputs);
            }
        }
    }

    private void check(List keys, StringBuffer buffer) throws UnsupportedEncodingException, ProtocolException {
        buffer.append("\r\n");
        String input = buffer.toString();
        SearchKey key = SearchKey.buildAnd(keys);
        ImapRequestLineReader reader = new ImapRequestLineReader(
                new ByteArrayInputStream(input.getBytes("US-ASCII")), 
                    new ByteArrayOutputStream());

        assertEquals(input, key, parser.decode(reader));
    }
}