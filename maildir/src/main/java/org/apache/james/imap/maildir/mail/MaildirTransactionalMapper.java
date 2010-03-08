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

package org.apache.james.imap.maildir.mail;

import java.util.concurrent.TimeUnit;

import org.apache.commons.transaction.file.TxFileResourceManager;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.transaction.AbstractTransactionalMapper;

/**
 * TransactionManager which supports Transactions for Maildir
 *
 */
public class MaildirTransactionalMapper extends AbstractTransactionalMapper{

    protected final TxFileResourceManager manager;
    
    public MaildirTransactionalMapper(final TxFileResourceManager manager) {
        this.manager = manager;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.AbstractTransactionalMapper#begin()
     */
    protected void begin() throws StorageException {
        manager.startTransaction(30, TimeUnit.SECONDS);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.AbstractTransactionalMapper#commit()
     */
    protected void commit() throws StorageException {
        manager.commitTransaction();
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.AbstractTransactionalMapper#rollback()
     */
    protected void rollback() throws StorageException {
        manager.rollbackTransaction();
    }

}
