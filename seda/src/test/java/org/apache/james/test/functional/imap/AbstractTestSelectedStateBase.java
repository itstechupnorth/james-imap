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


package org.apache.james.test.functional.imap;



/**
 * <p>Runs tests for commands valid only in the SELECTED state. A login session
 * and setup of a "seleted" mailbox precedes the execution of the test elements.
 * </p><p>
 * Recommended scripts:
 * </p><ul>
 * <li>Check"</li>
 * <li>Expunge"</li>
 * <li>Search"</li>
 * <li>FetchSingleMessage"</li>
 * <li>FetchMultipleMessages"</li>
 * <li>FetchPeek"</li>
 * <li>Store"</li>
 * <li>Copy"</li>
 * <li>Uid"</li>
 * </ul>
 */
public abstract class AbstractTestSelectedStateBase
        extends AbstractTestForAuthenticatedState
{
    public AbstractTestSelectedStateBase( HostSystem system )
    {
        super( system );
    }

    /**
     * Superclass sets up welcome message and login session in {@link #preElements}.
     * A "SELECT INBOX" session is then added to these elements.
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "SelectedStateSetup.test", preElements );
        addTestFile( "SelectedStateCleanup.test", postElements );
    }
}