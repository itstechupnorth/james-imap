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

package org.apache.james.imap.mailbox.util;

import org.apache.james.imap.mailbox.MailboxMetaData;
import org.apache.james.imap.mailbox.StandardMailboxMetaDataComparator;

public class SimpleMailboxMetaData implements MailboxMetaData, Comparable {

    public static MailboxMetaData createNoSelect(String name, String delimiter) {
        return new SimpleMailboxMetaData(name, delimiter, false, Selectability.NOSELECT);
    }

    private final String name;

    private final String delimiter;

    private final boolean noInferiors;

    private final Selectability selectability;

    public SimpleMailboxMetaData(String name, String delimiter) {
        this(name, delimiter, false, Selectability.NONE);
    }

    public SimpleMailboxMetaData(final String name, final String delimiter,
            final boolean noInferiors, final Selectability selectability) {
        super();
        this.name = name;
        this.delimiter = delimiter;
        this.noInferiors = noInferiors;
        this.selectability = selectability;
    }

    /**
     * Is this mailbox <code>\Noinferiors</code> as per RFC3501.
     * 
     * @return true if marked, false otherwise
     */
    public final boolean isNoInferiors() {
        return noInferiors;
    }

    /**
     * Gets the RFC3501 Selectability flag.
     */
    public final Selectability getSelectability() {
        return selectability;
    }

    public String getHierarchyDelimiter() {
        return delimiter;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "ListResult: " + name;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SimpleMailboxMetaData other = (SimpleMailboxMetaData) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public int compareTo(Object o) {
        return StandardMailboxMetaDataComparator.order(this, (MailboxMetaData) o);
    }

}