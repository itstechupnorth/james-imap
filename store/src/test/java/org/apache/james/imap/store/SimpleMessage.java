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
package org.apache.james.imap.store;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Message;

public class SimpleMessage implements Message {
    
    public byte[] body;
    public List<SimpleHeader> headers;

    public SimpleMessage(byte[] body, final List<SimpleHeader> headers) {
        super();
        this.body = body;
        this.headers = new ArrayList<SimpleHeader>(headers);
    }

    /**
     * Constructs a copy of the given message.
     * All properties are cloned except mailbox and UID.
     * @param mailboxId new mailbox ID
     * @param uid new UID
     * @param original message to be copied, not null
     */
    public SimpleMessage(SimpleMessage original) {
        super();
        this.body = original.getBody();
        final List<SimpleHeader> originalHeaders = original.headers;
        if (originalHeaders == null) {
            this.headers = new ArrayList<SimpleHeader>();
        } else {
            this.headers = new ArrayList<SimpleHeader>(originalHeaders.size());
            for (SimpleHeader header:originalHeaders) {
                this.headers.add(new SimpleHeader(header));
            }
        }
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#getBody()
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#getHeaders()
     */
    public List<Header> getHeaders() {
        return new ArrayList<Header>(headers);
    }
}
