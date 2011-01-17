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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.mail.Flags.Flag;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapSession;
import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.request.DayMonthYear;
import org.apache.james.imap.api.message.request.ImapRequest;
import org.apache.james.imap.api.message.request.SearchKey;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.imap.message.request.SearchRequest;
import org.apache.james.imap.message.response.SearchResponse;
import org.apache.james.imap.processor.base.MessageRangeException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.SearchQuery.Criterion;

public class SearchProcessor extends AbstractMailboxProcessor {

    public SearchProcessor(final ImapProcessor next, final MailboxManager mailboxManager,
            final StatusResponseFactory factory) {
        super(next, mailboxManager, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof SearchRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        try {
            final SearchRequest request = (SearchRequest) message;
            final SearchKey searchKey = request.getSearchKey();
            final boolean useUids = request.isUseUids();
            final MessageManager mailbox = getSelectedMailbox(session);

            final SearchQuery query = toQuery(searchKey, session);

            final Collection<Long> results = findIds(useUids, session, mailbox, query);
            final long[] ids = toArray(results);

            final SearchResponse response = new SearchResponse(ids);
            responder.respond(response);
            boolean omitExpunged = (!useUids);
            unsolicitedResponses(session, responder, omitExpunged, useUids);
            okComplete(command, tag, responder);
        } catch (MessageRangeException e) {
            taggedBad(command, tag, responder, HumanReadableText.INVALID_MESSAGESET);
        } catch (MailboxException e) {
            no(command, tag, responder, HumanReadableText.SEARCH_FAILED);
        }
    }

    private long[] toArray(final Collection<Long> results) {
        final Iterator<Long> it = results.iterator();
        final int length = results.size();
        long[] ids = new long[length];
        for (int i = 0; i < length; i++) {
            ids[i] = ((Long) it.next()).longValue();
        }
        return ids;
    }

    private Collection<Long> findIds(final boolean useUids,
            final ImapSession session, MessageManager mailbox, final SearchQuery query)
            throws MailboxException, MessageRangeException {
    	
        final Iterator<Long> it = mailbox.search(query, ImapSessionUtils
                .getMailboxSession(session));

        final Collection<Long> results = new TreeSet<Long>();
  
        while (it.hasNext()) {
            final long uid = it.next();
            final Long number;
            if (useUids) {
                number = new Long(uid);
            } else {
                final int msn = session.getSelected().msn(uid);
                number = new Long(msn);
            }
            if (number == SelectedMailbox.NO_SUCH_MESSAGE == false) results.add(number);
        }
        return results;
    }

    private SearchQuery toQuery(final SearchKey key, final ImapSession session) throws MessageRangeException {
        final SearchQuery result = new SearchQuery();
        final SelectedMailbox selected = session.getSelected();
        if (selected != null) {
            result.addRecentMessageUids(selected.getRecent());
        }
        final SearchQuery.Criterion criterion = toCriterion(key, session);
        result.andCriteria(criterion);
        return result;
    }

    private SearchQuery.Criterion toCriterion(final SearchKey key,
            final ImapSession session) throws MessageRangeException {
        final int type = key.getType();
        final DayMonthYear date = key.getDate();
        switch (type) {
            case SearchKey.TYPE_ALL:
                return SearchQuery.all();
            case SearchKey.TYPE_AND:
                return and(key.getKeys(), session);
            case SearchKey.TYPE_ANSWERED:
                return SearchQuery.flagIsSet(Flag.ANSWERED);
            case SearchKey.TYPE_BCC:
                return SearchQuery.headerContains(ImapConstants.RFC822_BCC, key
                        .getValue());
            case SearchKey.TYPE_BEFORE:
                return SearchQuery.internalDateBefore(date.getDay(), date
                        .getMonth(), date.getYear());
            case SearchKey.TYPE_BODY:
                return SearchQuery.bodyContains(key.getValue());
            case SearchKey.TYPE_CC:
                return SearchQuery.headerContains(ImapConstants.RFC822_CC, key
                        .getValue());
            case SearchKey.TYPE_DELETED:
                return SearchQuery.flagIsSet(Flag.DELETED);
            case SearchKey.TYPE_DRAFT:
                return SearchQuery.flagIsSet(Flag.DRAFT);
            case SearchKey.TYPE_FLAGGED:
                return SearchQuery.flagIsSet(Flag.FLAGGED);
            case SearchKey.TYPE_FROM:
                return SearchQuery.headerContains(ImapConstants.RFC822_FROM,
                        key.getValue());
            case SearchKey.TYPE_HEADER:
                return SearchQuery
                        .headerContains(key.getName(), key.getValue());
            case SearchKey.TYPE_KEYWORD:
                return SearchQuery.flagIsSet(key.getValue());
            case SearchKey.TYPE_LARGER:
                return SearchQuery.sizeGreaterThan(key.getSize());
            case SearchKey.TYPE_NEW:
                return SearchQuery.and(SearchQuery.flagIsSet(Flag.RECENT),
                        SearchQuery.flagIsUnSet(Flag.SEEN));
            case SearchKey.TYPE_NOT:
                return not(key.getKeys(), session);
            case SearchKey.TYPE_OLD:
                return SearchQuery.flagIsUnSet(Flag.RECENT);
            case SearchKey.TYPE_ON:
                return SearchQuery.internalDateOn(date.getDay(), date
                        .getMonth(), date.getYear());
            case SearchKey.TYPE_OR:
                return or(key.getKeys(), session);
            case SearchKey.TYPE_RECENT:
                return SearchQuery.flagIsSet(Flag.RECENT);
            case SearchKey.TYPE_SEEN:
                return SearchQuery.flagIsSet(Flag.SEEN);
            case SearchKey.TYPE_SENTBEFORE:
                return SearchQuery.headerDateBefore(ImapConstants.RFC822_DATE,
                        date.getDay(), date.getMonth(), date.getYear());
            case SearchKey.TYPE_SENTON:
                return SearchQuery.headerDateOn(ImapConstants.RFC822_DATE, date
                        .getDay(), date.getMonth(), date.getYear());
            case SearchKey.TYPE_SENTSINCE:
                return SearchQuery.headerDateAfter(ImapConstants.RFC822_DATE,
                        date.getDay(), date.getMonth(), date.getYear());
            case SearchKey.TYPE_SEQUENCE_SET:
                return sequence(key.getSequenceNumbers(), session, true);
            case SearchKey.TYPE_SINCE:
                return SearchQuery.internalDateAfter(date.getDay(), date
                        .getMonth(), date.getYear());
            case SearchKey.TYPE_SMALLER:
                return SearchQuery.sizeLessThan(key.getSize());
            case SearchKey.TYPE_SUBJECT:
                return SearchQuery.headerContains(ImapConstants.RFC822_SUBJECT,
                        key.getValue());
            case SearchKey.TYPE_TEXT:
                return SearchQuery.mailContains(key.getValue());
            case SearchKey.TYPE_TO:
                return SearchQuery.headerContains(ImapConstants.RFC822_TO, key
                        .getValue());
            case SearchKey.TYPE_UID:
                return sequence(key.getSequenceNumbers(), session, false);
            case SearchKey.TYPE_UNANSWERED:
                return SearchQuery.flagIsUnSet(Flag.ANSWERED);
            case SearchKey.TYPE_UNDELETED:
                return SearchQuery.flagIsUnSet(Flag.DELETED);
            case SearchKey.TYPE_UNDRAFT:
                return SearchQuery.flagIsUnSet(Flag.DRAFT);
            case SearchKey.TYPE_UNFLAGGED:
                return SearchQuery.flagIsUnSet(Flag.FLAGGED);
            case SearchKey.TYPE_UNKEYWORD:
                return SearchQuery.flagIsUnSet(key.getValue());
            case SearchKey.TYPE_UNSEEN:
                return SearchQuery.flagIsUnSet(Flag.SEEN);
            default:
                session.getLog().warn("Ignoring unknown search key.");
                return SearchQuery.all();
        }
    }

    private Criterion sequence(IdRange[] sequenceNumbers,
            final ImapSession session, boolean msn) throws MessageRangeException {
        final int length = sequenceNumbers.length;
        final SearchQuery.NumericRange[] ranges = new SearchQuery.NumericRange[length];
        for (int i = 0; i < length; i++) {
            final IdRange range = sequenceNumbers[i];
            long highVal = range.getHighVal();
            long lowVal = range.getLowVal();
            long lowUid;
            long highUid;
            final SelectedMailbox selected = session.getSelected();

            if (msn) {
                if (highVal == Long.MAX_VALUE) {
                    highUid = Long.MAX_VALUE;
                } else {
                    final int highMsn = (int) highVal;
                    highUid = selected.uid(highMsn);
                    
                    if (highUid == SelectedMailbox.NO_SUCH_MESSAGE) highUid = selected.getLastUid();
                }
                if (lowVal == Long.MIN_VALUE) {
                    lowUid = Long.MAX_VALUE;
                } else {
                    final int lowMsn = (int) lowVal;
                    lowUid = selected.uid(lowMsn);
                    if (lowUid == SelectedMailbox.NO_SUCH_MESSAGE) lowUid = selected.getFirstUid();

                }
            } else {
                lowUid = lowVal;
                highUid = highVal;
                
                
            	if (lowVal != Long.MIN_VALUE && lowVal < selected.getFirstUid()) {
            		lowUid = selected.getFirstUid();
            	} 
            	
            	if (highVal != Long.MAX_VALUE && highVal > selected.getLastUid()) {
            		highUid = selected.getLastUid();
            	} 
            }
            ranges[i] = new SearchQuery.NumericRange(lowUid, highUid);
        }
        return SearchQuery.uid(ranges);
    }

    private Criterion or(List<SearchKey> keys, final ImapSession session) throws MessageRangeException {
        final SearchKey keyOne = keys.get(0);
        final SearchKey keyTwo = keys.get(1);
        final Criterion criterionOne = toCriterion(keyOne, session);
        final Criterion criterionTwo = toCriterion(keyTwo, session);
        final Criterion result = SearchQuery.or(criterionOne, criterionTwo);
        return result;
    }

    private Criterion not(List<SearchKey> keys, final ImapSession session) throws MessageRangeException {
        final SearchKey key = keys.get(0);
        final Criterion criterion = toCriterion(key, session);
        final Criterion result = SearchQuery.not(criterion);
        return result;
    }

    private Criterion and(List<SearchKey> keys, final ImapSession session) throws MessageRangeException {
        final int size = keys.size();
        final List<Criterion> criteria = new ArrayList<Criterion>(size);
        for (final SearchKey key:keys) {
            final Criterion criterion = toCriterion(key, session);
            criteria.add(criterion);
        }
        final Criterion result = SearchQuery.and(criteria);
        return result;
    }
}
