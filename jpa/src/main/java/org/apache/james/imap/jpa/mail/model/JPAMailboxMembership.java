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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.james.imap.store.mail.model.Document;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

@Entity(name="Membership")
@IdClass(JPAMailboxMembership.MailboxIdUidKey.class)
@NamedQueries({
    @NamedQuery(name="findRecentMessagesInMailbox",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.recent = TRUE"),
    @NamedQuery(name="findUnseenMessagesInMailboxOrderByUid",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.seen = FALSE ORDER BY membership.uid ASC"),
    @NamedQuery(name="findMessagesInMailbox",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam"),
    @NamedQuery(name="findMessagesInMailboxBetweenUIDs",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.uid BETWEEN :fromParam AND :toParam"),        
    @NamedQuery(name="findMessagesInMailboxWithUID",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.uid=:uidParam"),                    
    @NamedQuery(name="findMessagesInMailboxAfterUID",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.uid>=:uidParam"),                    
    @NamedQuery(name="findDeletedMessagesInMailbox",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.deleted=TRUE"),                   
    @NamedQuery(name="findDeletedMessagesInMailboxBetweenUIDs",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.uid BETWEEN :fromParam AND :toParam AND membership.deleted=TRUE"),        
    @NamedQuery(name="findDeletedMessagesInMailboxWithUID",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.uid=:uidParam AND membership.deleted=TRUE"),                    
    @NamedQuery(name="findDeletedMessagesInMailboxAfterUID",
            query="SELECT membership FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.uid>=:uidParam AND membership.deleted=TRUE"),                    
    @NamedQuery(name="countUnseenMessagesInMailbox",
            query="SELECT COUNT(membership) FROM Membership membership WHERE membership.mailboxId = :idParam AND membership.seen=FALSE"),                     
    @NamedQuery(name="countMessagesInMailbox",
            query="SELECT COUNT(membership) FROM Membership membership WHERE membership.mailboxId = :idParam")                     
})
public class JPAMailboxMembership implements MailboxMembership {

    private static final String TOSTRING_SEPARATOR = " ";

    /** Identifies composite key */
    public static class MailboxIdUidKey implements Serializable {
        private static final long serialVersionUID = 7847632032426660997L;
        /** The value for the mailboxId field */
        public long mailboxId;
        /** The value for the uid field */
        public long uid;

        public MailboxIdUidKey() {}

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (int) (mailboxId ^ (mailboxId >>> 32));
            result = PRIME * result + (int) (uid ^ (uid >>> 32));
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
            final MailboxIdUidKey other = (MailboxIdUidKey) obj;
            if (mailboxId != other.mailboxId)
                return false;
            if (uid != other.uid)
                return false;
            return true;
        }
    }

    /** The value for the mailboxId field */
    @Id private long mailboxId;

    /** The value for the uid field */
    @Id private long uid;

    /** The value for the internalDate field */
    @Basic(optional=false) private Date internalDate;

    /** The value for the size field */
    @Basic(optional=false) private int size = 0;

    /** The value for the answered field */
    @Basic(optional=false) private boolean answered = false;

    /** The value for the deleted field */
    @Basic(optional=false) private boolean deleted = false;

    /** The value for the draft field */
    @Basic(optional=false) private boolean draft = false;

    /** The value for the flagged field */
    @Basic(optional=false) private boolean flagged = false;

    /** The value for the recent field */
    @Basic(optional=false) private boolean recent = false;

    /** The value for the seen field */
    @Basic(optional=false) private boolean seen = false;

    /** The value for the body field. Lazy loaded */
    @ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY) private JPAMessage message;
    
    /**
     * For enhancement only.
     */
    @Deprecated
    public JPAMailboxMembership() {}

    public JPAMailboxMembership(long mailboxId, long uid, Date internalDate, int size, Flags flags, 
            byte[] content, int bodyStartOctet, final List<JPAHeader> headers, final PropertyBuilder propertyBuilder) {
        super();
        this.mailboxId = mailboxId;
        this.uid = uid;
        this.internalDate = internalDate;
        this.size = size;
        this.message = new JPAMessage(content, bodyStartOctet, headers, propertyBuilder);
        setFlags(flags);
    }

    /**
     * Constructs a copy of the given message.
     * All properties are cloned except mailbox and UID.
     * @param mailboxId new mailbox ID
     * @param uid new UID
     * @param original message to be copied, not null
     */
    public JPAMailboxMembership(long mailboxId, long uid, JPAMailboxMembership original) {
        super();
        this.mailboxId = mailboxId;
        this.uid = uid;
        this.internalDate = original.getInternalDate();
        this.size = original.getSize();
        this.answered = original.isAnswered();
        this.deleted = original.isDeleted();
        this.draft = original.isDraft();
        this.flagged = original.isFlagged();
        this.recent = original.isRecent();
        this.seen = original.isSeen();
        this.message = new JPAMessage((JPAMessage) original.getDocument());
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#getInternalDate()
     */
    public Date getInternalDate() {
        return internalDate;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#getMailboxId()
     */
    public long getMailboxId() {
        return mailboxId;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#getSize()
     */
    public int getSize() {
        return size;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#getUid()
     */
    public long getUid() {
        return uid;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#isAnswered()
     */
    public boolean isAnswered() {
        return answered;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#isDeleted()
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#isDraft()
     */
    public boolean isDraft() {
        return draft;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#isFlagged()
     */
    public boolean isFlagged() {
        return flagged;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#isRecent()
     */
    public boolean isRecent() {
        return recent;
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#isSeen()
     */
    public boolean isSeen() {
        return seen;
    }
    
    /**
     * Gets the message member.
     * @return message, not null
     */
    public Document getDocument() {
        return message;
    }
    
    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#unsetRecent()
     */
    public void unsetRecent() {
        recent = false;
    }
    
    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#setFlags(javax.mail.Flags)
     */
    public void setFlags(Flags flags) {
        answered = flags.contains(Flags.Flag.ANSWERED);
        deleted = flags.contains(Flags.Flag.DELETED);
        draft = flags.contains(Flags.Flag.DRAFT);
        flagged = flags.contains(Flags.Flag.FLAGGED);
        recent = flags.contains(Flags.Flag.RECENT);
        seen = flags.contains(Flags.Flag.SEEN);
    }

    /**
     * @see org.apache.james.imap.store.mail.model.MailboxMembership#createFlags()
     */
    public Flags createFlags() {
        final Flags flags = new Flags();

        if (isAnswered()) {
            flags.add(Flags.Flag.ANSWERED);
        }
        if (isDeleted()) {
            flags.add(Flags.Flag.DELETED);
        }
        if (isDraft()) {
            flags.add(Flags.Flag.DRAFT);
        }
        if (isFlagged()) {
            flags.add(Flags.Flag.FLAGGED);
        }
        if (isRecent()) {
            flags.add(Flags.Flag.RECENT);
        }
        if (isSeen()) {
            flags.add(Flags.Flag.SEEN);
        }
        return flags;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (mailboxId ^ (mailboxId >>> 32));
        result = PRIME * result + (int) (uid ^ (uid >>> 32));
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
        final JPAMailboxMembership other = (JPAMailboxMembership) obj;
        if (mailboxId != other.mailboxId)
            return false;
        if (uid != other.uid)
            return false;
        return true;
    }

    public String toString()
    {
        final String retValue = 
            "mailbox("
            + "mailboxId = " + this.mailboxId + TOSTRING_SEPARATOR
            + "uid = " + this.uid + TOSTRING_SEPARATOR
            + "internalDate = " + this.internalDate + TOSTRING_SEPARATOR
            + "size = " + this.size + TOSTRING_SEPARATOR
            + "answered = " + this.answered + TOSTRING_SEPARATOR
            + "deleted = " + this.deleted + TOSTRING_SEPARATOR
            + "draft = " + this.draft + TOSTRING_SEPARATOR
            + "flagged = " + this.flagged + TOSTRING_SEPARATOR
            + "recent = " + this.recent + TOSTRING_SEPARATOR
            + "seen = " + this.seen + TOSTRING_SEPARATOR
            + " )";

        return retValue;
    }
}
