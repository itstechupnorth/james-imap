package org.apache.james.imap.jpa.om.map;

import org.apache.torque.TorqueException;

/**
 * This is a Torque Generated class that is used to load all database map
 * information at once. This is useful because Torque's default behaviour is to
 * do a "lazy" load of mapping information, e.g. loading it only when it is
 * needed.
 * <p>
 * 
 * @see org.apache.torque.map.DatabaseMap#initialize() DatabaseMap.initialize()
 */
public class MailboxmanagerMapInit {
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
}
