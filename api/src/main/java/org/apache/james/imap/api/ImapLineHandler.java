package org.apache.james.imap.api;


public interface ImapLineHandler {
    
    public void onLine(ImapSession session, byte data[], ImapMessageCallback callback);

}
