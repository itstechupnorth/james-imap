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
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxMetaData;
import org.apache.james.imap.mailbox.MailboxQuery;
import org.apache.james.imap.mailbox.MailboxMetaData.Children;
import org.apache.james.imap.mailbox.util.SimpleMailboxMetaData;
import org.apache.james.imap.message.request.ListRequest;
import org.apache.james.imap.message.response.ListResponse;
import org.apache.james.imap.processor.base.ImapSessionUtils;

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
            boolean hasChildren, boolean hasNoChildren, String hierarchyDelimiter, String mailboxName) {
        return new ListResponse(noInferior, noSelect, marked, unmarked,
                hasChildren, hasNoChildren, hierarchyDelimiter, mailboxName);
    }

    protected final void doProcess(final String baseReferenceName,
            final String mailboxPattern, final ImapSession session,
            final String tag, ImapCommand command, final Responder responder) {
        try {
            String referenceName = baseReferenceName;
            // Should the #user.userName section be removed from names returned?
            final boolean removeUserPrefix;

            final List<MailboxMetaData> results;

            final String user = ImapSessionUtils.getUserName(session);
            final String personalNamespace = ImapConstants.USER_NAMESPACE
                    + ImapConstants.HIERARCHY_DELIMITER_CHAR + user;

            if (mailboxPattern.length() == 0) {
                // An empty mailboxPattern signifies a request for the hierarchy
                // delimiter
                // and root name of the referenceName argument

                String referenceRoot;
                if (referenceName.startsWith(ImapConstants.NAMESPACE_PREFIX)) {
                    // A qualified reference name - get the first element,
                    // and don't remove the user prefix
                    removeUserPrefix = false;
                    int firstDelimiter = referenceName
                            .indexOf(ImapConstants.HIERARCHY_DELIMITER_CHAR);
                    if (firstDelimiter == -1) {
                        referenceRoot = referenceName;
                    } else {
                        referenceRoot = referenceName.substring(0,
                                firstDelimiter);
                    }
                } else {
                    // A relative reference name - need to remove user prefix
                    // from
                    // results.
                    referenceRoot = "";
                    removeUserPrefix = true;

                }

                // Get the mailbox for the reference name.
                results = new ArrayList<MailboxMetaData>(1);
                results.add(SimpleMailboxMetaData.createNoSelect(referenceRoot,
                        ImapConstants.HIERARCHY_DELIMITER));
            } else {

                // If the mailboxPattern is fully qualified, ignore the
                // reference name.
                if (mailboxPattern.charAt(0) == ImapConstants.NAMESPACE_PREFIX_CHAR) {
                    referenceName = "";
                }

                // If the search pattern is relative, need to remove user prefix
                // from results.
                removeUserPrefix = ((referenceName + mailboxPattern).charAt(0) != ImapConstants.NAMESPACE_PREFIX_CHAR);

                if (removeUserPrefix) {
                    referenceName = personalNamespace + "." + referenceName;
                }

                results = doList(session, referenceName, mailboxPattern);
            }

            final int prefixLength = personalNamespace.length();

            for (final MailboxMetaData metaData: results) {
                processResult(responder, removeUserPrefix, prefixLength, metaData);
            }

            okComplete(command, tag, responder);
        } catch (MailboxException e) {
            no(command, tag, responder, e, session);
        }
    }

    void processResult(final Responder responder,
            final boolean removeUserPrefix, int prefixLength,
            final MailboxMetaData listResult) {
        final String delimiter = listResult.getHierarchyDelimiter();
        final String mailboxName = mailboxName(removeUserPrefix, prefixLength,
                listResult);

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
                unmarked, hasChildren, hasNoChildren, delimiter, mailboxName));
    }

    private String mailboxName(final boolean removeUserPrefix,
            int prefixLength, final MailboxMetaData listResult) {
        final String mailboxName;
        final String name = listResult.getName();
        if (removeUserPrefix) {
            if (name.length() <= prefixLength) {
                mailboxName = "";
            } else {
                mailboxName = name.substring(prefixLength + 1);
            }
        } else {
            mailboxName = name;
        }
        return mailboxName;
    }

    protected final List<MailboxMetaData> doList(ImapSession session, String base,
            String pattern) throws MailboxException {
        final MailboxManager mailboxManager = getMailboxManager();
        final List<MailboxMetaData> results = mailboxManager.search(new MailboxQuery(
                base, pattern, '*', '%'), ImapSessionUtils.getMailboxSession(session));
        return results;
    }
}
