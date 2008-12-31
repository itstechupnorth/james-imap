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

package org.apache.james.imap.functional.jpa;

import org.apache.james.imap.functional.suite.ConcurrentSessions;

public class ConcurrentSessionsTest extends
        ConcurrentSessions {

    public ConcurrentSessionsTest() throws Exception {
        super(JPAHostSystem.build());
    }

    @Override
    public void testConcurrentDeleteSelectedITALY() throws Exception {
    }

    @Override
    public void testConcurrentDeleteSelectedKOREA() throws Exception {
    }

    @Override
    public void testConcurrentDeleteSelectedUS() throws Exception {
    }

    @Override
    public void testConcurrentExistsResponseITALY() throws Exception {
    }

    @Override
    public void testConcurrentExistsResponseKOREA() throws Exception {
    }

    @Override
    public void testConcurrentExistsResponseUS() throws Exception {
    }

    @Override
    public void testConcurrentFetchResponseITALY() throws Exception {
    }

    @Override
    public void testConcurrentFetchResponseKOREA() throws Exception {
    }

    @Override
    public void testConcurrentFetchResponseUS() throws Exception {
    }

    @Override
    public void testConcurrentRenameSelectedITALY() throws Exception {
    }

    @Override
    public void testConcurrentRenameSelectedKOREA() throws Exception {
    }

    @Override
    public void testConcurrentRenameSelectedUS() throws Exception {
    }
    
    

}
