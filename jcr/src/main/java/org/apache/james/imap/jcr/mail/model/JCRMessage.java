/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.imap.jcr.mail.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.jackrabbit.JcrConstants;
import org.apache.james.imap.jcr.JCRImapConstants;
import org.apache.james.imap.jcr.JCRUtils;
import org.apache.james.imap.jcr.Persistent;
import org.apache.james.imap.store.mail.model.AbstractDocument;
import org.apache.james.imap.store.mail.model.Document;
import org.apache.james.imap.store.mail.model.Header;
import org.apache.james.imap.store.mail.model.Property;
import org.apache.james.imap.store.mail.model.PropertyBuilder;

/**
 * JCR implementation of {@link Document}
 *
 */
public class JCRMessage extends AbstractDocument implements JCRImapConstants, Persistent{

    private Node node;
    private final Log logger;
    private byte[] content;
    private List<JCRHeader> headers;
    private long fullContentOctets;
    private String mediaType;
    private Long textualLineCount;
    private String subType;
    private List<JCRProperty> properties;
    private int bodyStartOctet;
    
    public final static String BODY_START_OCTET_PROPERTY = PROPERTY_PREFIX + "messageBodyStartOctet";
    public final static String FULL_CONTENT_OCTETS_PROPERTY = PROPERTY_PREFIX + "messageFullContentOctets";
    public final static String HEADERS_NODE = PROPERTY_PREFIX + "messageHeaders";
    public final static String PROPERTIES_NODE = PROPERTY_PREFIX + "messageProperties";

    public final static String TEXTUAL_LINE_COUNT_PROPERTY  = PROPERTY_PREFIX + "messageTextualLineCount";
    public final static String SUBTYPE_PROPERTY  = PROPERTY_PREFIX + "messageSubType";

    public JCRMessage(Node node, Log logger) {
        this.logger= logger;
        this.node = node;
    }
    
    public JCRMessage(byte[] content, final int bodyStartOctet, final List<JCRHeader> headers, final PropertyBuilder propertyBuilder, Log logger) {
        super();
        this.logger = logger;
        this.content = content;
        this.fullContentOctets = content.length;
        this.bodyStartOctet = bodyStartOctet;
        this.headers = new ArrayList<JCRHeader>(headers);
        this.textualLineCount = propertyBuilder.getTextualLineCount();
        this.mediaType = propertyBuilder.getMediaType();
        this.subType = propertyBuilder.getSubType();
        final List<Property> properties = propertyBuilder.toProperties();
        this.properties = new ArrayList<JCRProperty>(properties.size());
        int order = 0;
        for (final Property property:properties) {
            this.properties.add(new JCRProperty(property, order++, logger));
        }
        
    }


    /**
     * Create a copy of the given message
     * 
     * @param message
     */
    public JCRMessage(JCRMessage message, Log logger) {
        this.logger = logger;
        ByteBuffer buf = message.getFullContent().duplicate();
        int a = 0;
        this.content = new byte[buf.capacity()];
        while(buf.hasRemaining()) {
            content[a] = buf.get();
            a++;
        }
        this.fullContentOctets = content.length;
        this.bodyStartOctet = (int) (message.getFullContentOctets() - message.getBodyOctets());
        this.headers = new ArrayList<JCRHeader>();
        
        List<Header> originalHeaders = message.getHeaders();
        for (int i = 0; i < originalHeaders.size(); i++) {
            headers.add(new JCRHeader(originalHeaders.get(i),logger));
        }

        PropertyBuilder pBuilder = new PropertyBuilder(message.getProperties());
        this.textualLineCount = pBuilder.getTextualLineCount();
        this.mediaType = pBuilder.getMediaType();
        this.subType = pBuilder.getSubType();
        final List<Property> properties = pBuilder.toProperties();
        this.properties = new ArrayList<JCRProperty>(properties.size());
        int order = 0;
        for (final Property property:properties) {
            this.properties.add(new JCRProperty(property, order++, logger));
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Document#getFullContent()
     */
    public ByteBuffer getFullContent() {
        if (isPersistent()) {
            try {
                //TODO: Maybe we should cache this somehow...
                InputStream contentStream = node.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_DATA).getStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
 
                    byte[] buf = new byte[1024];
                    int i = 0;
                    while ((i = contentStream.read(buf)) != -1) {
                        out.write(buf, 0, i);
                    }

                return ByteBuffer.wrap(out.toByteArray());
            } catch (RepositoryException e) {
                logger.error("Unable to retrieve property " + JcrConstants.JCR_CONTENT, e);
            } catch (IOException e) {
                logger.error("Unable to retrieve property " + JcrConstants.JCR_CONTENT, e);
            }
            return null;
        }
        return ByteBuffer.wrap(content);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Document#getFullContentOctets()
     */
    public long getFullContentOctets() {
        if (isPersistent()) {
            try {
                return node.getProperty(FULL_CONTENT_OCTETS_PROPERTY).getLong();
            } catch (RepositoryException e) {
                logger.error("Unable to retrieve property " + FULL_CONTENT_OCTETS_PROPERTY, e);

            }
            return 0;
        }
        return fullContentOctets;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Document#getHeaders()
     */
    public List<Header> getHeaders() {
        if (isPersistent()) {
            try {
                List<Header> headers = new ArrayList<Header>();
                Node headersNode = node.getNode(HEADERS_NODE);
                NodeIterator nodeIt = headersNode.getNodes();
                while (nodeIt.hasNext()) {
                    headers.add(new JCRHeader(nodeIt.nextNode(), logger));
                }
                return headers;
            } catch (RepositoryException e) {
                logger.error("Unable to retrieve node " + HEADERS_NODE, e);
            }
        }
        return new ArrayList<Header>(headers);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Document#getMediaType()
     */
    public String getMediaType() {
        if (isPersistent()) {
            try {
                return node.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_MIMETYPE).getString();
            } catch (RepositoryException e) {
                logger.error("Unable to retrieve node " + JcrConstants.JCR_MIMETYPE, e);
            }
            return null;
        }
        return mediaType;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Document#getProperties()
     */
    public List<Property> getProperties() {
        if (isPersistent()) {
            try {
                List<Property> properties = new ArrayList<Property>();
                NodeIterator nodeIt = node.getNode(PROPERTIES_NODE).getNodes();
                while (nodeIt.hasNext()) {
                    properties.add(new JCRProperty(nodeIt.nextNode(), logger));
                }
                return properties;
            } catch (RepositoryException e) {
                logger.error("Unable to retrieve node " + PROPERTIES_NODE, e);
            }
        }
        return new ArrayList<Property>(properties);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Document#getSubType()
     */
    public String getSubType() {
        if (isPersistent()) {
            try {
                return node.getProperty(SUBTYPE_PROPERTY).getString();
            } catch (RepositoryException e) {
                logger.error("Unable to retrieve node " + SUBTYPE_PROPERTY, e);
            }
            return null;
        }
        return subType;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Document#getTextualLineCount()
     */
    public Long getTextualLineCount() {
        if (isPersistent()) {
            try {
                return node.getProperty(TEXTUAL_LINE_COUNT_PROPERTY).getLong();
            } catch (RepositoryException e) {
                //logger.error("Unable to retrieve property " + TEXTUAL_LINE_COUNT_PROPERTY, e);

            }
            return null;
        }
        return textualLineCount;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.jcr.Persistent#getNode()
     */
    public Node getNode() {
        return node;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.jcr.Persistent#isPersistent()
     */
    public boolean isPersistent() {
        return node != null;
    }

    public String getUUID() {
        if (isPersistent()) {
            try {
                return node.getUUID();
            } catch (RepositoryException e) {
                logger.error("Unable to access UUID", e);
            }
        }
        return null;
    }
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.jcr.Persistent#merge(javax.jcr.Node)
     */
    public void merge(Node node) throws RepositoryException {
        Node contentNode;
        if (node.hasNode(JcrConstants.JCR_CONTENT) == false) {
            contentNode = node.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        } else {
            contentNode = node.getNode(JcrConstants.JCR_CONTENT);
        }
        contentNode.setProperty(JcrConstants.JCR_DATA, new ByteArrayInputStream(getFullContent().array()));
        contentNode.setProperty(JcrConstants.JCR_MIMETYPE, getMediaType());

        node.setProperty(FULL_CONTENT_OCTETS_PROPERTY, getFullContentOctets());
        if (getTextualLineCount() != null) {
            node.setProperty(TEXTUAL_LINE_COUNT_PROPERTY, getTextualLineCount());
        }
        node.setProperty(SUBTYPE_PROPERTY, getSubType());
        node.setProperty(BODY_START_OCTET_PROPERTY, getBodyStartOctet());

        // copy the headers and store them in memory as pure pojos
        List<Header> currentHeaders = getHeaders();
        List<Header> newHeaders = new ArrayList<Header>();
        for (int i = 0 ; i < currentHeaders.size(); i++) {
            newHeaders.add(new JCRHeader(currentHeaders.get(i), logger));
        }
        
        Node headersNode;
        // check if some headers are already stored
        if (node.hasNode(HEADERS_NODE)) {
            
            headersNode = node.getNode(HEADERS_NODE);
            NodeIterator iterator = headersNode.getNodes();
            // remove old headers
            while(iterator.hasNext()) {
                iterator.nextNode().remove();
            }
        } else {
            headersNode = node.addNode(HEADERS_NODE);
            headersNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        }
        
            
        // add headers to the message again
        for (int i = 0; i < newHeaders.size(); i++) {
            JCRHeader header = (JCRHeader) newHeaders.get(i);
            Node headerNode = headersNode.addNode(header.getFieldName());
            header.merge(headerNode);
        }
      
        List<Property> currentProperties = getProperties();
        List<Property> newProperites = new ArrayList<Property>();
        for (int i = 0; i < currentProperties.size(); i++) {
            Property prop = currentProperties.get(i);
            newProperites.add(new JCRProperty(prop, i, logger));
        }
        Node propertiesNode;
        
        // check if some properties are already stored on this.
        if (node.hasNode(PROPERTIES_NODE)) {
            propertiesNode = node.getNode(PROPERTIES_NODE);
            
            // remove old properties, we will add a bunch of new ones
            NodeIterator iterator = propertiesNode.getNodes();
            while(iterator.hasNext()) {
                iterator.nextNode().remove();
            }
        } else {
            propertiesNode = node.addNode(PROPERTIES_NODE);
            propertiesNode.addMixin(JcrConstants.MIX_REFERENCEABLE);

        }
        

        // store new properties
        for (int i = 0; i < newProperites.size(); i++) {
            JCRProperty prop = (JCRProperty)newProperites.get(i);
            Node propNode = propertiesNode.addNode(JCRUtils.escapePath(String.valueOf(prop.getOrder())));
            prop.merge(propNode);
        }
      
        this.node = node;

        /*
        content = null;
        headers = new ArrayList<JCRHeader>();
        fullContentOctets = 0;
        mediaType = null;
        textualLineCount = null;
        subType = null;
        properties = null;
        bodyStartOctet = 0;
        */

    }
    
    @Override
    protected int getBodyStartOctet() {
        if (isPersistent()) {
            try {
                return new Long(node.getProperty(BODY_START_OCTET_PROPERTY).getLong()).intValue();
            } catch (RepositoryException e) {
                logger.error("Unable to retrieve property " + TEXTUAL_LINE_COUNT_PROPERTY, e);

            }
            return 0;
        }
        return bodyStartOctet;
    }
    

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + getUUID().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final JCRMessage other = (JCRMessage) obj;
        if (getUUID() != other.getUUID())
            return false;
        return true;
    }

    public String toString() {
        final String retValue = 
            "message("
            + "uuid = " + getUUID()
            + " )";
        return retValue;
    }


}
