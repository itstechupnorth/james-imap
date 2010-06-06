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


import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.Locked;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.mail.model.JCRMailbox;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.UidConsumer;
import org.apache.james.imap.store.mail.model.Mailbox;

/**
 * {@link UidConsumer} implementation for JCR. It use the {@link Locked} class to lock the Mailbox Node while
 * consume the next uid. So we can be sure that its really unique
 * 
 *
 */
public class JCRUidConsumer implements UidConsumer<String>{

    private MailboxSessionJCRRepository repos;
    private NodeLocker locker;
    
    public JCRUidConsumer(MailboxSessionJCRRepository repos, NodeLocker locker) {
        this.repos = repos;
        this.locker = locker;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.UidConsumer#reserveNextUid(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.imap.mailbox.MailboxSession)
     */
    public long reserveNextUid(Mailbox<String> mailbox, MailboxSession session) throws MailboxException {
        try {
            Session jcrSession = repos.login(session);
            Node node = jcrSession.getNodeByIdentifier(mailbox.getMailboxId());
            long result = locker.execute(new NodeLocker.NodeLockedExecution<Long>() {

                public Long execute(Node node) throws RepositoryException {
                    Property uidProp = node.getProperty(JCRMailbox.LASTUID_PROPERTY);
                    long uid = uidProp.getLong();
                    uid++;
                    uidProp.setValue(uid);
                    uidProp.getSession().save();
                    return uid;
                }

                public boolean isDeepLocked() {
                    return true;
                }
                
            }, node, Long.class);
            return result;
            
         
        } catch (RepositoryException e) {
            throw new MailboxException(HumanReadableText.COMSUME_UID_FAILED, e);
        } catch (InterruptedException e) {
            throw new MailboxException(HumanReadableText.COMSUME_UID_FAILED, e);
        }

    }

}
