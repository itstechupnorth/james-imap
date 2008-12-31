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
package org.apache.james.imap.jpa.om;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
    @NamedQuery(name="findMailboxById",
        query="SELECT mailbox FROM Mailbox mailbox WHERE mailbox.mailboxId = :idParam"),
    @NamedQuery(name="findMailboxByName",
        query="SELECT mailbox FROM Mailbox mailbox WHERE mailbox.name = :nameParam"),
    @NamedQuery(name="countMailboxesWithName",
        query="SELECT COUNT(mailbox) FROM Mailbox mailbox WHERE mailbox.name = :nameParam"),
    @NamedQuery(name="deleteAll",
                query="DELETE FROM Mailbox mailbox"),
    @NamedQuery(name="findMailboxWithNameLike",
                query="SELECT mailbox FROM Mailbox mailbox WHERE mailbox.name LIKE :nameParam")            
})
public class Mailbox {
    
    private static final String TAB = " ";

    /** The value for the mailboxId field */
    @Id @GeneratedValue private long mailboxId;
    
    /** The value for the name field */
    @Basic(optional=false) private String name;

    /** The value for the uidValidity field */
    @Basic(optional=false) private long uidValidity;

    
    /** The value for the lastUid field */
    @Basic(optional=false) private long lastUid = 0;

    
    /** The value for the messageCount field */
    @Basic(optional=false) private int messageCount = 0;

    
    /** The value for the size field */
    @Basic(optional=false) private long size = 0;

    /**
     * JPA only
     */
    @Deprecated
    public Mailbox() {
        super();
    }
    
    public Mailbox(String name, int uidValidity) {
        this();
        this.name= name;
        this.uidValidity = uidValidity;
    }

    public long getLastUid() {
        return lastUid;
    }

    public long getMailboxId() {
        return mailboxId;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public long getUidValidity() {
        return uidValidity;
    }

    public void consumeUid() {
        ++lastUid;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString()
    {
        final String retValue = "Mailbox ( "
            + "mailboxId = " + this.mailboxId + TAB
            + "name = " + this.name + TAB
            + "uidValidity = " + this.uidValidity + TAB
            + "lastUid = " + this.lastUid + TAB
            + "messageCount = " + this.messageCount + TAB
            + "size = " + this.size + TAB
            + " )";
        return retValue;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (mailboxId ^ (mailboxId >>> 32));
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
        final Mailbox other = (Mailbox) obj;
        if (mailboxId != other.mailboxId)
            return false;
        return true;
    }
}
