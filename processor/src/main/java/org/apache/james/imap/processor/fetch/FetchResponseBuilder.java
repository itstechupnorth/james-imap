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

/**
 * 
 */
package org.apache.james.imap.processor.fetch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.message.BodyFetchElement;
import org.apache.james.imap.api.message.FetchData;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.response.FetchResponse;
import org.apache.james.imap.processor.base.MessageRangeException;
import org.apache.james.mailbox.Content;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MimeDescriptor;
import org.apache.james.mime4j.field.address.parser.ParseException;

final class FetchResponseBuilder {

    private final EnvelopeBuilder envelopeBuilder;

    private int msn;

    private Long uid;

    private Flags flags;

    private Date internalDate;

    private Long size;

    private List<FetchResponse.BodyElement> elements;

    private FetchResponse.Envelope envelope;

    private FetchResponse.Structure body;

    private FetchResponse.Structure bodystructure;

    public FetchResponseBuilder(final EnvelopeBuilder envelopeBuilder) {
        super();
        this.envelopeBuilder = envelopeBuilder;
    }

    public void reset(int msn) {
        this.msn = msn;
        uid = null;
        flags = null;
        internalDate = null;
        size = null;
        body = null;
        bodystructure = null;
        elements = null;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public void setFlags(Flags flags) {
        this.flags = flags;
    }

    public FetchResponse build() {
        final FetchResponse result = new FetchResponse(msn, flags, uid,
                internalDate, size, envelope, body, bodystructure, elements);
        return result;
    }

    public FetchResponse build(FetchData fetch, MessageResult result, MessageManager mailbox, 
            ImapSession session, boolean useUids) throws MessageRangeException,
            ParseException, MailboxException  {
        final SelectedMailbox selected = session.getSelected();
        final long resultUid = result.getUid();
        final int resultMsn = selected.msn(resultUid);
        
        if (resultMsn == SelectedMailbox.NO_SUCH_MESSAGE) throw new MessageRangeException("No such message found with uid " + resultUid);
        
        setMsn(resultMsn);

        // Check if this fetch will cause the "SEEN" flag to be set on this
        // message
        // If so, update the flags, and ensure that a flags response is included
        // in the response.
        final MailboxSession mailboxSession = ImapSessionUtils
                .getMailboxSession(session);
        boolean ensureFlagsResponse = false;
        final Flags resultFlags = result.getFlags();
        if (fetch.isSetSeen() && !resultFlags.contains(Flags.Flag.SEEN)) {
            mailbox.setFlags(new Flags(Flags.Flag.SEEN), true, false,
                    MessageRange.one(resultUid), mailboxSession);
            resultFlags.add(Flags.Flag.SEEN);
            ensureFlagsResponse = true;
        }

        // FLAGS response
        if (fetch.isFlags() || ensureFlagsResponse) {
            if (selected.isRecent(resultUid)) {
                resultFlags.add(Flags.Flag.RECENT);
            }
            setFlags(resultFlags);
        }

        // INTERNALDATE response
        if (fetch.isInternalDate()) {
            setInternalDate(result.getInternalDate());
        }

        // RFC822.SIZE response
        if (fetch.isSize()) {
            setSize(result.getSize());
        }

        if (fetch.isEnvelope()) {
            this.envelope = buildEnvelope(result);
        }

        // Only create when needed
        if (fetch.isBody() || fetch.isBodyStructure()) {
            final MimeDescriptor descriptor = result
                    .getMimeDescriptor();

            // BODY response
            if (fetch.isBody()) {
                body = new MimeDescriptorStructure(false, descriptor,
                        envelopeBuilder);
            }

            // BODYSTRUCTURE response
            if (fetch.isBodyStructure()) {
                bodystructure = new MimeDescriptorStructure(true, descriptor,
                        envelopeBuilder);
            }
        }
        // UID response
        if (fetch.isUid()) {
            setUid(resultUid);
        }

        // BODY part responses.
        Collection<BodyFetchElement> elements = fetch.getBodyElements();
        this.elements = new ArrayList<FetchResponse.BodyElement>();
        for (Iterator<BodyFetchElement> iterator = elements.iterator(); iterator.hasNext();) {
            BodyFetchElement fetchElement = iterator.next();
            final FetchResponse.BodyElement element = bodyFetch(result,
                    fetchElement);
            if (element != null) {
                this.elements.add(element);
            }
        }
        return build();
    }

    private FetchResponse.Envelope buildEnvelope(final MessageResult result)
            throws MailboxException, ParseException {
        return envelopeBuilder.buildEnvelope(result);
    }

    private void setSize(long size) {
        this.size = size;
    }

    public void setInternalDate(Date internalDate) {
        this.internalDate = internalDate;
    }

    private void setMsn(int msn) {
        reset(msn);
    }

    private FetchResponse.BodyElement bodyFetch(
            final MessageResult messageResult, BodyFetchElement fetchElement)
            throws MailboxException {

        final Long firstOctet = fetchElement.getFirstOctet();
        final Long numberOfOctets = fetchElement.getNumberOfOctets();
        final String name = fetchElement.getResponseName();
        final int specifier = fetchElement.getSectionType();
        final int[] path = fetchElement.getPath();
        final Collection<String> names = fetchElement.getFieldNames();
        final boolean isBase = (path == null || path.length == 0);
        final FetchResponse.BodyElement fullResult = bodyContent(messageResult,
                name, specifier, path, names, isBase);
        final FetchResponse.BodyElement result = wrapIfPartialFetch(firstOctet,
                numberOfOctets, fullResult);
        return result;

    }

    private FetchResponse.BodyElement bodyContent(
            final MessageResult messageResult, final String name,
            final int specifier, final int[] path, final Collection<String> names,
            final boolean isBase) throws MailboxException {
        final FetchResponse.BodyElement fullResult;
        switch (specifier) {
            case BodyFetchElement.CONTENT:
                fullResult = content(messageResult, name, path, isBase);
                break;

            case BodyFetchElement.HEADER_FIELDS:
                fullResult = fields(messageResult, name, path, names, isBase);
                break;

            case BodyFetchElement.HEADER_NOT_FIELDS:
                fullResult = fieldsNot(messageResult, name, path, names, isBase);
                break;

            case BodyFetchElement.MIME:
                fullResult = mimeHeaders(messageResult, name, path, isBase);
                break;
            case BodyFetchElement.HEADER:
                fullResult = headers(messageResult, name, path, isBase);
                break;

            case BodyFetchElement.TEXT:
                fullResult = text(messageResult, name, path, isBase);
                break;

            default:
                fullResult = null;
                break;
        }
        return fullResult;
    }

    private FetchResponse.BodyElement wrapIfPartialFetch(final Long firstOctet,
            final Long numberOfOctets,
            final FetchResponse.BodyElement fullResult) {
        final FetchResponse.BodyElement result;
        if (firstOctet == null) {
            result = fullResult;
        } else {
            final long numberOfOctetsAsLong;
            if (numberOfOctets == null) {
                numberOfOctetsAsLong = Long.MAX_VALUE;
            } else {
                numberOfOctetsAsLong = numberOfOctets.longValue();
            }
            final long firstOctetAsLong = firstOctet.longValue();
            result = new PartialFetchBodyElement(fullResult, firstOctetAsLong,
                    numberOfOctetsAsLong);
        }
        return result;
    }

    private FetchResponse.BodyElement text(final MessageResult messageResult,
            String name, final int[] path, final boolean isBase)
            throws MailboxException {
        final FetchResponse.BodyElement result;
        final Content body;
        if (isBase) {
            body = messageResult.getBody();
        } else {
            MessageResult.MimePath mimePath = new MimePathImpl(path);
            body = messageResult.getBody(mimePath);
        }
        result = new ContentBodyElement(name, body);
        return result;
    }

    private FetchResponse.BodyElement mimeHeaders(
            final MessageResult messageResult, String name, final int[] path,
            final boolean isBase) throws MailboxException {
        final FetchResponse.BodyElement result;
        final Iterator<MessageResult.Header> headers = getMimeHeaders(messageResult, path, isBase);
        List<MessageResult.Header> lines = MessageResultUtils.getAll(headers);
        result = new HeaderBodyElement(name, lines);
        return result;
    }

    private FetchResponse.BodyElement headers(
            final MessageResult messageResult, String name, final int[] path,
            final boolean isBase) throws MailboxException {
        final FetchResponse.BodyElement result;
        final Iterator<MessageResult.Header> headers = getHeaders(messageResult, path, isBase);
        List<MessageResult.Header> lines = MessageResultUtils.getAll(headers);
        result = new HeaderBodyElement(name, lines);
        return result;
    }

    private FetchResponse.BodyElement fieldsNot(
            final MessageResult messageResult, String name, final int[] path,
            Collection<String> names, final boolean isBase) throws MailboxException {
        final FetchResponse.BodyElement result;
        final Iterator<MessageResult.Header> headers = getHeaders(messageResult, path, isBase);
        List<MessageResult.Header> lines = MessageResultUtils.getNotMatching(names, headers);
        result = new HeaderBodyElement(name, lines);
        return result;
    }

    private FetchResponse.BodyElement fields(final MessageResult messageResult,
            String name, final int[] path, Collection<String> names,
            final boolean isBase) throws MailboxException {
        final FetchResponse.BodyElement result;
        final Iterator<MessageResult.Header> headers = getHeaders(messageResult, path, isBase);
        List<MessageResult.Header> lines = MessageResultUtils.getMatching(names, headers);
        result = new HeaderBodyElement(name, lines);
        return result;
    }

    private Iterator<MessageResult.Header> getHeaders(final MessageResult messageResult,
            final int[] path, final boolean isBase)
            throws MailboxException {
        final Iterator<MessageResult.Header> headers;
        if (isBase) {
            headers = messageResult.headers();
        } else {
            MessageResult.MimePath mimePath = new MimePathImpl(path);
            headers = messageResult.iterateHeaders(mimePath);
        }
        return headers;
    }

    private Iterator<MessageResult.Header> getMimeHeaders(final MessageResult messageResult,
            final int[] path, final boolean isBase) throws MailboxException {
        MessageResult.MimePath mimePath = new MimePathImpl(path);
        final Iterator<MessageResult.Header> headers = messageResult.iterateMimeHeaders(mimePath);
        return headers;
    }

    private FetchResponse.BodyElement content(
            final MessageResult messageResult, String name, final int[] path,
            final boolean isBase) throws MailboxException {
        final FetchResponse.BodyElement result;
        final Content full;
        if (isBase) {
            full = messageResult.getFullContent();
        } else {
            MessageResult.MimePath mimePath = new MimePathImpl(path);
            full = messageResult.getMimeBody(mimePath);
        }
        result = new ContentBodyElement(name, full);
        return result;
    }
}