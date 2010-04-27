package org.apache.james.imap.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.james.imap.mailbox.MailboxSession;

public class MailboxSessionEntityManagerFactory {

    private EntityManagerFactory factory;
    private final static String ENTITYMANAGER = "ENTITYMANAGER";
    
    public MailboxSessionEntityManagerFactory(EntityManagerFactory factory) {
        this.factory = factory;
    }
    
    public EntityManager getEntityManager(MailboxSession session) {
        EntityManager manager = (EntityManager) session.getAttributes().get(ENTITYMANAGER);
        if (manager == null || manager.isOpen() == false) {
            manager = factory.createEntityManager();
            session.getAttributes().put(ENTITYMANAGER, manager);
        } 
        
        return manager;
    }
    
    public void closeEntityManager(MailboxSession session) {
        if (session != null) {
            EntityManager manager = (EntityManager) session.getAttributes().remove(ENTITYMANAGER);
            if ( manager != null && manager.isOpen()) {
                manager.close();
            }
        } 
    }
}
