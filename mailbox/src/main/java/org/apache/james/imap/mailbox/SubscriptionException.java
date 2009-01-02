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

import org.apache.james.api.imap.display.HumanReadableTextKey;

/**
 * Indicates exception during subscription processing.
 */
public class SubscriptionException extends MailboxException {

    private static final long serialVersionUID = -2057022968413471837L;

    private final HumanReadableTextKey key;

    public SubscriptionException(HumanReadableTextKey key, Throwable cause) {
        super(key.toString(), cause);
        this.key = key;
    }

    public SubscriptionException(HumanReadableTextKey key) {
        super(key.toString());
        this.key = key;
    }

    public SubscriptionException(Throwable cause) {
        super(cause);
        key = null;
    }

    /**
     * Gets the message key.
     * 
     * @return the key, possibly null
     */
    public final HumanReadableTextKey getKey() {
        return key;
    }

}