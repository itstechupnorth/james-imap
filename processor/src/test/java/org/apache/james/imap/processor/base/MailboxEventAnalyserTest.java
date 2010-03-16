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

package org.apache.james.imap.processor.base;

import static org.junit.Assert.*;

import java.util.Iterator;

import javax.mail.Flags;

import org.apache.james.imap.mailbox.MailboxListener;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class MailboxEventAnalyserTest {

    private static final long BASE_SESSION_ID = 99;

    MailboxEventAnalyser analyser;

    private Mockery mockery = new JUnit4Mockery();
    
    @Before
    public void setUp() throws Exception {
        analyser = new MailboxEventAnalyser(BASE_SESSION_ID, "Mailbox Name");
    }

    @Test
    public void testShouldBeNoSizeChangeOnOtherEvent() throws Exception {
        final MailboxListener.Event event = mockery.mock(MailboxListener.Event.class);
        mockery.checking(new Expectations() {{
            oneOf(event).getSessionId();will(returnValue(11L));
        }});
        analyser.event(event);
        assertFalse(analyser.isSizeChanged());
    }

    @Test
    public void testShouldBeNoSizeChangeOnAdded() throws Exception {
        analyser.event(new FakeMailboxListenerAdded(78, 11));
        assertTrue(analyser.isSizeChanged());
    }

    @Test
    public void testShouldNoSizeChangeAfterReset() throws Exception {
        analyser.event(new FakeMailboxListenerAdded(99, 11));
        analyser.reset();
        assertFalse(analyser.isSizeChanged());
    }

    @Test
    public void testShouldNotSetUidWhenNoSystemFlagChange() throws Exception {
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                90, new Flags(), 11);
        analyser.event(update);
        assertNotNull(analyser.flagUpdateUids());
        assertFalse(analyser.flagUpdateUids().iterator().hasNext());
    }

    @Test
    public void testShouldSetUidWhenSystemFlagChange() throws Exception {
        final long uid = 900L;
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                uid, new Flags(), 11);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.event(update);
        final Iterator iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(new Long(uid), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldClearFlagUidsUponReset() throws Exception {
        final long uid = 900L;
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                uid, new Flags(), 11);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.event(update);
        analyser.reset();
        assertNotNull(analyser.flagUpdateUids());
        assertFalse(analyser.flagUpdateUids().iterator().hasNext());
    }

    @Test
    public void testShouldNotSetUidWhenSystemFlagChangeDifferentSessionInSilentMode()
            throws Exception {
        final long uid = 900L;
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                uid, new Flags(), 11);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.setSilentFlagChanges(true);
        analyser.event(update);
        final Iterator iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(new Long(uid), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldNotSetUidWhenSystemFlagChangeSameSessionInSilentMode()
            throws Exception {
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                345, new Flags(), BASE_SESSION_ID);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.setSilentFlagChanges(true);
        analyser.event(update);
        final Iterator iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldNotSetUidWhenOnlyRecentFlagUpdated() throws Exception {
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                886, new Flags(), BASE_SESSION_ID);
        update.flags.add(Flags.Flag.RECENT);
        analyser.event(update);
        final Iterator iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }
}
