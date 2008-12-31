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

package org.apache.james.imap.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.imap.jpa.mail.model.Header;
import org.apache.james.imap.jpa.mail.model.Message;
import org.apache.james.imap.mailbox.MailboxException;
import org.apache.james.imap.mailbox.SearchQuery;
import org.apache.james.imap.mailbox.UnsupportedSearchException;
import org.apache.james.imap.mailbox.SearchQuery.NumericRange;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.field.datetime.DateTime;
import org.apache.james.mime4j.field.datetime.parser.DateTimeParser;
import org.apache.james.mime4j.field.datetime.parser.ParseException;

/**
 * Uility methods to help perform search operations.
 */
class MessageSearches {

    private Log log;

    private boolean isCustomLog = false;

    public final Log getLog() {
        if (log == null) {
            log = LogFactory.getLog(MessageSearches.class);
        }
        return log;
    }

    public final void setLog(Log log) {
        isCustomLog = true;
        this.log = log;
    }

    /**
     * Does the row match the given criteria?
     * 
     * @param query
     *            <code>SearchQuery</code>, not null
     * @param row
     *            <code>MessageRow</code>, not null
     * @return true if the row matches the given criteria, false otherwise
     * @throws MailboxException 
     */
    public boolean isMatch(final SearchQuery query, final Message message)
            throws MailboxException {
        final List criteria = query.getCriterias();
        final Collection recentMessageUids = query.getRecentMessageUids();
        boolean result = true;
        if (criteria != null) {
            for (Iterator it = criteria.iterator(); it.hasNext();) {
                final SearchQuery.Criterion criterion = (SearchQuery.Criterion) it
                        .next();
                if (!isMatch(criterion, message, recentMessageUids)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Does the row match the given criterion?
     * 
     * @param query
     *            <code>SearchQuery.Criterion</code>, not null
     * @param message
     *            <code>MessageRow</code>, not null
     * @return true if the row matches the given criterion, false otherwise
     * @throws MailboxException 
     */
    public boolean isMatch(SearchQuery.Criterion criterion, Message message,
            final Collection recentMessageUids) throws MailboxException {
        final boolean result;
        if (criterion instanceof SearchQuery.InternalDateCriterion) {
            result = matches((SearchQuery.InternalDateCriterion) criterion, message);
        } else if (criterion instanceof SearchQuery.SizeCriterion) {
            result = matches((SearchQuery.SizeCriterion) criterion, message);
        } else if (criterion instanceof SearchQuery.HeaderCriterion) {
            result = matches((SearchQuery.HeaderCriterion) criterion, message);
        } else if (criterion instanceof SearchQuery.UidCriterion) {
            result = matches((SearchQuery.UidCriterion) criterion, message);
        } else if (criterion instanceof SearchQuery.FlagCriterion) {
            result = matches((SearchQuery.FlagCriterion) criterion, message,
                    recentMessageUids);
        } else if (criterion instanceof SearchQuery.TextCriterion) {
            result = matches((SearchQuery.TextCriterion) criterion, message);
        } else if (criterion instanceof SearchQuery.AllCriterion) {
            result = true;
        } else if (criterion instanceof SearchQuery.ConjunctionCriterion) {
            result = matches((SearchQuery.ConjunctionCriterion) criterion, message,
                    recentMessageUids);
        } else {
            throw new UnsupportedSearchException();
        }
        return result;
    }

    private boolean matches(SearchQuery.TextCriterion criterion, Message message) throws MailboxException  {
        try {
            final SearchQuery.ContainsOperator operator = criterion
                    .getOperator();
            final String value = operator.getValue();
            final int type = criterion.getType();
            switch (type) {
                case SearchQuery.TextCriterion.BODY:
                    return bodyContains(value, message);
                case SearchQuery.TextCriterion.FULL_MESSAGE:
                    return messageContains(value, message);
                default:
                    throw new UnsupportedSearchException();
            }
        } catch (IOException e) {
            throw new MailboxException(e);
        }
    }

    private boolean bodyContains(String value, Message message)
            throws IOException, MimeException {
        final InputStream input = MessageRowUtils.toInput(message);
        final boolean result = isInMessage(value, input, false);
        return result;
    }

    private boolean isInMessage(String value, final InputStream input,
            boolean header) throws IOException, MimeException {
        final MessageSearcher searcher = new MessageSearcher(value, true,
                header);
        if (isCustomLog) {
            searcher.setLogger(log);
        }
        final boolean result = searcher.isFoundIn(input);
        return result;
    }

    private boolean messageContains(String value, Message message)
            throws IOException, MimeException {
        final InputStream input = MessageRowUtils.toInput(message);
        final boolean result = isInMessage(value, input, true);
        return result;
    }

    private boolean matches(SearchQuery.ConjunctionCriterion criterion,
            Message message, final Collection recentMessageUids) throws MailboxException {
        final int type = criterion.getType();
        final List criteria = criterion.getCriteria();
        switch (type) {
            case SearchQuery.ConjunctionCriterion.NOR:
                return nor(criteria, message, recentMessageUids);
            case SearchQuery.ConjunctionCriterion.OR:
                return or(criteria, message, recentMessageUids);
            case SearchQuery.ConjunctionCriterion.AND:
                return and(criteria, message, recentMessageUids);
            default:
                return false;
        }
    }

    private boolean and(final List criteria, final Message message, 
            final Collection recentMessageUids) throws MailboxException {
        boolean result = true;
        for (Iterator it = criteria.iterator(); it.hasNext();) {
            final SearchQuery.Criterion criterion = (SearchQuery.Criterion) it
                    .next();
            final boolean matches = isMatch(criterion, message, recentMessageUids);
            if (!matches) {
                result = false;
                break;
            }
        }
        return result;
    }

    private boolean or(final List criteria, final Message message,
            final Collection recentMessageUids) throws MailboxException {
        boolean result = false;
        for (Iterator it = criteria.iterator(); it.hasNext();) {
            final SearchQuery.Criterion criterion = (SearchQuery.Criterion) it
                    .next();
            final boolean matches = isMatch(criterion, message, recentMessageUids);
            if (matches) {
                result = true;
                break;
            }
        }
        return result;
    }

    private boolean nor(final List criteria, final Message message,
            final Collection recentMessageUids) throws MailboxException {
        boolean result = true;
        for (Iterator it = criteria.iterator(); it.hasNext();) {
            final SearchQuery.Criterion criterion = (SearchQuery.Criterion) it
                    .next();
            final boolean matches = isMatch(criterion, message, recentMessageUids);
            if (matches) {
                result = false;
                break;
            }
        }
        return result;
    }

    private boolean matches(SearchQuery.FlagCriterion criterion,
            Message message, final Collection recentMessageUids) {
        final SearchQuery.BooleanOperator operator = criterion.getOperator();
        final boolean isSet = operator.isSet();
        final Flags.Flag flag = criterion.getFlag();
        final boolean result;
        if (flag == Flags.Flag.ANSWERED) {
            result = isSet == message.isAnswered();
        } else if (flag == Flags.Flag.SEEN) {
            result = isSet == message.isSeen();
        } else if (flag == Flags.Flag.DRAFT) {
            result = isSet == message.isDraft();
        } else if (flag == Flags.Flag.FLAGGED) {
            result = isSet == message.isFlagged();
        } else if (flag == Flags.Flag.RECENT) {
            final long uid = message.getUid();
            result = isSet == recentMessageUids.contains(new Long(uid));
        } else if (flag == Flags.Flag.DELETED) {
            result = isSet == message.isDeleted();
        } else {
            result = false;
        }
        return result;
    }

    private boolean matches(SearchQuery.UidCriterion criterion, Message message) {
        final SearchQuery.InOperator operator = criterion.getOperator();
        final NumericRange[] ranges = operator.getRange();
        final long uid = message.getUid();
        final int length = ranges.length;
        boolean result = false;
        for (int i = 0; i < length; i++) {
            final NumericRange numericRange = ranges[i];
            if (numericRange.isIn(uid)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private boolean matches(SearchQuery.HeaderCriterion criterion, Message message) throws UnsupportedSearchException {
        final SearchQuery.HeaderOperator operator = criterion.getOperator();
        final String headerName = criterion.getHeaderName();
        final boolean result;
        if (operator instanceof SearchQuery.DateOperator) {
            result = matches((SearchQuery.DateOperator) operator, headerName, message);
        } else if (operator instanceof SearchQuery.ContainsOperator) {
            result = matches((SearchQuery.ContainsOperator) operator, headerName, message);
        } else if (operator instanceof SearchQuery.ExistsOperator) {
            result = exists(headerName, message);
        } else {
            throw new UnsupportedSearchException();
        }
        return result;
    }

    private boolean exists(String headerName, Message message) {
        boolean result = false;
        final List<Header> headers = message.getHeaders();
        for (Header header:headers) {
            final String name = header.getField();
            if (headerName.equalsIgnoreCase(name)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private boolean matches(final SearchQuery.ContainsOperator operator,
            final String headerName, final Message message) {
        final String text = operator.getValue().toUpperCase();
        boolean result = false;
        final List<Header> headers = message.getHeaders();
        for (Header header:headers) {
            final String name = header.getField();
            if (headerName.equalsIgnoreCase(name)) {
                final String value = header.getValue();
                if (value != null) {
                    if (value.toUpperCase().indexOf(text) > -1) {
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private boolean matches(final SearchQuery.DateOperator operator,
            final String headerName, final Message message) throws UnsupportedSearchException {
        final int day = operator.getDay();
        final int month = operator.getMonth();
        final int year = operator.getYear();
        final int iso = toISODate(day, month, year);
        final String value = headerValue(headerName, message);
        if (value == null) {
            return false;
        } else {
            try {
                final int isoFieldValue = toISODate(value);
                final int type = operator.getType();
                switch (type) {
                    case SearchQuery.DateOperator.AFTER:
                        return iso < isoFieldValue;
                    case SearchQuery.DateOperator.BEFORE:
                        return iso > isoFieldValue;
                    case SearchQuery.DateOperator.ON:
                        return iso == isoFieldValue;
                    default:
                        throw new UnsupportedSearchException();
                }
            } catch (ParseException e) {
                return false;
            }
        }
    }

    private String headerValue(final String headerName, final Message message) {
        final List<Header> headers = message.getHeaders();
        String value = null;
        for (Header header:headers) {
            final String name = header.getField();
            if (headerName.equalsIgnoreCase(name)) {
                value = header.getValue();
                break;
            }
        }
        return value;
    }

    private int toISODate(String value) throws ParseException {
        final StringReader reader = new StringReader(value);
        final DateTime dateTime = new DateTimeParser(reader).parseAll();
        final int isoFieldValue = toISODate(dateTime.getDay(), dateTime
                .getMonth(), dateTime.getYear());
        return isoFieldValue;
    }

    private boolean matches(SearchQuery.SizeCriterion criterion, Message message)
            throws UnsupportedSearchException {
        final SearchQuery.NumericOperator operator = criterion.getOperator();
        final int size = message.getSize();
        final long value = operator.getValue();
        final int type = operator.getType();
        switch (type) {
            case SearchQuery.NumericOperator.LESS_THAN:
                return size < value;
            case SearchQuery.NumericOperator.GREATER_THAN:
                return size > value;
            case SearchQuery.NumericOperator.EQUALS:
                return size == value;
            default:
                throw new UnsupportedSearchException();
        }
    }

    private boolean matches(SearchQuery.InternalDateCriterion criterion,
            Message message) throws UnsupportedSearchException {
        final SearchQuery.DateOperator operator = criterion.getOperator();
        final boolean result = matchesInternalDate(operator, message);
        return result;
    }

    private boolean matchesInternalDate(
            final SearchQuery.DateOperator operator, final Message message)
            throws UnsupportedSearchException {
        final int day = operator.getDay();
        final int month = operator.getMonth();
        final int year = operator.getYear();
        final Date internalDate = message.getInternalDate();
        final int type = operator.getType();
        switch (type) {
            case SearchQuery.DateOperator.ON:
                return on(day, month, year, internalDate);
            case SearchQuery.DateOperator.BEFORE:
                return before(day, month, year, internalDate);
            case SearchQuery.DateOperator.AFTER:
                return after(day, month, year, internalDate);
            default:
                throw new UnsupportedSearchException();
        }
    }

    private boolean on(final int day, final int month, final int year,
            final Date date) {
        final Calendar gmt = getGMT();
        gmt.setTime(date);
        return day == gmt.get(Calendar.DAY_OF_MONTH)
                && month == (gmt.get(Calendar.MONTH) + 1)
                && year == gmt.get(Calendar.YEAR);
    }

    private boolean before(final int day, final int month, final int year,
            final Date date) {
        return toISODate(date) < toISODate(day, month, year);
    }

    private boolean after(final int day, final int month, final int year,
            final Date date) {
        return toISODate(date) > toISODate(day, month, year);
    }

    private int toISODate(final Date date) {
        final Calendar gmt = getGMT();
        gmt.setTime(date);
        final int day = gmt.get(Calendar.DAY_OF_MONTH);
        final int month = (gmt.get(Calendar.MONTH) + 1);
        final int year = gmt.get(Calendar.YEAR);
        final int result = toISODate(day, month, year);
        return result;
    }

    private Calendar getGMT() {
        return Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.UK);
    }

    private static int toISODate(final int day, final int month, final int year) {
        final int result = (year * 10000) + (month * 100) + day;
        return result;
    }
}
