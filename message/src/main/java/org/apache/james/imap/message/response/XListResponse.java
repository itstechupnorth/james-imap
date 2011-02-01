package org.apache.james.imap.message.response;

import org.apache.james.imap.api.message.response.ImapResponseMessage;
import org.apache.james.imap.api.process.MailboxType;

/**
 * XLIST command response
 * 
 * @author ihsahn
 */
public class XListResponse extends AbstractListingResponse implements
        ImapResponseMessage {

    public XListResponse(final boolean noInferiors, final boolean noSelect,
            final boolean marked, final boolean unmarked,
            boolean hasChildren, boolean hasNoChildren, final String name, final char delimiter,final MailboxType type) {
        super(noInferiors, noSelect, marked, unmarked, hasChildren, hasNoChildren, name, delimiter,type);
    }
}
