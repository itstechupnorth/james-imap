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

package org.apache.james.imap.api.message.response;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.api.ImapCommand;

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
    public abstract void untaggedNoResponse(String displayMessage,
            String responseCode) throws IOException;

    /**
     * Writes flags to output using standard format.
     * 
     * @param flags
     *            <code>Flags</code>, not null
     */
    public abstract void flags(Flags flags) throws IOException;

    /**
     * Writes a complete FLAGS response.
     * 
     * @param flags
     *            <code>Flags</code>, not null
     */
    public abstract void flagsResponse(Flags flags) throws IOException;

    public abstract void existsResponse(long count) throws IOException;

    public abstract void recentResponse(long count) throws IOException;

    public abstract void expungeResponse(long msn) throws IOException;

    public abstract void searchResponse(long[] ids) throws IOException;

    /**
     * Starts a FETCH response by writing the opening star-FETCH-number-paren
     * sequence.
     * 
     * @param msn
     *            message number
     * @see #closeFetchResponse()
     */
    public abstract void openFetchResponse(long msn) throws IOException;

    /**
     * Ends a FETCH response by writing the closing paren-crlf sequence.
     */
    public abstract void closeFetchResponse() throws IOException;

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
    public abstract void startEnvelope(String date, String subject,
            boolean prefixWithName) throws IOException;

    /**
     * Starts a list of addresses.
     * 
     * @throws IOException
     */
    public abstract void startAddresses() throws IOException;

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
    public abstract void address(String name, String domainList,
            String mailbox, String host) throws IOException;

    /**
     * Ends a list of addresses.
     * 
     * @throws IOException
     */
    public abstract void endAddresses() throws IOException;

    /**
     * Ends a <code>FETCH ENVELOPE</code> production.
     * 
     * @param inReplyTo
     *            envelope in-reply-to, or null for <code>NIL</code>
     * @param messageId
     *            envelope message-id, or null for <code>NIL</code>
     * @throws IOException
     */
    public abstract void endEnvelope(String inReplyTo, String messageId)
            throws IOException;

    /**
     * Composes a <code>NIL</code>.
     * 
     * @throws IOException
     */
    public abstract void nil() throws IOException;

    public abstract void commandResponse(ImapCommand command, String message)
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
    public abstract void listResponse(String typeName, List<String> attributes,
            char hierarchyDelimiter, String name) throws IOException;

    /**
     * Writes the message provided to the client, prepended with the request
     * tag.
     * 
     * @param message
     *            The message to write to the client.
     */
    public abstract void taggedResponse(String message, String tag)
            throws IOException;

    /**
     * Writes the message provided to the client, prepended with the untagged
     * marker "*".
     * 
     * @param message
     *            The message to write to the client.
     */
    public abstract void untaggedResponse(String message) throws IOException;

    public abstract void byeResponse(String message) throws IOException;
    
    public abstract void hello(String message) throws IOException;

    public abstract void untagged() throws IOException;

    public abstract void commandName(final String name) throws IOException;

    public void message(final String message)
            throws IOException;

    public abstract void message(final long number) throws IOException;

    public abstract void end() throws IOException;

    public abstract void tag(String tag) throws IOException;

    public abstract void statusResponse(String tag, ImapCommand command,
            String type, String responseCode, Collection<String> parameters,
            long number, String text) throws IOException;

    public abstract void statusResponse(Long messages, Long recent,
            Long uidNext, Long uidValidity, Long unseen, String mailboxName)
            throws IOException;

    public void quote(String message) throws IOException;

    public void literal(Literal literal) throws IOException;

    public void openParen() throws IOException;

    public void closeParen() throws IOException;

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
    public void upperCaseAscii(final String message)
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
    public void quoteUpperCaseAscii(final String message)
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
    public void nillableQuote(String message)
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
    public void nillableQuotes(List<String> quotes)
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
    public void nillableComposition(String masterQuote,
            List<String> quotes) throws IOException;

    public void skipNextSpace() throws IOException;

    /**
     * Composes a <code>CAPABILITY</code> response.
     * See <code>7.2.1</code> of 
     * <a href='http://james.apache.org/server/rfclist/imap4/rfc2060.txt' rel='tag'>RFC2060</a>.
     * @param capabilities not null
     * @throws IOException 
     */
    public void capabilities(List<String> capabilities) throws IOException;
   
    /**
     * Starts a continuation response.
     * 
     * @param message
     *            the message, not null
     */
    public void continuation(String message) throws IOException;

    /**
     * Writes a response code.
     * 
     * @param responseCode
     *            the response code, not null
     */
     public void responseCode(String responseCode) throws IOException;


    /**
     * Closes a square bracket - writes a <code>[</code>.
     * 
     * @throws IOException
     */
     public void openSquareBracket() throws IOException;

    /**
     * Closes a square bracket - writes a <code>]</code>.
     * 
     * @throws IOException
     */
     public void closeSquareBracket() throws IOException;

    
    
    /**
     * Sends a server command continuation request '+' back to the client,
     * requesting more data to be sent.
     */
     public void commandContinuationRequest() throws IOException;

}