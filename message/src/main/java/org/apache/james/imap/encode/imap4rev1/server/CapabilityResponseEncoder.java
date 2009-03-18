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
package org.apache.james.imap.encode.imap4rev1.server;

import java.io.IOException;

import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.encode.base.AbstractChainedImapEncoder;
import org.apache.james.imap.message.response.imap4rev1.server.CapabilityResponse;

/**
 * Encodes <code>CAPABILITY</code> response.
 * See <code>7.2.1</code> of 
 * <a href='http://james.apache.org/server/rfclist/imap4/rfc2060.txt' rel='tag'>RFC2060</a>.
 */
public class CapabilityResponseEncoder extends AbstractChainedImapEncoder {

    public CapabilityResponseEncoder(ImapEncoder next) {
        super(next);
    }

    protected void doEncode(ImapMessage acceptableMessage,
            ImapResponseComposer composer) throws IOException {
        final CapabilityResponse response = (CapabilityResponse) acceptableMessage;
        composer.capabilities(response.getCapabilities());
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof CapabilityResponse);
    }
}
