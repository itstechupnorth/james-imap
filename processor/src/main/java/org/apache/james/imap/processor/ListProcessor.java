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

package org.apache.james.imap.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.message.request.ListRequest;
import org.apache.james.imap.message.response.ListResponse;
import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxMetaData;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxQuery;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MailboxMetaData.Children;
import org.apache.james.mailbox.util.SimpleMailboxMetaData;

public class ListProcessor extends AbstractMailboxProcessor {

    public ListProcessor(final ImapProcessor next,
            final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof ListRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final ListRequest request = (ListRequest) message;
        final String baseReferenceName = request.getBaseReferenceName();
        final String mailboxPatternString = request.getMailboxPattern();
        doProcess(baseReferenceName, mailboxPatternString, session, tag,
                command, responder);
    }

    protected ImapResponseMessage createResponse(boolean noInferior,
            boolean noSelect, boolean marked, boolean unmarked,
            boolean hasChildren, boolean hasNoChildren, String mailboxName, char delimiter) {
        return new ListResponse(noInferior, noSelect, marked, unmarked,
                hasChildren, hasNoChildren, mailboxName, delimiter);
    }

    /**
     * (from rfc3501)
     * The LIST command returns a subset of names from the complete set
     * of all names available to the client.  Zero or more untagged LIST
     * replies are returned, containing the name attributes, hierarchy
     * delimiter, and name; see the description of the LIST reply for
     * more detail.
     * ...
     * An empty ("" string) mailbox name argument is a special request to
     * return the hierarchy delimiter and the root name of the name given
     * in the reference.  The value returned as the root MAY be the empty
     * string if the reference is non-rooted or is an empty string.
     * 
     * @param referenceName
     * @param mailboxName
     * @param session
     * @param tag
     * @param command
     * @param responder
     */
    protected final void doProcess(final String referenceName,
            final String mailboxName, final ImapSession session,
            final String tag, ImapCommand command, final Responder responder) {
        try {
            // Should the namespace section be returned or not?
            final boolean isRelative;

            final List<MailboxMetaData> results;

            final String user = ImapSessionUtils.getUserName(session);
            final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);

            if (mailboxName.length() == 0) {
                // An empty mailboxName signifies a request for the hierarchy
                // delimiter and root name of the referenceName argument

                String referenceRoot;
                if (referenceName.startsWith(ImapConstants.NAMESPACE_PREFIX)) {
                    // A qualified reference name - get the root element
                    isRelative = false;
                    int firstDelimiter = referenceName.indexOf(mailboxSession.getPathDelimiter());
                    if (firstDelimiter == -1) {
                        referenceRoot = referenceName;
                    }
                    else {
                        referenceRoot = referenceName.substring(0, firstDelimiter);
                    }
                }
                else {
                    // A relative reference name, return "" to indicate it is non-rooted
                    referenceRoot = "";
                    isRelative = true;
                }
                // Get the mailbox for the reference name.
                MailboxPath rootPath = new MailboxPath(referenceRoot, "", "");
                results = new ArrayList<MailboxMetaData>(1);
                results.add(SimpleMailboxMetaData.createNoSelect(rootPath, mailboxSession.getPathDelimiter()));
            }
            else {
                // If the mailboxPattern is fully qualified, ignore the reference name.
                String finalReferencename = referenceName;
                if (mailboxName.charAt(0) == ImapConstants.NAMESPACE_PREFIX_CHAR) {
                    finalReferencename = "";
                }
                // Is the interpreted (combined) pattern relative?
                isRelative = ((finalReferencename + mailboxName).charAt(0) != ImapConstants.NAMESPACE_PREFIX_CHAR);

                MailboxPath basePath = null;
                if (isRelative) {
                    basePath = new MailboxPath(MailboxConstants.USER_NAMESPACE, user, finalReferencename);
                }
                else {
                    basePath = buildFullPath(session, finalReferencename);
                }

                results = getMailboxManager().search(new MailboxQuery(basePath, mailboxName, '*', '%', mailboxSession.getPathDelimiter()),
                                                     mailboxSession);
            }

            for (final MailboxMetaData metaData: results) {
                processResult(responder, isRelative, metaData);
            }

            okComplete(command, tag, responder);
        } catch (MailboxException e) {
            no(command, tag, responder, HumanReadableText.SEARCH_FAILED);
        }
    }

    void processResult(final Responder responder, final boolean relative, final MailboxMetaData listResult) {
        final char delimiter = listResult.getHierarchyDelimiter();
        final String mailboxName = mailboxName(relative, listResult.getPath(), delimiter);

        final Children inferiors = listResult.inferiors();
        final boolean noInferior = MailboxMetaData.Children.NO_INFERIORS.equals(inferiors);
        final boolean hasChildren = MailboxMetaData.Children.HAS_CHILDREN.equals(inferiors);
        final boolean hasNoChildren = MailboxMetaData.Children.HAS_NO_CHILDREN.equals(inferiors);
        boolean noSelect = false;
        boolean marked = false;
        boolean unmarked = false;
        switch (listResult.getSelectability()) {
            case MARKED:
                marked = true;
                break;
            case UNMARKED:
                unmarked = true;
                break;
            case NOSELECT:
                noSelect = true;
                break;
        }
        responder.respond(createResponse(noInferior, noSelect, marked,
                unmarked, hasChildren, hasNoChildren, mailboxName, delimiter));
    }

}
