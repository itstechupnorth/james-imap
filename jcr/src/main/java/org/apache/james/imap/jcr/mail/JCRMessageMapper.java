package org.apache.james.imap.jcr.mail;

import java.util.List;

import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.transaction.NonTransactionalMapper;

public class JCRMessageMapper extends NonTransactionalMapper implements MessageMapper{

    @Override
    public long countMessagesInMailbox() throws StorageException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long countUnseenMessagesInMailbox() throws StorageException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(MailboxMembership message) throws StorageException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<MailboxMembership> findInMailbox(MessageRange set) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MailboxMembership> findMarkedForDeletionInMailbox(MessageRange set) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MailboxMembership> findRecentMessagesInMailbox() throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MailboxMembership> findUnseenMessagesInMailboxOrderByUid() throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void save(MailboxMembership message) throws StorageException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<MailboxMembership> searchMailbox(SearchQuery query) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }
}
