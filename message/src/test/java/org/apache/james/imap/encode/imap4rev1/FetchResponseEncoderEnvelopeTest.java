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

package org.apache.james.imap.encode.imap4rev1;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse.Envelope.Address;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit3.MockObjectTestCase;

public class FetchResponseEncoderEnvelopeTest extends MockObjectTestCase {

    private static final String ADDRESS_ONE_HOST = "HOST";

    private static final String ADDRESS_ONE_MAILBOX = "MAILBOX";

    private static final String ADDRESS_ONE_DOMAIN_LIST = "DOMAIN LIST";

    private static final String ADDRESS_ONE_NAME = "NAME";

    private static final String ADDRESS_TWO_HOST = "2HOST";

    private static final String ADDRESS_TWO_MAILBOX = "2MAILBOX";

    private static final String ADDRESS_TWO_DOMAIN_LIST = "2DOMAIN LIST";

    private static final String ADDRESS_TWO_NAME = "2NAME";

    private static final int MSN = 100;

    Flags flags;

    ImapResponseComposer composer;

    ImapEncoder mockNextEncoder;

    FetchResponseEncoder encoder;

    ImapCommand mockCommand;

    FetchResponse message;

    FetchResponse.Envelope envelope;

    Address[] bcc;

    Address[] cc;

    String date;

    Address[] from;

    String inReplyTo;

    String messageId;

    Address[] replyTo;

    Address[] sender;

    String subject;

    Address[] to;

    protected void setUp() throws Exception {
        super.setUp();
        envelope = mock(FetchResponse.Envelope.class);

        bcc = null;
        cc = null;
        date = null;
        from = null;
        inReplyTo = null;
        messageId = null;
        replyTo = null;
        sender = null;
        subject = null;
        to = null;

        message = new FetchResponse(MSN, null, null, null, null, envelope, null, null, null);
        composer = mock(ImapResponseComposer.class);
        mockNextEncoder = mock(ImapEncoder.class);
        encoder = new FetchResponseEncoder(mockNextEncoder);
        mockCommand = mock(ImapCommand.class);
        flags = new Flags(Flags.Flag.DELETED);
    }

    private Address[] mockOneAddress() {
        Address[] one = { mockAddress(ADDRESS_ONE_NAME,
                ADDRESS_ONE_DOMAIN_LIST, ADDRESS_ONE_MAILBOX, ADDRESS_ONE_HOST) };
        return one;
    }

    private Address[] mockManyAddresses() {
        Address[] many = {
                mockAddress(ADDRESS_ONE_NAME, ADDRESS_ONE_DOMAIN_LIST,
                        ADDRESS_ONE_MAILBOX, ADDRESS_ONE_HOST),
                mockAddress(ADDRESS_TWO_NAME, ADDRESS_TWO_DOMAIN_LIST,
                        ADDRESS_TWO_MAILBOX, ADDRESS_TWO_HOST) };
        return many;
    }

    private Address mockAddress(final String name, final String domainList,
            final String mailbox, final String host) {
        final Address address = mock(Address.class, name + host);
        checking(new Expectations() {{
            oneOf (address).getPersonalName();will(returnValue(name));
            oneOf (address).getAtDomainList();will(returnValue(domainList));
            oneOf (address).getMailboxName();will(returnValue(mailbox));
            oneOf (address).getHostName();will(returnValue(host));
        }});
        return address;
    }

    private void envelopExpects() {
        checking(new Expectations() {{
            oneOf(envelope).getBcc();will(returnValue(bcc));
            oneOf(envelope).getCc();will(returnValue(cc));
            oneOf(envelope).getDate();will(returnValue(date));
            oneOf(envelope).getFrom();will(returnValue(from));
            oneOf(envelope).getInReplyTo();will(returnValue(inReplyTo));
            oneOf(envelope).getMessageId();will(returnValue(messageId));
            oneOf(envelope).getReplyTo();will(returnValue(replyTo));
            oneOf(envelope).getSender();will(returnValue(sender));
            oneOf(envelope).getSubject();will(returnValue(subject));
            oneOf(envelope).getTo();will(returnValue(to));
        }});
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldNilAllNullProperties() throws Exception {
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(6).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(null, null); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeDate() throws Exception {
        date = "a date";
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(date), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(6).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(null, null); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeSubject() throws Exception {
        subject = "some subject";
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(equal(subject)), with(equal(true))); inSequence(composition);
            exactly(6).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(null, null); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeInReplyTo() throws Exception {
        inReplyTo = "some reply to";
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(6).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(equal(inReplyTo)), with(aNull(String.class))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeMessageId() throws Exception {
        messageId = "some message id";
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(6).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeOneFromAddress() throws Exception {
        from = mockOneAddress();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(5).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeManyFromAddress() throws Exception {
        from = mockManyAddresses();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_TWO_NAME)), 
                    with(equal(ADDRESS_TWO_DOMAIN_LIST)),
                    with(equal(ADDRESS_TWO_MAILBOX)), 
                    with(equal(ADDRESS_TWO_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(5).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeOneSenderAddress() throws Exception {
        sender = mockOneAddress();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(1).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(4).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeManySenderAddress() throws Exception {
        sender = mockManyAddresses();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(1).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_TWO_NAME)), 
                    with(equal(ADDRESS_TWO_DOMAIN_LIST)),
                    with(equal(ADDRESS_TWO_MAILBOX)), 
                    with(equal(ADDRESS_TWO_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(4).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }
    

    public void testShouldComposeOneReplyToAddress() throws Exception {
        replyTo = mockOneAddress();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(2).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(3).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeManyReplyToAddress() throws Exception {
        replyTo = mockManyAddresses();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(2).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_TWO_NAME)), 
                    with(equal(ADDRESS_TWO_DOMAIN_LIST)),
                    with(equal(ADDRESS_TWO_MAILBOX)), 
                    with(equal(ADDRESS_TWO_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(3).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeOneToAddress() throws Exception {
        to = mockOneAddress();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(3).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(2).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeManyToAddress() throws Exception {
        to = mockManyAddresses();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(3).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_TWO_NAME)), 
                    with(equal(ADDRESS_TWO_DOMAIN_LIST)),
                    with(equal(ADDRESS_TWO_MAILBOX)), 
                    with(equal(ADDRESS_TWO_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(2).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeOneCcAddress() throws Exception {
        cc = mockOneAddress();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(4).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(1).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeManyCcAddress() throws Exception {
        cc = mockManyAddresses();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(4).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_TWO_NAME)), 
                    with(equal(ADDRESS_TWO_DOMAIN_LIST)),
                    with(equal(ADDRESS_TWO_MAILBOX)), 
                    with(equal(ADDRESS_TWO_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            exactly(1).of(composer).nil(); inSequence(composition);
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeOneBccAddress() throws Exception {
        bcc = mockOneAddress();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(5).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }

    public void testShouldComposeManyBccAddress() throws Exception {
        bcc = mockManyAddresses();
        envelopExpects();
        checking(new Expectations() {{
            final Sequence composition = sequence("composition");
            oneOf(composer).openFetchResponse(with(equal((long) MSN))); inSequence(composition);
            oneOf(composer).startEnvelope(with(aNull(String.class)), with(aNull(String.class)), with(equal(true))); inSequence(composition);
            exactly(5).of(composer).nil(); inSequence(composition);
            oneOf(composer).startAddresses(); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_ONE_NAME)), 
                    with(equal(ADDRESS_ONE_DOMAIN_LIST)),
                    with(equal(ADDRESS_ONE_MAILBOX)), 
                    with(equal(ADDRESS_ONE_HOST))); inSequence(composition);
            oneOf(composer).address(
                    with(equal(ADDRESS_TWO_NAME)), 
                    with(equal(ADDRESS_TWO_DOMAIN_LIST)),
                    with(equal(ADDRESS_TWO_MAILBOX)), 
                    with(equal(ADDRESS_TWO_HOST))); inSequence(composition);
            oneOf(composer).endAddresses(); inSequence(composition);        
            oneOf(composer).endEnvelope(with(aNull(String.class)), with(equal(messageId))); inSequence(composition);
            oneOf(composer).closeFetchResponse();inSequence(composition);
        }});
        encoder.doEncode(message, composer);
    }
}
