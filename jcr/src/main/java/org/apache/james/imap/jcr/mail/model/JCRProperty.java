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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.james.imap.jcr.JCRImapConstants;
import org.apache.james.imap.store.mail.model.AbstractComparableProperty;

/**
 * JCR implementation of a Property
 *
 */
public class JCRProperty extends AbstractComparableProperty<JCRProperty>{

    public final static String NAMESPACE_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "namespace";
    public final static String LOCALNAME_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "localName";
    public final static String VALUE_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "value";
    public final static String ORDER_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "order";
    
    private final String namespace;
    private final String localName;
    private final String value;
    private final int order;

    public JCRProperty(final String namespace, final String localName, final String value, final int order) {
        this.namespace = namespace;
        this.localName = localName;
        this.value = value;
        this.order = order;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.AbstractComparableProperty#getOrder()
     */
    public int getOrder() {
        return order;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Property#getLocalName()
     */
    public String getLocalName() {
        return localName;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Property#getNamespace()
     */
    public String getNamespace() {
        return namespace;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Property#getValue()
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Create a JCRProperty from the given node
     * 
     * @param node
     * @return property
     * @throws ValueFormatException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public static JCRProperty from(Node node) throws ValueFormatException, PathNotFoundException, RepositoryException {
        String namespace = node.getProperty(NAMESPACE_PROPERTY).getString();
        int order = new Long(node.getProperty(ORDER_PROPERTY).getLong()).intValue();
        String localname = node.getProperty(LOCALNAME_PROPERTY).getString();
        String value = node.getProperty(VALUE_PROPERTY).getString();

        return new JCRProperty(namespace, localname, value, order);
    }
    
    /**
     * Copy all value of the given Property to the node
     * 
     * @param node
     * @param property
     * @return node
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public static Node copy(Node node, AbstractComparableProperty<?> property) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        node.setProperty(NAMESPACE_PROPERTY, property.getNamespace());
        node.setProperty(ORDER_PROPERTY, property.getOrder());
        node.setProperty(LOCALNAME_PROPERTY, property.getLocalName());
        node.setProperty(VALUE_PROPERTY, property.getValue());
        return node;
    }

}
