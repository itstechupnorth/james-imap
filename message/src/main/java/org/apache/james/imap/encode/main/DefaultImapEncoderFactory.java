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

import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ImapEncoderFactory;
import org.apache.james.imap.encode.base.EndImapEncoder;
import org.apache.james.imap.encode.imap4rev1.ExistsResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.ExpungeResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.FetchResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.FlagsResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.RecentResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.StatusResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.legacy.CapabilityResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.server.LSubResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.server.ListResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.server.STATUSResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.server.SearchResponseEncoder;
import org.apache.james.imap.encode.imap4rev1.status.UntaggedNoResponseEncoder;

/**
 * TODO: perhaps a POJO would be better
 */
public class DefaultImapEncoderFactory implements ImapEncoderFactory {

    public static final ImapEncoder createDefaultEncoder() {
        final EndImapEncoder endImapEncoder = new EndImapEncoder();
        final StatusResponseEncoder statusResponseEncoder = new StatusResponseEncoder(
                endImapEncoder);
        final UntaggedNoResponseEncoder untaggedNoResponseEncoder = new UntaggedNoResponseEncoder(
                statusResponseEncoder);
        final RecentResponseEncoder recentResponseEncoder = new RecentResponseEncoder(
                untaggedNoResponseEncoder);
        final FetchResponseEncoder fetchResponseEncoder = new FetchResponseEncoder(
                recentResponseEncoder);
        final ExpungeResponseEncoder expungeResponseEncoder = new ExpungeResponseEncoder(
                fetchResponseEncoder);
        final ExistsResponseEncoder existsResponseEncoder = new ExistsResponseEncoder(
                expungeResponseEncoder);
        final STATUSResponseEncoder statusCommandResponseEncoder = new STATUSResponseEncoder(
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