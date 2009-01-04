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

package org.apache.james.imap.mailbox.util;

import java.util.Iterator;

import javax.mail.Flags;

import org.apache.james.imap.mailbox.MailboxListener;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class MailboxEventDispatcherFlagsTest extends MockObjectTestCase {

    MailboxEventDispatcher dispatcher;

    EventCollector collector;

    MessageResult result;

    int sessionId = 10;

    Mock mockMessageResult;

    protected void setUp() throws Exception {
        super.setUp();
        dispatcher = new MailboxEventDispatcher();
        collector = new EventCollector();
        dispatcher.addMailboxListener(collector);
        mockMessageResult = mock(MessageResult.class);
        mockMessageResult.expects(once()).method("getUid").will(
                returnValue(23L));
        result = (MessageResult) mockMessageResult.proxy();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldReturnNoChangesWhenOriginalNull() throws Exception {
        dispatcher.flagsUpdated(result.getUid(), sessionId, null, new Flags(
                Flags.Flag.DELETED));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }

    public void testShouldReturnNoChangesWhenSystemFlagsUnchanged() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(
                Flags.Flag.DELETED), new Flags(Flags.Flag.DELETED));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowAnsweredAdded() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(),
                new Flags(Flags.Flag.ANSWERED));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.ANSWERED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowAnsweredRemoved() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(
                Flags.Flag.ANSWERED), new Flags());
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.ANSWERED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowDeletedAdded() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(),
                new Flags(Flags.Flag.DELETED));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.DELETED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowDeletedRemoved() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(
                Flags.Flag.DELETED), new Flags());
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.DELETED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowDraftAdded() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(),
                new Flags(Flags.Flag.DRAFT));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.DRAFT, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowDraftRemoved() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(
                Flags.Flag.DRAFT), new Flags());
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.DRAFT, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowFlaggedAdded() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(),
                new Flags(Flags.Flag.FLAGGED));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.FLAGGED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowFlaggedRemoved() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(
                Flags.Flag.FLAGGED), new Flags());
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.FLAGGED, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowRecentAdded() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(),
                new Flags(Flags.Flag.RECENT));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.RECENT, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowRecentRemoved() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(
                Flags.Flag.RECENT), new Flags());
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.RECENT, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowSeenAdded() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(),
                new Flags(Flags.Flag.SEEN));
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.SEEN, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowSeenRemoved() {
        dispatcher.flagsUpdated(result.getUid(), sessionId, new Flags(
                Flags.Flag.SEEN), new Flags());
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.SEEN, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testShouldShowMixedChanges() {
        Flags originals = new Flags();
        originals.add(Flags.Flag.DRAFT);
        originals.add(Flags.Flag.RECENT);
        Flags updated = new Flags();
        updated.add(Flags.Flag.ANSWERED);
        updated.add(Flags.Flag.DRAFT);
        updated.add(Flags.Flag.SEEN);

        dispatcher.flagsUpdated(result.getUid(), sessionId, originals, updated);
        assertEquals(1, collector.events.size());
        assertTrue(collector.events.get(0) instanceof MailboxListener.FlagsUpdated);
        MailboxListener.FlagsUpdated event = (MailboxListener.FlagsUpdated) collector.events
                .get(0);
        Iterator iterator = event.flagsIterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.ANSWERED, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.RECENT, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Flags.Flag.SEEN, iterator.next());
        assertFalse(iterator.hasNext());
    }
}