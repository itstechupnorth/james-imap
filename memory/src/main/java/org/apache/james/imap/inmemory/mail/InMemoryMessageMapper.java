package org.apache.james.imap.inmemory.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.MailboxMembershipComparator;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.MailboxMembership;

public class InMemoryMessageMapper implements MessageMapper<Long> {

    private Map<Long, Map<Long, MailboxMembership<Long>>> mailboxByUid;
    private static final int INITIAL_SIZE = 256;
    
    public InMemoryMessageMapper() {
        this.mailboxByUid = new ConcurrentHashMap<Long, Map<Long, MailboxMembership<Long>>>(INITIAL_SIZE);
    }
    
    private Map<Long, MailboxMembership<Long>> getMembershipByUidForMailbox(Long mailboxId) {
        Map<Long, MailboxMembership<Long>> membershipByUid = mailboxByUid.get(mailboxId);
        if (membershipByUid == null) {
            membershipByUid = new ConcurrentHashMap<Long, MailboxMembership<Long>>(INITIAL_SIZE);
            mailboxByUid.put(mailboxId, membershipByUid);
        }
        return membershipByUid;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox(Long mailboxId) throws StorageException {
        return getMembershipByUidForMailbox(mailboxId).size();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countUnseenMessagesInMailbox()
     */
    public long countUnseenMessagesInMailbox(Long mailboxId) throws StorageException {
        long count = 0;
        for(MailboxMembership<Long> member:getMembershipByUidForMailbox(mailboxId).values()) {
            if (!member.isSeen()) {
                count++;
            }
        }
        return count;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#delete(org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void delete(Long mailboxId, MailboxMembership<Long> message) throws StorageException {
        getMembershipByUidForMailbox(mailboxId).remove(message.getUid());
    }

    @SuppressWarnings("unchecked")
    public List<MailboxMembership<Long>> findInMailbox(Long mailboxId, MessageRange set) throws StorageException {
        final List<MailboxMembership<Long>> results;
        final MessageRange.Type type = set.getType();
        switch (type) {
            case ALL:
                results = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailboxId).values());
                break;
            case FROM:
                results = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailboxId).values());
                for (final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
                   if (it.next().getUid()< set.getUidFrom()) {
                       it.remove(); 
                   }
                }
                break;
            case RANGE:
                results = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailboxId).values());
                for (final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
                   final long uid = it.next().getUid();
                if (uid<set.getUidFrom() || uid>set.getUidTo()) {
                       it.remove(); 
                   }
                }
                break;
            case ONE:
                results  = new ArrayList<MailboxMembership<Long>>(1);
                final MailboxMembership member = getMembershipByUidForMailbox(mailboxId).get(set.getUidFrom());
                if (member != null) {
                    results.add(member);
                }
                break;
            default:
                results = new ArrayList<MailboxMembership<Long>>();
                break;
        }
        Collections.sort(results, MailboxMembershipComparator.INSTANCE);
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findMarkedForDeletionInMailbox(org.apache.james.imap.mailbox.MessageRange)
     */
    public List<MailboxMembership<Long>> findMarkedForDeletionInMailbox(Long mailboxId, MessageRange set) throws StorageException {
        final List<MailboxMembership<Long>> results = findInMailbox(mailboxId, set);
        for(final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
            if (!it.next().isDeleted()) {
                it.remove();
            }
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findRecentMessagesInMailbox()
     */
    public List<MailboxMembership<Long>> findRecentMessagesInMailbox(Long mailboxId,int limit) throws StorageException {
        final List<MailboxMembership<Long>> results = new ArrayList<MailboxMembership<Long>>();
        for(MailboxMembership<Long> member:getMembershipByUidForMailbox(mailboxId).values()) {
            if (member.isRecent()) {
                results.add(member);
            }
        }
        Collections.sort(results, MailboxMembershipComparator.INSTANCE);
        if (limit > 0 && limit > results.size()) {
            return results.subList(0, limit -1);
        } 
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findUnseenMessagesInMailbox()
     */
    public List<MailboxMembership<Long>> findUnseenMessagesInMailbox(Long mailboxId, int limit) throws StorageException {
        final List<MailboxMembership<Long>> results = new ArrayList<MailboxMembership<Long>>();
        for(MailboxMembership<Long> member:getMembershipByUidForMailbox(mailboxId).values()) {
            if (!member.isSeen()) {
                results.add(member);
            }
        }
        Collections.sort(results, MailboxMembershipComparator.INSTANCE);
        if (limit > 0 && limit > results.size()) {
            return results.subList(0, limit -1);
        } 
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#save(org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void save(Long mailboxId, MailboxMembership<Long> message) throws StorageException {
        getMembershipByUidForMailbox(mailboxId).put(message.getUid(), message);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache.james.imap.mailbox.SearchQuery)
     */
    public List<MailboxMembership<Long>> searchMailbox(Long mailboxId, SearchQuery query) throws StorageException {
        return new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailboxId).values());
    }

    /**
     * There is no really Transaction handling here.. Just run it 
     */
    public void execute(Transaction transaction) throws MailboxException {
        transaction.run();
    }
    
    public void deleteAll() {
        mailboxByUid.clear();
    }

    public void endRequest() {
        // Do nothing
    }
    
}
