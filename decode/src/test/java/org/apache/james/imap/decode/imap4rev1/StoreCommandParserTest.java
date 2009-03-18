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

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.imap.api.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class StoreCommandParserTest extends MockObjectTestCase {

    StoreCommandParser parser;

    Imap4Rev1CommandFactory mockCommandFactory;

    Imap4Rev1MessageFactory mockMessageFactory;

    ImapCommand command;

    ImapMessage message;

    protected void setUp() throws Exception {
        super.setUp();
        parser = new StoreCommandParser();
        mockCommandFactory = mock(Imap4Rev1CommandFactory.class);
        checking(new Expectations() {{
            oneOf (mockCommandFactory).getStore();
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

    public void testShouldParseSilentDraftFlagged() throws Exception {
        IdRange[] ranges = { new IdRange(1) };
        Flags flags = new Flags();
        flags.add(Flags.Flag.DRAFT);
        flags.add(Flags.Flag.FLAGGED);
        check("1 FLAGS.SILENT (\\Draft \\Flagged)\r\n", ranges, true, null,
                flags, false, "A01");
    }

    private void check(String input, final IdRange[] idSet,final boolean silent,
            final Boolean sign, final Flags flags, final boolean useUids, final String tag)
            throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(
                new ByteArrayInputStream(input.getBytes("US-ASCII")),
                new ByteArrayOutputStream());

        checking(new Expectations() {{
            oneOf (mockMessageFactory).createStoreMessage(
                    with(equal(command)), 
                    with(equal(idSet)), 
                    with(equal(silent)),
                    with(equal(sign)), 
                    with(equal(flags)), 
                    with(equal(useUids)), 
                    with(same(tag))
                    );will(returnValue(message));
        }});
        parser.decode(command, reader, tag, useUids);
    }
}
