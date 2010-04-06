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
package org.apache.james.imap.store.mail.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Abstract base class for {@link Document}
 *
 */
public abstract class AbstractDocument implements Document{

    protected ByteBuffer getContentAsByteBuffer(InputStream content) {
        ByteArrayOutputStream full = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = content.read(buf)) != -1) {
                full.write(buf, 0, i);
            }
        } catch (IOException e) {
            // what todo here ?
        }
        return ByteBuffer.wrap(full.toByteArray()).asReadOnlyBuffer();
        
    }
    /**
     * @see org.apache.james.imap.store.mail.model.Document#getBodyContent()
     */    
    public ByteBuffer getBodyContent() {
        final ByteBuffer contentBuffer = getFullContent();
        contentBuffer.position(getBodyStartOctet());
        return contentBuffer.slice();
    }
    
    /**
     * The number of octets contained in the body of this part.
     * 
     * @return number of octets
     */
    public long getBodyOctets() {
        return getFullContentOctets() - getBodyStartOctet();
    }
    
    /**
     * Return the start octet of the body
     * 
     * @return startOctet
     */
    protected abstract int getBodyStartOctet();
    


    public final class ByteBufferInputStream extends InputStream {
        private ByteBuffer buf;

        public ByteBufferInputStream(ByteBuffer buf)  {
            this.buf = buf;
        }
        
        public synchronized int read() throws IOException {             
            return buf.hasRemaining() ? buf.get() : -1;
        }

        public synchronized int read(byte[] bytes, int off, int len) throws IOException {           
            int rv = Math.min(len, buf.remaining());                
            buf.get(bytes, off, rv);
            return rv == 0 ? -1 : rv;
        }
    }

}
