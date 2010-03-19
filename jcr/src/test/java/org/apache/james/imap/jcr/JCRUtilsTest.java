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

import static org.junit.Assert.*;

import org.junit.Test;

public class JCRUtilsTest {
    
    @Test
    public void testCreateScalingPath() {
        String path = "user/myname";
        
        // no scaling at all
        String scaledPath = JCRUtils.createScaledPath(path, 0);
        assertEquals(path, scaledPath);
        
        // max scaling
        String scaledPath2 = JCRUtils.createScaledPath(path, -1);     
        assertEquals("u/us/use/user/m/my/myn/myna/mynam/myname", scaledPath2);
        
        // max scaling
        String scaledPathInt = JCRUtils.createScaledPath(path, -1);     
        assertEquals("1", scaledPathInt);
        
        // test with scaling longer then any sub path
        String scaledPath3 = JCRUtils.createScaledPath(path, 10);
        assertEquals("u/us/use/user/m/my/myn/myna/mynam/myname", scaledPath3);
        
        
        // scaling of 2 
        String scaledPath4 = JCRUtils.createScaledPath(path, 2);
        assertEquals("u/us/user/m/my/myname", scaledPath4);
        
        // scaling of 4 
        String scaledPath5 = JCRUtils.createScaledPath(path, 4);
        assertEquals("u/us/use/user/m/my/myn/myna/myname", scaledPath5);
        
    }

}
