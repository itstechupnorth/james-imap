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
package org.apache.james.imap.decode.parser;

import java.util.List;

import org.apache.james.imap.api.DecodingException;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapMessageCallback;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.BodyFetchElement;
import org.apache.james.imap.api.message.FetchData;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.decode.FetchPartPathDecoder;
import org.apache.james.imap.decode.ImapRequestLine;
import org.apache.james.imap.message.request.FetchRequest;

/**
 * Parse FETCH commands
 *
 */
public class FetchCommandParser extends AbstractUidCommandParser {

    public FetchCommandParser() {
        super(ImapCommand.selectedStateCommand(ImapConstants.FETCH_COMMAND_NAME));
    }

    /**
     * Create a {@link FetchData} by reading from the {@link ImapRequestLine}
     * 
     * @param request
     * @return fetchData
     * @throws DecodingException
     */
    protected FetchData fetchRequest(ImapRequestLine request)
            throws DecodingException {
        FetchData fetch = new FetchData();

        char next = nextNonSpaceChar(request);
        if (request.nextChar() == '(') {
            consumeChar(request, '(');

            next = nextNonSpaceChar(request);
            while (next != ')') {
                addNextElement(request, fetch);
                next = nextNonSpaceChar(request);
            }
            consumeChar(request, ')');
        } else {
            addNextElement(request, fetch);

        }

        return fetch;
    }

    private void addNextElement(ImapRequestLine reader, FetchData fetch)
            throws DecodingException {
        // String name = element.toString();
        String name = readWord(reader, " [)\r\n");
        char next = reader.nextChar();
        // Simple elements with no '[]' parameters.
        if (next != '[') {
            if ("FAST".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
                fetch.setInternalDate(true);
                fetch.setSize(true);
            } else if ("FULL".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
                fetch.setInternalDate(true);
                fetch.setSize(true);
                fetch.setEnvelope(true);
                fetch.setBody(true);
            } else if ("ALL".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
                fetch.setInternalDate(true);
                fetch.setSize(true);
                fetch.setEnvelope(true);
            } else if ("FLAGS".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
            } else if ("RFC822.SIZE".equalsIgnoreCase(name)) {
                fetch.setSize(true);
            } else if ("ENVELOPE".equalsIgnoreCase(name)) {
                fetch.setEnvelope(true);
            } else if ("INTERNALDATE".equalsIgnoreCase(name)) {
                fetch.setInternalDate(true);
            } else if ("BODY".equalsIgnoreCase(name)) {
                fetch.setBody(true);
            } else if ("BODYSTRUCTURE".equalsIgnoreCase(name)) {
                fetch.setBodyStructure(true);
            } else if ("UID".equalsIgnoreCase(name)) {
                fetch.setUid(true);
            } else if ("RFC822".equalsIgnoreCase(name)) {
                fetch.add(BodyFetchElement.createRFC822(), false);
            } else if ("RFC822.HEADER".equalsIgnoreCase(name)) {
                fetch.add(BodyFetchElement.createRFC822Header(), true);
            } else if ("RFC822.TEXT".equalsIgnoreCase(name)) {
                fetch.add(BodyFetchElement.createRFC822Text(), false);
            } else {
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Invalid fetch attribute: " + name);
            }
        } else {
            consumeChar(reader, '[');

            String parameter = readWord(reader, "]");

            consumeChar(reader, ']');

            final Long firstOctet;
            final Long numberOfOctets;
            if (reader.nextChar() == '<') {
                consumeChar(reader, '<');
                firstOctet = new Long(number(reader));
                if (reader.nextChar() == '.') {
                    consumeChar(reader, '.');
                    numberOfOctets = new Long(nzNumber(reader));
                } else {
                    numberOfOctets = null;
                }
                consumeChar(reader, '>');
            } else {
                firstOctet = null;
                numberOfOctets = null;
            }

            final BodyFetchElement bodyFetchElement = createBodyElement(
                    parameter, firstOctet, numberOfOctets);
            final boolean isPeek = isPeek(name);
            fetch.add(bodyFetchElement, isPeek);
        }
    }

    private boolean isPeek(String name) throws DecodingException {
        final boolean isPeek;
        if ("BODY".equalsIgnoreCase(name)) {
            isPeek = false;
        } else if ("BODY.PEEK".equalsIgnoreCase(name)) {
            isPeek = true;
        } else {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, 
                    "Invalid fetch attibute: " + name + "[]");
        }
        return isPeek;
    }

    private BodyFetchElement createBodyElement(String parameter,
            Long firstOctet, Long numberOfOctets) throws DecodingException {
        final String responseName = "BODY[" + parameter + "]";
        FetchPartPathDecoder decoder = new FetchPartPathDecoder();
        decoder.decode(parameter);
        final int sectionType = getSectionType(decoder);

        final List<String> names = decoder.getNames();
        final int[] path = decoder.getPath();
        final BodyFetchElement bodyFetchElement = new BodyFetchElement(
                responseName, sectionType, path, names, firstOctet,
                numberOfOctets);
        return bodyFetchElement;
    }

    private int getSectionType(FetchPartPathDecoder decoder)
            throws DecodingException {
        final int specifier = decoder.getSpecifier();
        final int sectionType;
        switch (specifier) {
            case FetchPartPathDecoder.CONTENT:
                sectionType = BodyFetchElement.CONTENT;
                break;
            case FetchPartPathDecoder.HEADER:
                sectionType = BodyFetchElement.HEADER;
                break;
            case FetchPartPathDecoder.HEADER_FIELDS:
                sectionType = BodyFetchElement.HEADER_FIELDS;
                break;
            case FetchPartPathDecoder.HEADER_NOT_FIELDS:
                sectionType = BodyFetchElement.HEADER_NOT_FIELDS;
                break;
            case FetchPartPathDecoder.MIME:
                sectionType = BodyFetchElement.MIME;
                break;
            case FetchPartPathDecoder.TEXT:
                sectionType = BodyFetchElement.TEXT;
                break;
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Section type is unsupported.");
        }
        return sectionType;
    }

    private String readWord(ImapRequestLine request, String terminator)
            throws DecodingException {
        StringBuffer buf = new StringBuffer();
        char next = request.nextChar();
        while (terminator.indexOf(next) == -1) {
            buf.append(next);
            request.consume();
            next = request.nextChar();
        }
        return buf.toString();
    }

    private char nextNonSpaceChar(ImapRequestLine request)
            throws DecodingException {
        char next = request.nextChar();
        while (next == ' ') {
            request.consume();
            next = request.nextChar();
        }
        return next;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.parser.AbstractUidCommandParser#decode(org.apache.james.imap.api.ImapCommand, org.apache.james.imap.decode.ImapRequestLineReader, java.lang.String, boolean, org.apache.james.imap.api.process.ImapSession)
     */
    protected void decode(ImapCommand command, ImapRequestLine request, String tag, boolean useUids, ImapSession session, ImapMessageCallback callback) {
        try {
            IdRange[] idSet = parseIdRange(request);
            FetchData fetch = fetchRequest(request);
            endLine(request);

            final ImapMessage result = new FetchRequest(command, useUids, idSet, fetch, tag);
            callback.onMessage(result);
        } catch (DecodingException ex) {
            callback.onException(ex);
        }
    }

}
