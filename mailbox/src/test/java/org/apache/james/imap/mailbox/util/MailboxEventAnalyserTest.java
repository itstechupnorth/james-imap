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
import org.apache.james.imap.mailbox.FakeMailboxListenerAdded;
import org.apache.james.imap.mailbox.FakeMailboxListenerFlagsUpdate;
import org.apache.james.imap.mailbox.util.MailboxEventAnalyser;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class MailboxEventAnalyserTest extends MockObjectTestCase {

    private static final long BASE_SESSION_ID = 99;

    MailboxEventAnalyser analyser;

    protected void setUp() throws Exception {
        super.setUp();
        analyser = new MailboxEventAnalyser(BASE_SESSION_ID, "Mailbox Name");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldBeNoSizeChangeOnOtherEvent() throws Exception {
        final MailboxListener.Event event = mock(MailboxListener.Event.class);
        checking(new Expectations() {{
            oneOf(event).getSessionId();will(returnValue(11L));
        }});
        analyser.event(event);
        assertFalse(analyser.isSizeChanged());
    }

    public void testShouldBeNoSizeChangeOnAdded() throws Exception {
        analyser.event(new FakeMailboxListenerAdded(78, 11));
        assertTrue(analyser.isSizeChanged());
    }

    public void testShouldNoSizeChangeAfterReset() throws Exception {
        analyser.event(new FakeMailboxListenerAdded(99, 11));
        analyser.reset();
        assertFalse(analyser.isSizeChanged());
    }

    public void testShouldNotSetUidWhenNoSystemFlagChange() throws Exception {
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                90, new Flags(), 11);
        analyser.event(update);
        assertNotNull(analyser.flagUpdateUids());
        assertFalse(analyser.flagUpdateUids().iterator().hasNext());
    }

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
