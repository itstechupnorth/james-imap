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
package org.apache.james.imap.api.display;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility class which can be used to get a list of supported Charsets
 * 
 *
 */
public class CharsetUtil {

    private static Set<String> charsetNames;
    private static Set<Charset> charsets;


    // build the sets which holds the charsets and names
    static {
        Set<String>cNames = new HashSet<String>();
        Set<Charset> sets = new HashSet<Charset>();

        for (final Iterator<Charset> it = Charset.availableCharsets().values().iterator(); it.hasNext();) {
            final Charset charset = it.next();
            final Set<String> aliases = charset.aliases();
            cNames.add(charset.name());
            cNames.addAll(aliases);
            sets.add(charset);

        }
        charsetNames = Collections.unmodifiableSet(cNames);
        charsets = Collections.unmodifiableSet(sets);
    }

    /**
     * Return an unmodifiable {@link Set} which holds the names (and aliases) of all supported Charsets
     * 
     * @return supportedCharsetNames
     */
    public final static Set<String> getAvailableCharsetNames() {
        return charsetNames;
    }
    
    /**
     * Return an unmodifiable {@link Set} which holds all supported Charsets
     * 
     * @return supportedCharsets
     */
    public final static Set<Charset> getAvailableCharsets() {
        return charsets;
    }
    
}
