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
package org.apache.james.imap.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.store.transaction.AbstractTransactionalMapper;



/**
 * Abstract Mapper base class for Level 1 Implementations of JCR. So no real transaction management is used. 
 * 
 * The Session.save() will get called on commit() method
 *
 */
public abstract class AbstractJCRMapper extends AbstractTransactionalMapper implements JCRImapConstants{

    private final Session session;
    private final int scaling;

    public AbstractJCRMapper(final Session session, final int scaling) {
        this.session = session;
        this.scaling = scaling;
    }
    
    protected int getScaling() {
        return scaling;
    }
    
    /**
     * Return the JCR Session
     * 
     * @return session
     */
    protected Session getSession() {
        return session;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.TransactionalMapper#destroy()
     */
    public void destroy() {
        // Do nothing
    }

    /**
     * Begin is not supported by level 1 JCR implementations, so just do nothing
     */
    protected void begin() throws MailboxException {    
        // Do nothing
    }

    /**
     * Just call save on the underlying JCR Session, because level 1 JCR implementation does not offer Transactions
     */
    protected void commit() throws MailboxException {
        try {
            if (session.hasPendingChanges()) {
                session.save();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new MailboxException(HumanReadableText.SAVE_FAILED, e);
        }

    }

    /**
     * Rollback is not supported by level 1 JCR implementations, so just do nothing
     */
    protected void rollback() throws MailboxException {
        // no rollback supported by level 1 jcr
    }

    public void dispose() {
        session.logout();
    }
    
    
}
