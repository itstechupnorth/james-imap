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

public class MailboxException extends MessagingException {

    private static final long serialVersionUID = -4076702573622808863L;

    public MailboxException(final Exception cause) {
        this(cause.getMessage(), cause);
    }
    
    public MailboxException(final Throwable cause) {
        this(cause.getMessage(), cause);
    }
    
    public MailboxException(final String message, final Throwable cause) {
        super(message);
        initCause(cause);
    }
    
    public MailboxException(final String message, final Exception cause) {
        super(message, cause);
    }

    public MailboxException(String message) {
        super(message);
    }
}
