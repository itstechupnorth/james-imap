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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Message;

@Entity(name="Message")
@IdClass(JPAMessage.MessageId.class)
@NamedQueries({
    @NamedQuery(name="findRecentMessagesInMailbox",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam AND message.recent = TRUE"),
    @NamedQuery(name="findUnseenMessagesInMailboxOrderByUid",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam AND message.seen = FALSE ORDER BY message.uid ASC"),
    @NamedQuery(name="findMessagesInMailbox",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam"),
    @NamedQuery(name="findMessagesInMailboxBetweenUIDs",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam AND message.uid BETWEEN :fromParam AND :toParam"),        
    @NamedQuery(name="findMessagesInMailboxWithUID",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam AND message.uid=:uidParam"),                    
    @NamedQuery(name="findMessagesInMailboxAfterUID",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam AND message.uid>=:uidParam"),                    
    @NamedQuery(name="findDeletedMessagesInMailbox",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam AND message.deleted=TRUE"),                   
    @NamedQuery(name="findDeletedMessagesInMailboxBetweenUIDs",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam AND message.uid BETWEEN :fromParam AND :toParam AND message.deleted=TRUE"),        
    @NamedQuery(name="findDeletedMessagesInMailboxWithUID",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam AND message.uid=:uidParam AND message.deleted=TRUE"),                    
    @NamedQuery(name="findDeletedMessagesInMailboxAfterUID",
            query="SELECT message FROM Message message WHERE message.mailboxId = :idParam AND message.uid>=:uidParam AND message.deleted=TRUE"),                    
    @NamedQuery(name="countUnseenMessagesInMailbox",
            query="SELECT COUNT(message) FROM Message message WHERE message.mailboxId = :idParam AND message.seen=FALSE"),                     
    @NamedQuery(name="countMessagesInMailbox",
            query="SELECT COUNT(message) FROM Message message WHERE message.mailboxId = :idParam")                     
})
public class JPAMessage implements Message {

    private static final String TOSTRING_SEPARATOR = " ";

    /** Identifies composite key */
    public static class MessageId implements Serializable {
        private static final long serialVersionUID = 7847632032426660997L;
        /** The value for the mailboxId field */
        public long mailboxId;
        /** The value for the uid field */
        public long uid;

        public MessageId() {}

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
            final MessageId other = (MessageId) obj;
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
    @Basic(optional=false, fetch=FetchType.LAZY) private byte[] body;
    /** Headers for this message */
    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY) private List<JPAHeader> headers;

    /**
     * For enhancement only.
     */
    @Deprecated
    public JPAMessage() {}

    public JPAMessage(long mailboxId, long uid, Date internalDate, int size, Flags flags, byte[] body, final List<JPAHeader> headers) {
        super();
        this.mailboxId = mailboxId;
        this.uid = uid;
        this.internalDate = internalDate;
        this.size = size;
        this.body = body;
        setFlags(flags);
        this.headers = new ArrayList<JPAHeader>(headers);
    }

    /**
     * Constructs a copy of the given message.
     * All properties are cloned except mailbox and UID.
     * @param mailboxId new mailbox ID
     * @param uid new UID
     * @param original message to be copied, not null
     */
    public JPAMessage(long mailboxId, long uid, JPAMessage original) {
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
        this.body = original.getBody();
        final List<JPAHeader> originalHeaders = original.headers;
        if (originalHeaders == null) {
            this.headers = new ArrayList<JPAHeader>();
        } else {
            this.headers = new ArrayList<JPAHeader>(originalHeaders.size());
            for (JPAHeader header:originalHeaders) {
                this.headers.add(new JPAHeader(header));
            }
        }
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#getInternalDate()
     */
    public Date getInternalDate() {
        return internalDate;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#getMailboxId()
     */
    public long getMailboxId() {
        return mailboxId;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#getSize()
     */
    public int getSize() {
        return size;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#getUid()
     */
    public long getUid() {
        return uid;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#getBody()
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#isAnswered()
     */
    public boolean isAnswered() {
        return answered;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#isDeleted()
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#isDraft()
     */
    public boolean isDraft() {
        return draft;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#isFlagged()
     */
    public boolean isFlagged() {
        return flagged;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#isRecent()
     */
    public boolean isRecent() {
        return recent;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#isSeen()
     */
    public boolean isSeen() {
        return seen;
    }

    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#getHeaders()
     */
    public List<Header> getHeaders() {
        return new ArrayList<Header>(headers);
    }
    
    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#unsetRecent()
     */
    public void unsetRecent() {
        recent = false;
    }
    
    /**
     * @see org.apache.james.imap.jpa.mail.model.Message#setFlags(javax.mail.Flags)
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
     * @see org.apache.james.imap.jpa.mail.model.Message#createFlags()
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
        final JPAMessage other = (JPAMessage) obj;
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
