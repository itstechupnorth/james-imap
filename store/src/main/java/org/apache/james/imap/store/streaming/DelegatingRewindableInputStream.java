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

/**
 * {@link RewindableInputStream} implementation which just delegate the calls to {@link FileRewindableInputStream} 
 * or {@link InMemoryRewindableInputStream} depending on the size 
 * 
 *
 */
public class DelegatingRewindableInputStream extends RewindableInputStream{

    public final static long  MAX_INMEMORY_SIZE= 524288;
    
    public DelegatingRewindableInputStream(InputStream in, long size, long maxInmemorySize) throws IOException {
        super(null);
        if (size > maxInmemorySize) {
            this.in = new FileRewindableInputStream(in);
        } else {
            this.in = new InMemoryRewindableInputStream(in);

        }
    }

    public DelegatingRewindableInputStream(InputStream in, long size) throws IOException {
        this(in, size, MAX_INMEMORY_SIZE);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    protected void rewindIfNeeded() throws IOException {
        ((RewindableInputStream)in).rewindIfNeeded();
    }

}
