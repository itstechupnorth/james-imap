package org.apache.james.imap.api;


public interface ImapMessageCallback {

    public void onMessage(ImapMessage message);
    
    public void onException(DecodingException ex);
}
