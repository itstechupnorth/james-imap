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

package org.apache.james.imap.mailbox;

import javax.mail.MessagingException;

import org.apache.james.imap.api.display.HumanReadableText;

public class MailboxException extends MessagingException {

    private static final long serialVersionUID = 4612761817238115904L;

    private final HumanReadableText key;
    
    public MailboxException(final HumanReadableText key, final String message) {
        super(message);
        this.key = key;
    }

    
    public MailboxException(final HumanReadableText key) {
        super(key.toString());
        this.key = key;
    }

    public MailboxException(final HumanReadableText key, Exception cause) {
        super(key.getDefaultValue(), cause);
        this.key = key;
    }

    /**
     * Gets the message key.
     * 
     * @return the key, possibly null
     */
    public final HumanReadableText getKey() {
        final HumanReadableText key;
        if (this.key == null) {
            // API specifies not null but best to default to generic message 
            key = HumanReadableText.GENERIC_FAILURE_DURING_PROCESSING;
        } else {
            key = this.key;
        }
        return key;
    }
}
