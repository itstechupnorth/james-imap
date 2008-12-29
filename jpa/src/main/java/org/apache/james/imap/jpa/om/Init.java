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

import org.apache.torque.TorqueException;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.TableMap;

public class Init {
    public static final void init() throws TorqueException {
        org.apache.james.imap.jpa.om.MailboxRowPeer
                .getMapBuilder();
        org.apache.james.imap.jpa.om.MessageRowPeer
                .getMapBuilder();
        org.apache.james.imap.jpa.om.MessageFlagsPeer
                .getMapBuilder();
        org.apache.james.imap.jpa.om.MessageHeaderPeer
                .getMapBuilder();
        org.apache.james.imap.jpa.om.MessageBodyPeer
                .getMapBuilder();
    }
    
    public static void populateMessageFlags(TableMap tMap) {
        tMap.setJavaName("MessageFlags");
        tMap
                .setOMClass(org.apache.james.imap.jpa.om.MessageFlags.class);
        tMap
                .setPeerClass(org.apache.james.imap.jpa.om.MessageFlagsPeer.class);
        tMap.setPrimaryKeyMethod("none");

        ColumnMap cMap = null;

        // ------------- Column: mailbox_id --------------------
        cMap = new ColumnMap("mailbox_id", tMap);
        cMap.setType(new Long(0));
        cMap.setTorqueType("BIGINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(true);
        cMap.setNotNull(true);
        cMap.setJavaName("MailboxId");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("Mailbox Id");
        cMap.setInheritance("false");
        cMap.setForeignKey("message", "mailbox_id");
        cMap.setPosition(1);
        tMap.addColumn(cMap);
        // ------------- Column: uid --------------------
        cMap = new ColumnMap("uid", tMap);
        cMap.setType(new Long(0));
        cMap.setTorqueType("BIGINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(true);
        cMap.setNotNull(true);
        cMap.setJavaName("Uid");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("");
        cMap.setInheritance("false");
        cMap.setForeignKey("message", "uid");
        cMap.setPosition(2);
        tMap.addColumn(cMap);
        // ------------- Column: answered --------------------
        cMap = new ColumnMap("answered", tMap);
        cMap.setType(new Integer(0));
        cMap.setTorqueType("BOOLEANINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("Answered");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("");
        cMap.setDefault("0");
        cMap.setInheritance("false");
        cMap.setPosition(3);
        tMap.addColumn(cMap);
        // ------------- Column: deleted --------------------
        cMap = new ColumnMap("deleted", tMap);
        cMap.setType(new Integer(0));
        cMap.setTorqueType("BOOLEANINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("Deleted");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("");
        cMap.setDefault("0");
        cMap.setInheritance("false");
        cMap.setPosition(4);
        tMap.addColumn(cMap);
        // ------------- Column: draft --------------------
        cMap = new ColumnMap("draft", tMap);
        cMap.setType(new Integer(0));
        cMap.setTorqueType("BOOLEANINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("Draft");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("");
        cMap.setDefault("0");
        cMap.setInheritance("false");
        cMap.setPosition(5);
        tMap.addColumn(cMap);
        // ------------- Column: flagged --------------------
        cMap = new ColumnMap("flagged", tMap);
        cMap.setType(new Integer(0));
        cMap.setTorqueType("BOOLEANINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("Flagged");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("");
        cMap.setDefault("0");
        cMap.setInheritance("false");
        cMap.setPosition(6);
        tMap.addColumn(cMap);
        // ------------- Column: recent --------------------
        cMap = new ColumnMap("recent", tMap);
        cMap.setType(new Integer(0));
        cMap.setTorqueType("BOOLEANINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("Recent");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("");
        cMap.setDefault("0");
        cMap.setInheritance("false");
        cMap.setPosition(7);
        tMap.addColumn(cMap);
        // ------------- Column: seen --------------------
        cMap = new ColumnMap("seen", tMap);
        cMap.setType(new Integer(0));
        cMap.setTorqueType("BOOLEANINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("Seen");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("");
        cMap.setDefault("0");
        cMap.setInheritance("false");
        cMap.setPosition(8);
        tMap.addColumn(cMap);
        tMap.setUseInheritance(false);
    }
    
    public static void populateMessageBody(TableMap tMap) {
        tMap.setJavaName("MessageBody");
        tMap
                .setOMClass(org.apache.james.imap.jpa.om.MessageBody.class);
        tMap
                .setPeerClass(org.apache.james.imap.jpa.om.MessageBodyPeer.class);
        tMap.setPrimaryKeyMethod("none");

        ColumnMap cMap = null;

        // ------------- Column: mailbox_id --------------------
        cMap = new ColumnMap("mailbox_id", tMap);
        cMap.setType(new Long(0));
        cMap.setTorqueType("BIGINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(true);
        cMap.setNotNull(true);
        cMap.setJavaName("MailboxId");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("Mailbox Id");
        cMap.setInheritance("false");
        cMap.setForeignKey("message", "mailbox_id");
        cMap.setPosition(1);
        tMap.addColumn(cMap);
        // ------------- Column: uid --------------------
        cMap = new ColumnMap("uid", tMap);
        cMap.setType(new Long(0));
        cMap.setTorqueType("BIGINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(true);
        cMap.setNotNull(true);
        cMap.setJavaName("Uid");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("");
        cMap.setInheritance("false");
        cMap.setForeignKey("message", "uid");
        cMap.setPosition(2);
        tMap.addColumn(cMap);
        // ------------- Column: body --------------------
        cMap = new ColumnMap("body", tMap);
        cMap.setType(new Object());
        cMap.setTorqueType("BLOB");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("Body");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("value");
        cMap.setInheritance("false");
        cMap.setPosition(3);
        tMap.addColumn(cMap);
        tMap.setUseInheritance(false);
    }
    
    public static void populateMailboxRow(TableMap tMap) {
        tMap.setJavaName("MailboxRow");
        tMap
                .setOMClass(org.apache.james.imap.jpa.om.MailboxRow.class);
        tMap
                .setPeerClass(org.apache.james.imap.jpa.om.MailboxRowPeer.class);
        tMap.setDescription("Mailbox Table");
        tMap.setPrimaryKeyMethod(TableMap.NATIVE);
        tMap.setPrimaryKeyMethodInfo("mailbox_SEQ");

        ColumnMap cMap = null;

        // ------------- Column: mailbox_id --------------------
        cMap = new ColumnMap("mailbox_id", tMap);
        cMap.setType(new Long(0));
        cMap.setTorqueType("BIGINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(true);
        cMap.setNotNull(true);
        cMap.setJavaName("MailboxId");
        cMap.setAutoIncrement(true);
        cMap.setProtected(false);
        cMap.setDescription("Mailbox Id");
        cMap.setInheritance("false");
        cMap.setPosition(1);
        tMap.addColumn(cMap);
        // ------------- Column: name --------------------
        cMap = new ColumnMap("name", tMap);
        cMap.setType("");
        cMap.setTorqueType("VARCHAR");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("Name");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("full-namespace-name");
        cMap.setInheritance("false");
        cMap.setSize(255);
        cMap.setPosition(2);
        tMap.addColumn(cMap);
        // ------------- Column: uid_validity --------------------
        cMap = new ColumnMap("uid_validity", tMap);
        cMap.setType(new Long(0));
        cMap.setTorqueType("BIGINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("UidValidity");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("the last used uid (default 0)");
        cMap.setInheritance("false");
        cMap.setPosition(3);
        tMap.addColumn(cMap);
        // ------------- Column: last_uid --------------------
        cMap = new ColumnMap("last_uid", tMap);
        cMap.setType(new Long(0));
        cMap.setTorqueType("BIGINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(true);
        cMap.setJavaName("LastUid");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("the last used uid (default 0)");
        cMap.setInheritance("false");
        cMap.setPosition(4);
        tMap.addColumn(cMap);
        // ------------- Column: message_count --------------------
        cMap = new ColumnMap("message_count", tMap);
        cMap.setType(new Integer(0));
        cMap.setTorqueType("INTEGER");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(false);
        cMap.setJavaName("MessageCount");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("total message number");
        cMap.setDefault("0");
        cMap.setInheritance("false");
        cMap.setPosition(5);
        tMap.addColumn(cMap);
        // ------------- Column: size --------------------
        cMap = new ColumnMap("size", tMap);
        cMap.setType(new Long(0));
        cMap.setTorqueType("BIGINT");
        cMap.setUsePrimitive(true);
        cMap.setPrimaryKey(false);
        cMap.setNotNull(false);
        cMap.setJavaName("Size");
        cMap.setAutoIncrement(false);
        cMap.setProtected(false);
        cMap.setDescription("size of this mailbox in byte");
        cMap.setDefault("0");
        cMap.setInheritance("false");
        cMap.setPosition(6);
        tMap.addColumn(cMap);
        tMap.setUseInheritance(false);
    }
}
