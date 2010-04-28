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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link FilterInputStream} which support the get rewinded.
 * 
 * The rewinding will get delayed as long as possible. So if you call
 * rewind, it will only get performed when needed
 * 
 * Be sure to call {@link #close()} to cleanup temporary data when you 
 * are done with reading from the stream
 * 
 *
 */
public abstract class RewindableInputStream extends FilterInputStream{

    private boolean rewind;

    protected RewindableInputStream(InputStream in) {
        super(in);
    }

    /**
     * Return if the stream needs to get rewinded
     * 
     * @return needsRewind
     */
    public final boolean needsRewind() {
        return rewind;
    }
    
    /**
     * Rewind was done
     */
    protected final void rewindDone() {
        this.rewind = false;
    }
    
    /**
     * Mark the stream for rewind. The rewind itself should get delayed as long as possible
     */
    public final void rewind() {
        this.rewind = true;
    }

    /**
     * Perform the actual rewind 
     * @throws IOException
     */
    protected abstract void rewindIfNeeded() throws IOException;

}
