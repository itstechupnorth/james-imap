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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.junit.Before;
import org.junit.Test;

public class MailboxEventAnalyserTest {

    private static final long BASE_SESSION_ID = 99;

    
    private MailboxEventAnalyser analyser;
    MailboxPath mailboxPath = new MailboxPath("namespace", "user", "name");
    
    private final class MyMailboxSession implements MailboxSession {
        private long sessionId;

        public MyMailboxSession(long sessionId) {
            this.sessionId = sessionId;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.mock.MockMailboxSession#getSessionId()
         */
        public long getSessionId() {
            return sessionId;
        }

        public void close() {            
        }

        public Map<Object, Object> getAttributes() {
            return null;
        }

        public Log getLog() {
            return null;
        }

        public String getOtherUsersSpace() {
            return null;
        }

        public char getPathDelimiter() {
            return 0;
        }

        public String getPersonalSpace() {
            return null;
        }

        public Collection<String> getSharedSpaces() {
            return null;
        }

        public User getUser() {
            return null;
        }

        public boolean isOpen() {
            return false;
        }
        
        
    }
    
    @Before
    public void setUp() throws Exception {
        analyser = new MailboxEventAnalyser(BASE_SESSION_ID, mailboxPath);
    }

    @Test
    public void testShouldBeNoSizeChangeOnOtherEvent() throws Exception {
        final MailboxListener.Event event = new MailboxListener.Event(new MyMailboxSession(0), mailboxPath) {};
      
        analyser.event(event);
        assertFalse(analyser.isSizeChanged());
    }

    @Test
    public void testShouldBeNoSizeChangeOnAdded() throws Exception {
        analyser.event(new FakeMailboxListenerAdded(new MyMailboxSession(0), 11, mailboxPath));
        assertTrue(analyser.isSizeChanged());
    }

    @Test
    public void testShouldNoSizeChangeAfterReset() throws Exception {
        analyser.event(new FakeMailboxListenerAdded(new MyMailboxSession(99), 11, mailboxPath));
        analyser.reset();
        assertFalse(analyser.isSizeChanged());
    }

    @Test
    public void testShouldNotSetUidWhenNoSystemFlagChange() throws Exception {
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                new MyMailboxSession(11), 90, new Flags(), mailboxPath);
        analyser.event(update);
        assertNotNull(analyser.flagUpdateUids());
        assertFalse(analyser.flagUpdateUids().iterator().hasNext());
    }

    @Test
    public void testShouldSetUidWhenSystemFlagChange() throws Exception {
        final long uid = 900L;
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                new MyMailboxSession(11), uid, new Flags(), mailboxPath);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.event(update);
        final Iterator<Long> iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(new Long(uid), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldClearFlagUidsUponReset() throws Exception {
        final long uid = 900L;
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                new MyMailboxSession(11), uid, new Flags(), mailboxPath);
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
                new MyMailboxSession(11), uid, new Flags(), mailboxPath);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.setSilentFlagChanges(true);
        analyser.event(update);
        final Iterator<Long> iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertEquals(new Long(uid), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldNotSetUidWhenSystemFlagChangeSameSessionInSilentMode()
            throws Exception {
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                new MyMailboxSession(BASE_SESSION_ID) ,345, new Flags(), mailboxPath);
        update.flags.add(Flags.Flag.ANSWERED);
        analyser.setSilentFlagChanges(true);
        analyser.event(update);
        final Iterator<Long> iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldNotSetUidWhenOnlyRecentFlagUpdated() throws Exception {
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                new MyMailboxSession(BASE_SESSION_ID), 886, new Flags() ,mailboxPath);
        update.flags.add(Flags.Flag.RECENT);
        analyser.event(update);
        final Iterator<Long> iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }
}
