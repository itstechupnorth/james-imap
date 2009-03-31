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
package org.apache.james.imap.message.response;

import java.util.List;

import org.apache.james.imap.api.message.response.ImapResponseMessage;

/**
 * Describes a NAMESPACE response.
 */
public class NamespaceResponse implements ImapResponseMessage {

    private final List<Namespace> personal;

    private final List<Namespace> users;

    private final List<Namespace> shared;

    public NamespaceResponse(final List<Namespace> personal,
            final List<Namespace> users, final List<Namespace> shared) {
        super();
        this.personal = personal;
        this.users = users;
        this.shared = shared;
    }

    /**
     * Gets the personal namespace.
     * 
     * @return possibly null
     */
    public List<Namespace> getPersonal() {
        return personal;
    }

    /**
     * Gets shared namespaces.
     * 
     * @return possibly null
     */
    public List<Namespace> getShared() {
        return shared;
    }

    /**
     * Gets the namespaces for other users.
     * 
     * @return possibly null
     */
    public List<Namespace> getUsers() {
        return users;
    }

    /**
     * Describes a namespace.
     */
    public static final class Namespace {
        private final String prefix;

        private final char deliminator;

        public Namespace(final String prefix, final char deliminator) {
            super();
            this.prefix = prefix;
            this.deliminator = deliminator;
        }

        /**
         * Gets the deliminator used to separate mailboxes.
         * 
         * @return not null
         */
        public char getDeliminator() {
            return deliminator;
        }

        /**
         * Gets the leading prefix used by this namespace. 
         * @return not null
         */
        public String getPrefix() {
            return prefix;
        }
    }
}
