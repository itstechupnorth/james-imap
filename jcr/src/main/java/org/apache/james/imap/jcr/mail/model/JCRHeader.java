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
import org.apache.james.imap.store.mail.model.AbstractComparableHeader;
import org.apache.james.imap.store.mail.model.Header;

/**
 * JCR implementation of a Header
 * 
 *
 */
public class JCRHeader extends AbstractComparableHeader{

    public final static String FIELDNAME_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "fieldName";
    public final static String VALUE_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "value";
    public final static String LINENUMBER_PROPERTY = JCRImapConstants.PROPERTY_PREFIX + "lineNumber";

    private final String fieldName;
    private final String value;
    private final int lineNumber;
    
    public JCRHeader(final String fieldName, final String value, final int lineNumber) {
        this.fieldName = fieldName;
        this.value = value;
        this.lineNumber = lineNumber;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Header#getFieldName()
     */
    public String getFieldName() {
        return fieldName;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Header#getLineNumber()
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.mail.model.Header#getValue()
     */
    public String getValue() {
        return value;
    }


    /**
     * Create a JCRHeader from the given Node
     * 
     * @param node
     * @return jcrHeader
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public static JCRHeader from(Node node) throws PathNotFoundException, RepositoryException {
        String name = node.getProperty(FIELDNAME_PROPERTY).getString();
        String value = node.getProperty(VALUE_PROPERTY).getString();
        int number = new Long(node.getProperty(LINENUMBER_PROPERTY).getLong()).intValue();
        return new JCRHeader(name, value, number);
    }
    
    /**
     * Copy all value of the given Header to the node
     * 
     * @param node
     * @param header
     * @return node
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public static Node copy(Node node, Header header) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        node.setProperty(FIELDNAME_PROPERTY, header.getFieldName());
        node.setProperty(VALUE_PROPERTY, header.getValue());
        node.setProperty(LINENUMBER_PROPERTY, header.getLineNumber());
        return node;
    }
}
