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
package org.apache.james.imap.store.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import org.apache.james.mailbox.MessageResult;


/**
 * {@link AbstractFullContent} implementations which uses an {@link InputStream} as source for the 
 * body content
 *
 */
public class InputStreamFullContent extends AbstractFullContent{

    private final RewindableInputStream in;
    private long size;


    public InputStreamFullContent(final RewindableInputStream contents, final List<MessageResult.Header> headers) throws IOException{
        super(headers);
        this.in = contents;
        this.size = caculateSize();
    }

    


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Content#size()
     */
    public final long size() {
        return size;
    }

    @Override
    protected void bodyWriteTo(WritableByteChannel channel) throws IOException {
        // rewind the stream before write it to the channel
        in.rewind();
        
        // read all the content of the underlying InputStream in 16384 byte chunks, wrap them
        // in a ByteBuffer and finally write the Buffer to the channel
        byte[] buf = new byte[16384];
        int i = 0;
        while ((i = in.read(buf)) != -1) {
            ByteBuffer buffer = ByteBuffer.wrap(buf);
            // set the limit of the buffer to the returned bytes
            buffer.limit(i);
            channel.write(buffer);
        }  
    }


    @Override
    protected long getBodySize() throws IOException{
        return in.available();
    }

}
