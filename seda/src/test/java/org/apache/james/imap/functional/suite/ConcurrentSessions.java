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

package org.apache.james.imap.functional.suite;

import java.util.Locale;

import org.apache.james.imap.functional.FrameworkForAuthenticatedState;
import org.apache.james.test.functional.HostSystem;

public class ConcurrentSessions extends FrameworkForAuthenticatedState {

    public ConcurrentSessions(HostSystem system) {
        super(system);
    }
    
    public void testConcurrentExpungeResponseUS() throws Exception {
          scriptTest("ConcurrentExpungeResponse", Locale.US);
    }

    public void testConcurrentExpungeResponseITALY() throws Exception {
        scriptTest("ConcurrentExpungeResponse", Locale.ITALY);
    }

    public void testConcurrentExpungeResponseKOREA() throws Exception {
        scriptTest("ConcurrentExpungeResponse", Locale.KOREA);
    }

    public void testConcurrentCrossExpungeUS() throws Exception {
          scriptTest("ConcurrentCrossExpunge", Locale.US);
    }
    
    public void testConcurrentCrossExpungeITALY() throws Exception {
          scriptTest("ConcurrentCrossExpunge", Locale.ITALY);
    }
    
    public void testConcurrentCrossExpungeKOREA() throws Exception {
          scriptTest("ConcurrentCrossExpunge", Locale.KOREA);
    }
    
    public void testConcurrentRenameSelectedSubUS() throws Exception {
        scriptTest("ConcurrentRenameSelectedSub", Locale.US);
    }

    public void testConcurrentExistsResponseUS() throws Exception {
        scriptTest("ConcurrentExistsResponse", Locale.US);
    }

    public void testConcurrentDeleteSelectedUS() throws Exception {
        scriptTest("ConcurrentDeleteSelected", Locale.US);
    }

    public void testConcurrentFetchResponseUS() throws Exception {
        scriptTest("ConcurrentFetchResponse", Locale.US);
    }

    public void testConcurrentRenameSelectedUS() throws Exception {
        scriptTest("ConcurrentRenameSelected", Locale.US);
    }

    public void testConcurrentRenameSelectedSubKOREA() throws Exception {
        scriptTest("ConcurrentRenameSelectedSub", Locale.KOREA);
    }
    
    public void testConcurrentExistsResponseKOREA() throws Exception {
        scriptTest("ConcurrentExistsResponse", Locale.KOREA);
    }

    public void testConcurrentDeleteSelectedKOREA() throws Exception {
        scriptTest("ConcurrentDeleteSelected", Locale.KOREA);
    }

    public void testConcurrentFetchResponseKOREA() throws Exception {
        scriptTest("ConcurrentFetchResponse", Locale.KOREA);
    }

    public void testConcurrentRenameSelectedKOREA() throws Exception {
        scriptTest("ConcurrentRenameSelected", Locale.KOREA);
    }

    public void testConcurrentRenameSelectedSubITALY() throws Exception {
        scriptTest("ConcurrentRenameSelectedSub", Locale.ITALY);
    }
    
    public void testConcurrentExistsResponseITALY() throws Exception {
        scriptTest("ConcurrentExistsResponse", Locale.ITALY);
    }

    public void testConcurrentDeleteSelectedITALY() throws Exception {
        scriptTest("ConcurrentDeleteSelected", Locale.ITALY);
    }

    public void testConcurrentFetchResponseITALY() throws Exception {
        scriptTest("ConcurrentFetchResponse", Locale.ITALY);
    }

    public void testConcurrentRenameSelectedITALY() throws Exception {
        scriptTest("ConcurrentRenameSelected", Locale.ITALY);
    }
}
