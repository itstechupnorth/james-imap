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

package org.apache.james.imap.encode;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.message.response.Literal;

public interface ImapResponseComposer {

    /**
     * Writes an untagged NO response. Indicates that a warning. The command may
     * still complete sucessfully.
     * 
     * @param displayMessage
     *            message for display, not null
     * @param responseCode
     *            response code or null when there is no response code
     */
    public ImapResponseComposer untaggedNoResponse(String displayMessage,
            String responseCode) throws IOException;

    /**
     * Writes flags to output using standard format.
     * 
     * @param flags
     *            <code>Flags</code>, not null
     */
    public ImapResponseComposer flags(Flags flags) throws IOException;

    /**
     * Writes a complete FLAGS response.
     * 
     * @param flags
     *            <code>Flags</code>, not null
     */
    public ImapResponseComposer flagsResponse(Flags flags) throws IOException;

    public ImapResponseComposer existsResponse(long count) throws IOException;

    public ImapResponseComposer recentResponse(long count) throws IOException;

    public ImapResponseComposer expungeResponse(long msn) throws IOException;

    public ImapResponseComposer searchResponse(long[] ids) throws IOException;

    /**
     * Starts a FETCH response by writing the opening star-FETCH-number-paren
     * sequence.
     * 
     * @param msn
     *            message number
     * @see #closeFetchResponse()
     */
    public ImapResponseComposer openFetchResponse(long msn) throws IOException;

    /**
     * Ends a FETCH response by writing the closing paren-crlf sequence.
     */
    public ImapResponseComposer closeFetchResponse() throws IOException;

    /**
     * Starts a <code>FETCH ENVELOPE</code> production.
     * 
     * @param date
     *            envelope date, or null for <code>NIL</code>
     * @param subject
     *            envelope subject, or null for <code>NIL</code>
     * @param prefixWithName
     *            whether <code>ENVELOPE</code> should be prefixed
     * @throws IOException
     * @see {@link #endEnvelope(String, String)} must be called
     */
    public ImapResponseComposer startEnvelope(String date, String subject,
            boolean prefixWithName) throws IOException;

    /**
     * Starts a list of addresses.
     * 
     * @throws IOException
     */
    public ImapResponseComposer startAddresses() throws IOException;

    /**
     * Composes an address.
     * 
     * @param name
     *            personal name, or null for <code>NIL</code>
     * @param domainList
     *            route address list, or null for <code>NIL</code>
     * @param mailbox
     *            mailbox name, or null for <code>NIL</code>
     * @param host
     *            host name, or null for <code>NIL</code>
     * @throws IOException
     */
    public ImapResponseComposer address(String name, String domainList,
            String mailbox, String host) throws IOException;

    /**
     * Ends a list of addresses.
     * 
     * @throws IOException
     */
    public ImapResponseComposer endAddresses() throws IOException;

    /**
     * Ends a <code>FETCH ENVELOPE</code> production.
     * 
     * @param inReplyTo
     *            envelope in-reply-to, or null for <code>NIL</code>
     * @param messageId
     *            envelope message-id, or null for <code>NIL</code>
     * @throws IOException
     */
    public ImapResponseComposer endEnvelope(String inReplyTo, String messageId)
            throws IOException;

    /**
     * Composes a <code>NIL</code>.
     * 
     * @throws IOException
     */
    public ImapResponseComposer nil() throws IOException;

    public ImapResponseComposer commandResponse(ImapCommand command, String message)
            throws IOException;

    /**
     * Writes a list response
     * 
     * @param typeName
     *            <code>LIST</code> or <code>LSUB</code>.
     * @param attributes
     *            name attributes, or null if there are no attributes
     * @param hierarchyDelimiter
     *            hierarchy delimiter, or null if delimiter is <code>NIL</code>
     * @param name
     *            mailbox name
     */
    public ImapResponseComposer listResponse(String typeName, List<String> attributes,
            char hierarchyDelimiter, String name) throws IOException;

    /**
     * Writes the message provided to the client, prepended with the request
     * tag.
     * 
     * @param message
     *            The message to write to the client.
     */
    public ImapResponseComposer taggedResponse(String message, String tag)
            throws IOException;

    /**
     * Writes the message provided to the client, prepended with the untagged
     * marker "*".
     * 
     * @param message
     *            The message to write to the client.
     */
    public ImapResponseComposer untaggedResponse(String message) throws IOException;

    public ImapResponseComposer byeResponse(String message) throws IOException;
    
    public ImapResponseComposer hello(String message) throws IOException;

    public ImapResponseComposer untagged() throws IOException;

    public ImapResponseComposer commandName(final String name) throws IOException;

    public ImapResponseComposer message(final String message)
            throws IOException;

    public ImapResponseComposer message(final long number) throws IOException;

    public ImapResponseComposer end() throws IOException;

    public ImapResponseComposer tag(String tag) throws IOException;

    public ImapResponseComposer statusResponse(String tag, ImapCommand command,
            String type, String responseCode, Collection<String> parameters,
            boolean useParens, long number, String text) throws IOException;

    public ImapResponseComposer statusResponse(Long messages, Long recent,
            Long uidNext, Long uidValidity, Long unseen, String mailboxName)
            throws IOException;

    public void quote(String message) throws IOException;

    public void literal(Literal literal) throws IOException;

    public ImapResponseComposer openParen() throws IOException;

    public ImapResponseComposer closeParen() throws IOException;

    /**
     * Appends the given message after conversion to upper case. The message may
     * be assumed to be ASCII encoded. Conversion of characters MUST NOT be
     * performed according to the current locale but as per ASCII.
     * 
     * @param message
     *            ASCII encoded, not null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer upperCaseAscii(final String message)
            throws IOException;

    /**
     * Appends the given message after conversion to upper case. The message may
     * be assumed to be ASCII encoded. Conversion of characters MUST NOT be
     * performed according to the current locale but as per ASCII.
     * 
     * @param message
     *            ASCII encoded, not null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer quoteUpperCaseAscii(final String message)
            throws IOException;

    /**
     * Appends the given message after appropriate quoting (when not null) or
     * <code>NIL</code> (when null).
     * 
     * @param message
     *            possibly null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer nillableQuote(String message)
            throws IOException;

    /**
     * Composes a sequence of nillables quotes. When messages are null, a single
     * <code>NIL</code> is appended. Otherwise, each element is appended in
     * sequence as per {@link #nillableQuote(String)}.
     * 
     * @param quotes
     *            messages, possibly null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer nillableQuotes(List<String> quotes)
            throws IOException;

    /**
     * Composes a nillable composition. When the master quote is null,
     * <code>NIL</code> is appended. Otherwise, a parenthesized list is
     * created starting with the master quote. When the quotes are null,
     * <code>NIL</code> is appended only. Otherwise, each element is appended
     * in sequence as per {@link #nillableQuote(String)}
     * 
     * @param quote
     *            master, possibly null
     * @param quotes
     *            quotes, possibly null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer nillableComposition(String masterQuote,
            List<String> quotes) throws IOException;

    public void skipNextSpace() throws IOException;

    /**
     * Composes a <code>CAPABILITY</code> response.
     * See <code>7.2.1</code> of 
     * <a href='http://james.apache.org/server/rfclist/imap4/rfc2060.txt' rel='tag'>RFC2060</a>.
     * @param capabilities not null
     * @throws IOException 
     */
    public ImapResponseComposer capabilities(List<String> capabilities) throws IOException;
    
    /**
     * Writes a continuation response.
     * 
     * @param message
     *            message for display, not null
     */
    public ImapResponseComposer continuationResponse(String message) throws IOException;

}
