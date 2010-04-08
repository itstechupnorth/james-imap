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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link RewindableInputStream} implementations which stores the data into a {@link File}. This is
 * useful for big data
 * 
 */
public class FileRewindableInputStream extends AbstractRewindableInputStream{

    private File f;
    private FileOutputStream fOut;
    private FileInputStream fIn;

    public FileRewindableInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected OutputStream getRewindOutputStream() throws IOException {
        if (f == null) {
            f = File.createTempFile("rewindable", ".tmp");
        }
        if (fOut == null) {
            fOut = new FileOutputStream(f);
        
        }
        return fOut;
    }
    
    @Override
    protected void afterRewindComplete() throws IOException {
        fIn = new FileInputStream(f);        
    }

    @Override
    protected void dispose() throws IOException {
        if (f != null) {
            f.delete();
        }
    }

    @Override
    protected InputStream getRewindInputStream() throws IOException {
        if (f == null) {
            f = File.createTempFile("rewindable", ".tmp");
        }
        if (fIn == null) {

            fIn = new FileInputStream(f);
        }
        return fIn;
    }
}
