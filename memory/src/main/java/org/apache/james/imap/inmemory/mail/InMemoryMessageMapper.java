package org.apache.james.imap.inmemory.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.james.imap.inmemory.mail.model.InMemoryMailbox;
import org.apache.james.imap.inmemory.mail.model.SimpleMailboxMembership;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.store.MailboxMembershipComparator;
import org.apache.james.imap.store.SearchQueryIterator;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;

public class InMemoryMessageMapper implements MessageMapper<Long> {

    private Map<Long, Map<Long, MailboxMembership<Long>>> mailboxByUid;
    private static final int INITIAL_SIZE = 256;
    
    public InMemoryMessageMapper() {
        this.mailboxByUid = new ConcurrentHashMap<Long, Map<Long, MailboxMembership<Long>>>(INITIAL_SIZE);
    }
    
    private Map<Long, MailboxMembership<Long>> getMembershipByUidForMailbox(Mailbox<Long> mailbox) {
        Map<Long, MailboxMembership<Long>> membershipByUid = mailboxByUid.get(mailbox.getMailboxId());
        if (membershipByUid == null) {
            membershipByUid = new ConcurrentHashMap<Long, MailboxMembership<Long>>(INITIAL_SIZE);
            mailboxByUid.put(mailbox.getMailboxId(), membershipByUid);
        }
        return membershipByUid;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox()
     */
    public long countMessagesInMailbox(Mailbox<Long> mailbox) throws StorageException {
        return getMembershipByUidForMailbox(mailbox).size();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countUnseenMessagesInMailbox()
     */
    public long countUnseenMessagesInMailbox(Mailbox<Long> mailbox) throws StorageException {
        long count = 0;
        for(MailboxMembership<Long> member:getMembershipByUidForMailbox(mailbox).values()) {
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
    public void delete(Mailbox<Long> mailbox, MailboxMembership<Long> message) throws StorageException {
        getMembershipByUidForMailbox(mailbox).remove(message.getUid());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findInMailbox(java.lang.Object, org.apache.james.imap.mailbox.MessageRange)
     */
    @SuppressWarnings("unchecked")
    public List<MailboxMembership<Long>> findInMailbox(Mailbox<Long> mailbox, MessageRange set) throws StorageException {
        final List<MailboxMembership<Long>> results;
        final MessageRange.Type type = set.getType();
        switch (type) {
            case ALL:
                results = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailbox).values());
                break;
            case FROM:
                results = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailbox).values());
                for (final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
                   if (it.next().getUid()< set.getUidFrom()) {
                       it.remove(); 
                   }
                }
                break;
            case RANGE:
                results = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailbox).values());
                for (final Iterator<MailboxMembership<Long>> it=results.iterator();it.hasNext();) {
                   final long uid = it.next().getUid();
                if (uid<set.getUidFrom() || uid>set.getUidTo()) {
                       it.remove(); 
                   }
                }
                break;
            case ONE:
                results  = new ArrayList<MailboxMembership<Long>>(1);
                final MailboxMembership member = getMembershipByUidForMailbox(mailbox).get(set.getUidFrom());
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
    public List<MailboxMembership<Long>> findMarkedForDeletionInMailbox(Mailbox<Long> mailbox, MessageRange set) throws StorageException {
        final List<MailboxMembership<Long>> results = findInMailbox(mailbox, set);
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
    public List<MailboxMembership<Long>> findRecentMessagesInMailbox(Mailbox<Long> mailbox,int limit) throws StorageException {
        final List<MailboxMembership<Long>> results = new ArrayList<MailboxMembership<Long>>();
        for(MailboxMembership<Long> member:getMembershipByUidForMailbox(mailbox).values()) {
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
     * @see org.apache.james.imap.store.mail.MessageMapper#findFirstUnseenMessageUid(org.apache.james.imap.store.mail.model.Mailbox)
     */
    public Long findFirstUnseenMessageUid(Mailbox<Long> mailbox) throws StorageException {
        List<MailboxMembership<Long>> memberships = new ArrayList<MailboxMembership<Long>>(getMembershipByUidForMailbox(mailbox).values());
        Collections.sort(memberships, MailboxMembershipComparator.INSTANCE);
        for (int i = 0;  i < memberships.size(); i++) {
            MailboxMembership<Long> m = memberships.get(i);
            if (m.isSeen() == false) {
                return m.getUid();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#save(org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public long save(Mailbox<Long> mailbox, MailboxMembership<Long> message) throws StorageException {
        getMembershipByUidForMailbox(mailbox).put(message.getUid(), message);
        return message.getUid();
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.imap.mailbox.SearchQuery)
     */
    public Iterator<Long> searchMailbox(Mailbox<Long> mailbox, SearchQuery query) throws StorageException {
        List<MailboxMembership<?>> memberships = new ArrayList<MailboxMembership<?>>(getMembershipByUidForMailbox(mailbox).values());
        Collections.sort(memberships, MailboxMembershipComparator.INSTANCE);

        return new SearchQueryIterator(memberships.iterator(), query);
    }

    /**
     * There is no really Transaction handling here.. Just run it 
     */
    public <T> T execute(Transaction<T> transaction) throws MailboxException {
        return transaction.run();
    }
    
    public void deleteAll() {
        mailboxByUid.clear();
    }

    /**
     * Do nothing
     */
    public void endRequest() {
        // Do nothing
    }

  
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#copy(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public MailboxMembership<Long> copy(Mailbox<Long> mailbox, MailboxMembership<Long> original) throws StorageException {
        ((InMemoryMailbox) mailbox).consumeUid();
        SimpleMailboxMembership membership = new SimpleMailboxMembership(mailbox.getMailboxId(), mailbox.getLastUid(), (SimpleMailboxMembership) original);
        save(mailbox, membership);
        return membership;
    }
    
}
