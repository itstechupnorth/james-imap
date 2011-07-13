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

package org.apache.james.imap.encode.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.encode.ImapResponseWriter;
import org.apache.james.imap.message.response.Literal;

/**
 * Class providing methods to send response messages from the server to the
 * client.
 */
public class ImapResponseComposerImpl implements ImapConstants, ImapResponseComposer {

    public static final String ENVELOPE = "ENVELOPE";

    public static final String FETCH = "FETCH";

    public static final String EXPUNGE = "EXPUNGE";

    public static final String RECENT = "RECENT";

    public static final String EXISTS = "EXISTS";

    public static final String FLAGS = "FLAGS";

    public static final String FAILED = "failed.";

    private final ImapResponseWriter writer;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private static final int LOWER_CASE_OFFSET = 'a' - 'A';

    private final Charset usAscii;

    private boolean skipNextSpace;

    public ImapResponseComposerImpl(final ImapResponseWriter writer) {
        skipNextSpace = false;
        usAscii = Charset.forName("US-ASCII");
        this.writer = writer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#untaggedNoResponse(
     * java.lang.String, java.lang.String)
     */
    public ImapResponseComposer untaggedNoResponse(String displayMessage, String responseCode) throws IOException {
        untagged();
        message(NO);
        responseCode(responseCode);
        message(displayMessage);
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#continuationResponse
     * (java.lang.String)
     */
    public ImapResponseComposer continuationResponse(String message) throws IOException {
        writeASCII(CONTINUATION + SP + message);
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#flagsResponse(javax
     * .mail.Flags)
     */
    public ImapResponseComposer flagsResponse(Flags flags) throws IOException {
        untagged();
        flags(flags);
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#existsResponse(long)
     */
    public ImapResponseComposer existsResponse(long count) throws IOException {
        untagged();
        message(count);
        message(EXISTS);
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#recentResponse(long)
     */
    public ImapResponseComposer recentResponse(long count) throws IOException {
        untagged();
        message(count);
        message(RECENT);
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#expungeResponse(long)
     */
    public ImapResponseComposer expungeResponse(long msn) throws IOException {
        untagged();
        message(msn);
        message(EXPUNGE);
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#commandResponse(org
     * .apache.james.imap.api.ImapCommand, java.lang.String)
     */
    public ImapResponseComposer commandResponse(ImapCommand command, String message) throws IOException {
        untagged();
        commandName(command.getName());
        message(message);
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#taggedResponse(java
     * .lang.String, java.lang.String)
     */
    public ImapResponseComposer taggedResponse(String message, String tag) throws IOException {
        tag(tag);
        message(message);
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#untaggedResponse(java
     * .lang.String)
     */
    public ImapResponseComposer untaggedResponse(String message) throws IOException {
        untagged();
        message(message);
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#byeResponse(java.lang
     * .String)
     */
    public ImapResponseComposer byeResponse(String message) throws IOException {
        untaggedResponse(BYE + SP + message);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#hello(java.lang.String)
     */
    public ImapResponseComposer hello(String message) throws IOException {
        untaggedResponse(OK + SP + message);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.encode.ImapResponseComposer#untagged()
     */
    public ImapResponseComposer untagged() throws IOException {
        writeASCII(UNTAGGED);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#message(java.lang.String
     * )
     */
    public ImapResponseComposer message(final String message) throws IOException {
        if (message != null) {
            // TODO: consider message normalisation
            // TODO: CR/NFs in message must be replaced
            // TODO: probably best done in the writer
            space();
            writeASCII(message);

        }
        return this;
    }

    private void responseCode(final String responseCode) throws IOException {
        if (responseCode != null && !"".equals(responseCode)) {
            writeASCII(" [");
            writeASCII(responseCode);
            buffer.write(BYTE_CLOSE_SQUARE_BRACKET);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.encode.ImapResponseComposer#end()
     */
    public ImapResponseComposer end() throws IOException {
        buffer.write(LINE_END.getBytes());
        writer.write(ByteBuffer.wrap(buffer.toByteArray()));
        buffer.reset();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#tag(java.lang.String)
     */
    public ImapResponseComposer tag(String tag) throws IOException {
        writeASCII(tag);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#statusResponse(java
     * .lang.String, org.apache.james.imap.api.ImapCommand, java.lang.String,
     * java.lang.String, java.util.Collection, boolean, long, java.lang.String)
     */
    public ImapResponseComposer statusResponse(String tag, ImapCommand command, String type, String responseCode, Collection<String> parameters, boolean useParens, long number, String text) throws IOException {
        if (tag == null) {
            untagged();
        } else {
            tag(tag);
        }
        message(type);
        if (responseCode != null) {
            openSquareBracket();
            message(responseCode);
            if (number > 0) {
                message(number);
            }
            if (parameters != null && !parameters.isEmpty()) {
                if (useParens)
                    openParen();
                for (Iterator<String> it = parameters.iterator(); it.hasNext();) {
                    final String parameter = it.next();
                    message(parameter);
                }
                if (useParens)
                    closeParen();
            }
            closeSquareBracket();
        }
        if (command != null) {
            commandName(command.getName());
        }
        if (text != null && !"".equals(text)) {
            message(text);
        }
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#statusResponse(java
     * .lang.Long, java.lang.Long, java.lang.Long, java.lang.Long,
     * java.lang.Long, java.lang.String)
     */
    public ImapResponseComposer statusResponse(Long messages, Long recent, Long uidNext, Long uidValidity, Long unseen, String mailboxName) throws IOException {
        return statusResponse(messages, recent, uidNext, null, uidValidity, unseen, mailboxName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#listResponse(java.lang
     * .String, java.util.List, java.lang.String, java.lang.String)
     */
    public ImapResponseComposer listResponse(String typeName, List<String> attributes, char hierarchyDelimiter, String name) throws IOException {
        untagged();
        message(typeName);
        openParen();
        if (attributes != null) {
            for (Iterator<String> it = attributes.iterator(); it.hasNext();) {
                final String attribute = it.next();
                message(attribute);
            }
        }
        closeParen();

        if (hierarchyDelimiter == Character.UNASSIGNED) {
            message(NIL);
        } else {
            quote(Character.toString(hierarchyDelimiter));
        }
        quote(name);

        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.encode.ImapResponseComposer#closeParen()
     */
    public ImapResponseComposer closeParen() throws IOException {
        closeBracket(BYTE_CLOSING_PARENTHESIS);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.encode.ImapResponseComposer#openParen()
     */
    public ImapResponseComposer openParen() throws IOException {
        openBracket(BYTE_OPENING_PARENTHESIS);
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#searchResponse(long[], java.lang.Long)
     */
    public ImapResponseComposer searchResponse(long[] ids, Long highestModSeq) throws IOException {
        untagged();
        message(ImapConstants.SEARCH_RESPONSE_NAME);
        message(ids);
        
        // add MODSEQ
        if (highestModSeq != null) {
            openParen();
            message("MODSEQ");
            message(highestModSeq);
            closeParen();
        }
        end();
        return this;
    }

    private void message(long[] ids) throws IOException {
        if (ids != null) {
            final int length = ids.length;
            for (int i = 0; i < length; i++) {
                final long id = ids[i];
                message(id);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#flags(javax.mail.Flags)
     */
    public ImapResponseComposer flags(Flags flags) throws IOException {
        message(FLAGS);
        openParen();
        if (flags.contains(Flags.Flag.ANSWERED)) {
            message("\\Answered");
        }
        if (flags.contains(Flags.Flag.DELETED)) {
            message("\\Deleted");
        }
        if (flags.contains(Flags.Flag.DRAFT)) {
            message("\\Draft");
        }
        if (flags.contains(Flags.Flag.FLAGGED)) {
            message("\\Flagged");
        }
        if (flags.contains(Flags.Flag.RECENT)) {
            message("\\Recent");
        }
        if (flags.contains(Flags.Flag.SEEN)) {
            message("\\Seen");
        }
        
        String[] userFlags = flags.getUserFlags();
        for (int i = 0; i < userFlags.length; i++) {
            message(userFlags[i]);
        }
        closeParen();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#closeFetchResponse()
     */
    public ImapResponseComposer closeFetchResponse() throws IOException {
        closeParen();
        end();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#openFetchResponse(long)
     */
    public ImapResponseComposer openFetchResponse(long msn) throws IOException {
        untagged();
        message(msn);
        message(FETCH);
        openParen();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#address(java.lang.String
     * , java.lang.String, java.lang.String, java.lang.String)
     */
    public ImapResponseComposer address(String name, String domainList, String mailbox, String host) throws IOException {
        skipNextSpace();
        openParen();
        nillableQuote(name);
        nillableQuote(domainList);
        nillableQuote(mailbox);
        nillableQuote(host);
        closeParen();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.encode.ImapResponseComposer#endAddresses()
     */
    public ImapResponseComposer endAddresses() throws IOException {
        closeParen();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#endEnvelope(java.lang
     * .String, java.lang.String)
     */
    public ImapResponseComposer endEnvelope(String inReplyTo, String messageId) throws IOException {
        nillableQuote(inReplyTo);
        nillableQuote(messageId);
        closeParen();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.encode.ImapResponseComposer#nil()
     */
    public ImapResponseComposer nil() throws IOException {
        message(NIL);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.encode.ImapResponseComposer#startAddresses()
     */
    public ImapResponseComposer startAddresses() throws IOException {
        openParen();
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#startEnvelope(java.
     * lang.String, java.lang.String, boolean)
     */
    public ImapResponseComposer startEnvelope(String date, String subject, boolean prefixWithName) throws IOException {
        if (prefixWithName) {
            message(ENVELOPE);
        }
        openParen();
        nillableQuote(date);
        nillableQuote(subject);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#nillableQuote(java.
     * lang.String)
     */
    public ImapResponseComposer nillableQuote(String message) throws IOException {
        if (message == null) {
            nil();
        } else {
            quote(message);
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#nillableComposition
     * (java.lang.String, java.util.List)
     */
    public ImapResponseComposer nillableComposition(String masterQuote, List<String> quotes) throws IOException {
        if (masterQuote == null) {
            nil();
        } else {
            openParen();
            quote(masterQuote);
            nillableQuotes(quotes);
            closeParen();
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#nillableQuotes(java
     * .util.List)
     */
    public ImapResponseComposer nillableQuotes(List<String> quotes) throws IOException {
        if (quotes == null || quotes.size() == 0) {
            nil();
        } else {
            openParen();
            for (final String string : quotes) {
                nillableQuote(string);
            }
            closeParen();
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#upperCaseAscii(java
     * .lang.String)
     */
    public ImapResponseComposer upperCaseAscii(String message) throws IOException {
        if (message == null) {
            nil();
        } else {
            upperCaseAscii(message, false);
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#quoteUpperCaseAscii
     * (java.lang.String)
     */
    public ImapResponseComposer quoteUpperCaseAscii(String message) throws IOException {
        if (message == null) {
            nil();
        } else {
            upperCaseAscii(message, true);
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#capabilities(java.util
     * .List)
     */
    public ImapResponseComposer capabilities(List<String> capabilities) throws IOException {
        untagged();
        message(CAPABILITY_COMMAND_NAME);
        for (String capability : capabilities) {
            message(capability);
        }
        end();
        return this;
    }

    private void writeASCII(final String string) throws IOException {
        buffer.write(string.getBytes(usAscii));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.encode.ImapResponseComposer#message(long)
     */
    public ImapResponseComposer message(long number) throws IOException {
        space();
        writeASCII(Long.toString(number));
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#commandName(java.lang
     * .String)
     */
    public ImapResponseComposer commandName(String commandName) throws IOException {
        space();
        writeASCII(commandName);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#quote(java.lang.String)
     */
    public ImapResponseComposer quote(String message) throws IOException {
        space();
        final int length = message.length();
       
        buffer.write(BYTE_DQUOTE);
        for (int i = 0; i < length; i++) {
            char character = message.charAt(i);
            if (character == ImapConstants.BACK_SLASH || character == DQUOTE) {
                buffer.write(BYTE_BACK_SLASH);
            }
            // 7-bit ASCII only
            if (character > 128) {
                buffer.write(BYTE_QUESTION);
            } else {
                buffer.write((byte) character);
            }
        }
        buffer.write(BYTE_DQUOTE);
        return this;
    }


    private void closeBracket(final byte bracket) throws IOException {
        buffer.write(bracket);
        clearSkipNextSpace();
    }

    private void openBracket(final byte bracket) throws IOException {
        space();
        buffer.write(bracket);
        skipNextSpace();
    }

    private void clearSkipNextSpace() {
        skipNextSpace = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.encode.ImapResponseComposer#skipNextSpace()
     */
    public ImapResponseComposer skipNextSpace() {
        skipNextSpace = true;
        return this;
    }

    private void space() throws IOException {
        if (skipNextSpace) {
            skipNextSpace = false;
        } else {
            buffer.write(SP.getBytes());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.encode.ImapResponseComposer#literal(org.apache.
     * james.imap.message.response.Literal)
     */
    public ImapResponseComposer literal(Literal literal) throws IOException {
        space();
        buffer.write(BYTE_OPEN_BRACE);
        final long size = literal.size();
        writeASCII(Long.toString(size));
        buffer.write(BYTE_CLOSE_BRACE);
        end();
        if (size > 0) {
            writer.write(literal);
        }
        return this;
    }

    private void closeSquareBracket() throws IOException {
        closeBracket(BYTE_CLOSE_SQUARE_BRACKET);
    }

    private void openSquareBracket() throws IOException {
        openBracket(BYTE_OPEN_SQUARE_BRACKET);
    }

    private void upperCaseAscii(String message, boolean quote) throws IOException {
        space();
        final int length = message.length();
        if (quote) {
            buffer.write(BYTE_DQUOTE);
        }
        for (int i = 0; i < length; i++) {
            final char next = message.charAt(i);
            if (next >= 'a' && next <= 'z') {
                buffer.write((byte) (next - LOWER_CASE_OFFSET));
            } else {
                buffer.write((byte) (next));
            }
        }
        if (quote) {
            buffer.write(BYTE_DQUOTE);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#sequenceSet(org.apache.james.imap.api.message.IdRange[])
     */
    public ImapResponseComposer sequenceSet(IdRange[] ranges) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i< ranges.length; i++) {
            IdRange range = ranges[i];
            sb.append(range.getFormattedString());
            if (i + 1 < ranges.length) {
                sb.append(",");
            }
        }
        return message(sb.toString());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#searchResponse(long[])
     */
    public ImapResponseComposer searchResponse(long[] ids) throws IOException {
        return searchResponse(ids, null);
    }

    @Override
    public ImapResponseComposer statusResponse(Long messages, Long recent, Long uidNext, Long highestModSeq, Long uidValidity, Long unseen, String mailboxName) throws IOException {
        untagged();
        message(STATUS_COMMAND_NAME);
        quote(mailboxName);
        openParen();

        if (messages != null) {
            message(STATUS_MESSAGES);
            final long messagesValue = messages.longValue();
            message(messagesValue);
        }

        if (recent != null) {
            message(STATUS_RECENT);
            final long recentValue = recent.longValue();
            message(recentValue);
        }

        if (uidNext != null) {
            message(STATUS_UIDNEXT);
            final long uidNextValue = uidNext.longValue();
            message(uidNextValue);
        }
        
        if (highestModSeq != null) {
            message(STATUS_HIGHESTMODSEQ);
            message(highestModSeq);
        }

        if (uidValidity != null) {
            message(STATUS_UIDVALIDITY);
            final long uidValidityValue = uidValidity.longValue();
            message(uidValidityValue);
        }

        if (unseen != null) {
            message(STATUS_UNSEEN);
            final long unseenValue = unseen.longValue();
            message(unseenValue);
        }

        closeParen();
        end();
        return this;
    }

}
