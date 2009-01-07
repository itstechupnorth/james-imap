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

package org.apache.james.imap.store;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.MessageResult.Content;
import org.apache.james.imap.mailbox.MessageResult.FetchGroup;
import org.apache.james.imap.mailbox.MessageResult.MimePath;
import org.apache.james.imap.mailbox.util.MessageFlags;
import org.apache.james.imap.mailbox.util.MessageResultImpl;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Message;
import org.apache.james.mime4j.MimeException;

public class MessageRowUtils {

    public static final byte[] BYTES_NEW_LINE = { 0x0D, 0x0A };

    public static final byte[] BYTES_HEADER_FIELD_VALUE_SEP = { 0x3A, 0x20 };

    static final Charset US_ASCII = Charset.forName("US-ASCII");

    /**
     * Converts {@link Message} to {@link MessageFlags}.
     * 
     * @param messages not null
     * @return <code>MessageFlags</code>, not null
     */
    public static MessageFlags[] toMessageFlags(final Collection<Message> messages) {
        final MessageFlags[] results = new MessageFlags[messages.size()];
        int i = 0;
        for (Message message: messages) {
            final Flags flags = message.createFlags();
            final long uid = message.getUid();
            results[i++] = new MessageFlags(uid, flags);
        }
        return results;
    }

    public static List<ResultHeader> createHeaders(Message message) {
        final List<Header> headers = getSortedHeaders(message);

        final List<ResultHeader> results = new ArrayList<ResultHeader>(headers.size());
        for (Header header: headers) {
            final ResultHeader resultHeader = new ResultHeader(header);
            results.add(resultHeader);
        }
        return results;
    }

    private static List<Header> getSortedHeaders(Message message) {
        final List<Header> headers = new ArrayList<Header>(message.getHeaders());
        Collections.sort(headers);
        return headers;
    }

    public static Content createBodyContent(Message message) {
        final byte[] bytes = message.getBody();
        final ByteContent result = new ByteContent(bytes);
        return result;
    }

    public static Content createFullContent(final Message message, List headers) {
        if (headers == null) {
            headers = createHeaders(message);
        }
        final byte[] bytes = message.getBody();
        final FullContent results = new FullContent(bytes, headers);
        return results;
    }

    public static MessageResult loadMessageResult(final Message message, final FetchGroup fetchGroup) 
                throws MailboxException {

        MessageResultImpl messageResult = new MessageResultImpl();
        messageResult.setUid(message.getUid());
        if (fetchGroup != null) {
            int content = fetchGroup.content();
            if ((content & FetchGroup.FLAGS) > 0) {
                messageResult.setFlags(message.createFlags());
                content -= FetchGroup.FLAGS;
            }
            if ((content & FetchGroup.SIZE) > 0) {
                messageResult.setSize(message.getSize());
                content -= FetchGroup.SIZE;
            }
            if ((content & FetchGroup.INTERNAL_DATE) > 0) {
                messageResult.setInternalDate(message.getInternalDate());
                content -= FetchGroup.INTERNAL_DATE;
            }
            if ((content & FetchGroup.HEADERS) > 0) {
                addHeaders(message, messageResult);
                content -= FetchGroup.HEADERS;
            }
            if ((content & FetchGroup.BODY_CONTENT) > 0) {
                addBody(message, messageResult);
                content -= FetchGroup.BODY_CONTENT;
            }
            if ((content & FetchGroup.FULL_CONTENT) > 0) {
                addFullContent(message, messageResult);
                content -= FetchGroup.FULL_CONTENT;
            }
            try {
                if ((content & FetchGroup.MIME_DESCRIPTOR) > 0) {
                    addMimeDescriptor(message, messageResult);
                    content -= FetchGroup.MIME_DESCRIPTOR;
                }
                if (content != 0) {
                    throw new MailboxException("Unsupported result: " + content);
                }
            
                addPartContent(fetchGroup, message, messageResult);
            } catch (IOException e) {
                throw new MailboxException(e);
            }
        }
        return messageResult;
    }

    private static void addMimeDescriptor(Message message, MessageResultImpl messageResult) throws IOException {
            MessageResult.MimeDescriptor descriptor = MimeDescriptorImpl
                    .build(toInput(message));
            messageResult.setMimeDescriptor(descriptor);
    }

    private static void addFullContent(final Message messageRow, MessageResultImpl messageResult) 
            throws MailboxException {
        final List headers = messageResult.getHeaders();
        final Content content = createFullContent(messageRow, headers);
        messageResult.setFullContent(content);
    }

    private static void addBody(final Message message,
            MessageResultImpl messageResult) {
        final Content content = createBodyContent(message);
        messageResult.setBody(content);
    }

    private static void addHeaders(final Message message,
            MessageResultImpl messageResult) {
        final List<ResultHeader> headers = createHeaders(message);
        messageResult.setHeaders(headers);
    }

    private static void addPartContent(final FetchGroup fetchGroup,
            Message message, MessageResultImpl messageResult)
            throws MailboxException, IOException,
            MimeException {
        Collection partContent = fetchGroup.getPartContentDescriptors();
        if (partContent != null) {
            for (Iterator it = partContent.iterator(); it.hasNext();) {
                FetchGroup.PartContentDescriptor descriptor = (FetchGroup.PartContentDescriptor) it
                        .next();
                addPartContent(descriptor, message, messageResult);
            }
        }
    }

    private static void addPartContent(
            FetchGroup.PartContentDescriptor descriptor, Message message,
            MessageResultImpl messageResult) throws 
            MailboxException, IOException, MimeException {
        final MimePath mimePath = descriptor.path();
        final int content = descriptor.content();
        if ((content & MessageResult.FetchGroup.FULL_CONTENT) > 0) {
            addFullContent(message, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.BODY_CONTENT) > 0) {
            addBodyContent(message, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.MIME_CONTENT) > 0) {
            addMimeBodyContent(message, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.HEADERS) > 0) {
            addHeaders(message, messageResult, mimePath);
        }
        if ((content & MessageResult.FetchGroup.MIME_HEADERS) > 0) {
            addMimeHeaders(message, messageResult, mimePath);
        }
    }

    private static PartContentBuilder build(int[] path, final Message message)
            throws IOException, MimeException {
        final InputStream stream = toInput(message);
        PartContentBuilder result = new PartContentBuilder();
        result.parse(stream);
        try {
            for (int i = 0; i < path.length; i++) {
                final int next = path[i];
                result.to(next);
            }
        } catch (PartContentBuilder.PartNotFoundException e) {
            // Missing parts should return zero sized content
            // See http://markmail.org/message/2jconrj7scvdi5dj
            result.markEmpty();
        }
        return result;
    }

    public static InputStream toInput(final Message message) {
        final List<Header> headers = getSortedHeaders(message);
        final StringBuffer headersToString = new StringBuffer(headers.size() * 50);
        for (Header header: headers) {
            headersToString.append(header.getField());
            headersToString.append(": ");
            headersToString.append(header.getValue());
            headersToString.append("\r\n");
        }
        headersToString.append("\r\n");

        byte[] bodyContent = message.getBody();
        final MessageInputStream stream = new MessageInputStream(headersToString, bodyContent);
        return stream;
    }

    private static final class MessageInputStream extends InputStream {
        private final StringBuffer headers;

        private final ByteBuffer bodyContent;

        private int headerPosition = 0;

        public MessageInputStream(final StringBuffer headers,
                final byte[] bodyContent) {
            super();
            this.headers = headers;
            this.bodyContent = ByteBuffer.wrap(bodyContent);
        }

        public int read() throws IOException {
            final int result;
            if (headerPosition < headers.length()) {
                result = headers.charAt(headerPosition++);
            } else if (bodyContent.hasRemaining()) {
                result = bodyContent.get();
            } else {
                result = -1;
            }
            return result;
        }

    }

    private static final int[] path(MimePath mimePath) {
        final int[] result;
        if (mimePath == null) {
            result = null;
        } else {
            result = mimePath.getPositions();
        }
        return result;
    }

    private static void addHeaders(Message message,
            MessageResultImpl messageResult, MimePath mimePath)
            throws IOException, MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addHeaders(message, messageResult);
        } else {
            final PartContentBuilder builder = build(path, message);
            final List headers = builder.getMessageHeaders();
            messageResult.setHeaders(mimePath, headers.iterator());
        }
    }

    private static void addMimeHeaders(Message message,
            MessageResultImpl messageResult, MimePath mimePath)
            throws IOException, MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addHeaders(message, messageResult);
        } else {
            final PartContentBuilder builder = build(path, message);
            final List headers = builder.getMimeHeaders();
            messageResult.setMimeHeaders(mimePath, headers.iterator());
        }
    }

    private static void addBodyContent(Message message,
            MessageResultImpl messageResult, MimePath mimePath) throws IOException, MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addBody(message, messageResult);
        } else {
            final PartContentBuilder builder = build(path, message);
            final Content content = builder.getMessageBodyContent();
            messageResult.setBodyContent(mimePath, content);
        }
    }

    private static void addMimeBodyContent(Message message,
            MessageResultImpl messageResult, MimePath mimePath)
            throws IOException, MimeException {
        final int[] path = path(mimePath);
        final PartContentBuilder builder = build(path, message);
        final Content content = builder.getMimeBodyContent();
        messageResult.setMimeBodyContent(mimePath, content);
    }

    private static void addFullContent(Message message,
            MessageResultImpl messageResult, MimePath mimePath)
            throws MailboxException, IOException,
            MimeException {
        final int[] path = path(mimePath);
        if (path == null) {
            addFullContent(message, messageResult);
        } else {
            final PartContentBuilder builder = build(path, message);
            final Content content = builder.getFullContent();
            messageResult.setFullContent(mimePath, content);
        }
    }

    /**
     * Gets a comparator that evaluates {@link Message}'s on the basis of
     * their UIDs.
     * 
     * @return {@link Comparator}, not null
     */
    public static Comparator<Message> getUidComparator() {
        return UidComparator.INSTANCE;
    }

    private static final class UidComparator implements Comparator<Message> {
        private static final UidComparator INSTANCE = new UidComparator();

        public int compare(Message one, Message two) {
            final int result = (int) (one.getUid() - two.getUid());
            return result;
        }

    }
}
