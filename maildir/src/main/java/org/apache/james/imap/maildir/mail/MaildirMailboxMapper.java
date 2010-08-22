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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.james.imap.api.MailboxPath;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.MailboxNotFoundException;
import org.apache.james.imap.mailbox.StorageException;
import org.apache.james.imap.maildir.MaildirFolder;
import org.apache.james.imap.maildir.MaildirMessageName;
import org.apache.james.imap.maildir.MaildirStore;
import org.apache.james.imap.maildir.mail.model.MaildirMailbox;
import org.apache.james.imap.store.mail.MailboxMapper;
import org.apache.james.imap.store.mail.model.Mailbox;
import org.apache.james.imap.store.transaction.NonTransactionalMapper;

public class MaildirMailboxMapper extends NonTransactionalMapper implements MailboxMapper<Integer> {

    /**
     * The {@link MaildirStore} the mailboxes reside in
     */
    private final MaildirStore maildirStore;
    
    /**
     * A request-scoped list of mailboxes in order to refer to them via id
     */
    private ArrayList<Mailbox<Integer>> mailboxCache = new ArrayList<Mailbox<Integer>>();
    
    public MaildirMailboxMapper(String maildirLocation) {
        this.maildirStore = new MaildirStore(maildirLocation);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#delete(org.apache.james.imap.store.mail.model.Mailbox)
     */
    public void delete(Mailbox<Integer> mailbox) throws StorageException {
        String folderName = maildirStore.getFolderName(mailbox);
        File folder = new File(folderName);
        if (folder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(folder);
            } catch (IOException e) {
                throw new StorageException(HumanReadableText.DELETED_FAILED, e);
            }
        }
        else
            throw new StorageException(HumanReadableText.DELETED_FAILED,
                    new MailboxNotFoundException(mailbox.getName()));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#deleteAll()
     */
    public void deleteAll() throws StorageException {
        // not used
        throw new NotImplementedException();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxById(java.lang.Object)
     */
    public Mailbox<Integer> findMailboxById(Integer mailboxId) throws StorageException,
            MailboxNotFoundException {
        // not used
        throw new NotImplementedException();
    }
    
    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxByPath(org.apache.james.imap.api.MailboxPath)
     */
    public Mailbox<Integer> findMailboxByPath(MailboxPath mailboxPath)
            throws StorageException, MailboxNotFoundException {
        String folder = maildirStore.getFolderName(mailboxPath);
        File f = new File(folder);
        if (!f.isDirectory())
            throw new MailboxNotFoundException(mailboxPath);
        Mailbox<Integer> mailbox = maildirStore.loadMailbox(f, mailboxPath);
        return cacheMailbox(mailbox);
    }
    
    /* 
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#findMailboxWithPathLike(org.apache.james.imap.api.MailboxPath)
     */
    public List<Mailbox<Integer>> findMailboxWithPathLike(MailboxPath mailboxPath)
            throws StorageException {
        final Pattern searchPattern = Pattern.compile("[" + MaildirStore.maildirDelimiter + "]"
                + mailboxPath.getName().replace(".", "\\.").replace(MaildirStore.WILDCARD, ".*"));
        FilenameFilter filter = MaildirMessageName.createRegexFilter(searchPattern);
        File root = maildirStore.getMailboxRootForUser(mailboxPath.getUser());
        File[] folders = root.listFiles(filter);
        ArrayList<Mailbox<Integer>> mailboxList = new ArrayList<Mailbox<Integer>>();
        for (File folder : folders)
            if (folder.isDirectory()) {
                Mailbox<Integer> mailbox = maildirStore.loadMailbox(root, mailboxPath.getNamespace(), mailboxPath.getUser(), folder.getName());
                mailboxList.add(cacheMailbox(mailbox));
            }
        // INBOX is in the root of the folder
        if (Pattern.matches(mailboxPath.getName().replace(MaildirStore.WILDCARD, ".*"), MaildirStore.INBOX)) {
            Mailbox<Integer> mailbox = maildirStore.loadMailbox(root, mailboxPath.getNamespace(), mailboxPath.getUser(), "");
            mailboxList.add(0, cacheMailbox(mailbox));
        }
        return mailboxList;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#hasChildren(org.apache.james.imap.store.mail.model.Mailbox)
     */
    public boolean hasChildren(Mailbox<Integer> mailbox) throws StorageException, MailboxNotFoundException {
        String searchString = mailbox.getName() + MaildirStore.maildirDelimiter + MaildirStore.WILDCARD;
        List<Mailbox<Integer>> mailboxes = findMailboxWithPathLike(
                new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), searchString));
        return (mailboxes.size() > 0);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.MailboxMapper#save(org.apache.james.imap.store.mail.model.Mailbox)
     */
    public void save(Mailbox<Integer> mailbox) throws StorageException {
        try {
            Mailbox<Integer> originalMailbox = getCachedMailbox(mailbox.getMailboxId());
            MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
            if (originalMailbox.getName() != mailbox.getName()) {
                if (folder.exists())
                    throw new StorageException(HumanReadableText.MAILBOX_EXISTS, null);
                
                MaildirFolder originalFolder = maildirStore.createMaildirFolder(originalMailbox);
                // renaming the INBOX means to move its contents to the new folder 
                if (originalMailbox.getName().equals(MaildirStore.INBOX)) {
                    File inboxFolder = originalFolder.getRootFile();
                    File newFolder = folder.getRootFile();
                    if (!newFolder.mkdirs())
                        throw new StorageException(HumanReadableText.SAVE_FAILED, null);
                    originalFolder.getCurFolder().renameTo(folder.getCurFolder());
                    originalFolder.getNewFolder().renameTo(folder.getNewFolder());
                    originalFolder.getTmpFolder().renameTo(folder.getTmpFolder());
                    (new File(inboxFolder, MaildirFolder.UIDLIST_FILE)).renameTo(
                            (new File(newFolder, MaildirFolder.UIDLIST_FILE)));
                    (new File(inboxFolder, MaildirFolder.VALIDITY_FILE)).renameTo(
                            (new File(newFolder, MaildirFolder.VALIDITY_FILE)));
                    // recreate the INBOX folders, uidvalidity and uidlist will
                    // automatically be recreated later
                    originalFolder.getCurFolder().mkdir();
                    originalFolder.getNewFolder().mkdir();
                    originalFolder.getTmpFolder().mkdir();
                }
                else {
                    if (!originalFolder.getRootFile().renameTo(folder.getRootFile()))
                        throw new StorageException(HumanReadableText.SAVE_FAILED, 
                                new IOException("Could not rename folder " + originalFolder));
                }
            }
        } catch (MailboxNotFoundException e) {
            // it cannot be found and is thus new
            MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
            if (!folder.exists()) {
                boolean success = folder.getRootFile().mkdirs();
                if (!success)
                    throw new StorageException(HumanReadableText.SAVE_FAILED, null);
                success = folder.getCurFolder().mkdir();
                success = success && folder.getNewFolder().mkdir();
                success = success && folder.getTmpFolder().mkdir();
                if (!success)
                    throw new StorageException(HumanReadableText.SAVE_FAILED, new IOException("Needed folder structure can not be created"));
            }
            try {
                folder.setUidValidity(mailbox.getUidValidity());
            } catch (IOException ioe) {
                throw new StorageException(HumanReadableText.SAVE_FAILED, ioe);
            }
        }
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.transaction.TransactionalMapper#endRequest()
     */
    public void endRequest() {
        mailboxCache.clear();
    }
    
    /**
     * Stores a copy of a mailbox in a cache valid for one request. This is to enable
     * referring to renamed mailboxes via id.
     * @param mailbox The mailbox to cache
     * @return The id of the cached mailbox
     */
    private Mailbox<Integer> cacheMailbox(Mailbox<Integer> mailbox) {
        mailboxCache.add(new MaildirMailbox(mailbox));
        int id = mailboxCache.size() - 1;
        ((MaildirMailbox) mailbox).setMailboxId(id);
        return mailbox;
    }
    
    /**
     * Retrieves a mailbox from the cache
     * @param mailboxId The id of the mailbox to retrieve
     * @return The mailbox
     * @throws MailboxNotFoundException If the mailboxId is not in the cache
     */
    private Mailbox<Integer> getCachedMailbox(Integer mailboxId) throws MailboxNotFoundException {
        if (mailboxId == null)
            throw new MailboxNotFoundException("null");
        try {
            return mailboxCache.get(mailboxId);
        } catch (IndexOutOfBoundsException e) {
            throw new MailboxNotFoundException(mailboxId);
        }
    }

}
