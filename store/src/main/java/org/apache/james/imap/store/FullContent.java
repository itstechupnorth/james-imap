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
package org.apache.james.imap.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;

import org.apache.james.imap.mailbox.Content;
import org.apache.james.imap.mailbox.MessageResult;

/**
 * Content which holds the full content, including {@link Header} objets
 *
 */
public final class FullContent implements Content {
    private final ByteBuffer contents;

    private final List<MessageResult.Header> headers;

    private final long size;

    public FullContent(final ByteBuffer contents, final List<MessageResult.Header> headers) {
        this.contents = contents;
        this.headers = headers;
        this.size = caculateSize();
    }

    private long caculateSize() {
        long result = contents.limit();
        result += 2;
        for (final Iterator<MessageResult.Header> it = headers.iterator(); it.hasNext();) {
            final MessageResult.Header header = it.next();
            if (header != null) {
                result += header.size();
                result += 2;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Content#size()
     */
    public long size() {
        return size;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.mailbox.Content#writeTo(java.nio.channels.WritableByteChannel)
     */
    public void writeTo(WritableByteChannel channel) throws IOException {
        ByteBuffer newLine = ByteBuffer.wrap(ResultUtils.BYTES_NEW_LINE);
        for (final Iterator<MessageResult.Header> it = headers.iterator(); it.hasNext();) {
            final MessageResult.Header header = it.next();
            if (header != null) {
                header.writeTo(channel);
            }
            newLine.rewind();
            writeAll(channel, newLine);
        }
        newLine.rewind();
        writeAll(channel, newLine);
        contents.rewind();
        writeAll(channel, contents);
    }

    /**
     * Write all 
     * 
     * @param channel
     * @param buffer
     * @throws IOException
     */
    private void writeAll(WritableByteChannel channel, ByteBuffer buffer)
            throws IOException {
        while (channel.write(buffer) > 0) {
            // write more
        }
    }
}