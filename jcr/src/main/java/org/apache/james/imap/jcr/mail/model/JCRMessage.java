package org.apache.james.imap.jcr.mail.model;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.imap.store.mail.model.Document;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Property;
import org.apache.james.imap.store.mail.model.PropertyBuilder;


public class JCRMessage implements Document{

    
    private final byte[] content;
    private final int contentOctets;
    private final int bodyStartOctet;
    private final ArrayList<JCRHeader> headers;
    private final Long textualLineCount;
    private final String mediaType;
    private final String subType;
    private final ArrayList<JCRProperty> properties;

    private JCRMessage(byte[] content, final int bodyStartOctet, final List<JCRHeader> headers, final PropertyBuilder propertyBuilder) {
        super();
        this.content = content;
        this.contentOctets = content.length;
        this.bodyStartOctet = bodyStartOctet;
        this.headers = new ArrayList<JCRHeader>(headers);
        textualLineCount = propertyBuilder.getTextualLineCount();
        this.mediaType = propertyBuilder.getMediaType();
        this.subType = propertyBuilder.getSubType();
        final List<Property> properties = propertyBuilder.toProperties();
        this.properties = new ArrayList<JCRProperty>(properties.size());
        int order = 0;
        for (final Property property:properties) {
            this.properties.add(new JCRProperty(property.getNamespace(), property.getLocalName(), property.getValue(), order++));
        }
        
    }
    
    @Override
    public ByteBuffer getBodyContent() {
        return null;
    }

    @Override
    public long getBodyOctets() {
        return contentOctets;
    }

    @Override
    public ByteBuffer getFullContent() {
        return null;
    }

    @Override
    public long getFullContentOctets() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<Header> getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Property> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSubType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getTextualLineCount() {
        // TODO Auto-generated method stub
        return null;
    }

}
