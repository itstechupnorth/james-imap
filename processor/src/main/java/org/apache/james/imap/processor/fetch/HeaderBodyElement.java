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

/**
 * 
 */
package org.apache.james.imap.processor.fetch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;

import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.message.response.FetchResponse.BodyElement;
import org.apache.james.mailbox.MessageResult;

final class HeaderBodyElement implements BodyElement {
    private final String name;

    private final List<MessageResult.Header> headers;

    private final long size;

    public HeaderBodyElement(final String name, final List<MessageResult.Header> headers) {
        super();
        this.name = name;
        this.headers = headers;
        size = calculateSize(headers);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.message.response.FetchResponse.BodyElement#getName()
     */
    public String getName() {
        return name;
    }

    
    private long calculateSize(List<MessageResult.Header> headers) {
        final int result;
        if (headers.isEmpty()) {
            result = 0;
        } else {
            int count = 0;
            for (final Iterator<MessageResult.Header> it = headers.iterator(); it.hasNext();) {
                MessageResult.Header header = it.next();
                count += header.size() + 2;
            }
            result = count + 2;
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.message.response.FetchResponse.BodyElement#size()
     */
    public long size() {
        return size;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.message.response.FetchResponse.BodyElement#writeTo(java.nio.channels.WritableByteChannel)
     */
    public void writeTo(WritableByteChannel channel) throws IOException {
        ByteBuffer endLine = ByteBuffer.wrap(ImapConstants.BYTES_LINE_END);
        endLine.rewind();
        for (final Iterator<MessageResult.Header> it = headers.iterator(); it.hasNext();) {
            MessageResult.Header header = it.next();
            header.writeTo(channel);
            while (channel.write(endLine) > 0) {
            }
            endLine.rewind();
        }
        while (channel.write(endLine) > 0) {
        }
    }

}