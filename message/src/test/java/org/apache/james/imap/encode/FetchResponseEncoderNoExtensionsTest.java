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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.message.response.ImapResponseComposer;
import org.apache.james.imap.encode.FetchResponseEncoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.message.response.FetchResponse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class FetchResponseEncoderNoExtensionsTest  {

    Flags flags;

    ImapResponseComposer composer;

    FetchResponse.Structure stubStructure;

    ImapEncoder mockNextEncoder;

    FetchResponseEncoder encoder;

    ImapCommand stubCommand;

    private Mockery context = new JUnit4Mockery();
    
    @Before
    public void setUp() throws Exception {
        composer = context.mock(ImapResponseComposer.class);
        mockNextEncoder = context.mock(ImapEncoder.class);
        encoder = new FetchResponseEncoder(mockNextEncoder, true);
        stubCommand = ImapCommand.anyStateCommand("COMMAND");
        flags = new Flags(Flags.Flag.DELETED);
        stubStructure = context.mock(FetchResponse.Structure.class);
    }


    @Test
    public void testShouldNotAcceptUnknownResponse() throws Exception {
        assertFalse(encoder.isAcceptable(context.mock(ImapMessage.class)));
    }

    @Test
    public void testShouldAcceptFetchResponse() throws Exception {
        assertTrue(encoder.isAcceptable(new FetchResponse(11, null, null, null,
                null, null, null, null, null)));
    }

    @Test
    public void testShouldEncodeFlagsResponse() throws Exception {
        FetchResponse message = new FetchResponse(100, flags, null, null, null,
                null, null, null, null);
        context.checking(new Expectations() {{
            final Sequence sequence = context.sequence("composition");
            oneOf(composer).openFetchResponse(with(equal(100L))); inSequence(sequence);
            oneOf(composer).flags(with(equal(flags))); inSequence(sequence);
            oneOf(composer).closeFetchResponse(); inSequence(sequence);
        }});
        encoder.doEncode(message, composer, new FakeImapSession());
    }

    @Test
    public void testShouldEncodeUidResponse() throws Exception {
        FetchResponse message = new FetchResponse(100, null, new Long(72),
                null, null, null, null, null, null);
        context.checking(new Expectations() {{
            final Sequence sequence = context.sequence("composition");
            oneOf(composer).openFetchResponse(with(equal(100L))); inSequence(sequence);
            oneOf(composer).message(with(equal("UID"))); inSequence(sequence);
            oneOf(composer).message(with(equal(72L))); inSequence(sequence);
            oneOf(composer).closeFetchResponse(); inSequence(sequence);
        }});
        encoder.doEncode(message, composer, new FakeImapSession());
    }

    @Test
    public void testShouldEncodeAllResponse() throws Exception {
        FetchResponse message = new FetchResponse(100, flags, new Long(72),
                null, null, null, null, null, null);
        context.checking(new Expectations() {{
            final Sequence sequence = context.sequence("composition");
            oneOf(composer).openFetchResponse(with(equal(100L))); inSequence(sequence);
            oneOf(composer).flags(with(equal(flags))); inSequence(sequence);
            oneOf(composer).message(with(equal("UID"))); inSequence(sequence);
            oneOf(composer).message(with(equal(72L))); inSequence(sequence);
            oneOf(composer).closeFetchResponse(); inSequence(sequence);
        }});
        encoder.doEncode(message, composer, new FakeImapSession());
    }
    
    @Test
    public void testShouldNotAddExtensionsWithEncodingBodyStructure() throws Exception {
        FetchResponse message = new FetchResponse(100, flags, new Long(72),
                null, null, null, null, stubStructure, null);
        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("CHARSET", "US-ASCII");
        final List<String> parameterList= new ArrayList<String>();
        parameterList.add("CHARSET");
        parameterList.add("US-ASCII");
        
        context.checking(new Expectations() {{
            final long octets = 2279L;
            final long lines = 48L;
            allowing(stubStructure).getMediaType(); will(returnValue("TEXT"));
            allowing(stubStructure).getSubType(); will(returnValue("HTML"));
            allowing(stubStructure).getOctets();will(returnValue(octets));
            allowing(stubStructure).getLines();will(returnValue(lines));
            allowing(stubStructure).getParameters(); will(returnValue(parameterList));
            allowing(stubStructure).getEncoding(); will(returnValue("7BIT"));
            ignoring(stubStructure);
            
            final Sequence sequence = context.sequence("composition");
            oneOf(composer).openFetchResponse(with(equal(100L))); inSequence(sequence);
            oneOf(composer).flags(with(equal(flags))); inSequence(sequence);
            oneOf(composer).message(with(equal("BODYSTRUCTURE")));inSequence(sequence);
            oneOf(composer).openParen();inSequence(sequence);
            oneOf(composer).quoteUpperCaseAscii("TEXT");inSequence(sequence);
            oneOf(composer).quoteUpperCaseAscii("HTML");inSequence(sequence);
            oneOf(composer).nillableQuotes(parameterList);inSequence(sequence);
            oneOf(composer).nillableQuote("");inSequence(sequence);
            oneOf(composer).nillableQuote("");inSequence(sequence);
            oneOf(composer).quoteUpperCaseAscii("7BIT");inSequence(sequence);
            oneOf(composer).message(octets);inSequence(sequence);
            oneOf(composer).message(lines);inSequence(sequence);
            oneOf(composer).closeParen();inSequence(sequence);
            oneOf(composer).message(with(equal("UID"))); inSequence(sequence);
            oneOf(composer).message(with(equal(72L))); inSequence(sequence);
            oneOf(composer).closeFetchResponse(); inSequence(sequence);
           ;
        }});
        final FakeImapSession fakeImapSession = new FakeImapSession();
        encoder.doEncode(message, composer, fakeImapSession);
    }
}
