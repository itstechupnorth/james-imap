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
package org.apache.james.imap.jpa.mail.model;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.apache.james.imap.store.mail.model.Document;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Property;
import org.apache.james.imap.store.mail.model.PropertyBuilder;
import org.apache.openjpa.jdbc.sql.FirebirdDictionary;

@Entity(name="Message")
public class JPAMessage implements Document {
    /** Primary key */
    @Id@GeneratedValue private long id;

    /** The value for the body field. Lazy loaded */
    @Basic(optional=false, fetch=FetchType.LAZY) @Lob private byte[] content;
    /** Headers for this message */
    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY) private List<JPAHeader> headers;
    /** The first body octet */
    @Basic(optional=false) private int bodyStartOctet;
    /** Number of octets in the full document content */
    @Basic(optional=false) private long contentOctets;
    /** MIME media type */
    @Basic(optional=true) private String mediaType;
    /** MIME sub type */
    @Basic(optional=true) private String subType;
    /** Meta data for this message */
    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY) @OrderBy("line") private List<JPAProperty> properties;
    /** THE CRFL count when this document is textual, null otherwise */
    @Basic(optional=true) private Long textualLineCount;
    
    /**
     * For enhancement only.
     */
    @Deprecated
    public JPAMessage() {}

    public JPAMessage(byte[] content, final int bodyStartOctet, final List<JPAHeader> headers, final PropertyBuilder propertyBuilder) {
        super();
        this.content = content;
        this.contentOctets = content.length;
        this.bodyStartOctet = bodyStartOctet;
        this.headers = new ArrayList<JPAHeader>(headers);
        textualLineCount = propertyBuilder.getTextualLineCount();
        this.mediaType = propertyBuilder.getMediaType();
        this.subType = propertyBuilder.getSubType();
        final List<Property> properties = propertyBuilder.toProperties();
        this.properties = new ArrayList<JPAProperty>(properties.size());
        int order = 0;
        for (final Property property:properties) {
            this.properties.add(new JPAProperty(property, order++));
        }
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Document#getHeaders()
     */
    public List<Header> getHeaders() {
        return new ArrayList<Header>(headers);
    }
    
    /**
     * @see org.apache.james.imap.jpa.mail.model.Document#getBodyContent()
     */    
    public ByteBuffer getBodyContent() {
        final ByteBuffer contentBuffer = getFullContent();
        contentBuffer.position(bodyStartOctet);
        return contentBuffer.slice();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final JPAMessage other = (JPAMessage) obj;
        if (id != other.id)
            return false;
        return true;
    }

    public String toString()
    {
        final String retValue = 
            "message("
            + "id = " + id
            + " )";
        return retValue;
    }

    /**
     * The number of octets contained in the body of this part.
     * 
     * @return number of octets
     */
    public long getBodyOctets() {
        return contentOctets - bodyStartOctet;
    }

    /**
     * Gets the top level MIME content media type.
     * 
     * @return top level MIME content media type, or null if default
     */
    public String getMediaType() {
        return mediaType;
    }
    
    /**
     * Gets the MIME content subtype.
     * 
     * @return the MIME content subtype, or null if default
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Gets a read-only list of meta-data properties.
     * For properties with multiple values, this list will contain
     * several enteries with the same namespace and local name.
     * @return unmodifiable list of meta-data, not null
     */
    public List<Property> getProperties() {
        return new ArrayList<Property>(properties);
    }
    
    /**
     * Gets the number of CRLF in a textual document.
     * @return CRLF count when document is textual,
     * null otherwise
     */
    public Long getTextualLineCount() {
        return textualLineCount;
    }

    public ByteBuffer getFullContent() {
        return ByteBuffer.wrap(content).asReadOnlyBuffer();
    }

    public long getFullContentOctets() {
        return contentOctets;
    }
}
