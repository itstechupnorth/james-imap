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
package org.apache.james.imap.jcr;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.util.Text;
import org.apache.james.imap.mailbox.MailboxSession;

/**
 * Utilities used for JCR 
 *
 */
public class JCRUtils implements JCRImapConstants{

    /**
     * Identifier of stored JCR Session
     */
    public final static String JCR_SESSIONS = "JCR_SESSIONS";

    
    /**
     * Create a scaled path for the given path, where scaling is the nesting mode. 
     * There will be no escaping of illegal chars at this point
     * 
     * With scaling -1 it will create a sub-path for every  char in the path. 
     * 
     * So for example:
     * 
     * scaling  = -1 
     * user/test:
     *     u/us/use/user/t/te/tes/test
     *   
     * scaling = 2
     * user/test:
     *     u/us/user/t/te/test
     *     
     * scaling = 0
     * user/test:
     *     user/test
     *     
     *     
     * @param path
     * @param scaling
     * @return scaledPath
     */
    public static String createScaledPath(String path, int scaling) {
        if (scaling == MIN_SCALING) {
            return path;
        } else {
            StringBuffer buffer = new StringBuffer();
            String[] pathParts = path.split(NODE_DELIMITER);
            
            for (int a = 0; a < pathParts.length; a++) {
                String subPath =escape(pathParts[a]);
                
                for (int i = 0; i < subPath.length(); i++) {
                    
                    if ( scaling != MAX_SCALING && i == scaling) {                       
                        buffer.append(subPath);
                        
                        if (a +1 != pathParts.length) {
                            buffer.append(NODE_DELIMITER);
                        }
                        break;
                    } else {
                        buffer.append(subPath.substring(0,i +1));

                        if (i +1 != subPath.length() || a +1 != pathParts.length) {
                            buffer.append(NODE_DELIMITER);
                        }
                    }
                }
            }
            return buffer.toString();
        }
    }
  
    public static String escape(String string) {
        return Text.escapeIllegalJcrChars(string);
    }
    
    public static String createScaledPath(String[] paths, int scaling) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            sb.append(path);
            if (i + 1 != path.length()) {
                sb.append(NODE_DELIMITER);
            }
        }
        return createScaledPath(sb.toString(), scaling);
    }

    
    
    /**
     * Create a node path recursive from the given root {@link Node}. This includes
     * to create all subnodes
     * 
     * @param rootNode
     * @param nodePath
     * @return lastNode
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws LockException
     * @throws RepositoryException
     */
    public static Node createNodeRecursive(Node rootNode, String nodePath) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        return createNodeRecursive(rootNode, nodePath, null);
    }
    
    public static Node createNodeRecursive(Node rootNode, String nodePath, String primaryNodeTypeName) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        Node parent = rootNode;
        String nodeNames[] = nodePath.split(NODE_DELIMITER);
        for (int i = 0; i < nodeNames.length; i++) {
            String nodeName = escape(nodeNames[i]);
            if (parent.hasNode(nodeName)) {
                parent = parent.getNode(nodeName);
            } else {
                if (i +1 == nodeNames.length && primaryNodeTypeName != null) {
                    parent = parent.addNode(nodeName, primaryNodeTypeName);
                } else {
                    parent = parent.addNode(nodeName);
                }
            }
        }
        return parent;
    }
    /**
     * Create a path which can be used for nodes. It handles the escaping etc
     * 
     * @param subNodes
     * @return completePath
     */
    public static String escapePath(String... subNodes) {
        StringBuffer pathBuf = new StringBuffer();

        for (int i = 0; i < subNodes.length; i++) {
            String path = subNodes[i];
            pathBuf.append(escape(path));

            if (i + 1 != subNodes.length) {
                pathBuf.append(NODE_DELIMITER);
            }
        }
        return pathBuf.toString();
    }

    /**
     * Return a List of JCR Sessions for the given MailboxSession
     * 
     * @param session
     * @return jcrSessionList
     */
    @SuppressWarnings("unchecked")
    public static List<Session> getJCRSessions(MailboxSession session) {
        List<Session> sessions = null;
        if (session != null) {
            sessions = (List<Session>) session.getAttributes().get(JCR_SESSIONS);
        }
        if (sessions == null) {
            sessions = new ArrayList<Session>();
        }
        return sessions;
    }

	/**
	 * Add the JCR Session to the MailboxSession
	 * @param session
	 * @param jcrSession
	 */
    @SuppressWarnings("unchecked")
    public static void addJCRSession(MailboxSession session, Session jcrSession) {
        if (session != null) {
            List<Session> sessions = (List<Session>) session.getAttributes().get(JCR_SESSIONS); 
            if (sessions == null) {
                sessions = new ArrayList<Session>();
            }
            sessions.add(jcrSession);
            session.getAttributes().put(JCR_SESSIONS, sessions); 
        }
    }
    

    /**
     * Register the imap CND file in the workspace
     * 
     * @param repository
     * @param workspace
     * @param username
     * @param password
     */
    public static void registerCnd(Repository repository, String workspace, String username, String password) {
        try {
            Session session;
            if (username == null) {
                session = repository.login(workspace);
            } else {
                char pass[];
                if (password == null) {
                    pass = new char[0];
                } else {
                    pass = password.toCharArray();
                }
                session = repository.login(new SimpleCredentials(username, pass), workspace);
            }
            // Register the custom node types defined in the CND file
            InputStream is = Thread.currentThread().getContextClassLoader()
                                  .getResourceAsStream("org/apache/james/imap/jcr/imap.cnd");
            CndImporter.registerNodeTypes(new InputStreamReader(is), session);
            session.logout();
        } catch (Exception e) {
            throw new RuntimeException("Unable to register cnd file", e);
        }    
    }
    
}
