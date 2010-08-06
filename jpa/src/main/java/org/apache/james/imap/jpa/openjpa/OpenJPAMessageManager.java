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

package org.apache.james.imap.jpa.openjpa;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.jpa.JPAMailboxSessionMapperFactory;
import org.apache.james.imap.jpa.JPAMessageManager;
import org.apache.james.imap.jpa.mail.model.JPAHeader;
import org.apache.james.imap.jpa.mail.model.openjpa.JPAStreamingMailboxMembership;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.util.MailboxEventDispatcher;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

/**
 * OpenJPA implementation of Mailbox
 *
 */
public class OpenJPAMessageManager extends JPAMessageManager {

    private final boolean useStreaming;

    public OpenJPAMessageManager(JPAMailboxSessionMapperFactory mapperFactory,
            MailboxEventDispatcher dispatcher, Mailbox<Long> mailbox, MailboxSession session) throws MailboxException {
        this(mapperFactory, dispatcher, mailbox, session, false);
    }

    public OpenJPAMessageManager(JPAMailboxSessionMapperFactory mapperFactory,
            MailboxEventDispatcher dispatcher, Mailbox<Long> mailbox, MailboxSession session, final boolean useStreaming) throws MailboxException {
        super(mapperFactory, dispatcher, mailbox, session);
        this.useStreaming = useStreaming;
    }

    @Override
    protected MailboxMembership<Long> createMessage(Date internalDate, int size, int bodyStartOctet, InputStream document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) throws MailboxException {
        if (useStreaming) {
            final List<JPAHeader> jpaHeaders = new ArrayList<JPAHeader>(headers.size());
            for (Header header: headers) {
                jpaHeaders.add((JPAHeader) header);
            }
            return new JPAStreamingMailboxMembership(mailbox.getMailboxId(), internalDate, size, flags, document, bodyStartOctet, jpaHeaders, propertyBuilder);
        } else {
            return super.createMessage(internalDate, size, bodyStartOctet, document, flags, headers, propertyBuilder);
        }
    }

}