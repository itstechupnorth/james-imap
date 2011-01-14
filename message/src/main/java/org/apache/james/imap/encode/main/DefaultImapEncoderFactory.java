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

import org.apache.james.imap.api.display.Localizer;
import org.apache.james.imap.encode.CapabilityResponseEncoder;
import org.apache.james.imap.encode.ContinuationRequestEncoder;
import org.apache.james.imap.encode.ContinuationResponseEncoder;
import org.apache.james.imap.encode.ExistsResponseEncoder;
import org.apache.james.imap.encode.ExpungeResponseEncoder;
import org.apache.james.imap.encode.FetchResponseEncoder;
import org.apache.james.imap.encode.FlagsResponseEncoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.ImapEncoderFactory;
import org.apache.james.imap.encode.LSubResponseEncoder;
import org.apache.james.imap.encode.ListResponseEncoder;
import org.apache.james.imap.encode.NamespaceResponseEncoder;
import org.apache.james.imap.encode.RecentResponseEncoder;
import org.apache.james.imap.encode.MailboxStatusResponseEncoder;
import org.apache.james.imap.encode.SearchResponseEncoder;
import org.apache.james.imap.encode.StatusResponseEncoder;
import org.apache.james.imap.encode.base.EndImapEncoder;

/**
 * TODO: perhaps a POJO would be better
 */
public class DefaultImapEncoderFactory implements ImapEncoderFactory {

    /**
     * Builds the default encoder
     * @param localizer not null
     * @param neverAddBodyStructureExtensions true to activate a workaround for broken clients who
     * cannot parse BODYSTRUCTURE extensions, false to fully support RFC3501
     * @return not null
     */
    public static final ImapEncoder createDefaultEncoder(final Localizer localizer, final boolean neverAddBodyStructureExtensions) {
        final EndImapEncoder endImapEncoder = new EndImapEncoder();
        final ContinuationRequestEncoder contReqEncoder = new ContinuationRequestEncoder(endImapEncoder);
        final NamespaceResponseEncoder namespaceEncoder = new NamespaceResponseEncoder(contReqEncoder);
        final StatusResponseEncoder statusResponseEncoder = new StatusResponseEncoder(
                namespaceEncoder, localizer);
        final RecentResponseEncoder recentResponseEncoder = new RecentResponseEncoder(
                statusResponseEncoder);
        final FetchResponseEncoder fetchResponseEncoder = new FetchResponseEncoder(
                recentResponseEncoder, neverAddBodyStructureExtensions);
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
        final ContinuationResponseEncoder continuationResponseEncoder = new ContinuationResponseEncoder(
                capabilityResponseEncoder, localizer);
        return continuationResponseEncoder;
    }

    private final Localizer localizer;
    private final boolean neverAddBodyStructureExtensions;
    
    public DefaultImapEncoderFactory() {
        this(new DefaultLocalizer(), false);
    }
    
    /**
     * Constructs the default factory for encoders
     * @param localizer not null
     * @param neverAddBodyStructureExtensions true to activate a workaround for broken clients who
     * cannot parse BODYSTRUCTURE extensions, false to fully support RFC3501
     */
    public DefaultImapEncoderFactory(final Localizer localizer, boolean neverAddBodyStructureExtensions) {
        super();
        this.localizer = localizer;
        this.neverAddBodyStructureExtensions = neverAddBodyStructureExtensions;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.encode.ImapEncoderFactory#buildImapEncoder()
     */
    public ImapEncoder buildImapEncoder() {
        return createDefaultEncoder(localizer, neverAddBodyStructureExtensions);
    }

}
