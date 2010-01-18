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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
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

    public ImapResponseComposerImpl(final ImapResponseWriter writer) {
        this.writer = writer;
    }


    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#commandComplete(org.apache.james.imap.api.ImapCommand,
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
     * @see org.apache.james.imap.encode.ImapResponseComposer#untaggedNoResponse(java.lang.String,
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
     * @see org.apache.james.imap.encode.ImapResponseComposer#flagsResponse(javax.mail.Flags)
     */
    public void flagsResponse(Flags flags) throws IOException {
        untagged();
        flags(flags);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#existsResponse(int)
     */
    public void existsResponse(int count) throws IOException {
        untagged();
        message(count);
        message(EXISTS);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#recentResponse(int)
     */
    public void recentResponse(int count) throws IOException {
        untagged();
        message(count);
        message(RECENT);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#expungeResponse(int)
     */
    public void expungeResponse(int msn) throws IOException {
        untagged();
        message(msn);
        message(EXPUNGE);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#commandResponse(org.apache.james.imap.api.ImapCommand,
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
     * @see org.apache.james.imap.encode.ImapResponseComposer#taggedResponse(java.lang.String,
     *      java.lang.String)
     */
    public void taggedResponse(String message, String tag) throws IOException {
        tag(tag);
        message(message);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#untaggedResponse(java.lang.String)
     */
    public void untaggedResponse(String message) throws IOException {
        untagged();
        message(message);
        end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#byeResponse(java.lang.String)
     */
    public void byeResponse(String message) throws IOException {
        untaggedResponse(BYE + SP + message);
    }
    
    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#hello(java.lang.String)
     */
    public void hello(String message) throws IOException {
        untaggedResponse(OK + SP + message);
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#untagged()
     */
    public void untagged() throws IOException {
        writer.untagged();
    }

    private void commandName(final ImapCommand command) throws IOException {
        final String name = command.getName();
        commandName(name);
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#commandName(java.lang.String)
     */
    public void commandName(final String name) throws IOException {
        writer.commandName(name);
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#message(java.lang.String)
     */
    public ImapResponseComposer message(final String message)
            throws IOException {
        if (message != null) {
            // TODO: consider message normalisation
            // TODO: CR/NFs in message must be replaced
            // TODO: probably best done in the writer
            writer.message(message);
        }
        return this;
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#message(long)
     */
    public void message(final long number) throws IOException {
        writer.message(number);
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#responseCode(java.lang.String)
     */
    public void responseCode(final String responseCode) throws IOException {
        if (responseCode != null && !"".equals(responseCode)) {
            writer.responseCode(responseCode);
        }
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#end()
     */
    public void end() throws IOException {
        writer.end();
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#tag(java.lang.String)
     */
    public void tag(String tag) throws IOException {
        writer.tag(tag);
    }

    /**
     * @throws IOException
     * @see org.apache.james.imap.encode.ImapResponseComposer#statusResponse(java.lang.String,
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
     * @see org.apache.james.imap.encode.ImapResponseComposer#statusResponse(Long,
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

    public void listResponse(String typeName, List<String> attributes,
            String hierarchyDelimiter, String name) throws IOException {
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

        if (hierarchyDelimiter == null) {
            message(NIL);
        } else {
            quote(hierarchyDelimiter);
        }

        quote(name);

        end();
    }

    public void quote(String message) throws IOException {
        writer.quote(message);
    }

    public ImapResponseComposer closeParen() throws IOException {
        writer.closeParen();
        return this;
    }

    public ImapResponseComposer openParen() throws IOException {
        writer.openParen();
        return this;
    }

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

    public void closeFetchResponse() throws IOException {
        closeParen();
        end();
    }

    public void openFetchResponse(long msn) throws IOException {
        untagged();
        message(msn);
        message(FETCH);
        openParen();
    }

    public void literal(Literal literal) throws IOException {
        writer.literal(literal);
    }

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

    public void endAddresses() throws IOException {
        closeParen();
    }

    public void endEnvelope(String inReplyTo, String messageId)
            throws IOException {
        nillableQuote(inReplyTo);
        nillableQuote(messageId);
        closeParen();
    }

    public void nil() throws IOException {
        message(NIL);
    }

    public void startAddresses() throws IOException {
        openParen();
    }

    public void startEnvelope(String date, String subject,
            boolean prefixWithName) throws IOException {
        if (prefixWithName) {
            message(ENVELOPE);
        }
        openParen();
        nillableQuote(date);
        nillableQuote(subject);
    }

    public ImapResponseComposer nillableQuote(String message)
            throws IOException {
        if (message == null) {
            nil();
        } else {
            quote(message);
        }
        return this;
    }

    public void skipNextSpace() throws IOException {
        writer.skipNextSpace();
    }

    public void closeSquareBracket() throws IOException {
        writer.closeSquareBracket();
    }

    public void openSquareBracket() throws IOException {
        writer.openSquareBracket();
    }

    public ImapResponseComposer nillableComposition(String masterQuote,
            List<String> quotes) throws IOException {
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

    public ImapResponseComposer nillableQuotes(List<String> quotes)
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
        return this;
    }

    public ImapResponseComposer upperCaseAscii(String message)
            throws IOException {
        if (message == null) {
            nil();
        } else {
            writer.upperCaseAscii(message);
        }
        return this;
    }

    public ImapResponseComposer quoteUpperCaseAscii(String message)
            throws IOException {
        if (message == null) {
            nil();
        } else {
            writer.quoteUpperCaseAscii(message);
        }
        return this;
    }

    public ImapResponseComposer capabilities(List<String> capabilities) throws IOException {
        untagged();
        message(CAPABILITY_COMMAND_NAME);
        for(String capability:capabilities) {
            message(capability);
        }
        end();
        return this;
    }
}
