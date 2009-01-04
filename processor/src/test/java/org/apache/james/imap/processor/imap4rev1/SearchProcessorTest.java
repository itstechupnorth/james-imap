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

package org.apache.james.imap.processor.imap4rev1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.mail.Flags.Flag;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.request.DayMonthYear;
import org.apache.james.api.imap.message.request.SearchKey;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.api.imap.process.SelectedMailbox;
import org.apache.james.api.imap.process.ImapProcessor.Responder;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxManagerProvider;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.SearchQuery.Criterion;
import org.apache.james.imap.mailbox.util.FetchGroupImpl;
import org.apache.james.imap.message.request.imap4rev1.SearchRequest;
import org.apache.james.imap.message.response.imap4rev1.server.SearchResponse;
import org.apache.james.imap.processor.base.ImapSessionUtils;
import org.apache.james.imap.processor.imap4rev1.SearchProcessor;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class SearchProcessorTest extends MockObjectTestCase {
    private static final int DAY = 6;

    private static final int MONTH = 6;

    private static final int YEAR = 1944;

    private static final DayMonthYear DAY_MONTH_YEAR = new DayMonthYear(DAY,
            MONTH, YEAR);

    private static final long SIZE = 1729;

    private static final String KEYWORD = "BD3";

    private static final long[] EMPTY = {};

    private static final String TAG = "TAG";

    private static final String ADDRESS = "John Smith <john@example.org>";

    private static final String SUBJECT = "Myriad Harbour";

    private static final IdRange[] IDS = { new IdRange(1),
            new IdRange(42, 1048) };

    private static final SearchQuery.NumericRange[] RANGES = {
            new SearchQuery.NumericRange(1),
            new SearchQuery.NumericRange(42, 1048) };

    SearchProcessor processor;

    Mock next;

    Mock responder;

    Mock result;

    Mock session;

    Mock command;

    Mock serverResponseFactory;

    Mock statusResponse;

    Mock mailbox;

    Mock mailboxManagerProvider;
    
    Mock mailboxManager;
    
    Mock mailboxSession;
    
    ImapCommand imapCommand;

    ImapProcessor.Responder responderImpl;

    Mock selectedMailbox;

    protected void setUp() throws Exception {
        super.setUp();
        serverResponseFactory = mock(StatusResponseFactory.class);
        session = mock(ImapSession.class);
        command = mock(ImapCommand.class);
        imapCommand = (ImapCommand) command.proxy();
        next = mock(ImapProcessor.class);
        responder = mock(ImapProcessor.Responder.class);
        statusResponse = mock(StatusResponse.class);
        responderImpl = (ImapProcessor.Responder) responder.proxy();
        mailbox = mock(Mailbox.class);
        mailboxManagerProvider = mock(MailboxManagerProvider.class);
        mailboxManager = mock(MailboxManager.class);
        mailboxSession = mock(MailboxSession.class);
        selectedMailbox = mock(SelectedMailbox.class);
        
        processor = new SearchProcessor((ImapProcessor) next.proxy(), 
                (MailboxManagerProvider) mailboxManagerProvider.proxy(),
                (StatusResponseFactory) serverResponseFactory.proxy());
        expectOk();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSequenceSetLowerUnlimited() throws Exception {
        expectsGetSelectedMailbox();
        final IdRange[] ids = { new IdRange(Long.MAX_VALUE, 1729) };
        final SearchQuery.NumericRange[] ranges = { new SearchQuery.NumericRange(
                Long.MAX_VALUE, 1729L) };
        selectedMailbox.expects(once()).method("uid").with(eq(1729)).will(
                returnValue(1729L));
        
        allowUnsolicitedResponses();
        
        check(SearchKey.buildSequenceSet(ids), SearchQuery.uid(ranges));
    }

    private void allowUnsolicitedResponses() {
        session.expects(atMostOnce()).method("getAttribute").with(eq(ImapSessionUtils.MAILBOX_USER_ATTRIBUTE_SESSION_KEY)).will(returnValue("user"));
        session.expects(atMostOnce()).method("getAttribute").with(eq(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY)).will(returnValue(mailboxSession.proxy()));        
    }

    public void testSequenceSetUpperUnlimited() throws Exception {
        expectsGetSelectedMailbox();
        final IdRange[] ids = { new IdRange(1, Long.MAX_VALUE) };
        final SearchQuery.NumericRange[] ranges = { new SearchQuery.NumericRange(
                42, Long.MAX_VALUE) };
        selectedMailbox.expects(once()).method("uid").with(eq(1)).will(
                returnValue(42L));
        allowUnsolicitedResponses();
        check(SearchKey.buildSequenceSet(ids), SearchQuery.uid(ranges));
    }

    public void testSequenceSetMsnRange() throws Exception {
        expectsGetSelectedMailbox();
        final IdRange[] ids = { new IdRange(1, 5) };
        final SearchQuery.NumericRange[] ranges = { new SearchQuery.NumericRange(
                42, 1729) };
        selectedMailbox.expects(once()).method("uid").with(eq(1)).will(
                returnValue(42L));
        selectedMailbox.expects(once()).method("uid").with(eq(5)).will(
                returnValue(1729L));
        allowUnsolicitedResponses();
        check(SearchKey.buildSequenceSet(ids), SearchQuery.uid(ranges));
    }

    public void testSequenceSetSingleMsn() throws Exception {
        expectsGetSelectedMailbox();
        final IdRange[] ids = { new IdRange(1) };
        final SearchQuery.NumericRange[] ranges = { new SearchQuery.NumericRange(
                42) };
        
        selectedMailbox.expects(exactly(2)).method("uid").with(eq(1)).will(
                returnValue(42L));
        allowUnsolicitedResponses();
        check(SearchKey.buildSequenceSet(ids), SearchQuery.uid(ranges));
    }

    public void testALL() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildAll(), SearchQuery.all());
    }

    private void expectsGetSelectedMailbox() {
        mailboxManagerProvider.expects(atMostOnce()).method("getMailboxManager").will(returnValue(mailboxManager.proxy()));
        mailboxManager.expects(atMostOnce()).method("resolve").with(eq("user"), eq("name")).will(returnValue("user"));
        mailboxManager.expects(atMostOnce()).method("getMailbox").with(eq("user"), eq(false)).will(returnValue(mailbox.proxy()));
        session.expects(atLeastOnce()).method("getSelected").will(returnValue(selectedMailbox.proxy()));
        selectedMailbox.expects(atMostOnce()).method("isRecentUidRemoved").will(returnValue(false));
        selectedMailbox.expects(atLeastOnce()).method("isSizeChanged").will(returnValue(false));
        selectedMailbox.expects(atLeastOnce()).method("getName").will(returnValue("MailboxName"));
        selectedMailbox.expects(atMostOnce()).method("flagUpdateUids").will(returnValue(Collections.EMPTY_LIST));
        selectedMailbox.expects(atMostOnce()).method("resetEvents");
        mailboxManager.expects(atLeastOnce()).method("getMailbox").will(returnValue(mailbox.proxy()));
        selectedMailbox.expects(once()).method("getRecent").will(returnValue(new ArrayList<Long>()));
    }

    public void testANSWERED() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildAnswered(), SearchQuery.flagIsSet(Flag.ANSWERED));
    }

    public void testBCC() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildBcc(ADDRESS), SearchQuery.headerContains(
                ImapConstants.RFC822_BCC, ADDRESS));
    }

    public void testBEFORE() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildBefore(DAY_MONTH_YEAR), SearchQuery
                .internalDateBefore(DAY, MONTH, YEAR));
    }

    public void testBODY() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildBody(SUBJECT), SearchQuery.bodyContains(SUBJECT));
    }

    public void testCC() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildCc(ADDRESS), SearchQuery.headerContains(
                ImapConstants.RFC822_CC, ADDRESS));
    }

    public void testDELETED() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildDeleted(), SearchQuery.flagIsSet(Flag.DELETED));
    }

    public void testDRAFT() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildDraft(), SearchQuery.flagIsSet(Flag.DRAFT));
    }

    public void testFLAGGED() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildFlagged(), SearchQuery.flagIsSet(Flag.FLAGGED));
    }

    public void testFROM() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildFrom(ADDRESS), SearchQuery.headerContains(
                ImapConstants.RFC822_FROM, ADDRESS));
    }

    public void testHEADER() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildHeader(ImapConstants.RFC822_IN_REPLY_TO, ADDRESS),
                SearchQuery.headerContains(ImapConstants.RFC822_IN_REPLY_TO,
                        ADDRESS));
    }

    public void testKEYWORD() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildKeyword(KEYWORD), SearchQuery.flagIsSet(KEYWORD));
    }

    public void testLARGER() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildLarger(SIZE), SearchQuery.sizeGreaterThan(SIZE));
    }

    public void testNEW() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildNew(), SearchQuery.and(SearchQuery
                .flagIsSet(Flag.RECENT), SearchQuery.flagIsUnSet(Flag.SEEN)));
    }

    public void testNOT() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildNot(SearchKey.buildOn(DAY_MONTH_YEAR)),
                SearchQuery.not(SearchQuery.internalDateOn(DAY, MONTH, YEAR)));
    }

    public void testOLD() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildOld(), SearchQuery.flagIsUnSet(Flag.RECENT));
    }

    public void testON() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildOn(DAY_MONTH_YEAR), SearchQuery.internalDateOn(
                DAY, MONTH, YEAR));
    }

    public void testAND() throws Exception {
        expectsGetSelectedMailbox();
        List<SearchKey> keys = new ArrayList<SearchKey>();
        keys.add(SearchKey.buildOn(DAY_MONTH_YEAR));
        keys.add(SearchKey.buildOld());
        keys.add(SearchKey.buildLarger(SIZE));
        List<Criterion> criteria = new ArrayList<Criterion>();
        criteria.add(SearchQuery.internalDateOn(DAY, MONTH, YEAR));
        criteria.add(SearchQuery.flagIsUnSet(Flag.RECENT));
        criteria.add(SearchQuery.sizeGreaterThan(SIZE));
        check(SearchKey.buildAnd(keys), SearchQuery.and(criteria));
    }

    public void testOR() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildOr(SearchKey.buildOn(DAY_MONTH_YEAR), SearchKey
                .buildOld()), SearchQuery.or(SearchQuery.internalDateOn(DAY,
                MONTH, YEAR), SearchQuery.flagIsUnSet(Flag.RECENT)));
    }

    public void testRECENT() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildRecent(), SearchQuery.flagIsSet(Flag.RECENT));
    }

    public void testSEEN() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildSeen(), SearchQuery.flagIsSet(Flag.SEEN));
    }

    public void testSENTBEFORE() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildSentBefore(DAY_MONTH_YEAR), SearchQuery
                .headerDateBefore(ImapConstants.RFC822_DATE, DAY, MONTH, YEAR));
    }

    public void testSENTON() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildSentOn(DAY_MONTH_YEAR), SearchQuery.headerDateOn(
                ImapConstants.RFC822_DATE, DAY, MONTH, YEAR));
    }

    public void testSENTSINCE() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildSentSince(DAY_MONTH_YEAR), SearchQuery
                .headerDateAfter(ImapConstants.RFC822_DATE, DAY, MONTH, YEAR));
    }

    public void testSINCE() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildSince(DAY_MONTH_YEAR), SearchQuery
                .internalDateAfter(DAY, MONTH, YEAR));
    }

    public void testSMALLER() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildSmaller(SIZE), SearchQuery.sizeLessThan(SIZE));
    }

    public void testSUBJECT() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildSubject(SUBJECT), SearchQuery.headerContains(
                ImapConstants.RFC822_SUBJECT, SUBJECT));
    }

    public void testTEXT() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildText(SUBJECT), SearchQuery.mailContains(SUBJECT));
    }

    public void testTO() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildTo(ADDRESS), SearchQuery.headerContains(
                ImapConstants.RFC822_TO, ADDRESS));
    }

    public void testUID() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildUidSet(IDS), SearchQuery.uid(RANGES));
    }

    public void testUNANSWERED() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildUnanswered(), SearchQuery
                .flagIsUnSet(Flag.ANSWERED));
    }

    public void testUNDELETED() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildUndeleted(), SearchQuery.flagIsUnSet(Flag.DELETED));
    }

    public void testUNDRAFT() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildUndraft(), SearchQuery.flagIsUnSet(Flag.DRAFT));
    }

    public void testUNFLAGGED() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildUnflagged(), SearchQuery.flagIsUnSet(Flag.FLAGGED));
    }

    public void testUNKEYWORD() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildUnkeyword(KEYWORD), SearchQuery
                .flagIsUnSet(KEYWORD));
    }

    public void testUNSEEN() throws Exception {
        expectsGetSelectedMailbox();
        check(SearchKey.buildUnseen(), SearchQuery.flagIsUnSet(Flag.SEEN));
    }

    private void check(SearchKey key, SearchQuery.Criterion criterion)
            throws Exception {
        SearchQuery query = new SearchQuery();
        query.andCriteria(criterion);
        check(key, query);
    }

    private void check(SearchKey key, SearchQuery query) throws Exception {
        MailboxSession mailboxSession = (MailboxSession) mock(
                MailboxSession.class).proxy();
        session.expects(once()).method("getAttribute").with(
                eq(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY))
                .will(returnValue((MailboxSession) mailboxSession));
        mailbox.expects(once()).method("search").with(eq(query),
                eq(FetchGroupImpl.MINIMAL), eq(mailboxSession)).will(
                returnValue(new ArrayList().iterator()));
        responder.expects(once()).method("respond").with(
                eq(new SearchResponse(EMPTY)));
        SearchRequest message = new SearchRequest(imapCommand, key, false, TAG);
        processor.doProcess(message, (ImapSession) session.proxy(), TAG,
                (ImapCommand) command.proxy(), (Responder) responder.proxy());
    }

    private void expectOk() {
        StatusResponse response = (StatusResponse) statusResponse.proxy();
        serverResponseFactory.expects(once()).method("taggedOk").with(eq(TAG),
                same(imapCommand), eq(HumanReadableTextKey.COMPLETED)).will(
                returnValue(response));
        responder.expects(once()).method("respond").with(same(response));
    }
}
