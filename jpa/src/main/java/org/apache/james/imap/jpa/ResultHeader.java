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
package org.apache.james.imap.jpa;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import javax.mail.MessagingException;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.MessageResult.Content;
import org.apache.james.imap.store.mail.model.Header;

final class ResultHeader implements MessageResult.Header, MessageResult.Content {
    private final String name;

    private final String value;

    private final long size;

    public ResultHeader(final Header header) {
        this(header.getField(), header.getValue());
    }

    public ResultHeader(String name, String value) {
        this.name = name;
        this.value = value;
        size = name.length() + value.length() + 2;
    }

    public Content getContent() throws MessagingException {
        return this;
    }

    public String getName() throws MailboxException {
        return name;
    }

    public String getValue() throws MailboxException {
        return value;
    }

    public long size() {
        return size;
    }

    public void writeTo(StringBuffer buffer) {
        // TODO: sort out encoding
        for (int i = 0; i < name.length(); i++) {
            buffer.append((char) (byte) name.charAt(i));
        }
        buffer.append(':');
        buffer.append(' ');
        for (int i = 0; i < value.length(); i++) {
            buffer.append((char) (byte) value.charAt(i));
        }
    }

    public void writeTo(WritableByteChannel channel) throws IOException {
        writeAll(channel, MessageRowUtils.US_ASCII.encode(name));
        ByteBuffer buffer = ByteBuffer
                .wrap(MessageRowUtils.BYTES_HEADER_FIELD_VALUE_SEP);
        writeAll(channel, buffer);
        writeAll(channel, MessageRowUtils.US_ASCII.encode(value));
    }

    private void writeAll(WritableByteChannel channel, ByteBuffer buffer)
            throws IOException {
        while (channel.write(buffer) > 0) {
            // write more
        }
    }

    public String toString() {
        return "[HEADER " + name + ": " + value + "]";
    }
}