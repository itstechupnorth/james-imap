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
package org.apache.james.imap.jpa.mail.model.openjpa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jpa.mail.model.AbstractJPAMailboxMembership;
import org.apache.james.imap.jpa.mail.model.JPAHeader;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.store.mail.model.Document;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

@Entity(name="Membership")
public class JPAStreamingMailboxMembership extends AbstractJPAMailboxMembership{


    /** The value for the body field. Lazy loaded */
    @ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY) private JPAStreamingMessage message;
  
    
    /**
     * For enhancement only.
     */
    @Deprecated
    public JPAStreamingMailboxMembership() {}

    public JPAStreamingMailboxMembership(long mailboxId, long uid, Date internalDate, int size, Flags flags, 
            InputStream content, int bodyStartOctet, final List<JPAHeader> headers, final PropertyBuilder propertyBuilder) throws MailboxException {
        super(mailboxId, uid, internalDate, size, flags, bodyStartOctet, headers, propertyBuilder);  
        this.message = new JPAStreamingMessage(content, size, bodyStartOctet, headers, propertyBuilder);
        
       
    }

    public JPAStreamingMailboxMembership(long mailboxId, long uid, AbstractJPAMailboxMembership original) throws MailboxException {
        super(mailboxId, uid, original);
        try {
            this.message = new JPAStreamingMessage((JPAStreamingMessage) original.getDocument());
        } catch (IOException e) {
            throw new MailboxException(HumanReadableText.FAILURE_MAILBOX_EXISTS,e);
        }
    }
    
    /**
     * Gets the message member.
     * @return message, not null
     */
    public Document getDocument() {
        return message;
    }
    

}
