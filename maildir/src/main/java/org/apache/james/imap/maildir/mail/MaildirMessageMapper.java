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
package org.apache.james.imap.maildir.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.Map.Entry;

import javax.mail.util.SharedFileInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.james.imap.maildir.MaildirFolder;
import org.apache.james.imap.maildir.MaildirMessageName;
import org.apache.james.imap.maildir.MaildirStore;
import org.apache.james.imap.maildir.UidConstraint;
import org.apache.james.imap.maildir.mail.model.MaildirHeader;
import org.apache.james.imap.maildir.mail.model.MaildirMessage;
import org.apache.james.imap.store.SearchQueryIterator;
import org.apache.james.imap.store.mail.MessageMapper;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.mail.model.MailboxMembership;
import org.apache.james.imap.store.mail.model.PropertyBuilder;
import org.apache.james.imap.store.streaming.ConfigurableMimeTokenStream;
import org.apache.james.imap.store.streaming.CountingInputStream;
import org.apache.james.imap.store.transaction.NonTransactionalMapper;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.MessageRange.Type;
import org.apache.james.mailbox.SearchQuery.Criterion;
import org.apache.james.mailbox.SearchQuery.NumericRange;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.descriptor.MaximalBodyDescriptor;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.apache.james.mime4j.parser.MimeTokenStream;

public class MaildirMessageMapper extends NonTransactionalMapper implements MessageMapper<Integer> {

    private final MaildirStore maildirStore;
    private final int BUF_SIZE = 2048;

    public MaildirMessageMapper(String  maildirLocation) {
        this.maildirStore = new MaildirStore(maildirLocation);
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#copy(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public long copy(Mailbox<Integer> mailbox, MailboxMembership<Integer> original)
    throws MailboxException {
        MaildirMessage theCopy = new MaildirMessage(mailbox, (MaildirMessage) original);

        return save(mailbox, theCopy);
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countMessagesInMailbox(org.apache.james.imap.store.mail.model.Mailbox)
     */
    public long countMessagesInMailbox(Mailbox<Integer> mailbox) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        File newFolder = folder.getNewFolder();
        File curFolder = folder.getCurFolder();
        File[] newFiles = newFolder.listFiles();
        File[] curFiles = curFolder.listFiles();
        if (newFiles == null || curFiles == null)
            throw new MailboxException("Unable to count messages in Mailbox " + mailbox,
                    new IOException("Not a valid Maildir folder: " + maildirStore.getFolderName(mailbox)));
        int count = newFiles.length + curFiles.length;
        return count;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#countUnseenMessagesInMailbox(org.apache.james.imap.store.mail.model.Mailbox)
     */
    public long countUnseenMessagesInMailbox(Mailbox<Integer> mailbox) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        File newFolder = folder.getNewFolder();
        File curFolder = folder.getCurFolder();
        String[] unseenMessages = curFolder.list(MaildirMessageName.FILTER_UNSEEN_MESSAGES);
        String[] newUnseenMessages = newFolder.list(MaildirMessageName.FILTER_UNSEEN_MESSAGES);
        if (newUnseenMessages == null || unseenMessages == null)
            throw new MailboxException("Unable to count unseen messages in Mailbox " + mailbox,
                    new IOException("Not a valid Maildir folder: " + maildirStore.getFolderName(mailbox)));
        int count = newUnseenMessages.length + unseenMessages.length;
        return count;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#delete(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public void delete(Mailbox<Integer> mailbox, MailboxMembership<Integer> message) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        try {
            folder.delete(message.getUid());
        } catch (IOException e) {
            throw new MailboxException("Unable to delete Message " + message + " in Mailbox " + mailbox, e);
        }
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findInMailbox(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    public List<MailboxMembership<Integer>> findInMailbox(Mailbox<Integer> mailbox, MessageRange set)
    throws MailboxException {
        final List<MailboxMembership<Integer>> results;
        final long from = set.getUidFrom();
        final long to = set.getUidTo();
        final Type type = set.getType();
        switch (type) {
        default:
        case ALL:
            results = findMessagesInMailboxBetweenUIDs(mailbox, null, 0, -1);
            break;
        case FROM:
            results = findMessagesInMailboxBetweenUIDs(mailbox, null, from, -1);
            break;
        case ONE:
            results = findMessageInMailboxWithUID(mailbox, from);
            break;
        case RANGE:
            results = findMessagesInMailboxBetweenUIDs(mailbox, null, from, to);
            break;       
        }
        return results;
    }

    private List<MailboxMembership<Integer>> findMessageInMailboxWithUID(Mailbox<Integer> mailbox, long uid)
    throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        MaildirMessageName messageName = null;
        try {
             messageName = folder.getMessageNameByUid(uid);
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Message with uid " + uid + " in Mailbox " + mailbox, e );
        }
        ArrayList<MailboxMembership<Integer>> messages = new ArrayList<MailboxMembership<Integer>>();
        if (messageName != null) {
            MaildirMessage message = loadMessage(mailbox, messageName, uid);
            messages.add(message);
        }
        return messages;
    }

    private List<MailboxMembership<Integer>> findMessagesInMailboxBetweenUIDs(Mailbox<Integer> mailbox,
            FilenameFilter filter, long from, long to) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        SortedMap<Long, MaildirMessageName> uidMap = null;
        try {
            if (filter != null)
                uidMap = folder.getUidMap(filter, from, to);
            else
                uidMap = folder.getUidMap(from, to);
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Messages in Mailbox " + mailbox, e );
        }
        ArrayList<MailboxMembership<Integer>> messages = new ArrayList<MailboxMembership<Integer>>();
        for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet()) {
            messages.add(loadMessage(mailbox, entry.getValue(), entry.getKey()));
        }
        return messages;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findMarkedForDeletionInMailbox(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    public List<MailboxMembership<Integer>> findMarkedForDeletionInMailbox(Mailbox<Integer> mailbox, MessageRange set) throws MailboxException {
        List<MailboxMembership<Integer>> results = new ArrayList<MailboxMembership<Integer>>();
        final long from = set.getUidFrom();
        final long to = set.getUidTo();
        final Type type = set.getType();
        switch (type) {
        default:
        case ALL:
            results = findMessagesInMailbox(mailbox, MaildirMessageName.FILTER_DELETED_MESSAGES, -1);
            break;
        case FROM:
            results = findMessagesInMailboxBetweenUIDs(mailbox, MaildirMessageName.FILTER_DELETED_MESSAGES, from, -1);
            break;
        case ONE:
            results = findDeletedMessageInMailboxWithUID(mailbox, from);
            break;
        case RANGE:
            results = findMessagesInMailboxBetweenUIDs(mailbox, MaildirMessageName.FILTER_DELETED_MESSAGES, from, to);
            break;       
        }
        return results;
    }

    private List<MailboxMembership<Integer>> findMessagesInMailbox(Mailbox<Integer> mailbox,
            FilenameFilter filter, int limit) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        SortedMap<Long, MaildirMessageName> uidMap = null;
        try {
            uidMap = folder.getUidMap(filter, limit);
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Messages in Mailbox " + mailbox, e );
        }
        ArrayList<MailboxMembership<Integer>> filtered = new ArrayList<MailboxMembership<Integer>>(uidMap.size());
        for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet())
            filtered.add(loadMessage(mailbox, entry.getValue(), entry.getKey()));
        return filtered;
    }

    private List<MailboxMembership<Integer>> findDeletedMessageInMailboxWithUID(
            Mailbox<Integer> mailbox, long uid) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        MaildirMessageName messageName = null;
        try {
             messageName = folder.getMessageNameByUid(uid);
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Messages in Mailbox " + mailbox, e );
        }
        ArrayList<MailboxMembership<Integer>> messages = new ArrayList<MailboxMembership<Integer>>();
        if (MaildirMessageName.FILTER_DELETED_MESSAGES.accept(null, messageName.getFullName())) {
            MaildirMessage message = loadMessage(mailbox, messageName, uid);
            messages.add(message);
        }
        return messages;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findRecentMessagesInMailbox(org.apache.james.imap.store.mail.model.Mailbox, int)
     */
    public List<MailboxMembership<Integer>> findRecentMessagesInMailbox(Mailbox<Integer> mailbox, int limit)
    throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        SortedMap<Long, MaildirMessageName> recentMessageNames;
        try {
            recentMessageNames = folder.getRecentMessages(limit);
        } catch (IOException e) {
            throw new MailboxException("Failure while search recent messages in Mailbox " + mailbox, e );
        }
        List<MailboxMembership<Integer>> recentMessages = new ArrayList<MailboxMembership<Integer>>(recentMessageNames.size());
        for (Entry<Long, MaildirMessageName> entry : recentMessageNames.entrySet())
            recentMessages.add(loadMessage(mailbox, entry.getValue(), entry.getKey()));
        return recentMessages;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#findFirstUnseenMessageUid(org.apache.james.imap.store.mail.model.Mailbox)
     */
    public Long findFirstUnseenMessageUid(Mailbox<Integer> mailbox)
    throws MailboxException {
        List<MailboxMembership<Integer>> result = findMessagesInMailbox(mailbox, MaildirMessageName.FILTER_UNSEEN_MESSAGES, 1);
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0).getUid();
        }
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#save(org.apache.james.imap.store.mail.model.Mailbox,
     * org.apache.james.imap.store.mail.model.MailboxMembership)
     */
    public long save(Mailbox<Integer> mailbox, MailboxMembership<Integer> message)
    throws MailboxException {
        MaildirMessage maildirMessage = (MaildirMessage) message;
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        long uid = 0;
        // a new message
        if (maildirMessage.isNew()) {
            // save file to "tmp" folder
            File tmpFolder = folder.getTmpFolder();
            // The only case in which we could get problems with clashing names is if the system clock
            // has been set backwards, then the server is restarted with the same pid, delivers the same
            // number of messages since its start in the exact same millisecond as done before and the
            // random number generator returns the same number.
            // In order to prevent this case we would need to check ALL files in all folders and compare
            // them to this message name. We rather let this happen once in a billion years...
            MaildirMessageName messageName = MaildirMessageName.createUniqueName(folder,
                    message.getMessage().getFullContentOctets());
            File messageFile = new File(tmpFolder, messageName.getFullName());
            FileOutputStream fos = null;
            try {
                messageFile.createNewFile();
                fos = new FileOutputStream(messageFile);
                InputStream input = message.getMessage().getFullContent();
                byte[] b = new byte[BUF_SIZE];
                int len = 0;
                while ((len = input.read(b)) != -1)
                    fos.write(b, 0, len);
            }
            catch (IOException ioe) {
                throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, ioe );
            }
            finally {
                try {
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File newMessageFile = null;
            // delivered via SMTP, goes to ./new without flags
            if (maildirMessage.isRecent()) {
                messageName.setFlags(message.createFlags());
                newMessageFile = new File(folder.getNewFolder(), messageName.getFullName());
                //System.out.println("save new recent " + message + " as " + newMessageFile.getName());
            }
            // appended via IMAP (might already have flags etc, goes to ./cur directly)
            else {
                messageName.setFlags(message.createFlags());
                newMessageFile = new File(folder.getCurFolder(), messageName.getFullName());
                //System.out.println("save new not recent " + message + " as " + newMessageFile.getName());
            }
            try {
                FileUtils.moveFile(messageFile, newMessageFile);
            } catch (IOException e) {
                System.err.println(newMessageFile);
                // TODO: Try copy and delete
                throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, e );
            }
            try {
                uid = folder.appendMessage(newMessageFile.getName());
            } catch (IOException e) {
                throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, e );
            }
        }
        // the message already exists and its flags need to be updated (everything else is immutable)
        else {
            try {
                MaildirMessageName messageName = folder.getMessageNameByUid(message.getUid());
                File messageFile = messageName.getFile();
                //System.out.println("save existing " + message + " as " + messageFile.getName());
                messageName.setFlags(message.createFlags());
                // this automatically moves messages from new to cur if needed
                String newMessageName = messageName.getFullName();
                messageFile.renameTo(new File(folder.getCurFolder(), newMessageName));
                uid = message.getUid();
                folder.update(uid, newMessageName);
            } catch (IOException e) {
                throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, e );
            }
        }
        return uid;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MessageMapper#searchMailbox(org.apache.james.imap.store.mail.model.Mailbox, org.apache.james.mailbox.SearchQuery)
     */
    public Iterator<Long> searchMailbox(Mailbox<Integer> mailbox, SearchQuery query)
    throws MailboxException {
        final List<Criterion> criteria = query.getCriterias();
        boolean range = false;
        int rangeLength = -1;
        UidConstraint constraint = new UidConstraint();
        
        if (criteria.size() == 1) {
            final Criterion firstCriterion = criteria.get(0);
            if (firstCriterion instanceof SearchQuery.UidCriterion) {
                final SearchQuery.UidCriterion uidCriterion = (SearchQuery.UidCriterion) firstCriterion;
                final NumericRange[] ranges = uidCriterion.getOperator().getRange();
                rangeLength = ranges.length;

                for (int i = 0; i < ranges.length; i++) {
                    final long low = ranges[i].getLowValue();
                    final long high = ranges[i].getHighValue();

                    if (low == Long.MAX_VALUE) {
                        constraint.lessOrEquals(high);
                        range = true;
                    } else if (low == high) {
                        constraint.equals(low);
                        range = false;
                    } else {
                        constraint.between(low, high);
                        range = true;
                    }
                }
            }
        }
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        SortedMap<Long, MaildirMessageName> uidMap;
        try {
            uidMap = folder.getUidMap(0, -1);
        } catch (IOException e) {
            throw new MailboxException("Failure while search in Mailbox " + mailbox, e );
        }
        LinkedList<MailboxMembership<?>> messages = new LinkedList<MailboxMembership<?>>();
        for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet()) {
            //System.out.println("check " + entry.getKey());
            if (constraint.isAllowed(entry.getKey())) {
                //System.out.println("allow " + entry.getKey());
                messages.add(loadMessage(mailbox, entry.getValue(), entry.getKey()));
                // Check if we only need to fetch 1 message, if so we can set a limit to speed up things
                if (rangeLength == 1 && range == false) break;
            }
        }
        return new SearchQueryIterator(messages.iterator(), query);
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.TransactionalMapper#endRequest()
     */
    public void endRequest() {
        // not used
        
    }
    
    /**
     * Creates a {@link MaildirMessage} object with data loaded from the repository.
     * @param mailbox The mailbox which the message resides in
     * @param messageName The name of the message
     * @return the {@link MaildirMessage} filled with data from the respective file
     * @throws MailboxException if there was an error parsing the message or the message could not be found
     */
    private MaildirMessage loadMessage(Mailbox<Integer> mailbox, MaildirMessageName messageName, Long uid)
    throws MailboxException {
        MaildirMessage message = null;
        try {
            File messageFile = messageName.getFile();
            message = parseMessage(messageFile, mailbox);
        } catch (IOException e) {
            throw new MailboxException("Parsing of message failed in Mailbox " + mailbox, e );
        }
        message.setFlags(messageName.getFlags());
        message.setInternalDate(messageName.getInternalDate());
        message.setUid(uid);
        return message;
    }
    
    /******************************************************************************
     * Until there is a database for properties, mail needs to be parse exactlty as
     * it is done when appending. Hence, this part below is basically a copy of
     * some parts of {@link StoreMessageManager}.
     * TODO: This code below needs to be extracted to some common method(s)!
     */

    /**
     * The initial size of the headers list
     */
    private static final int INITIAL_SIZE_HEADERS = 32;
    
    /**
     * Read a message file and extract the meta data:
     *   - size
     *   - headers
     *   - media type
     *   - content properties
     *   - charset
     *   - boundary
     *   - line count
     * It is missing the flags and internalDate
     * @param messageFile
     * @param mailbox
     * @return
     * @throws IOException
     */
    private MaildirMessage parseMessage(File messageFile, Mailbox<Integer> mailbox) throws IOException {
        SharedFileInputStream tmpMsgIn = null;
        try {
            tmpMsgIn = new SharedFileInputStream(messageFile);

            final int size = tmpMsgIn.available();
            final int bodyStartOctet = bodyStartOctet(tmpMsgIn);

            // Disable line length... This should be handled by the smtp server component and not the parser itself
            // https://issues.apache.org/jira/browse/IMAP-122
            MimeEntityConfig config = new MimeEntityConfig();
            config.setMaximalBodyDescriptor(true);
            config.setMaxLineLen(-1);
            final ConfigurableMimeTokenStream parser = new ConfigurableMimeTokenStream(config);

            parser.setRecursionMode(MimeTokenStream.M_NO_RECURSE);
            parser.parse(tmpMsgIn.newStream(0, -1));
            final List<Header> headers = new ArrayList<Header>(INITIAL_SIZE_HEADERS);

            int lineNumber = 0;
            int next = parser.next();
            while (next != MimeTokenStream.T_BODY
                    && next != MimeTokenStream.T_END_OF_STREAM
                    && next != MimeTokenStream.T_START_MULTIPART) {
                if (next == MimeTokenStream.T_FIELD) {
                    String fieldValue = parser.getField().getBody();
                    if (fieldValue.endsWith("\r\f")) {
                        fieldValue = fieldValue.substring(0,fieldValue.length() - 2);
                    }
                    if (fieldValue.startsWith(" ")) {
                        fieldValue = fieldValue.substring(1);
                    }
                    headers.add(new MaildirHeader(lineNumber, parser.getField().getName(), fieldValue));
                }
                next = parser.next();
            }
            final MaximalBodyDescriptor descriptor = (MaximalBodyDescriptor) parser.getBodyDescriptor();
            final PropertyBuilder propertyBuilder = new PropertyBuilder();
            final String mediaType;
            final String mediaTypeFromHeader = descriptor.getMediaType();
            final String subType;
            if (mediaTypeFromHeader == null) {
                mediaType = "text";
                subType = "plain";
            } else {
                mediaType = mediaTypeFromHeader;
                subType = descriptor.getSubType();
            }
            propertyBuilder.setMediaType(mediaType);
            propertyBuilder.setSubType(subType);
            propertyBuilder.setContentID(descriptor.getContentId());
            propertyBuilder.setContentDescription(descriptor.getContentDescription());
            propertyBuilder.setContentLocation(descriptor.getContentLocation());
            propertyBuilder.setContentMD5(descriptor.getContentMD5Raw());
            propertyBuilder.setContentTransferEncoding(descriptor.getTransferEncoding());
            propertyBuilder.setContentLanguage(descriptor.getContentLanguage());
            propertyBuilder.setContentDispositionType(descriptor.getContentDispositionType());
            propertyBuilder.setContentDispositionParameters(descriptor.getContentDispositionParameters());
            propertyBuilder.setContentTypeParameters(descriptor.getContentTypeParameters());
            // Add missing types
            final String codeset = descriptor.getCharset();
            if (codeset == null) {
                if ("TEXT".equalsIgnoreCase(mediaType)) {
                    propertyBuilder.setCharset("us-ascii");
                }
            } else {
                propertyBuilder.setCharset(codeset);
            }

            final String boundary = descriptor.getBoundary();
            if (boundary != null) {
                propertyBuilder.setBoundary(boundary);
            }   
            if ("text".equalsIgnoreCase(mediaType)) {
                final CountingInputStream bodyStream = new CountingInputStream(parser.getInputStream());
                bodyStream.readAll();
                long lines = bodyStream.getLineCount();

                next = parser.next();
                if (next == MimeTokenStream.T_EPILOGUE)  {
                    final CountingInputStream epilogueStream = new CountingInputStream(parser.getInputStream());
                    epilogueStream.readAll();
                    lines+=epilogueStream.getLineCount();
                }
                propertyBuilder.setTextualLineCount(lines);
            }
            FileInputStream documentIn = new FileInputStream(messageFile);
            final List<MaildirHeader> maildirHeaders = new ArrayList<MaildirHeader>(headers.size());
            for (Header header: headers) {
                maildirHeaders.add((MaildirHeader) header);
            }
            return new MaildirMessage(mailbox, size, documentIn, bodyStartOctet, maildirHeaders, propertyBuilder);
        }
        catch (MimeException e) {
            // has successfully been parsen when appending, shouldn't give any problems
        }
        finally {
            if (tmpMsgIn != null) {
                try {
                    tmpMsgIn.close();
                } catch (IOException e) {
                    // ignore on close
                }
            }
        }
        return null;
    }
    
    /**
     * Return the position in the given {@link InputStream} at which the Body of the 
     * Message starts
     * 
     * @param msgIn
     * @return bodyStartOctet
     * @throws IOException
     */
    private int bodyStartOctet(InputStream msgIn) throws IOException{
        // we need to pushback maximal 3 bytes
        PushbackInputStream in = new PushbackInputStream(msgIn,3);
        int bodyStartOctet = in.available();
        int i = -1;
        int count = 0;
        while ((i = in.read()) != -1 && in.available() > 4) {
            if (i == 0x0D) {
                int a = in.read();
                if (a == 0x0A) {
                    int b = in.read();

                    if (b == 0x0D) {
                        int c = in.read();

                        if (c == 0x0A) {
                            bodyStartOctet = count+4;
                            break;
                        }
                        in.unread(c);
                    }
                    in.unread(b);
                }
                in.unread(a);
            }
            count++;
        }
        return bodyStartOctet;
    }

}
