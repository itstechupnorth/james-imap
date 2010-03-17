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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.jackrabbit.JcrConstants;
import org.apache.james.imap.jcr.Persistent;
import org.apache.james.imap.jcr.JCRImapConstants;

import org.apache.james.imap.store.mail.model.AbstractComparableProperty;
import org.apache.james.imap.store.mail.model.Property;

/**
 * JCR implementation of {@link Property}
 *
 */
public class JCRProperty extends AbstractComparableProperty<JCRProperty> implements JCRImapConstants, Persistent {

    private Node node;
    private final Log logger;
    private String namespace;
    private String localName;
    private String value;
    private int order;

    public final static String NAMESPACE_PROPERTY = PROPERTY_PREFIX + "namespace";
    public final static String LOCALNAME_PROPERTY = PROPERTY_PREFIX + "localName";
    public final static String VALUE_PROPERTY = PROPERTY_PREFIX + "value";
    public final static String ORDER_PROPERTY = PROPERTY_PREFIX + "order";

    public JCRProperty(final Node node, final Log logger) {
        this.node = node;
        this.logger = logger;
    }

    public JCRProperty(final String namespace, final String localName, final String value, final int order, Log logger) {
        this.namespace = namespace;
        this.localName = localName;
        this.value = value;
        this.order = order;
        this.logger = logger;
    }

    public JCRProperty(Property property, int order, Log logger) {
        this(property.getNamespace(), property.getLocalName(), property.getValue(), order, logger);
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.store.mail.model.AbstractComparableProperty#getOrder
     * ()
     */
    public int getOrder() {
        if (isPersistent()) {
            try {
                return new Long(node.getProperty(ORDER_PROPERTY).getLong()).intValue();
            } catch (RepositoryException e) {
                logger.error("Unable to access Property " + ORDER_PROPERTY, e);
            }
            return 0;
        }
        return order;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.jcr.IsPersistent#getNode()
     */
    public Node getNode() {
        return node;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.store.mail.model.Property#getLocalName()
     */
    public String getLocalName() {
        if (isPersistent()) {
            try {
                return node.getProperty(LOCALNAME_PROPERTY).getString();
            } catch (RepositoryException e) {
                logger.error("Unable to access Property " + LOCALNAME_PROPERTY, e);
            }
            return null;
        }
        return localName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.store.mail.model.Property#getNamespace()
     */
    public String getNamespace() {
        if (isPersistent()) {
            try {
                return node.getProperty(NAMESPACE_PROPERTY).getString();
            } catch (RepositoryException e) {
                logger.error("Unable to access Property " + NAMESPACE_PROPERTY, e);
            }
            return null;
        }
        return namespace;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.store.mail.model.Property#getValue()
     */
    public String getValue() {
        if (isPersistent()) {
            try {
                return node.getProperty(VALUE_PROPERTY).getString();
            } catch (RepositoryException e) {
                logger.error("Unable to access Property " + VALUE_PROPERTY, e);
            }
            return null;
        }
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.jcr.IsPersistent#isPersistent()
     */
    public boolean isPersistent() {
        return node != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.jcr.IsPersistent#merge(javax.jcr.Node)
     */
    public void merge(Node node) throws RepositoryException {
        node.setProperty(NAMESPACE_PROPERTY, getNamespace());
        node.setProperty(ORDER_PROPERTY, getOrder());
        node.setProperty(LOCALNAME_PROPERTY, getLocalName());
        node.setProperty(VALUE_PROPERTY, getValue());

        this.node = node;
        /*
        namespace = null;
        order = 0;
        localName = null;
        value = null;
        */
    }
    
    
    public String getUUID() {
        if (isPersistent()) {
            try {
                return node.getUUID();
            } catch (RepositoryException e) {
                logger.error("Unable to access property " + JcrConstants.JCR_UUID, e);
            }
        }
        return null;  
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
        final JCRProperty other = (JCRProperty) obj;
        if (getUUID() != other.getUUID())
            return false;
        return true;
    }

    /**
     * Constructs a <code>String</code> with all attributes
     * in name = value format.
     *
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        final String result = "Property ( "
            + "uuid = " + this.getUUID() + " "
            + "localName = " + this.getLocalName() + " "
            + "namespace = " + this.getNamespace() + " "
            + "value = " + this.getValue() 
            + " )";
    
        return result;
    }
}
