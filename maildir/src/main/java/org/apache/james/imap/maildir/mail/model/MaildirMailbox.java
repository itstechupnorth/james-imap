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
package org.apache.james.imap.maildir.mail.model;

import org.apache.james.imap.store.mail.model.Mailbox;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("mailbox")
public class MaildirMailbox implements Mailbox {

	private String name;

	private long id;
	private long lastUid;

	private int uidValidity;

	public MaildirMailbox(String name, int uidValidity, long lastUid) {
		this.name = name;
		this.uidValidity = uidValidity;
		this.lastUid = lastUid;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.mail.model.Mailbox#consumeUid()
	 */
	public void consumeUid() {
		lastUid++;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.mail.model.Mailbox#getLastUid()
	 */
	public long getLastUid() {
		return lastUid;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.mail.model.Mailbox#getMailboxId()
	 */
	public long getMailboxId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.mail.model.Mailbox#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.mail.model.Mailbox#getUidValidity()
	 */
	public long getUidValidity() {
		return uidValidity;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.imap.store.mail.model.Mailbox#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof MaildirMailbox) {
			if (id == ((MaildirMailbox) obj).getMailboxId()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (int) (id ^ (id >>> 32));
		return result;
	}

}
