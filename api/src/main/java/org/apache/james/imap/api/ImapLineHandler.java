package org.apache.james.imap.api;

import org.apache.james.imap.api.process.ImapSession;

public interface ImapLineHandler {
    
    public void onLine(ImapSession session, byte data[], ImapMessageCallback callback);

}
