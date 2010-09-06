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
package org.apache.james.imap.maildir;

import org.apache.james.imap.maildir.mail.MaildirMailboxMapper;
import org.apache.james.imap.maildir.mail.MaildirMessageMapper;
import org.apache.james.imap.maildir.user.MaildirSubscriptionMapper;
import org.apache.james.imap.store.MailboxSessionMapperFactory;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.user.SubscriptionMapper;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SubscriptionException;

public class MaildirMailboxSessionMapperFactory extends
        MailboxSessionMapperFactory<Integer> {

    private final String root;
    
    public MaildirMailboxSessionMapperFactory(String root) {
        this.root = root;
    }
    
    @Override
    protected MailboxMapper<Integer> createMailboxMapper(MailboxSession session)
            throws MailboxException {
        return new MaildirMailboxMapper(root);
    }

    @Override
    protected MessageMapper<Integer> createMessageMapper(MailboxSession session)
            throws MailboxException {
        return new MaildirMessageMapper(root);
    }

    @Override
    protected SubscriptionMapper createSubscriptionMapper(MailboxSession session)
            throws SubscriptionException {
        return new MaildirSubscriptionMapper(root);
    }

}
