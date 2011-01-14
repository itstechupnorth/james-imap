package org.apache.james.imap.message.request;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.decode.ImapDecoder;

public class ContinuationRequest extends AbstractImapRequest {

    private ImapDecoder decoder;

    public ContinuationRequest(final ImapCommand command, final String tag, final ImapDecoder decoder) {
        super(tag, command);
        this.decoder = decoder;
    }
    
    public ImapDecoder getDecoder() {
        return decoder;
    }
}
