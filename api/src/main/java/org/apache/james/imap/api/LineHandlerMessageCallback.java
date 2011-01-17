package org.apache.james.imap.api;

import org.apache.james.imap.api.process.ImapSession;

public class LineHandlerMessageCallback implements ImapMessageCallback{

    private ImapMessageCallback callback;
    private ImapSession session;

    public LineHandlerMessageCallback(ImapSession session, ImapMessageCallback callback) {
        this.callback = callback;
        this.session = session;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageCallback#onMessage(org.apache.james.imap.api.ImapMessage)
     */
    public void onMessage(ImapMessage message) {
        session.popImapLineHandler();
        callback.onMessage(message);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.api.ImapMessageCallback#onException(org.apache.james.imap.api.DecodingException)
     */
    public void onException(DecodingException ex) {
        session.popImapLineHandler();
        callback.onException(ex);
    }

}
