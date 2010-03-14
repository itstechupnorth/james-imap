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
package org.apache.james.imap.store.transaction;

import org.apache.james.imap.mailbox.MailboxException;

/**
 * Mapper which not do any real transaction handling. It just execute the execute() method
 * of the Transaction object without any special handling.
 *  
 * This class is mostly useful for Mapper implementations which not support Transactions
 *
 */
public abstract class NonTransactionalMapper extends AbstractTransactionalMapper {


    /**
     * Do nothing because we don't support transaction
     */
    protected void begin() throws MailboxException {
        // do nothing
        
    }


    /**
     * Do nothing because we don't support transaction
     */
    protected void commit() throws MailboxException {
        // do nothing
        
    }


    /**
     * Do nothing because we don't support transaction
     */
    protected void rollback() throws MailboxException {
        // do nothing
        
    }

}
