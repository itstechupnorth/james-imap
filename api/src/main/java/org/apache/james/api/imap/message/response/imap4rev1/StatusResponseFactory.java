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

package org.apache.james.api.imap.message.response.imap4rev1;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.display.HumanReadableTextKey;

/**
 * Constructs {@link StatusResponse} instances.
 * This interface enforces RFC2060 rules.
 */
public interface StatusResponseFactory {

    /**
     * Creates a tagged OK status response.
     * @param tag operation tag, not null
     * @param command <code>ImapCommand</code>, not null
     * @param displayTextKey key to the human readable code to be displayed
     * @param code <code>ResponseCode</code>, not null
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse taggedOk(String tag, ImapCommand command, HumanReadableTextKey displayTextKey, StatusResponse.ResponseCode code);
    
    /**
     * Creates a tagged NO status response.
     * @param tag <code>CharSequence</code>, not null
     * @param command <code>ImapCommand</code>, not null
     * @param displayTextKey key to the human readable code to be displayed
     * @param code <code>ResponseCode</code>, not null
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse taggedNo(String tag, ImapCommand command, HumanReadableTextKey displayTextKey, StatusResponse.ResponseCode code);
    
    /**
     * Creates a tagged BAD status response.
     * @param tag <code>CharSequence</code>, not null
     * @param command <code>ImapCommand</code>, not null
     * @param displayTextKey key to the human readable code to be displayed
     * @param code <code>ResponseCode</code>, not null
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse taggedBad(String tag, ImapCommand command, HumanReadableTextKey displayTextKey, StatusResponse.ResponseCode code);

    /**
     * Creates a untagged OK status response.
     * @param displayTextKey key to the human readable code to be displayed
     * @param code <code>ResponseCode</code>, not null
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse untaggedOk(HumanReadableTextKey displayTextKey, StatusResponse.ResponseCode code);

    /**
     * Creates a untagged NO status response.
     * @param displayTextKey key to the human readable code to be displayed
     * @param code <code>ResponseCode</code>, not null
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse untaggedNo(HumanReadableTextKey displayTextKey, StatusResponse.ResponseCode code);

    /**
     * Creates a untagged BAD status response.
     * @param displayTextKey key to the human readable code to be displayed
     * @param code <code>ResponseCode</code>, not null
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse untaggedBad(HumanReadableTextKey displayTextKey, StatusResponse.ResponseCode code);

    /**
     * Creates a PREAUTH status response.
     * These are always untagged.
     * @param displayTextKey key to the human readable code to be displayed
     * @param code <code>ResponseCode</code>, not null
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse preauth(HumanReadableTextKey displayTextKey, StatusResponse.ResponseCode code);

    /**
     * Creates a BYE status response.
     * These are always untagged.
     * @param displayTextKey key to the human readable code to be displayed
     * @param code <code>ResponseCode</code>, not null
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse bye(HumanReadableTextKey displayTextKey, StatusResponse.ResponseCode code);

    /**
     * Creates a tagged OK status response.
     * @param tag <code>CharSequence</code>, not null
     * @param command <code>ImapCommand</code>, not null
     * @param displayTextKey key to the human readable code to be displayed
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse taggedOk(String tag, ImapCommand command, HumanReadableTextKey displayTextKey);
    
    /**
     * Creates a tagged NO status response.
     * @param tag <code>CharSequence</code>, not null
     * @param command <code>ImapCommand</code>, not null
     * @param displayTextKey key to the human readable code to be displayed
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse taggedNo(String tag, ImapCommand command, HumanReadableTextKey displayTextKey);
    
    /**
     * Creates a tagged BAD status response.
     * @param tag <code>CharSequence</code>, not null
     * @param command <code>ImapCommand</code>, not null
     * @param displayTextKey key to the human readable code to be displayed
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse taggedBad(String tag, ImapCommand command, HumanReadableTextKey displayTextKey);

    /**
     * Creates a untagged OK status response.
     * @param displayTextKey key to the human readable code to be displayed
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse untaggedOk(HumanReadableTextKey displayTextKey);

    /**
     * Creates a untagged NO status response.
     * @param displayTextKey key to the human readable code to be displayed
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse untaggedNo(HumanReadableTextKey displayTextKey);

    /**
     * Creates a untagged BAD status response.
     * @param displayTextKey key to the human readable code to be displayed
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse untaggedBad(HumanReadableTextKey displayTextKey);

    /**
     * Creates a PREAUTH status response.
     * These are always untagged.
     * @param displayTextKey key to the human readable code to be displayed
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse preauth(HumanReadableTextKey displayTextKey);

    /**
     * Creates a BYE status response.
     * These are always untagged.
     * @param displayTextKey key to the human readable code to be displayed
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse bye(HumanReadableTextKey displayTextKey);

}