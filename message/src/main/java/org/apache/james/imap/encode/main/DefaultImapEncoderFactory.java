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

import org.apache.james.imap.encode.CapabilityResponseEncoder;
import org.apache.james.imap.encode.ExistsResponseEncoder;
import org.apache.james.imap.encode.ExpungeResponseEncoder;
import org.apache.james.imap.encode.FetchResponseEncoder;
import org.apache.james.imap.encode.FlagsResponseEncoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ImapEncoderFactory;
import org.apache.james.imap.encode.LSubResponseEncoder;
import org.apache.james.imap.encode.ListResponseEncoder;
import org.apache.james.imap.encode.RecentResponseEncoder;
import org.apache.james.imap.encode.MailboxStatusResponseEncoder;
import org.apache.james.imap.encode.SearchResponseEncoder;
import org.apache.james.imap.encode.StatusResponseEncoder;
import org.apache.james.imap.encode.base.EndImapEncoder;

/**
 * TODO: perhaps a POJO would be better
 */
public class DefaultImapEncoderFactory implements ImapEncoderFactory {

    public static final ImapEncoder createDefaultEncoder() {
        final EndImapEncoder endImapEncoder = new EndImapEncoder();
        final StatusResponseEncoder statusResponseEncoder = new StatusResponseEncoder(
                endImapEncoder);
        final RecentResponseEncoder recentResponseEncoder = new RecentResponseEncoder(
                statusResponseEncoder);
        final FetchResponseEncoder fetchResponseEncoder = new FetchResponseEncoder(
                recentResponseEncoder);
        final ExpungeResponseEncoder expungeResponseEncoder = new ExpungeResponseEncoder(
                fetchResponseEncoder);
        final ExistsResponseEncoder existsResponseEncoder = new ExistsResponseEncoder(
                expungeResponseEncoder);
        final MailboxStatusResponseEncoder statusCommandResponseEncoder = new MailboxStatusResponseEncoder(
                existsResponseEncoder);
        final SearchResponseEncoder searchResponseEncoder = new SearchResponseEncoder(
                statusCommandResponseEncoder);
        final LSubResponseEncoder lsubResponseEncoder = new LSubResponseEncoder(
                searchResponseEncoder);
        final ListResponseEncoder listResponseEncoder = new ListResponseEncoder(
                lsubResponseEncoder);
        final FlagsResponseEncoder flagsResponseEncoder = new FlagsResponseEncoder(
                listResponseEncoder);
        final CapabilityResponseEncoder capabilityResponseEncoder = new CapabilityResponseEncoder(
                flagsResponseEncoder);
        return capabilityResponseEncoder;
    }

    public ImapEncoder buildImapEncoder() {
        return createDefaultEncoder();
    }

}
