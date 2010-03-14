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

import javax.persistence.EntityManagerFactory;

import org.apache.james.imap.jpa.JPAMailbox;
import org.apache.james.imap.jpa.mail.JPAMailboxMapper;
import org.apache.james.imap.jpa.mail.openjpa.OpenJPAMailboxMapper;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.mail.model.Mailbox;

/**
 * OpenJPA implementation of Mailbox
 *
 */
public class OpenJPAMailbox extends JPAMailbox{

    public final static String MAILBOX_MAPPER = "MAILBOX_MAPPER";
    public OpenJPAMailbox(Mailbox mailbox,
    		MailboxSession session, EntityManagerFactory entityManagerfactory) {
		super(mailbox, session, entityManagerfactory);
	}


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.jpa.JPAMailbox#createMailboxMapper(org.apache.james.imap.mailbox.MailboxSession)
     */
	protected JPAMailboxMapper createMailboxMapper(MailboxSession session) {
        JPAMailboxMapper mapper = new OpenJPAMailboxMapper(entityManagerFactory.createEntityManager());

        return mapper;
    }

}
