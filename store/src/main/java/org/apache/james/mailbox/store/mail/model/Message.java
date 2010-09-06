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
package org.apache.james.mailbox.store.mail.model;

import java.io.IOException;
import java.util.List;

import org.apache.james.mailbox.store.streaming.RewindableInputStream;

/**
 * A MIME message, consisting of meta-data (including MIME headers)
 * plus body content. In the case of multipart documents, this body content
 * has internal structure described by the meta-data.
 */
public interface Message {


    /**
     * Gets the full content (including headers) of the document.
     * @return fullContent, not null
     */
    public abstract RewindableInputStream getFullContent() throws IOException;
    
    /**
     * Gets the body content of the document.
     * Headers are excluded.
     * @return body, not null
     */
    public abstract RewindableInputStream getBodyContent() throws IOException;

    /**
     * Gets the top level MIME content media type.
     * 
     * @return top level MIME content media type, or null if default
     */
    public abstract String getMediaType();

    /**
     * Gets the MIME content subtype.
     * 
     * @return the MIME content subtype, or null if default
     */
    public abstract String getSubType();
    
    /**
     * The number of octets contained in the body of this document.
     * 
     * @return number of octets
     */
    public abstract long getBodyOctets();
    
    /**
     * The number of octets contained in the full content of this document.
     * 
     * @return number of octets
     */
    public abstract long getFullContentOctets();
    
    /**
     * Gets the number of CRLF in a textual document.
     * @return CRLF count when document is textual,
     * null otherwise
     */
    public Long getTextualLineCount();
    
    /**
     * Gets a read-only list of headers.
     * @return unmodifiable list of headers, not null
     */
    public abstract List<Header> getHeaders();
    
    /**
     * Gets a read-only list of meta-data properties.
     * For properties with multiple values, this list will contain
     * several enteries with the same namespace and local name.
     * @return unmodifiable list of meta-data, not null
     */
    public abstract List<Property> getProperties();
}