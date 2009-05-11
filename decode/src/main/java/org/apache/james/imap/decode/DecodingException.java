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

package org.apache.james.imap.decode;

import org.apache.james.imap.api.display.HumanReadableTextKey;

/**
 * <p>Indicates that decoding failured.</p>
 * <p>All decoding exception should be supplied with:</p>
 * <ul>
 * <li>A finely grained descriptive string</li>
 * <li>A coursely grained key for i18n</li>
 * </ul>
 * <p>
 * The following keys are frequently used when decoding:
 * </p>
 * <ul>
 * <li>{@link HumanReadableTextKey#ILLEGAL_ARGUMENTS}</li>
 * <li>{@link HumanReadableTextKey#BAD_IO_ENCODING}</li>
 * </ul>
 */
public class DecodingException extends Exception {

    private static final long serialVersionUID = 8719349386686261422L;

    private final HumanReadableTextKey key;
    
    /**
     * Constructs a decoding exception
     * @param key coursely grained i18n, not null
     * @param s specific description suitable for logging, not null
     */
    public DecodingException(final HumanReadableTextKey key, final String s) {
        super(s);
        this.key = key;
    }

    /**
     * Constructs a decoding exception.
     * @param key coursely grained i18n, not null
     * @param s specific description suitable for logging, not null
     * @param t cause, not null
     */
    public DecodingException(final HumanReadableTextKey key, final String s, final Throwable t) {
        super(s, t);
        this.key = key;
    }

    /**
     * Gets the message key.
     * 
     * @return the key, not null
     */
    public final HumanReadableTextKey getKey() {
        final HumanReadableTextKey key;
        if (this.key == null) {
            // API specifies not null but best to default to generic message 
            key = HumanReadableTextKey.ILLEGAL_ARGUMENTS;
        } else {
            key = this.key;
        }
        return key;
    }

}
