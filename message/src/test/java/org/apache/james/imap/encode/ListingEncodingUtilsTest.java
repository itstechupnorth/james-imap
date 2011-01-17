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
package org.apache.james.imap.encode;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.imap.api.message.response.ImapResponseComposer;
import org.apache.james.imap.message.response.ListResponse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class ListingEncodingUtilsTest  {

    final String nameParameter = "LIST";
    final String typeNameParameters = "A Type Name";
    
    List<String> attributesOutput;
    
    ImapResponseComposer mock;
    
    private Mockery context = new JUnit4Mockery();
    
    @Before
    public void setUp() throws Exception {
        mock = context.mock(ImapResponseComposer.class);
        attributesOutput = new ArrayList<String>();
        
        context.checking (new Expectations() {{
            oneOf(mock).listResponse(with(equal(typeNameParameters)), with(equal(attributesOutput)), 
                    with(equal('.')), with(equal(nameParameter)));
        }});
    }

    @Test
    public void testShouldAddHasChildrenToAttributes() throws Exception {
        // Setup 
        attributesOutput.add("\\HasChildren");
        ListResponse input = new ListResponse(false, false, false, false, true, false, nameParameter, '.');
            
        // Exercise
        ListingEncodingUtils.encodeListingResponse(typeNameParameters, mock, input);
    }
    
    @Test
    public void testShouldAddHasNoChildrenToAttributes() throws Exception {
        // Setup 
        attributesOutput.add("\\HasNoChildren");
        ListResponse input = new ListResponse(false, false, false, false, false, true, nameParameter, '.');
            
        // Exercise
        ListingEncodingUtils.encodeListingResponse(typeNameParameters, mock, input);
    }
}
