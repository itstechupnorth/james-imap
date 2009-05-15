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

package org.apache.james.imap.decode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.james.imap.api.display.HumanReadableText;

/**
 * Wraps the client input reader with a bunch of convenience methods, allowing
 * lookahead=1 on the underlying character stream. TODO need to look at
 * encoding, and whether we should be wrapping an InputStream instead.
 * 
 * @version $Revision: 109034 $
 */
public class ImapRequestLineReader {
    private InputStream input;

    private OutputStream output;

    private boolean nextSeen = false;

    private char nextChar; // unknown

    public ImapRequestLineReader(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    /**
     * Reads the next regular, non-space character in the current line. Spaces
     * are skipped over, but end-of-line characters will cause a
     * {@link DecodingException} to be thrown. This method will continue to
     * return the same character until the {@link #consume()} method is called.
     * 
     * @return The next non-space character.
     * @throws DecodingException
     *             If the end-of-line or end-of-stream is reached.
     */
    public char nextWordChar() throws DecodingException {
        char next = nextChar();
        while (next == ' ') {
            consume();
            next = nextChar();
        }

        if (next == '\r' || next == '\n') {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Missing argument.");
        }

        return next;
    }

    /**
     * Reads the next character in the current line. This method will continue
     * to return the same character until the {@link #consume()} method is
     * called.
     * 
     * @return The next character TODO: character encoding is variable and
     *         cannot be determine at the token level; this char is not accurate
     *         reported; should be an octet
     * @throws DecodingException
     *             If the end-of-stream is reached.
     */
    public char nextChar() throws DecodingException {
        if (!nextSeen) {
            int next = -1;

            try {
                next = input.read();
            } catch (IOException e) {
                throw new DecodingException(HumanReadableText.SOCKET_IO_FAILURE, 
                        "Error reading from stream.", e);
            }
            if (next == -1) {
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, 
                        "Unexpected end of stream.");
            }

            nextSeen = true;
            nextChar = (char) next;
        }
        return nextChar;
    }

    /**
     * Moves the request line reader to end of the line, checking that no
     * non-space character are found.
     * 
     * @throws DecodingException
     *             If more non-space tokens are found in this line, or the
     *             end-of-file is reached.
     */
    public void eol() throws DecodingException {
        char next = nextChar();

        // Ignore trailing spaces.
        while (next == ' ') {
            consume();
            next = nextChar();
        }

        // handle DOS and unix end-of-lines
        if (next == '\r') {
            consume();
            next = nextChar();
        }

        // Check if we found extra characters.
        if (next != '\n') {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, 
                    "Expected end-of-line, found '" + (char) next + "'.");
        }
    }

    /**
     * Consumes the current character in the reader, so that subsequent calls to
     * the request will provide a new character. This method does *not* read the
     * new character, or check if such a character exists. If no current
     * character has been seen, the method moves to the next character, consumes
     * it, and moves on to the subsequent one.
     * 
     * @throws DecodingException
     *             if a the current character can't be obtained (eg we're at
     *             end-of-file).
     */
    public char consume() throws DecodingException {
        char current = nextChar();
        nextSeen = false;
        nextChar = 0;
        return current;
    }

    /**
     * Reads and consumes a number of characters from the underlying reader,
     * filling the char array provided. TODO: remove unnecessary copying of
     * bits; line reader should maintain an internal ByteBuffer;
     * 
     * @param holder
     *            A char array which will be filled with chars read from the
     *            underlying reader.
     * @throws DecodingException
     *             If a char can't be read into each array element.
     */
    public void read(byte[] holder) throws DecodingException {
        int readTotal = 0;
        try {
            while (readTotal < holder.length) {
                int count = 0;
                count = input
                        .read(holder, readTotal, holder.length - readTotal);
                if (count == -1) {
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unexpected end of stream.");
                }
                readTotal += count;
            }
            // Unset the next char.
            nextSeen = false;
            nextChar = 0;
        } catch (IOException e) {
            throw new DecodingException(HumanReadableText.SOCKET_IO_FAILURE, 
                    "Error reading from stream.", e);
        }

    }

    /**
     * Sends a server command continuation request '+' back to the client,
     * requesting more data to be sent.
     */
    public void commandContinuationRequest() throws DecodingException {
        try {
            output.write('+');
            output.write('\r');
            output.write('\n');
            output.flush();
        } catch (IOException e) {
            throw new DecodingException(
                    HumanReadableText.SOCKET_IO_FAILURE, 
                    "Unexpected exception in sending command continuation request.",
                    e);
        }
    }

    public void consumeLine() throws DecodingException {
        char next = nextChar();
        while (next != '\n') {
            consume();
            next = nextChar();
        }
        consume();
    }
}
