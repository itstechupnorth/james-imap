package org.apache.james.imap.jcr;

import java.util.Date;
import java.util.List;

import javax.jcr.Repository;
import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.james.imap.jcr.mail.model.JCRHeader;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.store.StoreMailbox;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

public class JCRMailbox extends StoreMailbox{

    private final Repository repos;
    private final String workspace;
    private final Log log;
    public JCRMailbox(final Mailbox mailbox, final MailboxSession session, final Repository repos, final String workspace, final Log log) {
        super(mailbox, session );
        this.repos = repos;
        this.workspace = workspace;
        this.log = log;
    }

    @Override
    protected MailboxMembership copyMessage(MailboxMembership originalMessage, long uid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        return new JCRHeader(name, value, lineNumber, log);
    }

    @Override
    protected MailboxMembership createMessage(Date internalDate, long uid, int size, int bodyStartOctet, byte[] document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected MessageMapper createMessageMapper(MailboxSession session) {
        
        return null;
    }

    @Override
    protected Mailbox getMailboxRow() throws MailboxException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Mailbox reserveNextUid() throws MailboxException {
        return null;
    }

}
