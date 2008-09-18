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

package org.apache.james.mailboxmanager.mock;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.james.imap.functional.ExperimentalHostSystem;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.mailboxmanager.torque.TorqueMailboxManager;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.james.mailboxmanager.torque.om.MessageBodyPeer;
import org.apache.james.mailboxmanager.torque.om.MessageFlagsPeer;
import org.apache.james.mailboxmanager.torque.om.MessageHeaderPeer;
import org.apache.james.mailboxmanager.torque.om.MessageRowPeer;
import org.apache.torque.Torque;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.Transaction;

public class TorqueMailboxManagerProviderSingleton {

    // TODO: replicates code in server
    private static final String[] tableNames = new String[] {
            MailboxRowPeer.TABLE_NAME, MessageRowPeer.TABLE_NAME,
            MessageFlagsPeer.TABLE_NAME, MessageHeaderPeer.TABLE_NAME,
            MessageBodyPeer.TABLE_NAME };

    // TODO: replicates code in server
    private static final String[] CREATE_STATEMENTS = {
            "CREATE TABLE mailbox"
                    + "("
                    + "  mailbox_id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,"
                    + "        name VARCHAR(255) NOT NULL,"
                    + "        uid_validity BIGINT NOT NULL,"
                    + "        last_uid BIGINT NOT NULL,"
                    + "        message_count INTEGER default 0,"
                    + "        size BIGINT default 0,"
                    + "        PRIMARY KEY(mailbox_id),"
                    + "        UNIQUE (name))",
            "        CREATE TABLE message"
                    + "    ("
                    + "        mailbox_id BIGINT NOT NULL,"
                    + "        uid BIGINT NOT NULL,"
                    + "        internal_date TIMESTAMP,"
                    + "        size INTEGER,"
                    + "        PRIMARY KEY(mailbox_id,uid),"
                    + "        FOREIGN KEY (mailbox_id) REFERENCES mailbox (mailbox_id)"
                    + "            ON DELETE CASCADE" + "      )",
            "CREATE TABLE message_flags"
                    + "    ("
                    + "        mailbox_id BIGINT NOT NULL,"
                    + "        uid BIGINT NOT NULL,"
                    + "        answered INTEGER default 0 NOT NULL,"
                    + "        deleted INTEGER default 0 NOT NULL,"
                    + "        draft INTEGER default 0 NOT NULL,"
                    + "        flagged INTEGER default 0 NOT NULL,"
                    + "        recent INTEGER default 0 NOT NULL,"
                    + "        seen INTEGER default 0 NOT NULL,"
                    + "        PRIMARY KEY(mailbox_id,uid),"
                    + "        FOREIGN KEY (mailbox_id, uid) REFERENCES message (mailbox_id, uid)"
                    + "            ON DELETE CASCADE" + "      )",
            "CREATE TABLE message_header"
                    + "    ("
                    + "        mailbox_id BIGINT NOT NULL,"
                    + "        uid BIGINT NOT NULL,"
                    + "        line_number INTEGER NOT NULL,"
                    + "        field VARCHAR(256) NOT NULL,"
                    + "        value VARCHAR(1024) NOT NULL,"
                    + "        PRIMARY KEY(mailbox_id,uid,line_number),"
                    + "        FOREIGN KEY (mailbox_id, uid) REFERENCES message (mailbox_id, uid)"
                    + "            ON DELETE CASCADE" + "      )",
            "CREATE TABLE message_body"
                    + "    ("
                    + "        mailbox_id BIGINT NOT NULL,"
                    + "        uid BIGINT NOT NULL,"
                    + "        body BLOB NOT NULL,"
                    + "        PRIMARY KEY(mailbox_id,uid),"
                    + "        FOREIGN KEY (mailbox_id, uid) REFERENCES message (mailbox_id, uid)"
                    + "            ON DELETE CASCADE" + "      )" };

    // TODO: replicates code in server
    public static void initialize() throws Exception {
        BaseConfiguration torqueConf = configureDefaults();
        Connection conn = null;
        Torque.init(torqueConf);
        conn = Transaction.begin(MailboxRowPeer.DATABASE_NAME);

        DatabaseMetaData dbMetaData = conn.getMetaData();

        for (int i = 0; i < tableNames.length; i++) {
            if (!tableExists(dbMetaData, tableNames[i])) {
                BasePeer.executeStatement(CREATE_STATEMENTS[i], conn);
                System.out.println("Created table " + tableNames[i]);
                System.out.println(CREATE_STATEMENTS[i]);
            }
        }

        Transaction.commit(conn);
        System.out.println("MailboxManager has been initialized");
    }

    // TODO: replicates code in server
    private static boolean tableExists(DatabaseMetaData dbMetaData,
            String tableName) throws SQLException {
        return (tableExistsCaseSensitive(dbMetaData, tableName)
                || tableExistsCaseSensitive(dbMetaData, tableName
                        .toUpperCase(Locale.US)) || tableExistsCaseSensitive(
                dbMetaData, tableName.toLowerCase(Locale.US)));
    }

    // TODO: replicates code in server
    private static boolean tableExistsCaseSensitive(
            DatabaseMetaData dbMetaData, String tableName) throws SQLException {
        ResultSet rsTables = dbMetaData.getTables(null, null, tableName, null);
        try {
            boolean found = rsTables.next();
            return found;
        } finally {
            if (rsTables != null) {
                rsTables.close();
            }
        }
    }

    // TODO: replicates code in server
    public static BaseConfiguration configureDefaults()
            throws org.apache.commons.configuration.ConfigurationException {
        BaseConfiguration torqueConf = new BaseConfiguration();
        torqueConf.addProperty("torque.database.default", "mailboxmanager");
        torqueConf.addProperty("torque.database.mailboxmanager.adapter",
                "derby");
        torqueConf.addProperty("torque.dsfactory.mailboxmanager.factory",
                "org.apache.torque.dsfactory.SharedPoolDataSourceFactory");
        torqueConf.addProperty(
                "torque.dsfactory.mailboxmanager.connection.driver",
                "org.apache.derby.jdbc.EmbeddedDriver");
        torqueConf.addProperty(
                "torque.dsfactory.mailboxmanager.connection.url",
                "jdbc:derby:target/testdb;create=true");
        torqueConf.addProperty(
                "torque.dsfactory.mailboxmanager.connection.user", "app");
        torqueConf.addProperty(
                "torque.dsfactory.mailboxmanager.connection.password", "app");
        torqueConf.addProperty(
                "torque.dsfactory.mailboxmanager.pool.maxActive", "100");
        return torqueConf;
    }

    private static TorqueMailboxManager torqueMailboxManager;

    private static SimpleUserManager userManager;

    private static SimpleMailboxManagerProvider provider;

    public static final ExperimentalHostSystem host = new ExperimentalHostSystem();

    public synchronized static MailboxManagerProvider getTorqueMailboxManagerProviderInstance()
            throws Exception {
        if (provider == null) {
            getMailboxManager();
            provider = new SimpleMailboxManagerProvider();
            provider.setMailboxManager(torqueMailboxManager);
        }
        return provider;

    }

    public static void addUser(String user, String password) {
        userManager.addUser(user, password);
    }

    private static TorqueMailboxManager getMailboxManager() throws Exception {
        if (torqueMailboxManager == null) {
            userManager = new SimpleUserManager();
            initialize();
            torqueMailboxManager = new TorqueMailboxManager(userManager);
        }
        return torqueMailboxManager;
    }

    public static void reset() throws Exception {
        getMailboxManager().deleteEverything();
    }

}
