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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.james.imap.mailbox.MailboxPath;
import org.apache.james.imap.store.mail.model.Mailbox;

@Entity(name="Mailbox")
@NamedQueries({
    @NamedQuery(name="findMailboxById",
        query="SELECT mailbox FROM Mailbox mailbox WHERE mailbox.mailboxId = :idParam"),
    @NamedQuery(name="findMailboxByName",
        query="SELECT mailbox FROM Mailbox mailbox WHERE mailbox.name = :nameParam and mailbox.user is NULL and mailbox.namespace= :namespaceParam"),
    @NamedQuery(name="findMailboxByNameWithUser",
        query="SELECT mailbox FROM Mailbox mailbox WHERE mailbox.name = :nameParam and mailbox.user= :userParam and mailbox.namespace= :namespaceParam"),
    @NamedQuery(name="deleteAll",
                query="DELETE FROM Mailbox mailbox"),
    @NamedQuery(name="findMailboxWithNameLikeWithUser",
                query="SELECT mailbox FROM Mailbox mailbox WHERE mailbox.name LIKE :nameParam and mailbox.user= :userParam and mailbox.namespace= :namespaceParam"),
    @NamedQuery(name="findMailboxWithNameLike",
                query="SELECT mailbox FROM Mailbox mailbox WHERE mailbox.name LIKE :nameParam and mailbox.user is NULL and mailbox.namespace= :namespaceParam"),
    @NamedQuery(name="countMailboxesWithNameLikeWithUser",
                query="SELECT COUNT(mailbox) FROM Mailbox mailbox WHERE mailbox.name LIKE :nameParam and mailbox.user= :userParam and mailbox.namespace= :namespaceParam"),
    @NamedQuery(name="countMailboxesWithNameLike",
                query="SELECT COUNT(mailbox) FROM Mailbox mailbox WHERE mailbox.name LIKE :nameParam and mailbox.user is NULL and mailbox.namespace= :namespaceParam")     
})
public class JPAMailbox implements Mailbox<Long> {
    
    private static final String TAB = " ";

    /** The value for the mailboxId field */
    @Id @GeneratedValue private long mailboxId;
    
    /** The value for the name field */
    @Basic(optional=false) @Column(nullable = false) private String name;

    /** The value for the uidValidity field */
    @Basic(optional=false) private long uidValidity;

    
    /** The value for the lastUid field */
    @Basic(optional=false) private long lastUid = 0;
    
    @Basic(optional=false) @Column(nullable = true)  private String user;
    @Basic(optional=false) @Column(nullable = false) private String namespace;

    /**
     * JPA only
     */
    @Deprecated
    public JPAMailbox() {
        super();
    }
    
    public JPAMailbox(MailboxPath path, int uidValidity) {
        this();
        this.name = path.getName();
        this.user = path.getUser();
        this.namespace = path.getNamespace();
        this.uidValidity = uidValidity;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.Mailbox#getLastUid()
     */
    public long getLastUid() {
        return lastUid;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.Mailbox#getMailboxId()
     */
    public Long getMailboxId() {
        return mailboxId;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.Mailbox#getName()
     */
    public String getName() {
        return name;
    }


    /**
     * @see org.apache.james.imap.store.mail.model.Mailbox#getUidValidity()
     */
    public long getUidValidity() {
        return uidValidity;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.Mailbox#consumeUid()
     */
    public void consumeUid() {
        ++lastUid;
    }
    
    /**
     * @see org.apache.james.imap.store.mail.model.Mailbox#setName(java.lang.String)
     */
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
        final JPAMailbox other = (JPAMailbox) obj;
        if (mailboxId != other.mailboxId)
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#getNamespace()
     */
    public String getNamespace() {
        return namespace;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#getUser()
     */
    public String getUser() {
        return user;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#setNamespace(java.lang.String)
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Mailbox#setUser(java.lang.String)
     */
    public void setUser(String user) {
        this.user = user;
    }

    public void setLastUid(long uid) {
        this.lastUid = uid;
    }
}
