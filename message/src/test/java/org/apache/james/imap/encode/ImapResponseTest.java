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

import javax.mail.Flags;

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.encode.base.ImapResponseComposerImpl;
import org.apache.james.imap.encode.imap4rev1.legacy.MockImapResponseWriter;
import org.jmock.MockObjectTestCase;

public class ImapResponseTest extends MockObjectTestCase {

    private static final String TAG = "TAG";

    ImapResponseComposer response;

    MockImapResponseWriter writer;

    protected void setUp() throws Exception {
        super.setUp();
        writer = new MockImapResponseWriter();
        response = new ImapResponseComposerImpl(writer);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCommandError() throws Exception {
        String message = "A message";
        response.commandError(message, TAG);
        assertEquals(4, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(
                ImapConstants.BAD), writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(3));
    }

    public void testBadResponse() throws Exception {
        String message = "A message";
        response.badResponse(message);
        assertEquals(4, writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(
                ImapConstants.BAD), writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(3));
    }

    public void testOkResponse() throws Exception {
        String message = "A message";
        String code = "A code";
        response.okResponse(code, message);
        assertEquals(5, writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(
                ImapConstants.OK), writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.ResponseCodeOperation(code),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(4));
    }

    public void testFlagsResponse() throws Exception {
        Flags flags = new Flags();
        response.flagsResponse(flags);
        assertEquals(5, writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(
                ImapResponseComposerImpl.FLAGS), writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.BracketOperation(true, false),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.BracketOperation(false, false),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(4));
    }

    public void testExistsResponse() throws Exception {
        int count = 5;
        response.existsResponse(count);
        assertEquals(4, writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.NumericMessageOperation(count),
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(
                ImapResponseComposerImpl.EXISTS), writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(3));
    }

    public void testRecentResponse() throws Exception {
        int count = 5;
        response.recentResponse(count);
        assertEquals(4, writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.NumericMessageOperation(count),
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(
                ImapResponseComposerImpl.RECENT), writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(3));
    }

    public void testExpungeResponse() throws Exception {
        int count = 5;
        response.expungeResponse(count);
        assertEquals(4, writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.NumericMessageOperation(count),
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(
                ImapResponseComposerImpl.EXPUNGE), writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(3));
    }

    public void testTaggedResponse() throws Exception {
        String message = "A message";
        response.taggedResponse(message, TAG);
        assertEquals(3, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message),
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(2));
    }

    public void testUntaggedResponse() throws Exception {
        String message = "A message";
        response.untaggedResponse(message);
        assertEquals(3, writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message),
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(2));
    }

    public void testByeResponse() throws Exception {
        String message = "A message";
        response.byeResponse(message);
        assertEquals(3, writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(),
                writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(
                ImapResponseComposerImpl.BYE + ImapResponseComposerImpl.SP
                        + message), writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.EndOperation(),
                writer.operations.get(2));
    }
}