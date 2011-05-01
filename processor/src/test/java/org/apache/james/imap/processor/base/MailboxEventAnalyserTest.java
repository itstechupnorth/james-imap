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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.process.ImapLineHandler;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.UpdatedFlags;
import org.junit.Test;
import org.slf4j.Logger;

public class MailboxEventAnalyserTest {

    private static final long BASE_SESSION_ID = 99;

    
    private MailboxPath mailboxPath = new MailboxPath("namespace", "user", "name");
    
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

        public Logger getLog() {
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

        public SessionType getType() {
            return SessionType.System;
        }
        
        
    }
    
    private class MyImapSession implements ImapSession{
        private MailboxSession mSession;

        public MyImapSession(MailboxSession mSession) {
            this.mSession = mSession;
        }
        
        public boolean supportStartTLS() {
            return false;
        }
        
        public boolean startTLS() {
            return false;
        }
        
        public boolean startCompression() {
            return false;
        }
        
        public void setAttribute(String key, Object value) {            
        }
        
        public void selected(SelectedMailbox mailbox) {
            
        }
        
        public void pushLineHandler(ImapLineHandler lineHandler) {
            
        }
        
        public void popLineHandler() {
            
        }
        
        public void logout() {
            
        }
        
        public boolean isCompressionSupported() {
            return false;
        }
        
        public ImapSessionState getState() {
            return ImapSessionState.AUTHENTICATED;
        }
        
        public SelectedMailbox getSelected() {
            return null;
        }
        
        public Logger getLog() {
            return null;
        }
        
        public Object getAttribute(String key) {
            if (key.equals(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY)) {
                return mSession;
            }
            return null;
        }
        
        public void deselect() {
            
        }
        
        public void authenticated() {
            
        }
    };
    

    @Test
    public void testShouldBeNoSizeChangeOnOtherEvent() throws Exception {
        MyMailboxSession mSession = new MyMailboxSession(0);
        
        MyImapSession imapsession = new MyImapSession(mSession);
        
        MailboxEventAnalyser analyser = new MailboxEventAnalyser(imapsession, mailboxPath, new Flags());

        final MailboxListener.Event event = new MailboxListener.Event(mSession, mailboxPath) {};
      
        analyser.event(event);
        assertFalse(analyser.isSizeChanged());
    }

    @Test
    public void testShouldBeNoSizeChangeOnAdded() throws Exception {
        MyMailboxSession mSession = new MyMailboxSession(0);
        
        MyImapSession imapsession = new MyImapSession(mSession);
        
        MailboxEventAnalyser analyser = new MailboxEventAnalyser(imapsession, mailboxPath, new Flags());
        
        analyser.event(new FakeMailboxListenerAdded(mSession, Arrays.asList(11L), mailboxPath));
        assertTrue(analyser.isSizeChanged());
    }

    @Test
    public void testShouldNoSizeChangeAfterReset() throws Exception {
        MyMailboxSession mSession = new MyMailboxSession(99);
        
        MyImapSession imapsession = new MyImapSession(mSession);
        
        MailboxEventAnalyser analyser = new MailboxEventAnalyser(imapsession, mailboxPath, new Flags());
        
        analyser.event(new FakeMailboxListenerAdded(mSession,  Arrays.asList(11L), mailboxPath));
        analyser.reset();
        assertFalse(analyser.isSizeChanged());
    }

    @Test
    public void testShouldNotSetUidWhenNoSystemFlagChange() throws Exception {
        MyMailboxSession mSession = new MyMailboxSession(11);
        
        MyImapSession imapsession = new MyImapSession(mSession);
        
        MailboxEventAnalyser analyser = new MailboxEventAnalyser(imapsession, mailboxPath, new Flags());
        
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                mSession,  Arrays.asList(90L),  Arrays.asList(new UpdatedFlags(90, new Flags(), new Flags())), mailboxPath);
        analyser.event(update);
        assertNotNull(analyser.flagUpdateUids());
        assertFalse(analyser.flagUpdateUids().iterator().hasNext());
    }

    @Test
    public void testShouldSetUidWhenSystemFlagChange() throws Exception {
        final long uid = 900L;
        MyMailboxSession mSession = new MyMailboxSession(11);
        
        MyImapSession imapsession = new MyImapSession(mSession);
        
        MailboxEventAnalyser analyser = new MailboxEventAnalyser(imapsession, mailboxPath, new Flags());
        
        
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                mSession, Arrays.asList(uid), Arrays.asList(new UpdatedFlags(uid, new Flags(), new Flags(Flags.Flag.ANSWERED))), mailboxPath);
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
        MyMailboxSession mSession = new MyMailboxSession(11);
        MyImapSession imapsession = new MyImapSession(mSession);
        MailboxEventAnalyser analyser = new MailboxEventAnalyser(imapsession, mailboxPath, new Flags());
        
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                mSession, Arrays.asList(uid), Arrays.asList(new UpdatedFlags(uid, new Flags(), new Flags(Flags.Flag.ANSWERED))), mailboxPath);
        analyser.event(update);
        analyser.event(update);
        analyser.reset();
        assertNotNull(analyser.flagUpdateUids());
        assertFalse(analyser.flagUpdateUids().iterator().hasNext());
    }

    @Test
    public void testShouldSetUidWhenSystemFlagChangeDifferentSessionInSilentMode()
            throws Exception {
        final long uid = 900L;
        
        MyMailboxSession mSession = new MyMailboxSession(11);
        MyImapSession imapsession = new MyImapSession(mSession);
        MailboxEventAnalyser analyser = new MailboxEventAnalyser(imapsession, mailboxPath,new Flags());
        
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                new MyMailboxSession(BASE_SESSION_ID), Arrays.asList(uid), Arrays.asList(new UpdatedFlags(uid, new Flags(), new Flags(Flags.Flag.ANSWERED))), mailboxPath);
        analyser.event(update);
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
        MyMailboxSession mSession = new MyMailboxSession(BASE_SESSION_ID);
        MyImapSession imapsession = new MyImapSession(mSession);
        MailboxEventAnalyser analyser = new MailboxEventAnalyser(imapsession, mailboxPath, new Flags());
        
        
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                mSession, Arrays.asList(345L), Arrays.asList(new UpdatedFlags(345, new Flags(), new Flags())), mailboxPath);
        analyser.event(update);
        analyser.setSilentFlagChanges(true);
        analyser.event(update);
        final Iterator<Long> iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testShouldNotSetUidWhenOnlyRecentFlagUpdated() throws Exception {
        MyMailboxSession mSession = new MyMailboxSession(BASE_SESSION_ID);
        MyImapSession imapsession = new MyImapSession(mSession);
        MailboxEventAnalyser analyser = new MailboxEventAnalyser(imapsession, mailboxPath, new Flags());
        
        
        final FakeMailboxListenerFlagsUpdate update = new FakeMailboxListenerFlagsUpdate(
                mSession, Arrays.asList(886L), Arrays.asList(new UpdatedFlags(886, new Flags(), new Flags(Flags.Flag.RECENT))), mailboxPath);
        analyser.event(update);
        final Iterator<Long> iterator = analyser.flagUpdateUids().iterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
    }
}
