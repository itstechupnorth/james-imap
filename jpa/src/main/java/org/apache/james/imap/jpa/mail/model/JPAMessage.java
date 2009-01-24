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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Document;

@Entity(name="Message")
public class JPAMessage implements Document {
    /** Primary key */
    @Id@GeneratedValue private long id;

    /** The value for the body field. Lazy loaded */
    @Basic(optional=false, fetch=FetchType.LAZY) private byte[] body;
    /** Headers for this message */
    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY) private List<JPAHeader> headers;

    /**
     * For enhancement only.
     */
    @Deprecated
    public JPAMessage() {}

    public JPAMessage(byte[] body, final List<JPAHeader> headers) {
        super();
        this.body = body;
        this.headers = new ArrayList<JPAHeader>(headers);
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Document#getHeaders()
     */
    public List<Header> getHeaders() {
        return new ArrayList<Header>(headers);
    }
    
    /**
     * @see org.apache.james.imap.jpa.mail.model.Document#getBody()
     */    
    public byte[] getBody() {
        return body;
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
}
