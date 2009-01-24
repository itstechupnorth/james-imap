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

package org.apache.james.imap.mailbox;

import java.util.ArrayList;
import java.util.Date;

import org.apache.james.imap.mailbox.MessageResult.FetchGroup;
import org.apache.james.imap.mailbox.util.MessageResultImpl;
import org.apache.james.imap.mailbox.util.MessageResultUtils;
import org.jmock.integration.junit3.MockObjectTestCase;

public class MessageResultImplIncludedResultsTest extends MockObjectTestCase {

    MessageResultImpl result;

    MessageResult.Content content;

    protected void setUp() throws Exception {
        super.setUp();
        result = new MessageResultImpl();
        content = mock(MessageResult.Content.class);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldIncludedResultsWhenHeadersSet() throws Exception {
        result.setHeaders(null);
        assertEquals(FetchGroup.MINIMAL, result.getIncludedResults().content());
        result.setHeaders(new ArrayList());
        assertEquals(FetchGroup.HEADERS, result.getIncludedResults().content());
        result = new MessageResultImpl(this.result);
        assertEquals(FetchGroup.HEADERS, result.getIncludedResults().content());
    }

    public void testShouldIncludedResultsWhenFullMessageSet() throws Exception {
        result.setFullContent(null);
        assertEquals(FetchGroup.MINIMAL, result.getIncludedResults().content());
        result.setFullContent(content);
        assertEquals(FetchGroup.FULL_CONTENT, result.getIncludedResults()
                .content());
        result = new MessageResultImpl(this.result);
        assertEquals(FetchGroup.FULL_CONTENT, result.getIncludedResults()
                .content());
    }

    public void testShouldIncludedResultsWhenMessageBodySet() throws Exception {
        result.setBody(null);
        assertEquals(FetchGroup.MINIMAL, result.getIncludedResults().content());
        result.setBody(content);
        assertEquals(FetchGroup.BODY_CONTENT, result.getIncludedResults()
                .content());
        result = new MessageResultImpl(this.result);
        assertEquals(FetchGroup.BODY_CONTENT, result.getIncludedResults()
                .content());
    }

    public void testShouldIncludedResultsWhenAllSet() {
        result.setBody(content);
        assertEquals(FetchGroup.BODY_CONTENT, result
                .getIncludedResults().content());
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        result.setFullContent(content);
        assertEquals(FetchGroup.BODY_CONTENT
                | FetchGroup.FULL_CONTENT, result.getIncludedResults()
                .content());
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        assertTrue(MessageResultUtils.isFullContentIncluded(result));
        result.setHeaders(new ArrayList());
        assertEquals(FetchGroup.BODY_CONTENT
                | FetchGroup.FULL_CONTENT | FetchGroup.HEADERS, result
                .getIncludedResults().content());
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        assertTrue(MessageResultUtils.isFullContentIncluded(result));
        assertTrue(MessageResultUtils.isHeadersIncluded(result));
        result.setInternalDate(new Date());
        assertEquals(FetchGroup.BODY_CONTENT
                | FetchGroup.FULL_CONTENT | FetchGroup.HEADERS, result.getIncludedResults()
                .content());
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        assertTrue(MessageResultUtils.isFullContentIncluded(result));
        assertTrue(MessageResultUtils.isHeadersIncluded(result));
        result.setSize(100);
        assertEquals(FetchGroup.BODY_CONTENT
                | FetchGroup.FULL_CONTENT | FetchGroup.HEADERS, result
                .getIncludedResults().content());
        assertTrue(MessageResultUtils.isBodyContentIncluded(result));
        assertTrue(MessageResultUtils.isFullContentIncluded(result));
        assertTrue(MessageResultUtils.isHeadersIncluded(result));
    }
}
