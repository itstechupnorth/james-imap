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
package org.apache.james.imap.jpa.user.model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.james.imap.store.user.model.Subscription;

/**
 * A subscription to a mailbox by a user.
 */
@Entity(name="Subscription")
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"USER", "MAILBOX"}))
@NamedQueries({
    @NamedQuery(name="findFindMailboxSubscriptionForUser",
        query="SELECT subscription FROM Subscription subscription WHERE subscription.user = :userParam AND subscription.mailbox = :mailboxParam"),          
    @NamedQuery(name="findSubscriptionsForUser",
        query="SELECT subscription FROM Subscription subscription WHERE subscription.user = :userParam")                  
})
public class JPASubscription implements Subscription {

    private static final String TO_STRING_SEPARATOR = "  ";
    /** Primary key */
    @GeneratedValue
    @Id private long id;
    /** Name of the subscribed user */
    @Basic(optional=false) private String user;
    /** Subscribed mailbox */
    @Basic(optional=false) private String mailbox;
    
    /**
     * Used by JPA
     */
    @Deprecated
    public JPASubscription() {}
    
    /**
     * Constructs a user subscription.
     * @param user not null
     * @param mailbox not null
     */
    public JPASubscription(String user, String mailbox) {
        super();
        this.user = user;
        this.mailbox = mailbox;
    }

    /**
     * @see org.apache.james.imap.jpa.user.model.Subscription#getMailbox()
     */
    public String getMailbox() {
        return mailbox;
    }
    
    /**
     * @see org.apache.james.imap.jpa.user.model.Subscription#getUser()
     */
    public String getUser() {
        return user;
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
        final JPASubscription other = (JPASubscription) obj;
        if (id != other.id)
            return false;
        return true;
    }

    /**
     * Renders output suitable for debugging.
     *
     * @return output suitable for debugging
     */
    public String toString()
    {
        final String result = "Subscription ( "
            + "id = " + this.id + TO_STRING_SEPARATOR
            + "user = " + this.user + TO_STRING_SEPARATOR
            + "mailbox = " + this.mailbox + TO_STRING_SEPARATOR
            + " )";
    
        return result;
    }
    
    
}
