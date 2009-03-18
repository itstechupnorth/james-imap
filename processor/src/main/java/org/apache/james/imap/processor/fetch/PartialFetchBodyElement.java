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

package org.apache.james.imap.processor.fetch;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import org.apache.james.imap.message.response.FetchResponse.BodyElement;

/**
 * Wraps full content to implement a partial fetch.
 */
final class PartialFetchBodyElement implements BodyElement {

    private final BodyElement delegate;

    private final long firstOctet;

    private final long numberOfOctets;

    private final String name;

    public PartialFetchBodyElement(final BodyElement delegate,
            final long firstOctet, final long numberOfOctets) {
        super();
        this.delegate = delegate;
        this.firstOctet = firstOctet;
        this.numberOfOctets = numberOfOctets;
        name = delegate.getName() + "<" + firstOctet + ">";
    }

    public String getName() {
        return name;
    }

    public long size() {
        final long size = delegate.size();
        final long lastOctet = this.numberOfOctets + firstOctet;
        final long result;
        if (firstOctet > size) {
            result = 0;
        } else if (size > lastOctet) {
            result = numberOfOctets;
        } else {
            result = size - firstOctet;
        }
        return result;
    }

    public void writeTo(WritableByteChannel channel) throws IOException {
        PartialWritableByteChannel partialChannel = new PartialWritableByteChannel(
                channel, firstOctet, size());
        delegate.writeTo(partialChannel);
    }

}