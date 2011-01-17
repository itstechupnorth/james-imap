package org.apache.james.imap.decode;

import org.apache.james.imap.api.DecodingException;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.ImapMessageCallback;

public class MockImapMessageCallback implements ImapMessageCallback{

    private ImapMessage m;
    private DecodingException ex;

    public void onMessage(ImapMessage m) {
        this.m = m;
    }

    public void onException(DecodingException ex) {
        this.ex = ex;
    }
    
    public ImapMessage getMessage() {
        return m;
    }
    
    public DecodingException getException() {
        return ex;
    }

}
