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

package org.apache.james.imap.encode.main;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.display.Locales;
import org.apache.james.imap.api.display.Localizer;

/**
 * Just uses {@link HumanReadableText#getDefaultValue()}. This implementation is
 * independent of user, client and system locale.
 */
public class DefaultLocalizer implements Localizer {

    /**
     * @see Localizer#localize(HumanReadableText, Locales)
     */
    public String localize(HumanReadableText text, Locales locales) {
        final String result;
        if (text == null) {
            result = null;
        } else {
            result = text.getDefaultValue();
        }
        return result;
    }

}
