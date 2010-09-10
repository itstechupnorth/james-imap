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
package org.apache.james.imap.main;

import org.apache.commons.logging.Log;

/**
 * Decorates a log adding contextual information.
 */
public class ContextualLog implements Log {
    
    private final Object context;
    private final Log decoratedLog;
    
    public ContextualLog(final Object context, final Log decoratedLog) {
        super();
        this.context = context;
        this.decoratedLog = decoratedLog;
    }

    public void debug(Object message) {
        if (isDebugEnabled()) {
            if (context == null) {
                decoratedLog.debug(message);
            } else {
                decoratedLog.debug(new Message(context, message));
            }
        }
    }

    public void debug(Object message, Throwable t) {
        if (isDebugEnabled()) {
            if (context == null) {
                decoratedLog.debug(message, t);
            } else {
                decoratedLog.debug(new Message(context, message), t);
            }
        }
    }

    public void error(Object message) {
        if (isErrorEnabled()) {
            if (context == null) {
                decoratedLog.error(message);
            } else {
                decoratedLog.error(new Message(context, message));
            }
        }
    }

    public void error(Object message, Throwable t) {
        if (isErrorEnabled()) {
            if (context == null) {
                decoratedLog.error(message, t);
            } else {
                decoratedLog.error(new Message(context, message), t);
            }
        }
    }

    public void fatal(Object message) {
        if (isFatalEnabled()) {
            if (context == null) {
                decoratedLog.fatal(message);
            } else {
                decoratedLog.fatal(new Message(context, message));
            }
        }
    }

    public void fatal(Object message, Throwable t) {
        if (isFatalEnabled()) {
            if (context == null) {
                decoratedLog.fatal(message, t);
            } else {
                decoratedLog.fatal(new Message(context, message), t);
            }
        }
    }

    public void info(Object message) {
        if (isInfoEnabled()) {
            if (context == null) {
                decoratedLog.info(message);
            } else {
                decoratedLog.info(new Message(context, message));
            }
        }
    }

    public void info(Object message, Throwable t) {
        if (isInfoEnabled()) {
            if (context == null) {
                decoratedLog.info(message, t);
            } else {
                decoratedLog.info(new Message(context, message), t);
            }
        }
    }

    public boolean isDebugEnabled() {
        return decoratedLog.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return decoratedLog.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return decoratedLog.isFatalEnabled();
    }

    public boolean isInfoEnabled() {
        return decoratedLog.isInfoEnabled();
    }

    public boolean isTraceEnabled() {
        return decoratedLog.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return decoratedLog.isWarnEnabled();
    }

    public void trace(Object message) {
        if (isTraceEnabled()) {
            if (context == null) {
                decoratedLog.trace(message);
            } else {
                decoratedLog.trace(new Message(context, message));
            }
        }
    }

    public void trace(Object message, Throwable t) {
        if (isTraceEnabled()) {
            if (context == null) {
                decoratedLog.trace(message, t);
            } else {
                decoratedLog.trace(new Message(context, message), t);
            }
        }
    }

    public void warn(Object message) {
        if (isWarnEnabled()) {
            if (context == null) {
                decoratedLog.warn(message);
            } else {
                decoratedLog.warn(new Message(context, message));
            }
        }        
    }

    public void warn(Object message, Throwable t) {
        if (isWarnEnabled()) {
            if (context == null) {
                decoratedLog.warn(message, t);
            } else {
                decoratedLog.warn(new Message(context, message), t);
            }
        }        
    }

    /**
     * Renders this object suitably for logging.
     * 
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        final String result  = "ContextualLog ( "
            + "context = " + this.context
            + " )";
    
        return result;
    }

    /**
     * Combines context with original message.
     * For logging systems which support object rendering,
     * a contextual logging aware renderer should be used.
     */
    public static final class Message {
        private final Object context;
        private final Object message;
        
        public Message(final Object context, final Object message) {
            super();
            this.context = context;
            this.message = message;
        }
        
        public Object getContext() {
            return context;
        }
        
        public Object getMessage() {
            return message;
        }
        
        public String toString() {
            return context + " " + message;
        }
    }
}
