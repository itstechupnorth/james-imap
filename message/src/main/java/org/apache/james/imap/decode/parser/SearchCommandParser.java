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

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.james.imap.api.DecodingException;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapMessageCallback;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.request.DayMonthYear;
import org.apache.james.imap.api.message.request.SearchKey;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.decode.ImapRequestLine;
import org.apache.james.imap.message.request.SearchRequest;

/**
 * Parse SEARCH commands
 *
 */
public class SearchCommandParser extends AbstractUidCommandParser {

    /** Lazy loaded */
    private Collection<String> charsetNames;

    public SearchCommandParser() {
        super(ImapCommand.selectedStateCommand(ImapConstants.SEARCH_COMMAND_NAME));
    }

    /**
     * Parses the request argument into a valid search term.
     * 
     * @param request
     *            <code>ImapRequestLineReader</code>, not null
     * @param charset
     *            <code>Charset</code> or null if there is no charset
     * @param isFirstToken
     *            true when this is the first token read, false otherwise
     */
    protected SearchKey searchKey(ImapRequestLine request, Charset charset,
            boolean isFirstToken) throws DecodingException,
            IllegalCharsetNameException, UnsupportedCharsetException {
        final char next = request.nextChar();
        if (next >= '0' && next <= '9' || next == '*') {
            return sequenceSet(request);
        } else if (next == '(') {
            return paren(request, charset);
        } else {
            final int cap = consumeAndCap(request);
            switch (cap) {
                case 'A':
                    return a(request);
                case 'B':
                    return b(request, charset);
                case 'C':
                    return c(request, isFirstToken, charset);
                case 'D':
                    return d(request);
                case 'E':
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                case 'F':
                    return f(request, charset);
                case 'G':
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                case 'H':
                    return header(request, charset);
                case 'I':
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                case 'J':
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                case 'K':
                    return keyword(request);
                case 'L':
                    return larger(request);
                case 'M':
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                case 'N':
                    return n(request, charset);
                case 'O':
                    return o(request, charset);
                case 'P':
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                case 'Q':
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                case 'R':
                    return recent(request);
                case 'S':
                    return s(request, charset);
                case 'T':
                    return t(request, charset);
                case 'U':
                    return u(request);
                default:
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            }
        }
    }

    private SearchKey paren(ImapRequestLine request, Charset charset)
            throws DecodingException {
        request.consume();
        List<SearchKey> keys = new ArrayList<SearchKey>();
        addUntilParen(request, keys, charset);
        return SearchKey.buildAnd(keys);
    }

    private void addUntilParen(ImapRequestLine request, List<SearchKey> keys,
            Charset charset) throws DecodingException {
        final char next = request.nextWordChar();
        if (next == ')') {
            request.consume();
        } else {
            final SearchKey key = searchKey(request, null, false);
            keys.add(key);
            addUntilParen(request, keys, charset);
        }
    }

    private int consumeAndCap(ImapRequestLine request)
            throws DecodingException {
        final char next = request.consume();
        final int cap = next > 'Z' ? next ^ 32 : next;
        return cap;
    }

    private SearchKey cc(ImapRequestLine request, final Charset charset)
            throws DecodingException {
        final SearchKey result;
        nextIsSpace(request);
        final String value = astring(request, charset);
        result = SearchKey.buildCc(value);
        return result;
    }

    private SearchKey c(ImapRequestLine request,
            final boolean isFirstToken, final Charset charset)
            throws DecodingException, IllegalCharsetNameException,
            UnsupportedCharsetException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'C':
                return cc(request, charset);
            case 'H':
                return charset(request, isFirstToken);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey charset(ImapRequestLine request,
            final boolean isFirstToken) throws DecodingException,
            IllegalCharsetNameException, UnsupportedCharsetException {
        final SearchKey result;
        nextIsA(request);
        nextIsR(request);
        nextIsS(request);
        nextIsE(request);
        nextIsT(request);
        nextIsSpace(request);
        if (!isFirstToken) {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
        final String value = astring(request);
        final Charset charset = Charset.forName(value);
        request.nextWordChar();
        result = searchKey(request, charset, false);
        return result;
    }

    private SearchKey u(ImapRequestLine request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'I':
                return uid(request);
            case 'N':
                return un(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey un(ImapRequestLine request)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'A':
                return unanswered(request);
            case 'D':
                return und(request);
            case 'F':
                return unflagged(request);
            case 'K':
                return unkeyword(request);
            case 'S':
                return unseen(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey und(ImapRequestLine request)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E':
                return undeleted(request);
            case 'R':
                return undraft(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey t(ImapRequestLine request, final Charset charset)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E':
                return text(request, charset);
            case 'O':
                return to(request, charset);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey s(ImapRequestLine request, final Charset charset)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E':
                return se(request);
            case 'I':
                return since(request);
            case 'M':
                return smaller(request);
            case 'U':
                return subject(request, charset);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey se(ImapRequestLine request)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E':
                return seen(request);
            case 'N':
                return sen(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey sen(ImapRequestLine request)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'T':
                return sent(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey sent(ImapRequestLine request)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'B':
                return sentBefore(request);
            case 'O':
                return sentOn(request);
            case 'S':
                return sentSince(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey o(ImapRequestLine request, Charset charset)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'L':
                return old(request);
            case 'N':
                return on(request);
            case 'R':
                return or(request, charset);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey n(ImapRequestLine request, Charset charset)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E':
                return _new(request);
            case 'O':
                return not(request, charset);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey f(ImapRequestLine request, final Charset charset)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'L':
                return flagged(request);
            case 'R':
                return from(request, charset);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey d(ImapRequestLine request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E':
                return deleted(request);
            case 'R':
                return draft(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey keyword(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsY(request);
        nextIsW(request);
        nextIsO(request);
        nextIsR(request);
        nextIsD(request);
        nextIsSpace(request);
        final String value = atom(request);
        result = SearchKey.buildKeyword(value);
        return result;
    }

    private SearchKey unkeyword(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsY(request);
        nextIsW(request);
        nextIsO(request);
        nextIsR(request);
        nextIsD(request);
        nextIsSpace(request);
        final String value = atom(request);
        result = SearchKey.buildUnkeyword(value);
        return result;
    }

    private SearchKey header(ImapRequestLine request,
            final Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsA(request);
        nextIsD(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        final String field = astring(request, charset);
        nextIsSpace(request);
        final String value = astring(request, charset);
        result = SearchKey.buildHeader(field, value);
        return result;
    }

    private SearchKey larger(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsR(request);
        nextIsG(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        final long value = number(request);
        result = SearchKey.buildLarger(value);
        return result;
    }

    private SearchKey smaller(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsL(request);
        nextIsL(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        final long value = number(request);
        result = SearchKey.buildSmaller(value);
        return result;
    }

    private SearchKey from(ImapRequestLine request, final Charset charset)
            throws DecodingException {
        final SearchKey result;
        nextIsO(request);
        nextIsM(request);
        nextIsSpace(request);
        final String value = astring(request, charset);
        result = SearchKey.buildFrom(value);
        return result;
    }

    private SearchKey flagged(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsG(request);
        nextIsG(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildFlagged();
        return result;
    }

    private SearchKey unseen(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsE(request);
        nextIsN(request);
        result = SearchKey.buildUnseen();
        return result;
    }

    private SearchKey undraft(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsF(request);
        nextIsT(request);
        result = SearchKey.buildUndraft();
        return result;
    }

    private SearchKey undeleted(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsL(request);
        nextIsE(request);
        nextIsT(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildUndeleted();
        return result;
    }

    private SearchKey unflagged(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsL(request);
        nextIsA(request);
        nextIsG(request);
        nextIsG(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildUnflagged();
        return result;
    }

    private SearchKey unanswered(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsN(request);
        nextIsS(request);
        nextIsW(request);
        nextIsE(request);
        nextIsR(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildUnanswered();
        return result;
    }

    private SearchKey old(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsD(request);
        result = SearchKey.buildOld();
        return result;
    }

    private SearchKey or(ImapRequestLine request, Charset charset)
            throws DecodingException {
        final SearchKey result;
        nextIsSpace(request);
        final SearchKey firstKey = searchKey(request, charset, false);
        nextIsSpace(request);
        final SearchKey secondKey = searchKey(request, charset, false);
        result = SearchKey.buildOr(firstKey, secondKey);
        return result;
    }

    private SearchKey not(ImapRequestLine request, Charset charset)
            throws DecodingException {
        final SearchKey result;
        nextIsT(request);
        nextIsSpace(request);
        final SearchKey nextKey = searchKey(request, charset, false);
        result = SearchKey.buildNot(nextKey);
        return result;
    }

    private SearchKey _new(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsW(request);
        result = SearchKey.buildNew();
        return result;
    }

    private SearchKey recent(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsC(request);
        nextIsE(request);
        nextIsN(request);
        nextIsT(request);
        result = SearchKey.buildRecent();
        return result;
    }

    private SearchKey seen(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsN(request);
        result = SearchKey.buildSeen();
        return result;
    }

    private SearchKey draft(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsA(request);
        nextIsF(request);
        nextIsT(request);
        result = SearchKey.buildDraft();
        return result;
    }

    private SearchKey deleted(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsL(request);
        nextIsE(request);
        nextIsT(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildDeleted();
        return result;
    }

    private SearchKey b(ImapRequestLine request, Charset charset)
            throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'C':
                return bcc(request, charset);
            case 'E':
                return before(request);
            case 'O':
                return body(request, charset);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey body(ImapRequestLine request, final Charset charset)
            throws DecodingException {
        final SearchKey result;
        nextIsD(request);
        nextIsY(request);
        nextIsSpace(request);
        final String value = astring(request, charset);
        result = SearchKey.buildBody(value);
        return result;
    }

    private SearchKey on(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildOn(value);
        return result;
    }

    private SearchKey sentBefore(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsE(request);
        nextIsF(request);
        nextIsO(request);
        nextIsR(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildSentBefore(value);
        return result;
    }

    private SearchKey sentSince(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsI(request);
        nextIsN(request);
        nextIsC(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildSentSince(value);
        return result;
    }

    private SearchKey since(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsN(request);
        nextIsC(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildSince(value);
        return result;
    }

    private SearchKey sentOn(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsN(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildSentOn(value);
        return result;
    }

    private SearchKey before(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsF(request);
        nextIsO(request);
        nextIsR(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildBefore(value);
        return result;
    }

    private SearchKey bcc(ImapRequestLine request, Charset charset)
            throws DecodingException {
        final SearchKey result;
        nextIsC(request);
        nextIsSpace(request);
        final String value = astring(request, charset);
        result = SearchKey.buildBcc(value);
        return result;
    }

    private SearchKey text(ImapRequestLine request, final Charset charset)
            throws DecodingException {
        final SearchKey result;
        nextIsX(request);
        nextIsT(request);
        nextIsSpace(request);
        final String value = astring(request, charset);
        result = SearchKey.buildText(value);
        return result;
    }

    private SearchKey uid(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsD(request);
        nextIsSpace(request);
        final IdRange[] range = parseIdRange(request);
        result = SearchKey.buildUidSet(range);
        return result;
    }

    private SearchKey sequenceSet(ImapRequestLine request)
            throws DecodingException {
        final IdRange[] range = parseIdRange(request);
        final SearchKey result = SearchKey.buildSequenceSet(range);
        return result;
    }

    private SearchKey to(ImapRequestLine request, final Charset charset)
            throws DecodingException {
        final SearchKey result;
        nextIsSpace(request);
        final String value = astring(request, charset);
        result = SearchKey.buildTo(value);
        return result;
    }

    private SearchKey subject(ImapRequestLine request,
            final Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsB(request);
        nextIsJ(request);
        nextIsE(request);
        nextIsC(request);
        nextIsT(request);
        nextIsSpace(request);
        final String value = astring(request, charset);
        result = SearchKey.buildSubject(value);
        return result;
    }

    private SearchKey a(ImapRequestLine request) throws DecodingException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'L':
                return all(request);
            case 'N':
                return answered(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey answered(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsS(request);
        nextIsW(request);
        nextIsE(request);
        nextIsR(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildAnswered();
        return result;
    }

    private SearchKey all(ImapRequestLine request)
            throws DecodingException {
        final SearchKey result;
        nextIsL(request);
        result = SearchKey.buildAll();
        return result;
    }

    private void nextIsSpace(ImapRequestLine request)
            throws DecodingException {
        final char next = request.consume();
        if (next != ' ') {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private void nextIsG(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'G', 'g');
    }

    private void nextIsM(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'M', 'm');
    }

    private void nextIsI(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'I', 'i');
    }

    private void nextIsN(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'N', 'n');
    }

    private void nextIsA(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'A', 'a');
    }

    private void nextIsT(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'T', 't');
    }

    private void nextIsY(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'Y', 'y');
    }

    private void nextIsX(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'X', 'x');
    }

    private void nextIsO(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'O', 'o');
    }

    private void nextIsF(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'F', 'f');
    }

    private void nextIsJ(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'J', 'j');
    }

    private void nextIsC(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'C', 'c');
    }

    private void nextIsD(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'D', 'd');
    }

    private void nextIsB(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'B', 'b');
    }

    private void nextIsR(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'R', 'r');
    }

    private void nextIsE(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'E', 'e');
    }

    private void nextIsW(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'W', 'w');
    }

    private void nextIsS(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'S', 's');
    }

    private void nextIsL(ImapRequestLine request)
            throws DecodingException {
        nextIs(request, 'L', 'l');
    }

    private void nextIs(ImapRequestLine request, final char upper,
            final char lower) throws DecodingException {
        final char next = request.consume();
        if (next != upper && next != lower) {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    public SearchKey decode(ImapRequestLine request)
            throws DecodingException, IllegalCharsetNameException,
            UnsupportedCharsetException {
        request.nextWordChar();
        final SearchKey firstKey = searchKey(request, null, true);
        final SearchKey result;
        if (request.isConsumed() == false && request.nextChar() == ' ') {
            List<SearchKey> keys = new ArrayList<SearchKey>();
            keys.add(firstKey);
            while (request.isConsumed() == false && request.nextChar() == ' ') {
                request.nextWordChar();
                final SearchKey key = searchKey(request, null, false);
                keys.add(key);
            }
            result = SearchKey.buildAnd(keys);
        } else {
            result = firstKey;
        }
        endLine(request);
        return result;
    }

    private ImapMessage unsupportedCharset(final String tag,
            final ImapCommand command) {
        loadCharsetNames();
        final StatusResponseFactory factory = getStatusResponseFactory();
        final ResponseCode badCharset = StatusResponse.ResponseCode
                .badCharset(charsetNames);
        final StatusResponse result = factory.taggedNo(tag, command,
                HumanReadableText.BAD_CHARSET, badCharset);
        return result;
    }

    private synchronized void loadCharsetNames() {
        if (charsetNames == null) {
            charsetNames = new HashSet<String>();
            for (final Iterator<Charset> it = Charset.availableCharsets().values()
                    .iterator(); it.hasNext();) {
                final Charset charset = it.next();
                final Set<String> aliases = charset.aliases();
                charsetNames.addAll(aliases);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.parser.AbstractUidCommandParser#decode(org.apache.james.imap.api.ImapCommand, org.apache.james.imap.decode.ImapRequestLineReader, java.lang.String, boolean, org.apache.james.imap.api.process.ImapSession)
     */
    protected void decode(ImapCommand command,
            ImapRequestLine request, String tag, boolean useUids, ImapSession session, ImapMessageCallback callback) {
        try {
            // Parse the search term from the request
            final SearchKey key = decode(request);

            final ImapMessage result = new SearchRequest(command, key, useUids, tag);
            callback.onMessage(result);
        } catch (IllegalCharsetNameException e) {
            session.getLog().debug("Unable to decode request", e);
            callback.onMessage(unsupportedCharset(tag, command));
        } catch (UnsupportedCharsetException e) {
            session.getLog().debug("Unable to decode request", e);
            callback.onMessage(unsupportedCharset(tag, command));
        } catch (DecodingException e) {
            callback.onException(e);
        }
       
    }
}
