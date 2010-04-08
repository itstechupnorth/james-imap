package org.apache.james.imap.store;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link FilterInputStream} which support the get rewinded. This is done by copy over every byte
 * over to another {@link OutputStream}. What {@link OutputStream} to use is up to the implementations.
 * 
 * The rewinding will get delayed as long as possible. So if you call
 * rewind, it will only get performed when needed
 * 
 *
 */
public abstract class AbstractRewindableInputStream extends RewindableInputStream{

    protected boolean end = false;

    public AbstractRewindableInputStream(InputStream in) throws IOException {
        super(in);
    }

    protected abstract OutputStream getRewindOutputStream() throws IOException;
    
    protected abstract InputStream getRewindInputStream() throws IOException;

    protected abstract void dispose() throws IOException;
    
    protected abstract void afterRewindComplete() throws IOException;
    
    @Override
    public void close() throws IOException {
        try {
            in.close();
            OutputStream out = getRewindOutputStream();
            if (out != null) {
                out.close();
            }
            InputStream in = getRewindInputStream();
            if (in != null) {
                in.close();
            }
        } finally {
            dispose();
        }
    }

    
    
    @Override
    public int read() throws IOException {   
        int i;
        if (needsRewind()) {

            rewindIfNeeded();
        }
        
        if (end == false) {
            i = in.read();
            if (i == -1) {
                end = true;
            } else {
                getRewindOutputStream().write(i);
            }
        } else {
            i = getRewindInputStream().read();
        }
        return i;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (needsRewind()) {
            rewindIfNeeded();
        }    
        
        int i;
        if (end == false) {
            i = in.read(b, off, len);
            if (i == -1) {
                end = true;
            }
            getRewindOutputStream().write(b, off, len);
        } else {
            i = getRewindInputStream().read(b,off,len);
        }
        return i;
    }

    @Override
    public void rewindIfNeeded() throws IOException {
        if (needsRewind()) {
            rewindDone();
            
            if (end == false) {
                while ( read() != -1);
            }
            // we don't need the original inputstream anymore so close it
            in.close();
            afterRewindComplete();
        }        
    }

    @Override
    public int available() throws IOException {
        if (end == false) {
            return in.available();
        } else {
            return getRewindInputStream().available();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        for (int i = 0; i < n; i++) {
            if (read() == -1) {
                return n -i;
            }
            if (end) break;
        }
        return 0;
    }
}
