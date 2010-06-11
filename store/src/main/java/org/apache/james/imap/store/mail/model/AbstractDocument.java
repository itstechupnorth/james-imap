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

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.imap.store.streaming.LazySkippingInputStream;
import org.apache.james.imap.store.streaming.RewindableInputStream;


/**
 * Abstract base class for {@link Document}
 *
 */
public abstract class AbstractDocument implements Document{
    
    
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
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Document#getFullContent()
     */
    public RewindableInputStream getFullContent() throws IOException {
        return new RewindableInputStream(getRawFullContent()) {
            
            @Override
            protected void rewindIfNeeded() throws IOException {
                in = getFullContent();
            }
        };
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Document#getBodyContent()
     */
    public RewindableInputStream getBodyContent() throws IOException {
        return new RewindableInputStream(new LazySkippingInputStream(getRawFullContent(), getBodyStartOctet())) {
            
            @Override
            protected void rewindIfNeeded() throws IOException {
                in = new LazySkippingInputStream(getRawFullContent(), getBodyStartOctet());
            }
        };
    }
    
    /**
     * Return the raw {@link InputStream} of the full content. The InputStream must not be read already. So it need to be on start position
     * 
     * @return rawFullContent
     */
    protected abstract InputStream getRawFullContent();
    

}
