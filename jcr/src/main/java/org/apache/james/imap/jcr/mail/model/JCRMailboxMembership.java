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
package org.apache.james.imap.jcr.mail.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.jackrabbit.JcrConstants;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.jcr.JCRImapConstants;
import org.apache.james.imap.jcr.Persistent;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.store.mail.model.AbstractMailboxMembership;
import org.apache.james.imap.store.mail.model.Document;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

/**
 * JCR implementation of {@link MailboxMembership}
 *
 */
public class JCRMailboxMembership extends AbstractMailboxMembership<String> implements
		Persistent, JCRImapConstants {
    private static final String TOSTRING_SEPARATOR = " ";

	public final static String MAILBOX_UUID_PROPERTY = "imap:mailboxUUID";
	public final static String UID_PROPERTY = "imap:uid";
	public final static String SIZE_PROPERTY = "imap:size";
	public final static String ANSWERED_PROPERTY = "imap:answered";
	public final static String DELETED_PROPERTY = "imap:deleted";
	public final static String DRAFT_PROPERTY =  "imap:draft";
	public final static String FLAGGED_PROPERTY = "imap:flagged";
	public final static String RECENT_PROPERTY = "imap:recent";
	public final static String SEEN_PROPERTY = "imap:seen";
	public final static String INTERNAL_DATE_PROPERTY = "imap:internalDate";
    public final static String MESSAGE_NODE = "message";

	public final static String MESSAGE_NODE_TYPE = "imap:message";
	
	private String mailboxUUID;
	private long uid;
	private Date internalDate;
	private int size;
	private JCRMessage message;
	private boolean answered;
	private boolean deleted;
	private boolean draft;
	private boolean flagged;
	private boolean recent;
	private boolean seen;
	private Log logger;
	private Node node;

	public JCRMailboxMembership(String mailboxUUID, long uid,
			Date internalDate, int size, Flags flags, InputStream content,
			int bodyStartOctet, final List<JCRHeader> headers,
			final PropertyBuilder propertyBuilder, Log logger) {
		super();
		this.mailboxUUID = mailboxUUID;
		this.uid = uid;
		this.internalDate = internalDate;
		this.size = size;
		this.message = new JCRMessage(content, size, bodyStartOctet, headers,
				propertyBuilder, logger);
		this.logger = logger;
		setFlags(flags);
	}

	/**
	 * Constructs a copy of the given message. All properties are cloned except
	 * mailbox and UID.
	 * 
	 * @param mailboxId
	 *            new mailbox ID
	 * @param uid
	 *            new UID
	 * @param original
	 *            message to be copied, not null
	 * @throws MailboxException 
	 */
	public JCRMailboxMembership(String mailboxUUID, long uid,
			JCRMailboxMembership original, Log logger) throws MailboxException {
		super();
		this.mailboxUUID = mailboxUUID;
		this.uid = uid;
		this.internalDate = original.getInternalDate();
		this.size = original.getSize();
		this.answered = original.isAnswered();
		this.deleted = original.isDeleted();
		this.draft = original.isDraft();
		this.flagged = original.isFlagged();
		this.recent = original.isRecent();
		this.seen = original.isSeen();
		try {
            this.message = new JCRMessage((JCRMessage) original.getDocument(),
            		logger);
        } catch (IOException e) {
            throw new MailboxException(HumanReadableText.FAILURE_MAIL_PARSE, e);
        }
	}

	public JCRMailboxMembership(Node node, Log logger) {
		this.logger = logger;
		this.node = node;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.james.imap.store.mail.model.MailboxMembership#getDocument()
	 */
	public Document getDocument() {
	    if (isPersistent()) {
	        try {
	            return new JCRMessage(node.getNode(MESSAGE_NODE), logger);
	        } catch (RepositoryException e) {
                logger.error("Unable to access node " + MESSAGE_NODE,
                                e);
            }
	    }
		return message;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.james.imap.store.mail.model.MailboxMembership#getInternalDate
	 * ()
	 */
	public Date getInternalDate() {
		if (isPersistent()) {
			try {
				return node.getProperty(INTERNAL_DATE_PROPERTY).getDate().getTime();

			} catch (RepositoryException e) {
				logger.error("Unable to access property " + FLAGGED_PROPERTY,
								e);
			}
			return null;
		}
		return internalDate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.james.imap.store.mail.model.MailboxMembership#getMailboxId()
	 */
	public String getMailboxId() {
	    if (isPersistent()) {
            try {
                return node.getProperty(MAILBOX_UUID_PROPERTY).getString();
            } catch (RepositoryException e) {
                logger.error("Unable to access property "
                        + MAILBOX_UUID_PROPERTY, e);
            }
        }
        return mailboxUUID;
    }


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.store.mail.model.MailboxMembership#getSize()
	 */
	public int getSize() {
		if (isPersistent()) {
			try {
				return new Long(node.getProperty(SIZE_PROPERTY).getLong())
						.intValue();

			} catch (RepositoryException e) {
				logger
						.error("Unable to access property " + FLAGGED_PROPERTY,
								e);
			}
			return 0;
		}
		return size;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.store.mail.model.MailboxMembership#getUid()
	 */
	public long getUid() {
		if (isPersistent()) {
			try {
				return node.getProperty(UID_PROPERTY).getLong();

			} catch (RepositoryException e) {
				logger.error("Unable to access property " + UID_PROPERTY, e);
			}
			return 0;
		}
		return uid;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.james.imap.store.mail.model.MailboxMembership#isAnswered()
	 */
	public boolean isAnswered() {
		if (isPersistent()) {
			try {
				return node.getProperty(ANSWERED_PROPERTY).getBoolean();

			} catch (RepositoryException e) {
				logger.error("Unable to access property " + ANSWERED_PROPERTY,
						e);
			}
			return false;
		}
		return answered;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.store.mail.model.MailboxMembership#isDeleted()
	 */
	public boolean isDeleted() {
		if (isPersistent()) {
			try {
				return node.getProperty(DELETED_PROPERTY).getBoolean();

			} catch (RepositoryException e) {
				logger.error("Unable to access property " + DELETED_PROPERTY,
								e);
			}
			return false;
		}
		return deleted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.store.mail.model.MailboxMembership#isDraft()
	 */
	public boolean isDraft() {
		if (isPersistent()) {
			try {
				return node.getProperty(DRAFT_PROPERTY).getBoolean();

			} catch (RepositoryException e) {
				logger.error("Unable to access property " + DRAFT_PROPERTY, e);
			}
			return false;
		}
		return draft;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.store.mail.model.MailboxMembership#isFlagged()
	 */
	public boolean isFlagged() {
		if (isPersistent()) {
			try {
				return node.getProperty(FLAGGED_PROPERTY).getBoolean();

			} catch (RepositoryException e) {
				logger.error("Unable to access property " + FLAGGED_PROPERTY,
								e);
			}
			return false;
		}
		return flagged;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.store.mail.model.MailboxMembership#isRecent()
	 */
	public boolean isRecent() {
		if (isPersistent()) {
			try {
				return node.getProperty(RECENT_PROPERTY).getBoolean();

			} catch (RepositoryException e) {
				logger.error("Unable to access property " + RECENT_PROPERTY, e);
			}
			return false;
		}
		return recent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.store.mail.model.MailboxMembership#isSeen()
	 */
	public boolean isSeen() {
		if (isPersistent()) {
			try {
				return node.getProperty(SEEN_PROPERTY).getBoolean();

			} catch (RepositoryException e) {
				logger.error("Unable to access property " + SEEN_PROPERTY, e);
			}
			return false;
		}
		return seen;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.james.imap.store.mail.model.MailboxMembership#setFlags(javax
	 * .mail.Flags)
	 */
	public void setFlags(Flags flags) {
		if (isPersistent()) {
			try {
				node.setProperty(ANSWERED_PROPERTY,
						flags.contains(Flags.Flag.ANSWERED));
				node.setProperty(DELETED_PROPERTY,
						flags.contains(Flags.Flag.DELETED));
				node.setProperty(DRAFT_PROPERTY,
						flags.contains(Flags.Flag.DRAFT));
				node.setProperty(FLAGGED_PROPERTY,
						flags.contains(Flags.Flag.FLAGGED));
				node.setProperty(RECENT_PROPERTY,
						flags.contains(Flags.Flag.RECENT));
				node.setProperty(SEEN_PROPERTY,
						flags.contains(Flags.Flag.SEEN));
			} catch (RepositoryException e) {
				logger.error("Unable to set flags", e);
			}
		} else {
			answered = flags.contains(Flags.Flag.ANSWERED);
			deleted = flags.contains(Flags.Flag.DELETED);
			draft = flags.contains(Flags.Flag.DRAFT);
			flagged = flags.contains(Flags.Flag.FLAGGED);
			recent = flags.contains(Flags.Flag.RECENT);
			seen = flags.contains(Flags.Flag.SEEN);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.james.imap.store.mail.model.MailboxMembership#unsetRecent()
	 */
	public void unsetRecent() {
		if (isPersistent()) {
			try {
				node.setProperty(RECENT_PROPERTY, false);

			} catch (RepositoryException e) {
				logger.error("Unable to access property " + RECENT_PROPERTY, e);
			}
		} else {
			recent = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.jcr.Persistent#getNode()
	 */
	public Node getNode() {
		return node;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.jcr.Persistent#isPersistent()
	 */
	public boolean isPersistent() {
		return node != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.imap.jcr.Persistent#merge(javax.jcr.Node)
	 */
	public void merge(Node node) throws RepositoryException, IOException {
		node.setProperty(MAILBOX_UUID_PROPERTY, getMailboxId());
		node.setProperty(UID_PROPERTY, getUid());
		node.setProperty(SIZE_PROPERTY, getSize());
		node.setProperty(ANSWERED_PROPERTY, isAnswered());
		node.setProperty(DELETED_PROPERTY, isDeleted());
		node.setProperty(DRAFT_PROPERTY, isDraft());
		node.setProperty(FLAGGED_PROPERTY, isFlagged());
		node.setProperty(RECENT_PROPERTY, isRecent());
		
		node.setProperty(SEEN_PROPERTY, isSeen());
		
		if (getInternalDate() == null) {
		    internalDate = new Date();
		}
		
		Calendar cal = Calendar.getInstance();
		
		cal.setTime(getInternalDate());
		node.setProperty(INTERNAL_DATE_PROPERTY, cal);

		Node messageNode;
		if (node.hasNode(MESSAGE_NODE)) {
		    messageNode = node.getNode(MESSAGE_NODE);
		} else {
	        messageNode = node.addNode(MESSAGE_NODE, MESSAGE_NODE_TYPE);
	        messageNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
		}
		((JCRMessage)getDocument()).merge(messageNode);

		this.node = node;

		/*
		answered = false;
		deleted = false;
		draft = false;
		flagged = false;
		internalDate = null;
		mailboxUUID = null;
		message = null;
		recent = false;
		seen = false;
		size = 0;
		uid = 0;
		*/
	}

    public String getId() {
        if (isPersistent()) {
            try {
                return node.getUUID();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + JcrConstants.JCR_UUID, e);
            }
        }
        return null;      
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + getMailboxId().hashCode();
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
        final JCRMailboxMembership other = (JCRMailboxMembership) obj;
        if (getMailboxId() != other.getMailboxId())
            return false;
        if (getId() != other.getId())
            return false;
        return true;
    }

    public String toString() {
        final String retValue = 
            "mailbox("
            + "mailboxUUID = " + this.getMailboxId() + TOSTRING_SEPARATOR
            + "uuid = " + this.getId() + TOSTRING_SEPARATOR
            + "internalDate = " + this.getInternalDate() + TOSTRING_SEPARATOR
            + "size = " + this.getSize() + TOSTRING_SEPARATOR
            + "answered = " + this.isAnswered() + TOSTRING_SEPARATOR
            + "deleted = " + this.isDeleted() + TOSTRING_SEPARATOR
            + "draft = " + this.isDraft() + TOSTRING_SEPARATOR
            + "flagged = " + this.isFlagged() + TOSTRING_SEPARATOR
            + "recent = " + this.isRecent() + TOSTRING_SEPARATOR
            + "seen = " + this.isSeen() + TOSTRING_SEPARATOR
            + " )";

        return retValue;
    }

}
