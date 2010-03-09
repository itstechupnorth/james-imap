package org.apache.james.imap.jcr.mail.model;

import java.util.Date;

import javax.mail.Flags;

import org.apache.james.imap.store.mail.model.AbstractMailboxMembership;
import org.apache.james.imap.store.mail.model.Document;

public class JCRMailboxMembership extends AbstractMailboxMembership{

    @Override
    public Document getDocument() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getInternalDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getMailboxId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getUid() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isAnswered() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDeleted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDraft() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isFlagged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRecent() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSeen() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setFlags(Flags flags) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void unsetRecent() {
        // TODO Auto-generated method stub
        
    }

}
