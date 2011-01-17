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

import javax.mail.Flags;

import org.apache.james.imap.encode.base.AbstractImapResponseComposer;
import org.junit.Before;
import org.junit.Test;

public class ImapResponseTest  {

    @Test
    public void stub() {
        
    }
    /*
    private static final String TAG = "TAG";

    ImapResponseComposer response;

    MockImapResponseComposer writer;

    @Before
    public void setUp() throws Exception {
        writer = new MockImapResponseComposer();
        response = new MockImapResponseComposer();
    }

   

    @Test
    public void testFlagsResponse() throws Exception {
        Flags flags = new Flags();
        response.flagsResponse(flags);
        assertEquals(5, writer.operations.size());
        assertEquals(new MockImapResponseComposer.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseComposer.TextMessageOperation(
                AbstractImapResponseComposer.FLAGS), writer.operations.get(1));
        assertEquals(new MockImapResponseComposer.BracketOperation(true, false),
                writer.operations.get(2));
        assertEquals(new MockImapResponseComposer.BracketOperation(false, false),
                writer.operations.get(3));
        assertEquals(new MockImapResponseComposer.EndOperation(),
                writer.operations.get(4));
    }

    @Test
    public void testExistsResponse() throws Exception {
        int count = 5;
        response.existsResponse(count);
        assertEquals(4, writer.operations.size());
        assertEquals(new MockImapResponseComposer.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseComposer.NumericMessageOperation(count),
                writer.operations.get(1));
        assertEquals(new MockImapResponseComposer.TextMessageOperation(
                AbstractImapResponseComposer.EXISTS), writer.operations.get(2));
        assertEquals(new MockImapResponseComposer.EndOperation(),
                writer.operations.get(3));
    }

    @Test
    public void testRecentResponse() throws Exception {
        int count = 5;
        response.recentResponse(count);
        assertEquals(4, writer.operations.size());
        assertEquals(new MockImapResponseComposer.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseComposer.NumericMessageOperation(count),
                writer.operations.get(1));
        assertEquals(new MockImapResponseComposer.TextMessageOperation(
                AbstractImapResponseComposer.RECENT), writer.operations.get(2));
        assertEquals(new MockImapResponseComposer.EndOperation(),
                writer.operations.get(3));
    }

    @Test
    public void testExpungeResponse() throws Exception {
        int count = 5;
        response.expungeResponse(count);
        assertEquals(4, writer.operations.size());
        assertEquals(new MockImapResponseComposer.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseComposer.NumericMessageOperation(count),
                writer.operations.get(1));
        assertEquals(new MockImapResponseComposer.TextMessageOperation(
                AbstractImapResponseComposer.EXPUNGE), writer.operations.get(2));
        assertEquals(new MockImapResponseComposer.EndOperation(),
                writer.operations.get(3));
    }

    @Test
    public void testTaggedResponse() throws Exception {
        String message = "A message";
        response.taggedResponse(message, TAG);
        assertEquals(3, writer.operations.size());
        assertEquals(new MockImapResponseComposer.TagOperation(TAG),
                writer.operations.get(0));
        assertEquals(new MockImapResponseComposer.TextMessageOperation(message),
                writer.operations.get(1));
        assertEquals(new MockImapResponseComposer.EndOperation(),
                writer.operations.get(2));
    }

    @Test
    public void testUntaggedResponse() throws Exception {
        String message = "A message";
        response.untaggedResponse(message);
        assertEquals(3, writer.operations.size());
        assertEquals(new MockImapResponseComposer.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseComposer.TextMessageOperation(message),
                writer.operations.get(1));
        assertEquals(new MockImapResponseComposer.EndOperation(),
                writer.operations.get(2));
    }

    @Test
    public void testByeResponse() throws Exception {
        String message = "A message";
        response.byeResponse(message);
        assertEquals(3, writer.operations.size());
        assertEquals(new MockImapResponseComposer.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseComposer.TextMessageOperation(
                AbstractImapResponseComposer.BYE + AbstractImapResponseComposer.SP
                        + message), writer.operations.get(1));
        assertEquals(new MockImapResponseComposer.EndOperation(),
                writer.operations.get(2));
    }
    */
}
