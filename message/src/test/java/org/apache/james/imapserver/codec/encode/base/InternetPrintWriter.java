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



package org.apache.james.imapserver.codec.encode.base;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Writes to a wrapped Writer class, ensuring that all line separators are '\r\n', regardless
 * of platform.
 */
public class InternetPrintWriter
    extends PrintWriter {

    /**
     * The line separator to use.
     */
    private static String lineSeparator = "\r\n";

    /**
     * Whether the Writer autoflushes on line feeds
     */
    private final boolean autoFlush;

    /**
     * Constructor that takes a writer to wrap.
     *
     * @param out the wrapped Writer
     */
    public InternetPrintWriter (Writer out) {
        super (out);
        autoFlush = false;
    }

    /**
     * Constructor that takes a writer to wrap.
     *
     * @param out the wrapped Writer
     * @param autoFlush whether to flush after each print call
     */
    public InternetPrintWriter (Writer out, boolean autoFlush) {
        super (out, autoFlush);
        this.autoFlush = autoFlush;
    }

    /**
     * Constructor that takes a stream to wrap.
     *
     * @param out the wrapped OutputStream
     */
    public InternetPrintWriter (OutputStream out) {
        super (out);
        autoFlush = false;
    }

    /**
     * Constructor that takes a stream to wrap.
     *
     * @param out the wrapped OutputStream
     * @param autoFlush whether to flush after each print call
     */
    public InternetPrintWriter (OutputStream out, boolean autoFlush) {
        super (out, autoFlush);
        this.autoFlush = autoFlush;
    }

    /**
     * Print a line separator.
     */
    public void println () {
        synchronized (lock) {
            write(lineSeparator);
            if (autoFlush) {
                flush();
            }
        }
    }

    /**
     * Print a boolean followed by a line separator.
     *
     * @param x the boolean to print
     */
    public void println(boolean x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    /**
     * Print a char followed by a line separator.
     *
     * @param x the char to print
     */
    public void println(char x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    /**
     * Print a int followed by a line separator.
     *
     * @param x the int to print
     */
    public void println (int x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    /**
     * Print a long followed by a line separator.
     *
     * @param x the long to print
     */
    public void println (long x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    /**
     * Print a float followed by a line separator.
     *
     * @param x the float to print
     */
    public void println (float x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    /**
     * Print a double followed by a line separator.
     *
     * @param x the double to print
     */
    public void println (double x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    /**
     * Print a character array followed by a line separator.
     *
     * @param x the character array to print
     */
    public void println (char[] x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    /**
     * Print a String followed by a line separator.
     *
     * @param x the String to print
     */
    public void println (String x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    /**
     * Print an Object followed by a line separator.
     *
     * @param x the Object to print
     */
    public void println (Object x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }
}
