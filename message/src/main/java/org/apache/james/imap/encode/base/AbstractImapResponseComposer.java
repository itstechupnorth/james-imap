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

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.message.response.ImapResponseComposer;
import org.apache.james.imap.api.message.response.Literal;

/**
 * Class providing methods to send response messages from the server to the
 * client.
 */
public abstract class AbstractImapResponseComposer implements ImapConstants, ImapResponseComposer {

    public static final String ENVELOPE = "ENVELOPE";

    public static final String FETCH = "FETCH";

    public static final String EXPUNGE = "EXPUNGE";

    public static final String RECENT = "RECENT";

    public static final String EXISTS = "EXISTS";

    public static final String FLAGS = "FLAGS";

    public static final String FAILED = "failed.";

    private static final int LOWER_CASE_OFFSET = 'a' - 'A';

    private static final int DEFAULT_BUFFER_SIZE = 128;

    private final Charset usAscii;

    private final ByteBuffer buffer;

    private boolean skipNextSpace;

    public AbstractImapResponseComposer() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public AbstractImapResponseComposer(final int bufferSize) {
        skipNextSpace = false;
        buffer = ByteBuffer.allocate(bufferSize);
        usAscii = Charset.forName("US-ASCII");
    }


    /**
     * Write the {@link ByteBuffer} to the client
     * 
     * @param buffer
     * @throws IOException
     */
    protected abstract void write(final ByteBuffer buffer) throws IOException ;

    /**
     * Write the {@link Literal} to the client
     * 
     * @param literal
     * @throws IOException
     */
    protected abstract void writeLiteral(final Literal literal) throws IOException ;

    
    private void writeASCII(final String string) throws IOException {
        final ByteBuffer buffer = usAscii.encode(string);
        write(buffer);
    }

    
    private void write(byte[] bytes) throws IOException {
        final ByteBuffer wrap = ByteBuffer.wrap(bytes);
        write(wrap);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#untagged()
     */
    public void untagged() throws IOException {
        writeASCII(UNTAGGED);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#tag(java.lang.String)
     */
    public void tag(String tag) throws IOException {
        writeASCII(tag);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#message(java.lang.String)
     */
    public void message(String message) throws IOException {
        if (message != null) {
            space();
            writeASCII(message);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#message(long)
     */
    public void message(long number) throws IOException {
        space();
        writeASCII(Long.toString(number));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#responseCode(java.lang.String)
     */
    public void responseCode(String responseCode) throws IOException {
        if (responseCode != null && !"".equals(responseCode)) {
            writeASCII(" [");
            writeASCII(responseCode);
            write(BYTES_CLOSE_SQUARE_BRACKET);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#end()
     */
    public void end() throws IOException {
        write(BYTES_LINE_END);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#commandName(java.lang.String)
     */
    public void commandName(String commandName) throws IOException {
        space();
        writeASCII(commandName);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#quote(java.lang.String)
     */
    public void quote(String message) throws IOException {
        space();
        final int length = message.length();
        buffer.clear();
        buffer.put(BYTE_DQUOTE);
        for (int i = 0; i < length; i++) {
            writeIfFull();
            char character = message.charAt(i);
            if (character == ImapConstants.BACK_SLASH || character == DQUOTE) {
                buffer.put(BYTE_BACK_SLASH);
            }
            writeIfFull();
            // 7-bit ASCII only
            if (character > 128) {
                buffer.put(BYTE_QUESTION);
            } else {
                buffer.put((byte) character);
            }
        }
        writeIfFull();
        buffer.put(BYTE_DQUOTE);
        buffer.flip();
        write(buffer);
    }

    private void writeIfFull() throws IOException {
        if (!buffer.hasRemaining()) {
            buffer.flip();
            write(buffer);
            buffer.clear();
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#closeParen()
     */
    public void closeParen() throws IOException {
        closeBracket(BYTES_CLOSING_PARENTHESIS);
    }

    private void closeBracket(final byte[] bracket) throws IOException {
        write(bracket);
        clearSkipNextSpace();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#openParen()
     */
    public void openParen() throws IOException {
        openBracket(BYTES_OPENING_PARENTHESIS);
    }

    private void openBracket(final byte[] bracket) throws IOException {
        space();
        write(bracket);
        skipNextSpace();
    }

    private void clearSkipNextSpace() {
        skipNextSpace = false;
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#skipNextSpace()
     */
    public void skipNextSpace() {
        skipNextSpace = true;
    }

    private void space() throws IOException {
        if (skipNextSpace) {
            skipNextSpace = false;
        } else {
            write(BYTES_SPACE);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#literal(org.apache.james.imap.message.response.Literal)
     */
    public void literal(Literal literal) throws IOException {
        space();
        write(BYTES_OPEN_BRACE);
        final long size = literal.size();
        writeASCII(Long.toString(size));
        write(BYTES_CLOSE_BRACE);
        write(BYTES_LINE_END);
        if (size > 0) {
            writeLiteral(literal);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#closeSquareBracket()
     */
    public void closeSquareBracket() throws IOException {
        closeBracket(BYTES_CLOSE_SQUARE_BRACKET);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#openSquareBracket()
     */
    public void openSquareBracket() throws IOException {
        openBracket(BYTES_OPEN_SQUARE_BRACKET);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#upperCaseAscii(java.lang.String)
     */
    public void upperCaseAscii(String message) throws IOException {
        upperCaseAscii(message, false);
    }

    private void upperCaseAscii(String message, boolean quote)
            throws IOException {
        if (message == null) {
            nil();
        } else {
            space();
            final int length = message.length();
            buffer.clear();
            if (quote) {
                buffer.put(BYTE_DQUOTE);
            }
            for (int i = 0; i < length; i++) {
                writeIfFull();
                final char next = message.charAt(i);
                if (next >= 'a' && next <= 'z') {
                    buffer.put((byte) (next - LOWER_CASE_OFFSET));
                } else {
                    buffer.put((byte) (next));
                }
            }
            writeIfFull();
            if (quote) {
                buffer.put(BYTE_DQUOTE);
            }
            buffer.flip();
            write(buffer);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#quoteUpperCaseAscii(java.lang.String)
     */
    public void quoteUpperCaseAscii(String message) throws IOException {
        upperCaseAscii(message, true);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#continuation(java.lang.String)
     */
    public void continuation(String message) throws IOException {
        writeASCII(CONTINUATION + SP + message);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseWriter#()
     */
    public void commandContinuationRequest() throws IOException {
        writeASCII(CONTINUATION);
        end();
    }
    
    
    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#commandComplete(org.apache.james.imap.api.ImapCommand,
     *      java.lang.String, java.lang.String)
     */
    public void commandComplete(final ImapCommand command,
            final String responseCode, final String tag) throws IOException {
        tag(tag);
        message(OK);
        responseCode(responseCode);
        commandName(command);
        message("completed.");
        end();
    }
    
    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#untaggedNoResponse(java.lang.String,
     *      java.lang.String)
     */
    public void untaggedNoResponse(String displayMessage, String responseCode)
            throws IOException {
        untagged();
        message(NO);
        responseCode(responseCode);
        message(displayMessage);
        end();
    }

    
    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#flagsResponse(javax.mail.Flags)
     */
    public void flagsResponse(Flags flags) throws IOException {
        untagged();
        flags(flags);
        end();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#existsResponse(long)
     */
    public void existsResponse(long count) throws IOException {
        untagged();
        message(count);
        message(EXISTS);
        end();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#recentResponse(long)
     */
    public void recentResponse(long count) throws IOException {
        untagged();
        message(count);
        message(RECENT);
        end();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#expungeResponse(long)
     */
    public void expungeResponse(long msn) throws IOException {
        untagged();
        message(msn);
        message(EXPUNGE);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#commandResponse(org.apache.james.imap.api.ImapCommand,
     *      java.lang.String)
     */
    public void commandResponse(ImapCommand command, String message)
            throws IOException {
        untagged();
        commandName(command);
        message(message);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#taggedResponse(java.lang.String,
     *      java.lang.String)
     */
    public void taggedResponse(String message, String tag) throws IOException {
        tag(tag);
        message(message);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#untaggedResponse(java.lang.String)
     */
    public void untaggedResponse(String message) throws IOException {
        untagged();
        message(message);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#byeResponse(java.lang.String)
     */
    public void byeResponse(String message) throws IOException {
        untaggedResponse(BYE + SP + message);
    }
    
    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#hello(java.lang.String)
     */
    public void hello(String message) throws IOException {
        untaggedResponse(OK + SP + message);
    }


    private void commandName(final ImapCommand command) throws IOException {
        final String name = command.getName();
        commandName(name);
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#statusResponse(java.lang.String,
     *      org.apache.james.imap.api.ImapCommand, java.lang.String,
     *      java.lang.String, Collection, long, java.lang.String)
     */
    public void statusResponse(String tag, ImapCommand command, String type,
            String responseCode, Collection<String> parameters, long number, String text)
            throws IOException {
        if (tag == null) {
            untagged();
        } else {
            tag(tag);
        }
        message(type);
        if (responseCode != null) {
            openSquareBracket();
            message(responseCode);
            if (parameters != null && !parameters.isEmpty()) {
                openParen();
                for (Iterator<String> it = parameters.iterator(); it.hasNext();) {
                    final String parameter = it.next();
                    message(parameter);
                }
                closeParen();
            }
            if (number > 0) {
                message(number);
            }
            closeSquareBracket();
        }
        if (command != null) {
            commandName(command);
        }
        if (text != null && !"".equals(text)) {
            message(text);
        }
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.api.message.response.ImapResponseComposer#statusResponse(Long,
     *      Long, Long, Long, Long, String)
     */
    public void statusResponse(Long messages, Long recent, Long uidNext,
            Long uidValidity, Long unseen, String mailboxName)
            throws IOException {
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
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#listResponse(java.lang.String, java.util.List, java.lang.String, java.lang.String)
     */
    public void listResponse(String typeName, List<String> attributes,
            char hierarchyDelimiter, String name) throws IOException {
        untagged();
        message(typeName);
        openParen();
        if (attributes != null) {
            for (Iterator<String> it = attributes.iterator(); it.hasNext();) {
                final String attribute =  it.next();
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
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#searchResponse(long[])
     */
    public void searchResponse(long[] ids) throws IOException {
        untagged();
        message(ImapConstants.SEARCH_RESPONSE_NAME);
        message(ids);
        end();
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
     * @see org.apache.james.imap.encode.ImapResponseComposer#flags(javax.mail.Flags)
     */
    public void flags(Flags flags) throws IOException {
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
        closeParen();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#closeFetchResponse()
     */
    public void closeFetchResponse() throws IOException {
        closeParen();
        end();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#openFetchResponse(long)
     */
    public void openFetchResponse(long msn) throws IOException {
        untagged();
        message(msn);
        message(FETCH);
        openParen();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#address(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public void address(String name, String domainList, String mailbox,
            String host) throws IOException {
        skipNextSpace();
        openParen();
        nillableQuote(name);
        nillableQuote(domainList);
        nillableQuote(mailbox);
        nillableQuote(host);
        closeParen();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#endAddresses()
     */
    public void endAddresses() throws IOException {
        closeParen();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#endEnvelope(java.lang.String, java.lang.String)
     */
    public void endEnvelope(String inReplyTo, String messageId)
            throws IOException {
        nillableQuote(inReplyTo);
        nillableQuote(messageId);
        closeParen();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#nil()
     */
    public void nil() throws IOException {
        message(NIL);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#startAddresses()
     */
    public void startAddresses() throws IOException {
        openParen();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#startEnvelope(java.lang.String, java.lang.String, boolean)
     */
    public void startEnvelope(String date, String subject,
            boolean prefixWithName) throws IOException {
        if (prefixWithName) {
            message(ENVELOPE);
        }
        openParen();
        nillableQuote(date);
        nillableQuote(subject);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#nillableQuote(java.lang.String)
     */
    public void nillableQuote(String message)
            throws IOException {
        if (message == null) {
            nil();
        } else {
            quote(message);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#nillableComposition(java.lang.String, java.util.List)
     */
    public void nillableComposition(String masterQuote,
            List<String> quotes) throws IOException {
        if (masterQuote == null) {
            nil();
        } else {
            openParen();
            quote(masterQuote);
            nillableQuotes(quotes);
            closeParen();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#nillableQuotes(java.util.List)
     */
    public void nillableQuotes(List<String> quotes)
            throws IOException {
        if (quotes == null || quotes.size() == 0) {
            nil();
        } else {
            openParen();
            for (final String string:quotes) {
                nillableQuote(string);
            }
            closeParen();
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapResponseComposer#capabilities(java.util.List)
     */
    public void capabilities(List<String> capabilities) throws IOException {
        untagged();
        message(CAPABILITY_COMMAND_NAME);
        for(String capability:capabilities) {
            message(capability);
        }
        end();
    }
}
