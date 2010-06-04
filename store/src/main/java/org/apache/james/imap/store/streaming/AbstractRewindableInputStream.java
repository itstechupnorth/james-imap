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
import java.io.OutputStream;

/**
 * {@link RewindableInputStream} which support the get rewinded. This is done by copy over every byte
 * over to another {@link OutputStream}. What {@link OutputStream} to use is up to the implementations.
 * 
 * The rewinding will get delayed as long as possible. So if you call
 * rewind, it will only get performed when needed
 * 
 * Be sure to call {@link #close()} once you done reading from the object. This will
 * remove all temporary data
 * 
 *
 */
public abstract class AbstractRewindableInputStream extends RewindableInputStream{

    protected boolean end = false;

    public AbstractRewindableInputStream(InputStream in) throws IOException {
        super(in);
    }

    /**
     * Return the OutputStream to which the data should get written when they are read the 
     * first time
     * 
     * @return output
     * @throws IOException
     */
    protected abstract OutputStream getRewindOutputStream() throws IOException;
    
    /**
     * Return the InputStream which should get used after the stream was rewinded
     * 
     * @return rewindInput
     * @throws IOException
     */
    protected abstract InputStream getRewindInputStream() throws IOException;

    /**
     * Dispose all temporary data
     * 
     * @throws IOException
     */
    protected abstract void dispose() throws IOException;
    
    /**
     * Get called after the rewind was complete
     * 
     * @throws IOException
     */
    protected abstract void afterRewindComplete() throws IOException;
    
    /**
     * Close the stream and dispose all temporary data
     * 
     */
    public void close() throws IOException {
        try {
            in.close();
            OutputStream out = getRewindOutputStream();
            if (out != null) {
                out.close();
            }
            InputStream in = getRewindInputStream();
            if (in != null) {
                in.close();
            }
        } finally {
            dispose();
        }
    }

    
    /**
     * Read data and write and store it for later usage once the rewind was done
     */
    @Override
    public int read() throws IOException {   
        int i;
        if (needsRewind()) {

            rewindIfNeeded();
        }
        
        if (end == false) {
            i = in.read();
            if (i == -1) {
                end = true;
            } else {
                getRewindOutputStream().write(i);
            }
        } else {
            i = getRewindInputStream().read();
        }
        return i;
    }

    /**
     * Read data and write and store it for later usage once the rewind was done
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (needsRewind()) {
            rewindIfNeeded();
        }    
        
        int i;
        if (end == false) {
            i = in.read(b, off, len);
            if (i == -1) {
                end = true;
            }
            getRewindOutputStream().write(b, off, len);
        } else {
            i = getRewindInputStream().read(b,off,len);
        }
        return i;
    }

    /**
     * Read data and write and store it for later usage once the rewind was done
     */
    @Override
    public void rewindIfNeeded() throws IOException {
        if (needsRewind()) {
            rewindDone();
            
            if (end == false) {
                while ( read() != -1);
            }
            // we don't need the original InputStream anymore so close it
            in.close();
            afterRewindComplete();
        }        
    }

    @Override
    public int available() throws IOException {
        if (end == false) {
            return in.available();
        } else {
            return getRewindInputStream().available();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        for (int i = 0; i < n; i++) {
            if (read() == -1) {
                return n -i;
            }
            if (end) break;
        }
        return 0;
    }
}
