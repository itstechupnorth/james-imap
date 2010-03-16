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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Session;

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
     * Create a path which can be used for nodes. It handles the escaping etc
     * 
     * @param subNodes
     * @return completePath
     */
	public static String createPath(String... subNodes) {
		StringBuffer pathBuf = new StringBuffer();
		
		for (int i = 0; i < subNodes.length; i++ ) {
			String path = subNodes[i];
			/*
			if (path.startsWith(PROPERTY_PREFIX) == false) {
				pathBuf.append(PROPERTY_PREFIX);
			}
			*/
			pathBuf.append(Text.escapeIllegalJcrChars(path));
			
			if (i +1 != subNodes.length) {
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
}
