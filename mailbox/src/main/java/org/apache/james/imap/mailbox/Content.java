package org.apache.james.imap.mailbox;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import javax.mail.MessagingException;

/**
 * IMAP needs to know the size of the content before it starts to write it
 * out. This interface allows direct writing whilst exposing total size.
 */
public interface Content {

    /**
     * Writes content to the given channel.
     * 
     * @param channel
     *            <code>Channel</code> open, not null
     * @throws MailboxException
     * @throws IOException
     *             when channel IO fails
     */
    public void writeTo(WritableByteChannel channel) throws IOException;

    /**
     * Size (in octets) of the content.
     * 
     * @return number of octets to be written
     * @throws MessagingException
     */
    public long size();
}