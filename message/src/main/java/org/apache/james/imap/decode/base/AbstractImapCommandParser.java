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

package org.apache.james.imap.decode.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.imap.api.DecodingException;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapMessageCallback;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.request.DayMonthYear;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.decode.DecoderUtils;
import org.apache.james.imap.decode.ImapCommandParser;
import org.apache.james.imap.decode.ImapRequestLine;
import org.apache.james.imap.decode.MessagingImapCommandParser;

/**
 * <p>
 * <strong>Note:</strong>
 * </p>
 * 
 * @version $Revision: 109034 $
 */
public abstract class AbstractImapCommandParser implements ImapCommandParser, MessagingImapCommandParser {
    private static final int QUOTED_BUFFER_INITIAL_CAPACITY = 64;

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    
    private final ImapCommand command;

    private StatusResponseFactory statusResponseFactory;

    public AbstractImapCommandParser(final ImapCommand command) {
        super();
        this.command = command;
    }

    public ImapCommand getCommand() {
        return command;
    }


    public final StatusResponseFactory getStatusResponseFactory() {
        return statusResponseFactory;
    }

    public final void setStatusResponseFactory(
            StatusResponseFactory statusResponseFactory) {
        this.statusResponseFactory = statusResponseFactory;
    }

    /**
     * Parses a request into a command message for later processing.
     * 
     * @param request
     *            <code>ImapRequestLineReader</code>, not null
     * @return <code>ImapCommandMessage</code>, not null
     */
    public final void parse(ImapRequestLine request, final String tag, final ImapSession session, final ImapMessageCallback callback) {
        if (!command.validForState(session.getState())) {
            ImapMessage result = statusResponseFactory.taggedNo(tag, command,
                    HumanReadableText.INVALID_COMMAND);
            callback.onMessage(result);
        } else {
            decode(command, request, tag, session, new ImapMessageCallback() {
                    
                public void onMessage(ImapMessage message) {
                    callback.onMessage(message);
                }
                    
                public void onException(DecodingException ex) {
                    session.getLog().debug("Cannot parse protocol ", ex);
                    ImapMessage result = statusResponseFactory.taggedBad(tag, command, ex.getKey());
                    onMessage(result);                        
                }
            });
        }
    }

 
    
    /**
     * Parses a request into a command message for later processing.
     * @param command
     *            <code>ImapCommand</code> to be parsed, not null
     * @param request
     *            <code>ImapRequestLineReader</code>, not null
     * @param tag command tag, not null
     * @param session imap session 
     * @return <code>ImapCommandMessage</code>, not null
     * @throws DecodingException
     *             if the request cannot be parsed
     */
    protected abstract void decode(ImapCommand command,
            ImapRequestLine request, String tag, ImapSession session, ImapMessageCallback callback);
    

    /**
     * Reads an argument of type "atom" from the request.
     */
    public static String atom(ImapRequestLine request)
            throws DecodingException {
        return consumeWord(request, new ATOM_CHARValidator());
    }

    /**
     * Reads a command "tag" from the request.
     */
    public static String tag(ImapRequestLine request)
            throws DecodingException {
        CharacterValidator validator = new TagCharValidator();
        return consumeWord(request, validator);
    }

    /**
     * Reads an argument of type "astring" from the request.
     */
    public String astring(ImapRequestLine request)
            throws DecodingException {
        return astring(request, null);
    }

    /**
     * Reads an argument of type "astring" from the request.
     */
    public String astring(ImapRequestLine request, Charset charset)
            throws DecodingException {
        char next = request.nextWordChar();
        switch (next) {
            case '"':
                return consumeQuoted(request, charset);
            case '{':
                return consumeLiteralLine(request, charset);
            default:
                return atom(request);
        }
    }

    /**
     * Reads an argument of type "nstring" from the request.
     */
    /*
    public String nstring(ImapRequestLineReader request)
            throws DecodingException {
        char next = request.nextWordChar();
        switch (next) {
            case '"':
                return consumeQuoted(request);
            case '{':
                return consumeLiteralLine(request, null);
            default:
                String value = atom(request);
                if ("NIL".equals(value)) {
                    return null;
                } else {
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, 
                            "Invalid nstring value: valid values are '\"...\"', '{12} CRLF *CHAR8', and 'NIL'.");
                }
        }
    }
    */

    /**
     * Reads a "mailbox" argument from the request. Not implemented *exactly* as
     * per spec, since a quoted or literal "inbox" still yeilds "INBOX" (ie
     * still case-insensitive if quoted or literal). I think this makes sense.
     * 
     * mailbox ::= "INBOX" / astring ;; INBOX is case-insensitive. All case
     * variants of ;; INBOX (e.g. "iNbOx") MUST be interpreted as INBOX ;; not
     * as an astring.
     */
    public String mailbox(ImapRequestLine request)
            throws DecodingException {
        String mailbox = astring(request);
        if (mailbox.equalsIgnoreCase(ImapConstants.INBOX_NAME)) {
            return ImapConstants.INBOX_NAME;
        } else {
            return mailbox;
        }
    }

    /**
     * Reads one <code>date</code> argument from the request.
     * 
     * @param request
     *            <code>ImapRequestLineReader</code>, not null
     * @return <code>DayMonthYear</code>, not null
     * @throws DecodingException
     */
    public DayMonthYear date(ImapRequestLine request)
            throws DecodingException {

        final char one = request.consume();
        final char two = request.consume();
        final int day;
        if (two == '-') {
            day = DecoderUtils.decodeFixedDay(' ', one);
        } else {
            day = DecoderUtils.decodeFixedDay(one, two);
            nextIsDash(request);
        }

        final char monthFirstChar = request.consume();
        final char monthSecondChar = request.consume();
        final char monthThirdChar = request.consume();
        final int month = DecoderUtils.decodeMonth(monthFirstChar,
                monthSecondChar, monthThirdChar) + 1;
        nextIsDash(request);
        final char milleniumChar = request.consume();
        final char centuryChar = request.consume();
        final char decadeChar = request.consume();
        final char yearChar = request.consume();
        final int year = DecoderUtils.decodeYear(milleniumChar, centuryChar,
                decadeChar, yearChar);
        final DayMonthYear result = new DayMonthYear(day, month, year);
        return result;
    }

    private void nextIsDash(ImapRequestLine request)
            throws DecodingException {
        final char next = request.consume();
        if (next != '-') {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Expected dash but was " + next);
        }
    }

    /**
     * Reads a "date-time" argument from the request.
     */
    public Date dateTime(ImapRequestLine request)
            throws DecodingException {
        char next = request.nextWordChar();
        String dateString;
        if (next == '"') {
            dateString = consumeQuoted(request);
        } else {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "DateTime values must be quoted.");
        }

        return DecoderUtils.decodeDateTime(dateString);
    }

    /**
     * Reads the next "word from the request, comprising all characters up to
     * the next SPACE. Characters are tested by the supplied CharacterValidator,
     * and an exception is thrown if invalid characters are encountered.
     */
    protected static String consumeWord(ImapRequestLine request,
            CharacterValidator validator) throws DecodingException {
        StringBuffer atom = new StringBuffer();

        char next = request.nextWordChar();
        while (!isWhitespace(next)) {
            if (validator.isValid(next)) {
                atom.append(next);
                request.consume();
            } else {
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Invalid character: '" + next + "'");
            }
            if (request.isConsumed()) break;
            next = request.nextChar();
        }
        return atom.toString();
    }

    private static boolean isWhitespace(char next) {
        return (next == ' ' || next == '\n' || next == '\r' || next == '\t');
    }

    /**
     * Reads an argument of type "literal" from the request, in the format: "{"
     * charCount "}" CRLF *CHAR8 Note before calling, the request should be
     * positioned so that nextChar is '{'. Leading whitespace is not skipped in
     * this method.
     * 
     * @param charset ,
     *            or null for <code>US-ASCII</code>
     */
    protected String consumeLiteralLine(final ImapRequestLine request,
            final Charset charset) throws DecodingException {
        if (charset == null) {
            return consumeLiteralLine(request, US_ASCII);
        } else {
            try {
            
                //TODO: This is not really RFC conform
                int size = consumeLiteralSize(request);
                byte[] data = new byte[size];
                
                int read = 0;
                while( read < size) {
                    data[read++] = (byte) request.nextChar();
                    request.consume();
                }
                return decode(charset, ByteBuffer.wrap(data));
                //IOUtils.copy(consumeLiteral(request),out);
            } catch (IOException e) {
                throw new DecodingException(HumanReadableText.BAD_IO_ENCODING, "Bad character encoding",  e);
            }
        }
    }

    protected int consumeLiteralSize(final ImapRequestLine request) throws DecodingException {
        // The 1st character must be '{'
        consumeChar(request, '{');

        StringBuffer digits = new StringBuffer();
        char next = request.nextChar();
        while (next != '}' && next != '+') {
            digits.append(next);
            request.consume();
            next = request.nextChar();
        }

        // If the number is *not* suffixed with a '+', we *are* using a
        // synchronized literal,
        // and we need to send command continuation request before reading
        // data.
        boolean synchronizedLiteral = true;
        // '+' indicates a non-synchronized literal (no command continuation
        // request)
        if (next == '+') {
            synchronizedLiteral = false;
            consumeChar(request, '+');
        }

        // Consume the '}' and the newline
        consumeChar(request, '}');

        if (synchronizedLiteral) {
            request.commandContinuationRequest();
        }

        final int size = Integer.parseInt(digits.toString());
        return size;
    }

    private String decode(final Charset charset, final ByteBuffer buffer)
            throws DecodingException {
        try {
            final String result = charset.newDecoder().onMalformedInput(
                    CodingErrorAction.REPORT).onUnmappableCharacter(
                    CodingErrorAction.REPORT).decode(buffer).toString();
            return result;

        } catch (IllegalStateException e) {
            throw new DecodingException(HumanReadableText.BAD_IO_ENCODING, "Bad character encoding", e);
        } catch (MalformedInputException e) {
            throw new DecodingException(HumanReadableText.BAD_IO_ENCODING, "Bad character encoding", e);
        } catch (UnmappableCharacterException e) {
            throw new DecodingException(HumanReadableText.BAD_IO_ENCODING, "Bad character encoding", e);
        } catch (CharacterCodingException e) {
            throw new DecodingException(HumanReadableText.BAD_IO_ENCODING, "Bad character encoding", e);
        }
    }


    /**
     * Consumes the next character in the request, checking that it matches the
     * expected one. This method should be used when the
     */
    protected void consumeChar(ImapRequestLine request, char expected)
            throws DecodingException {
        char consumed = request.consume();
        if (consumed != expected) {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, 
                    "Expected:'" + expected + "' found:'" + consumed + "'");
        }
    }

    /**
     * Reads a quoted string value from the request.
     */
    protected String consumeQuoted(ImapRequestLine request)
            throws DecodingException {
        return consumeQuoted(request, null);
    }

    /**
     * Reads a quoted string value from the request.
     */
    protected String consumeQuoted(ImapRequestLine request,
            Charset charset) throws DecodingException {
        if (charset == null) {
            return consumeQuoted(request, US_ASCII);
        } else {
            // The 1st character must be '"'
            consumeChar(request, '"');
            final QuotedStringDecoder decoder = new QuotedStringDecoder(charset);
            final String result = decoder.decode(request);
            consumeChar(request, '"');
            return result;
        }
    }

    /**
     * Reads a "flags" argument from the request.
     */
    public Flags flagList(ImapRequestLine request)
            throws DecodingException {
        Flags flags = new Flags();
        request.nextWordChar();
        consumeChar(request, '(');
        CharacterValidator validator = new NoopCharValidator();
        String nextWord = consumeWord(request, validator);
        while (!nextWord.endsWith(")")) {
            DecoderUtils.setFlag(nextWord, flags);
            nextWord = consumeWord(request, validator);
        }
        // Got the closing ")", may be attached to a word.
        if (nextWord.length() > 1) {
            int parenIndex = nextWord.indexOf(')');
            if (parenIndex > 0) {
                final String nextFlag = nextWord.substring(0, parenIndex);
                DecoderUtils.setFlag(nextFlag, flags);
            }
        }

        return flags;
    }

    /**
     * Reads an argument of type "number" from the request.
     */
    public long number(ImapRequestLine request) throws DecodingException {
        return readDigits(request, 0, 0, true);
    }

    private long readDigits(final ImapRequestLine request, int add,
            final long total, final boolean first) throws DecodingException {
        final char next;
        if (first) {
            next = request.nextWordChar();
        } else {
            request.consume();
            next = request.nextChar();
        }
        final long currentTotal = (10 * total) + add;
        switch (next) {
            case '0':
                return readDigits(request, 0, currentTotal, false);
            case '1':
                return readDigits(request, 1, currentTotal, false);
            case '2':
                return readDigits(request, 2, currentTotal, false);
            case '3':
                return readDigits(request, 3, currentTotal, false);
            case '4':
                return readDigits(request, 4, currentTotal, false);
            case '5':
                return readDigits(request, 5, currentTotal, false);
            case '6':
                return readDigits(request, 6, currentTotal, false);
            case '7':
                return readDigits(request, 7, currentTotal, false);
            case '8':
                return readDigits(request, 8, currentTotal, false);
            case '9':
                return readDigits(request, 9, currentTotal, false);
            case '.':
            case ' ':
            case '>':
            case '\r':
            case '\n':
            case '\t':
                return currentTotal;
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Expected a digit but was " + next);
        }
    }

    /**
     * Reads an argument of type "nznumber" (a non-zero number) (NOTE this isn't
     * strictly as per the spec, since the spec disallows numbers such as "0123"
     * as nzNumbers (although it's ok as a "number". I think the spec is a bit
     * shonky.)
     */
    public long nzNumber(ImapRequestLine request)
            throws DecodingException {
        long number = number(request);
        if (number == 0) {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Zero value not permitted.");
        }
        return number;
    }

    private static boolean isCHAR(char chr) {
        return (chr >= 0x01 && chr <= 0x7f);
    }

    protected static boolean isListWildcard(char chr) {
        return (chr == '*' || chr == '%');
    }

    private static boolean isQuotedSpecial(char chr) {
        return (chr == '"' || chr == '\\');
    }

    /**
     * Consumes the request up to and including the eno-of-line.
     * 
     * @param request
     *            The request
     * @throws DecodingException
     *             If characters are encountered before the endLine.
     */
    public void endLine(ImapRequestLine request) throws DecodingException {
        request.eol();
    }

    /**
     * Reads a "message set" argument, and parses into an IdSet. Currently only
     * supports a single range of values.
     */
    public IdRange[] parseIdRange(ImapRequestLine request)
            throws DecodingException {
        CharacterValidator validator = new MessageSetCharValidator();
        String nextWord = consumeWord(request, validator);

        int commaPos = nextWord.indexOf(',');
        if (commaPos == -1) {
            return new IdRange[] { parseRange(nextWord) };
        }

        ArrayList<IdRange> rangeList = new ArrayList<IdRange>();
        int pos = 0;
        while (commaPos != -1) {
            String range = nextWord.substring(pos, commaPos);
            IdRange set = parseRange(range);
            rangeList.add(set);

            pos = commaPos + 1;
            commaPos = nextWord.indexOf(',', pos);
        }
        String range = nextWord.substring(pos);
        rangeList.add(parseRange(range));
        
        // merge the ranges to minimize the needed queries.
        // See IMAP-211
        List<IdRange> merged = IdRange.mergeRanges(rangeList);
        return (IdRange[]) merged.toArray(new IdRange[merged.size()]);
    }

    
    /**
     * Parse a range which use a ":" as delimiter
     * 
     * @param range
     * @return idRange
     * @throws DecodingException
     */
    private IdRange parseRange(String range) throws DecodingException {
        int pos = range.indexOf(':');
        try {
            if (pos == -1) {
                long value = parseUnsignedInteger(range);
                return new IdRange(value);
            } else {
                // Make sure we detect the low and high value
                // See https://issues.apache.org/jira/browse/IMAP-212
                long val1 = parseUnsignedInteger(range.substring(0, pos));
                long val2 = parseUnsignedInteger(range.substring(pos + 1));
                if (val1 <= val2) {
                    return new IdRange(val1, val2);
                } else if(val1 == Long.MAX_VALUE) {
                    return new IdRange(Long.MIN_VALUE, val2);
                } else {
                    return new IdRange(val2, val1);
                }
            }
        } catch (NumberFormatException e) {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Invalid message set.", e);
        }
    }

    private long  parseUnsignedInteger(String value) throws DecodingException{
        if (value.length() == 1 && value.charAt(0) == '*') {
            return Long.MAX_VALUE;
        } else {
            long number = Long.parseLong(value);
            if (number < ImapConstants.MIN_NZ_NUMBER || number > ImapConstants.MAX_NZ_NUMBER) throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Invalid message set. Numbers must be unsigned 32-bit Integers");
            return number;
        
        }
    }

    /**
     * Provides the ability to ensure characters are part of a permitted set.
     */
    public interface CharacterValidator {
        /**
         * Validates the supplied character.
         * 
         * @param chr
         *            The character to validate.
         * @return <code>true</code> if chr is valid, <code>false</code> if
         *         not.
         */
        boolean isValid(char chr);
    }

    public static class NoopCharValidator implements CharacterValidator {
        public boolean isValid(char chr) {
            return true;
        }
    }

    public static class ATOM_CHARValidator implements CharacterValidator {
        public boolean isValid(char chr) {
            return (isCHAR(chr) && !isAtomSpecial(chr) && !isListWildcard(chr) && !isQuotedSpecial(chr));
        }

        private boolean isAtomSpecial(char chr) {
            return (chr == '(' || chr == ')' || chr == '{' || chr == ' ' || chr == Character.CONTROL);
        }
    }

    public static class TagCharValidator extends ATOM_CHARValidator {
        public boolean isValid(char chr) {
            if (chr == '+')
                return false;
            return super.isValid(chr);
        }
    }

    public static class MessageSetCharValidator implements CharacterValidator {
        public boolean isValid(char chr) {
            return (isDigit(chr) || chr == ':' || chr == '*' || chr == ',');
        }

        private boolean isDigit(char chr) {
            return '0' <= chr && chr <= '9';
        }
    }

    /**
     * Decodes contents of a quoted string. Charset aware. One shot, not thread
     * safe.
     */
    private static class QuotedStringDecoder {
        /** Decoder suitable for charset */
        private final CharsetDecoder decoder;

        /** byte buffer will be filled then flushed to character buffer */
        private final ByteBuffer buffer;

        /** character buffer may be dynamically resized */
        CharBuffer charBuffer;

        public QuotedStringDecoder(Charset charset) {
            decoder = charset.newDecoder();
            buffer = ByteBuffer.allocate(QUOTED_BUFFER_INITIAL_CAPACITY);
            charBuffer = CharBuffer.allocate(QUOTED_BUFFER_INITIAL_CAPACITY);
        }

        public String decode(ImapRequestLine request)
                throws DecodingException {
            try {
                decoder.reset();
                char next = request.nextChar();
                while (next != '"') {
                    // fill up byte buffer before decoding
                    if (!buffer.hasRemaining()) {
                        decodeByteBufferToCharacterBuffer(false);
                    }
                    if (next == '\\') {
                        request.consume();
                        next = request.nextChar();
                        if (!isQuotedSpecial(next)) {
                            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, 
                                    "Invalid escaped character in quote: '"
                                            + next + "'");
                        }
                    }
                    // TODO: nextChar does not report accurate chars so safe to
                    // cast to byte
                    buffer.put((byte) next);
                    request.consume();
                    next = request.nextChar();
                }
                completeDecoding();
                final String result = charBuffer.toString();
                return result;

            } catch (IllegalStateException e) {
                throw new DecodingException(HumanReadableText.BAD_IO_ENCODING, "Bad character encoding", e);
            }
        }

        private void completeDecoding() throws DecodingException {
            decodeByteBufferToCharacterBuffer(true);
            flush();
            charBuffer.flip();
        }

        private void flush() throws DecodingException {
            final CoderResult coderResult = decoder.flush(charBuffer);
            if (coderResult.isOverflow()) {
                upsizeCharBuffer();
                flush();
            } else if (coderResult.isError()) {
                throw new DecodingException(HumanReadableText.BAD_IO_ENCODING, "Bad character encoding");
            }
        }

        /**
         * Decodes contents of the byte buffer to the character buffer. The
         * character buffer will be replaced by a larger one if required.
         * 
         * @param endOfInput
         *            is the input ended
         */
        private CoderResult decodeByteBufferToCharacterBuffer(
                final boolean endOfInput) throws DecodingException {
            buffer.flip();
            return decodeMoreBytesToCharacterBuffer(endOfInput);
        }

        private CoderResult decodeMoreBytesToCharacterBuffer(
                final boolean endOfInput) throws DecodingException {
            final CoderResult coderResult = decoder.decode(buffer, charBuffer,
                    endOfInput);
            if (coderResult.isOverflow()) {
                upsizeCharBuffer();
                return decodeMoreBytesToCharacterBuffer(endOfInput);
            } else if (coderResult.isError()) {
                throw new DecodingException(HumanReadableText.BAD_IO_ENCODING, "Bad character encoding");
            } else if (coderResult.isUnderflow()) {
                buffer.clear();
            }
            return coderResult;
        }

        /**
         * Increases the size of the character buffer.
         */
        private void upsizeCharBuffer() {
            final int oldCapacity = charBuffer.capacity();
            CharBuffer oldBuffer = charBuffer;
            charBuffer = CharBuffer.allocate(oldCapacity
                    + QUOTED_BUFFER_INITIAL_CAPACITY);
            oldBuffer.flip();
            charBuffer.put(oldBuffer);
        }
    }
    
}
