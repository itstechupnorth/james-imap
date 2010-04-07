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
package org.apache.james.imap.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link FilterInputStream} which support the get rewinded. This is done by copy over every byte
 * to a File after it was read. The rewinding will get delayed as long as possible. So if you call
 * rewind, it will only get performed when needed
 * 
 *
 */
public class RewindableInputStream extends FilterInputStream{

    private File f;
    private OutputStream fOut;
    private InputStream fIn;
    protected boolean end = false;
    protected boolean rewind;

    public RewindableInputStream(InputStream in) throws IOException {
        super(in);
        f = File.createTempFile("rewindable", ".tmp");
        fOut = new FileOutputStream(f);
        fIn = new FileInputStream(f);
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
            fOut.close();
            fIn.close();
        } finally {
            f.delete();
        }
    }

    @Override
    public int read() throws IOException {        
        // rewind if we need to
        rewindIfNeeded();
        
        int i;

        if (end == false) {
            i = in.read();
            if (i == -1) {
                end = true;
            } else {
                fOut.write(i);
            }
        } else {
            i = fIn.read();
        }
        return i;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        
        // rewind if we need to
        rewindIfNeeded();
        
        int i;
        if (end == false) {
            i = in.read(b, off, len);
            if (i == -1) {
                end = true;
            }
            fOut.write(b, off, len);
        } else {
            i = fIn.read(b,off,len);
        }
        return i;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b,0,b.length);
    }
    
    /**
     * Mark the stream for rewind. The rewind itself will get delayed as long as possible
     */
    public void rewind() {
        rewind = true;
    }

    /**
     * Check if the stream needs to get rewind
     * 
     * @return true if the stream is marked for rewind
     */
    protected boolean needsRewind() {
        return rewind;
    }
    
    /**
     * Do the real rewind if needed
     * 
     * @throws IOException
     */
    protected void rewindIfNeeded() throws IOException {
        if (needsRewind()) {
            rewind = false;

            if (end == false) {
                while ( read() != -1);
            }
            fIn = new FileInputStream(f);
        }
    }
    
    @Override
    public int available() throws IOException {
        if (end == false) {
            return in.available();
        } else {
            return fIn.available();
        }
    }

    /**
     * Mark is not supported
     */
    public synchronized void mark(int readlimit) {
        // do nothing
    }

    /**
     * Mark is not supported
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Reset is not supported
     */
    public synchronized void reset() throws IOException {
        // do nothing
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
