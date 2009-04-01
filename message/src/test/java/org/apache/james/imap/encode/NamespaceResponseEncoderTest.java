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

import java.util.ArrayList;
import java.util.List;

import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.message.response.NamespaceResponse;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit3.MockObjectTestCase;

public class NamespaceResponseEncoderTest extends MockObjectTestCase {

    ImapSession dummySession;

    ImapResponseComposer mockComposer;

    NamespaceResponseEncoder subject;

    protected void setUp() throws Exception {
        super.setUp();
        dummySession = mock(ImapSession.class);
        final ImapEncoder stubNextEncoderInChain = mock(ImapEncoder.class);
        subject = new NamespaceResponseEncoder(stubNextEncoderInChain);
        mockComposer = mock(ImapResponseComposer.class);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testOneSharedNamespaceShouldWriteNilThenPrefixThenDeliminatorThenNil()
            throws Exception {
        final String aPrefix = "A Prefix";
        final String aDeliminator = "@";
        checking(new Expectations() {
            {
                final Sequence sequence = sequence("Composition order");
                oneOf(mockComposer).untagged(); inSequence(sequence);
                oneOf(mockComposer).commandName(ImapConstants.NAMESPACE_COMMAND_NAME);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).openParen();  inSequence(sequence);
                oneOf(mockComposer).openParen(); inSequence(sequence);
                oneOf(mockComposer).quote(aPrefix); inSequence(sequence);
                oneOf(mockComposer).quote(aDeliminator); inSequence(sequence);
                oneOf(mockComposer).closeParen(); inSequence(sequence);
                oneOf(mockComposer).closeParen(); inSequence(sequence);
                oneOf(mockComposer).end(); inSequence(sequence);
            }
        });
        List<NamespaceResponse.Namespace> namespaces = new ArrayList<NamespaceResponse.Namespace>();
        namespaces.add(new NamespaceResponse.Namespace(aPrefix, aDeliminator
                .charAt(0)));
        subject.doEncode(new NamespaceResponse(null, null, namespaces),
                mockComposer, dummySession);
    }

    public void testOneUsersNamespaceShouldWriteNilThenPrefixThenDeliminatorThenNil()
            throws Exception {
        final String aPrefix = "A Prefix";
        final String aDeliminator = "@";
        checking(new Expectations() {
            {
                final Sequence sequence = sequence("Composition order");
                oneOf(mockComposer).untagged(); inSequence(sequence);
                oneOf(mockComposer).commandName(ImapConstants.NAMESPACE_COMMAND_NAME);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).openParen(); inSequence(sequence);
                oneOf(mockComposer).openParen(); inSequence(sequence);
                oneOf(mockComposer).quote(aPrefix); inSequence(sequence);
                oneOf(mockComposer).quote(aDeliminator); inSequence(sequence);
                oneOf(mockComposer).closeParen(); inSequence(sequence);
                oneOf(mockComposer).closeParen(); inSequence(sequence);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).end(); inSequence(sequence);
            }
        });
        List<NamespaceResponse.Namespace> namespaces = new ArrayList<NamespaceResponse.Namespace>();
        namespaces.add(new NamespaceResponse.Namespace(aPrefix, aDeliminator
                .charAt(0)));
        subject.doEncode(new NamespaceResponse(null, namespaces, null),
                mockComposer, dummySession);
    }

    public void testOnePersonalNamespaceShouldWritePrefixThenDeliminatorThenNilNil()
            throws Exception {
        final String aPrefix = "A Prefix";
        final String aDeliminator = "@";
        checking(new Expectations() {
            {
                final Sequence sequence = sequence("Composition order");
                oneOf(mockComposer).untagged(); inSequence(sequence);
                oneOf(mockComposer).commandName(ImapConstants.NAMESPACE_COMMAND_NAME);
                oneOf(mockComposer).openParen(); inSequence(sequence);
                oneOf(mockComposer).openParen(); inSequence(sequence);
                oneOf(mockComposer).quote(aPrefix); inSequence(sequence);
                oneOf(mockComposer).quote(aDeliminator); inSequence(sequence);
                oneOf(mockComposer).closeParen(); inSequence(sequence);
                oneOf(mockComposer).closeParen(); inSequence(sequence);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).end(); inSequence(sequence);
            }
        });
        List<NamespaceResponse.Namespace> namespaces = new ArrayList<NamespaceResponse.Namespace>();
        namespaces.add(new NamespaceResponse.Namespace(aPrefix, aDeliminator
                .charAt(0)));
        subject.doEncode(new NamespaceResponse(namespaces, null, null),
                mockComposer, dummySession);
    }

    public void testTwoPersonalNamespaceShouldWritePrefixThenDeliminatorThenNilNil()
            throws Exception {
        final String aPrefix = "A Prefix";
        final String aDeliminator = "@";
        final String anotherPrefix = "Another Prefix";
        final String anotherDeliminator = "^";
        checking(new Expectations() {
            {
                final Sequence sequence = sequence("Composition order");
                oneOf(mockComposer).untagged(); inSequence(sequence);
                oneOf(mockComposer).commandName(ImapConstants.NAMESPACE_COMMAND_NAME);
                oneOf(mockComposer).openParen(); inSequence(sequence);
                oneOf(mockComposer).openParen(); inSequence(sequence);
                oneOf(mockComposer).quote(aPrefix); inSequence(sequence);
                oneOf(mockComposer).quote(aDeliminator); inSequence(sequence);
                oneOf(mockComposer).closeParen(); inSequence(sequence);
                oneOf(mockComposer).openParen(); inSequence(sequence);
                oneOf(mockComposer).quote(anotherPrefix); inSequence(sequence);
                oneOf(mockComposer).quote(anotherDeliminator); inSequence(sequence);
                oneOf(mockComposer).closeParen(); inSequence(sequence);
                oneOf(mockComposer).closeParen(); inSequence(sequence);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).end(); inSequence(sequence);
            }
        });
        List<NamespaceResponse.Namespace> namespaces = new ArrayList<NamespaceResponse.Namespace>();
        namespaces.add(new NamespaceResponse.Namespace(aPrefix, aDeliminator
                .charAt(0)));
        namespaces.add(new NamespaceResponse.Namespace(anotherPrefix,
                anotherDeliminator.charAt(0)));
        subject.doEncode(new NamespaceResponse(namespaces, null, null),
                mockComposer, dummySession);
    }

    public void testAllNullShouldWriteAllNIL() throws Exception {
        checking(new Expectations() {
            {
                final Sequence sequence = sequence("Composition order");
                oneOf(mockComposer).untagged(); inSequence(sequence);
                oneOf(mockComposer).commandName(ImapConstants.NAMESPACE_COMMAND_NAME);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).nil(); inSequence(sequence);
                oneOf(mockComposer).end(); inSequence(sequence);
            }
        });
        subject.doEncode(new NamespaceResponse(null, null, null), mockComposer,
                dummySession);
    }

    public void testNamespaceResponseIsAcceptable() throws Exception {
        assertFalse(subject.isAcceptable(mock(ImapMessage.class)));
        assertTrue(subject
                .isAcceptable(new NamespaceResponse(null, null, null)));
    }

}