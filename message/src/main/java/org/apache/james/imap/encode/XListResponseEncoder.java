package org.apache.james.imap.encode;

import java.io.IOException;

import org.apache.james.imap.encode.base.AbstractChainedImapEncoder;
import org.apache.james.imap.message.response.AbstractListingResponse;
import org.apache.james.imap.message.response.XListResponse;

import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.process.ImapSession;

/**
 *
 * @author ihsahn
 */
public class XListResponseEncoder extends AbstractChainedImapEncoder {

    public XListResponseEncoder(ImapEncoder next) {
        super(next);
    }

    protected void doEncode(final ImapMessage acceptableMessage,
            final ImapResponseComposer composer, ImapSession session) throws IOException {
        final AbstractListingResponse response = (AbstractListingResponse) acceptableMessage;
        ListingEncodingUtils.encodeListingResponse(
                ImapConstants.XLIST_RESPONSE_NAME, composer, response);
    }


    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof XListResponse);
    }
}
