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

package org.apache.james.imap.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.mail.Flags;

import junit.framework.TestCase;

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.store.MessageSearches;
import org.apache.james.imap.store.mail.model.Message;

public class SearchUtilsTest extends TestCase {

    private static final String RHUBARD = "Rhubard";

    private static final String CUSTARD = "Custard";

    private static final Date SUN_SEP_9TH_2001 = new Date(1000000000000L);

    private static final int SIZE = 1729;

    private static final String DATE_FIELD = ImapConstants.RFC822_DATE;

    private static final String SUBJECT_FIELD = ImapConstants.RFC822_SUBJECT;

    private static final String RFC822_SUN_SEP_9TH_2001 = "Sun, 9 Sep 2001 09:10:48 +0000 (GMT)";

    private static final String TEXT = RHUBARD + RHUBARD + RHUBARD;

    MessageBuilder builder;

    MessageSearches searches;

    Collection<Long> recent;

    protected void setUp() throws Exception {
        super.setUp();
        recent = new ArrayList<Long>();
        builder = new MessageBuilder();
        builder.uid = 1009;
        searches = new MessageSearches();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMatchSizeLessThan() throws Exception {
        builder.size = SIZE;
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.sizeLessThan(SIZE - 1), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.sizeLessThan(SIZE), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.sizeLessThan(SIZE + 1), row,
                recent));
        assertTrue(searches.isMatch(
                SearchQuery.sizeLessThan(Integer.MAX_VALUE), row, recent));
    }

    public void testMatchSizeMoreThan() throws Exception {
        builder.size = SIZE;
        Message row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.sizeGreaterThan(SIZE - 1), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.sizeGreaterThan(SIZE), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.sizeGreaterThan(SIZE + 1),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .sizeGreaterThan(Integer.MAX_VALUE), row, recent));
    }

    public void testMatchSizeEquals() throws Exception {
        builder.size = SIZE;
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.sizeEquals(SIZE - 1), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.sizeEquals(SIZE), row, recent));
        assertFalse(searches.isMatch(SearchQuery.sizeEquals(SIZE + 1), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.sizeEquals(Integer.MAX_VALUE),
                row, recent));
    }

    public void testMatchInternalDateEquals() throws Exception {
        builder.internalDate = SUN_SEP_9TH_2001;
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(9, 9, 2000),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(8, 9, 2001),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.internalDateOn(9, 9, 2001),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(10, 9, 2001),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateOn(9, 9, 2002),
                row, recent));
    }

    public void testMatchInternalDateBefore() throws Exception {
        builder.internalDate = SUN_SEP_9TH_2001;
        Message row = builder.build();
        assertFalse(searches.isMatch(
                SearchQuery.internalDateBefore(9, 9, 2000), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.internalDateBefore(8, 9, 2001), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.internalDateBefore(9, 9, 2001), row, recent));
        assertTrue(searches.isMatch(
                SearchQuery.internalDateBefore(10, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.internalDateBefore(9, 9, 2002),
                row, recent));
    }

    public void testMatchInternalDateAfter() throws Exception {
        builder.internalDate = SUN_SEP_9TH_2001;
        Message row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.internalDateAfter(9, 9, 2000),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.internalDateAfter(8, 9, 2001),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateAfter(9, 9, 2001),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.internalDateAfter(10, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.internalDateAfter(9, 9, 2002),
                row, recent));
    }

    public void testMatchHeaderDateAfter() throws Exception {
        builder.header(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        Message row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2000), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 8,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter("BOGUS", 9, 9,
                2001), row, recent));
    }

    public void testShouldMatchCapsHeaderDateAfter() throws Exception {
        builder.header(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        Message row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2000), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 8,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter("BOGUS", 9, 9,
                2001), row, recent));
    }

    public void testShouldMatchLowersHeaderDateAfter() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2000), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 8,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateAfter("BOGUS", 9, 9,
                2001), row, recent));
    }

    public void testMatchHeaderDateOn() throws Exception {
        builder.header(DATE_FIELD, RFC822_SUN_SEP_9TH_2001);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 8, 9,
                2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 10,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn("BOGUS", 9, 9,
                2001), row, recent));
    }

    public void testShouldMatchCapsHeaderDateOn() throws Exception {
        builder.header(DATE_FIELD.toUpperCase(), RFC822_SUN_SEP_9TH_2001);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 8, 9,
                2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 10,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn("BOGUS", 9, 9,
                2001), row, recent));
    }

    public void testShouldMatchLowersHeaderDateOn() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 8, 9,
                2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 10,
                9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn(DATE_FIELD, 9, 9,
                2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateOn("BOGUS", 9, 9,
                2001), row, recent));
    }

    public void testMatchHeaderDateBefore() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                8, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore("BOGUS", 9,
                9, 2001), row, recent));
    }

    public void testShouldMatchCapsHeaderDateBefore() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                8, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore("BOGUS", 9,
                9, 2001), row, recent));
    }

    public void testShouldMatchLowersHeaderDateBefore() throws Exception {
        builder.header(DATE_FIELD.toLowerCase(), RFC822_SUN_SEP_9TH_2001);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2000), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                8, 9, 2001), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                9, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD,
                10, 9, 2001), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerDateBefore(DATE_FIELD, 9,
                9, 2002), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerDateBefore("BOGUS", 9,
                9, 2001), row, recent));
    }

    public void testMatchHeaderContainsCaps() throws Exception {
        builder.header(SUBJECT_FIELD, TEXT.toUpperCase());
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    public void testMatchHeaderContainsLowers() throws Exception {
        builder.header(SUBJECT_FIELD, TEXT.toUpperCase());
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    public void testMatchHeaderContains() throws Exception {
        builder.header(SUBJECT_FIELD, TEXT.toUpperCase());
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    public void testShouldMatchLowerHeaderContains() throws Exception {
        builder.header(SUBJECT_FIELD.toLowerCase(), TEXT);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    public void testShouldMatchCapsHeaderContains() throws Exception {
        builder.header(SUBJECT_FIELD.toUpperCase(), TEXT);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                CUSTARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(DATE_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                TEXT), row, recent));
        assertTrue(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                RHUBARD), row, recent));
        assertFalse(searches.isMatch(SearchQuery.headerContains(SUBJECT_FIELD,
                CUSTARD), row, recent));
    }

    public void testMatchHeaderExists() throws Exception {
        builder.header(SUBJECT_FIELD, TEXT);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerExists(DATE_FIELD), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.headerExists(SUBJECT_FIELD),
                row, recent));
    }

    public void testShouldMatchLowersHeaderExists() throws Exception {
        builder.header(SUBJECT_FIELD.toLowerCase(), TEXT);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerExists(DATE_FIELD), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.headerExists(SUBJECT_FIELD),
                row, recent));
    }

    public void testShouldMatchUppersHeaderExists() throws Exception {
        builder.header(SUBJECT_FIELD.toLowerCase(), TEXT);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.headerExists(DATE_FIELD), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.headerExists(SUBJECT_FIELD),
                row, recent));
    }

    public void testShouldMatchUidRange() throws Exception {
        builder.setKey(1, 1729);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.uid(range(1, 1)), row, recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1728, 1728)), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1729, 1729)), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1730, 1730)), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1, 1728)), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1, 1729)), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1729, 1800)), row,
                recent));
        assertFalse(searches.isMatch(SearchQuery
                .uid(range(1730, Long.MAX_VALUE)), row, recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1730,
                Long.MAX_VALUE, 1, 1728)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1730, Long.MAX_VALUE,
                1, 1729)), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .uid(range(1, 1728, 1800, 1810)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.uid(range(1, 1, 1729, 1729)),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.uid(range(1, 1, 1800, 1800)),
                row, recent));
    }

    public void testShouldMatchSeenFlagSet() throws Exception {
        builder.setFlags(true, false, false, false, false, false);
        Message row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    public void testShouldMatchAnsweredFlagSet() throws Exception {
        builder.setFlags(false, false, true, false, false, false);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.ANSWERED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    public void testShouldMatchFlaggedFlagSet() throws Exception {
        builder.setFlags(false, true, false, false, false, false);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    public void testShouldMatchDraftFlagSet() throws Exception {
        builder.setFlags(false, false, false, true, false, false);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    public void testShouldMatchDeletedFlagSet() throws Exception {
        builder.setFlags(false, false, false, false, true, false);
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    public void testShouldMatchSeenRecentSet() throws Exception {
        builder.setFlags(false, false, false, false, false, false);
        Message row = builder.build();
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.FLAGGED),
                row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.DELETED),
                row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsSet(Flags.Flag.RECENT),
                row, recent));
    }

    public void testShouldMatchSeenFlagUnSet() throws Exception {
        builder.setFlags(false, true, true, true, true, true);
        Message row = builder.build();
        recent.add(new Long(row.getUid()));
        assertTrue(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    public void testShouldMatchAnsweredFlagUnSet() throws Exception {
        builder.setFlags(true, true, false, true, true, true);
        Message row = builder.build();
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertTrue(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    public void testShouldMatchFlaggedFlagUnSet() throws Exception {
        builder.setFlags(true, false, true, true, true, true);
        Message row = builder.build();
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertTrue(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    public void testShouldMatchDraftFlagUnSet() throws Exception {
        builder.setFlags(true, true, true, false, true, true);
        Message row = builder.build();
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    public void testShouldMatchDeletedFlagUnSet() throws Exception {
        builder.setFlags(true, true, true, true, false, true);
        Message row = builder.build();
        recent.add(new Long(row.getUid()));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertTrue(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertFalse(searches.isMatch(
                SearchQuery.flagIsUnSet(Flags.Flag.RECENT), row, recent));
    }

    public void testShouldMatchSeenRecentUnSet() throws Exception {
        builder.setFlags(true, true, true, true, true, true);
        Message row = builder.build();
        recent.add(new Long(row.getUid() + 1));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.SEEN),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.FLAGGED), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.ANSWERED), row, recent));
        assertFalse(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.DRAFT),
                row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .flagIsUnSet(Flags.Flag.DELETED), row, recent));
        assertTrue(searches.isMatch(SearchQuery.flagIsUnSet(Flags.Flag.RECENT),
                row, recent));
    }

    public void testShouldMatchAll() throws Exception {
        Message row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.all(), row, recent));
    }

    public void testShouldMatchNot() throws Exception {
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.not(SearchQuery.all()), row,
                recent));
        assertTrue(searches.isMatch(SearchQuery.not(SearchQuery
                .headerExists(DATE_FIELD)), row, recent));
    }

    public void testShouldMatchOr() throws Exception {
        Message row = builder.build();
        assertTrue(searches.isMatch(SearchQuery.or(SearchQuery.all(),
                SearchQuery.headerExists(DATE_FIELD)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.or(SearchQuery
                .headerExists(DATE_FIELD), SearchQuery.all()), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .or(SearchQuery.headerExists(DATE_FIELD), SearchQuery
                        .headerExists(DATE_FIELD)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.or(SearchQuery.all(),
                SearchQuery.all()), row, recent));
    }

    public void testShouldMatchAnd() throws Exception {
        Message row = builder.build();
        assertFalse(searches.isMatch(SearchQuery.and(SearchQuery.all(),
                SearchQuery.headerExists(DATE_FIELD)), row, recent));
        assertFalse(searches.isMatch(SearchQuery.and(SearchQuery
                .headerExists(DATE_FIELD), SearchQuery.all()), row, recent));
        assertFalse(searches.isMatch(SearchQuery
                .and(SearchQuery.headerExists(DATE_FIELD), SearchQuery
                        .headerExists(DATE_FIELD)), row, recent));
        assertTrue(searches.isMatch(SearchQuery.and(SearchQuery.all(),
                SearchQuery.all()), row, recent));
    }
    
    private SearchQuery.NumericRange[] range(long low, long high) {
        SearchQuery.NumericRange[] results = { new SearchQuery.NumericRange(
                low, high) };
        return results;
    }

    private SearchQuery.NumericRange[] range(long lowOne, long highOne,
            long lowTwo, long highTwo) {
        SearchQuery.NumericRange[] results = {
                new SearchQuery.NumericRange(lowOne, highOne),
                new SearchQuery.NumericRange(lowTwo, highTwo) };
        return results;
    }
}
