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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

import org.apache.james.imap.store.streaming.RewindableInputStream;
import org.junit.Before;
import org.junit.Test;

public abstract class RewindableInputStreamTest {

    private RewindableInputStream in;
    private final static String CONTENT = "test\nblah!\n";

    @Before
    public void setUp() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(CONTENT.getBytes());

        in = create(new ByteArrayInputStream(out.toByteArray()));

    }

    protected abstract RewindableInputStream create(InputStream in) throws IOException;
    
    public void tearDown() throws IOException {
        in.close();
    }

    @Test
    public void testRewindAfterCompleteReading() throws IOException {

        consume();
        in.rewind();
        consume();
    }

    @Test
    public void testRewindAfterPartReading() throws IOException {

        consume(3);
        in.rewind();
        consume();
    }

    private void consume() throws IOException {
        int i = -1;
        int a = 0;
        while ((i = in.read()) != -1) {
            assertEquals(CONTENT.charAt(a), (char) i);
            a++;
        }

        // everything really read
        assertEquals(a, CONTENT.length());
    }

    private void consume(int n) throws IOException {
        int i = -1;
        int a = 0;
        while ((i = in.read()) != -1) {
            assertEquals(CONTENT.charAt(a), (char) i);
            a++;
            if (i == n) {
                break;
            }
        }
    }
}
