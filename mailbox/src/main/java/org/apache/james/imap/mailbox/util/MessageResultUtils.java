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

package org.apache.james.imap.mailbox.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageResult;

public class MessageResultUtils {

    /**
     * Gets all header lines.
     * 
     * @param iterator
     *            {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>MessageResult.Header<code>'s,
     * in their natural order
     * 
     * @throws MessagingException
     */
    public static List getAll(final Iterator iterator) {
        final List results = new ArrayList();
        if (iterator != null) {
            while (iterator.hasNext()) {
                results.add(iterator.next());
            }
        }
        return results;
    }

    /**
     * Gets header lines whose header names matches (ignoring case) any of those
     * given.
     * 
     * @param names
     *            header names to be matched, not null
     * @param iterator
     *            {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>MessageResult.Header</code>'s, in
     *         their natural order
     * @throws MessagingException
     */
    public static List getMatching(final String[] names, final Iterator iterator)
            throws MessagingException {
        final List results = new ArrayList(20);
        if (iterator != null) {
            while (iterator.hasNext()) {
                MessageResult.Header header = (MessageResult.Header) iterator
                        .next();
                final String headerName = header.getName();
                if (headerName != null) {
                    final int length = names.length;
                    for (int i = 0; i < length; i++) {
                        final String name = names[i];
                        if (headerName.equalsIgnoreCase(name)) {
                            results.add(header);
                            break;
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * Gets header lines whose header names matches (ignoring case) any of those
     * given.
     * 
     * @param names
     *            header names to be matched, not null
     * @param iterator
     *            {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>MessageResult.Header</code>'s, in
     *         their natural order
     * @throws MessagingException
     */
    public static List getMatching(final Collection names,
            final Iterator iterator) throws MessagingException {
        final List result = matching(names, iterator, false);
        return result;
    }

    private static List matching(final Collection names,
            final Iterator iterator, boolean not)
            throws MailboxException {
        final List results = new ArrayList(names.size());
        if (iterator != null) {
            while (iterator.hasNext()) {
                final MessageResult.Header header = (MessageResult.Header) iterator
                        .next();
                final boolean match = contains(names, header);
                final boolean add = (not && !match) || (!not && match);
                if (add) {
                    results.add(header);
                }
            }
        }
        return results;
    }

    private static boolean contains(final Collection names,
            MessageResult.Header header) throws MailboxException {
        boolean match = false;
        final String headerName = header.getName();
        if (headerName != null) {
            for (final Iterator it = names.iterator(); it.hasNext();) {
                final String name = (String) it.next();
                if (name.equalsIgnoreCase(headerName)) {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }

    /**
     * Gets header lines whose header names matches (ignoring case) any of those
     * given.
     * 
     * @param names
     *            header names to be matched, not null
     * @param iterator
     *            {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>MessageResult.Header</code>'s, in
     *         their natural order
     * @throws MessagingException
     */
    public static List getNotMatching(final Collection names,
            final Iterator iterator) throws MessagingException {
        final List result = matching(names, iterator, true);
        return result;
    }

    /**
     * Gets a header matching the given name. The matching is case-insensitive.
     * 
     * @param name
     *            name to be matched, not null
     * @param iterator
     *            <code>Iterator</code> of <code>MessageResult.Header</code>'s,
     *            not null
     * @return <code>MessageResult.Header</code>, or null if the header does
     *         not exist
     * @throws MessagingException
     */
    public static MessageResult.Header getMatching(final String name,
            final Iterator<MessageResult.Header> iterator) throws MessagingException {
        MessageResult.Header result = null;
        if (name != null) {
            while (iterator.hasNext()) {
                MessageResult.Header header = iterator.next();
                final String headerName = header.getName();
                if (name.equalsIgnoreCase(headerName)) {
                    result = header;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Gets header lines whose header name fails to match (ignoring case) all of
     * the given names.
     * 
     * @param names
     *            header names, not null
     * @param iterator
     *            {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>@MessageResult.Header</code>'s, in their natural order
     * @throws MessagingException
     */
    public static List getNotMatching(final String[] names,
            final Iterator iterator) throws MessagingException {
        final List results = new ArrayList(20);
        if (iterator != null) {
            while (iterator.hasNext()) {
                MessageResult.Header header = (MessageResult.Header) iterator
                        .next();
                final String headerName = header.getName();
                if (headerName != null) {
                    final int length = names.length;
                    boolean match = false;
                    for (int i = 0; i < length; i++) {
                        final String name = names[i];
                        if (headerName.equalsIgnoreCase(name)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        results.add(header);
                    }
                }
            }
        }
        return results;
    }
}
