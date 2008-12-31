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
package org.apache.james.imap.functional;

import org.apache.commons.logging.Log;

public class VerboseConsoleLog implements Log {

    public void debug(Object message) {
        System.out.println(message);
    }

    public void debug(Object message, Throwable t) {
        System.out.println(message);
        t.printStackTrace();
    }

    public void error(Object message) {
        System.out.println(message);
        
    }

    public void error(Object message, Throwable t) {
        System.out.println(message);
        t.printStackTrace();
    }

    public void fatal(Object message) {
        System.out.println(message);
        
    }

    public void fatal(Object message, Throwable t) {
        System.out.println(message);
        t.printStackTrace();
    }

    public void info(Object message) {
        System.out.println(message);
        
    }

    public void info(Object message, Throwable t) {
        System.out.println(message);
        t.printStackTrace();
    }

    public boolean isDebugEnabled() {
        return true;
    }

    public boolean isErrorEnabled() {
        return true;
    }

    public boolean isFatalEnabled() {
        return true;
    }

    public boolean isInfoEnabled() {
        return true;
    }

    public boolean isTraceEnabled() {
        return true;
    }

    public boolean isWarnEnabled() {
        return true;
    }

    public void trace(Object message) {
        System.out.println(message);
        
    }

    public void trace(Object message, Throwable t) {
        System.out.println(message);
        t.printStackTrace();
    }

    public void warn(Object message) {
        System.out.println(message);
        
    }

    public void warn(Object message, Throwable t) {
        System.out.println(message);
        t.printStackTrace();
    }

}
