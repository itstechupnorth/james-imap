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
package org.apache.james.imap.encode.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.apache.james.imap.api.message.response.Literal;

public class ChannelImapResponseComposer extends AbstractImapResponseComposer{

    private final WritableByteChannel out;


    public ChannelImapResponseComposer(final WritableByteChannel out) {
        super();
        this.out = out;
    }

    public ChannelImapResponseComposer(final WritableByteChannel out,
            final int bufferSize) {
        super(bufferSize);
        this.out = out;
      
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.base.AbstractImapResponseComposer#write(java.nio.ByteBuffer)
     */
    protected void write(final ByteBuffer buffer) throws IOException {
        //System.err.print(new String(buffer.array()));
        while (out.write(buffer) > 0) {
            // Write all
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.base.AbstractImapResponseComposer#writeLiteral(org.apache.james.imap.message.response.Literal)
     */
    protected void writeLiteral(Literal literal) throws IOException {
        literal.writeTo(out);
    }
}
