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

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Decorates a log adding contextual information.
 */
public class ContextualLog implements Logger {

    private final Object context;
    private final Logger decoratedLog;

    public ContextualLog(final Object context, final Logger decoratedLog) {
        super();
        this.context = context;
        this.decoratedLog = decoratedLog;
    }

    public void debug(String message) {
        if (isDebugEnabled()) {
            if (context == null) {
                decoratedLog.debug(message);
            } else {
                decoratedLog.debug(new Message(context, message).toString());
            }
        }
    }

    public void debug(String message, Throwable t) {
        if (isDebugEnabled()) {
            if (context == null) {
                decoratedLog.debug(message, t);
            } else {
                decoratedLog.debug(new Message(context, message).toString(), t);
            }
        }
    }

    public void error(String message) {
        if (isErrorEnabled()) {
            if (context == null) {
                decoratedLog.error(message);
            } else {
                decoratedLog.error(new Message(context, message).toString());
            }
        }
    }

    public void error(String message, Throwable t) {
        if (isErrorEnabled()) {
            if (context == null) {
                decoratedLog.error(message, t);
            } else {
                decoratedLog.error(new Message(context, message).toString(), t);
            }
        }
    }

    public void info(String message) {
        if (isInfoEnabled()) {
            if (context == null) {
                decoratedLog.info(message);
            } else {
                decoratedLog.info(new Message(context, message).toString());
            }
        }
    }

    public void info(String message, Throwable t) {
        if (isInfoEnabled()) {
            if (context == null) {
                decoratedLog.info(message, t);
            } else {
                decoratedLog.info(new Message(context, message).toString(), t);
            }
        }
    }

    public boolean isDebugEnabled() {
        return decoratedLog.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return decoratedLog.isErrorEnabled();
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

    public void trace(String message) {
        if (isTraceEnabled()) {
            if (context == null) {
                decoratedLog.trace(message);
            } else {
                decoratedLog.trace(new Message(context, message).toString());
            }
        }
    }

    public void trace(String message, Throwable t) {
        if (isTraceEnabled()) {
            if (context == null) {
                decoratedLog.trace(message, t);
            } else {
                decoratedLog.trace(new Message(context, message).toString(), t);
            }
        }
    }

    public void warn(String message) {
        if (isWarnEnabled()) {
            if (context == null) {
                decoratedLog.warn(message);
            } else {
                decoratedLog.warn(new Message(context, message).toString());
            }
        }
    }

    public void warn(String message, Throwable t) {
        if (isWarnEnabled()) {
            if (context == null) {
                decoratedLog.warn(message, t);
            } else {
                decoratedLog.warn(new Message(context, message).toString(), t);
            }
        }
    }

    /**
     * Renders this object suitably for logging.
     * 
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        final String result = "ContextualLog ( " + "context = " + this.context + " )";

        return result;
    }

    /**
     * Combines context with original message. For logging systems which support
     * object rendering, a contextual logging aware renderer should be used.
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

    public String getName() {
        return decoratedLog.getName();
    }

    public void trace(String message, Object arg) {
        if (isTraceEnabled()) {
            if (context == null) {
                decoratedLog.trace(message, arg);
            } else {
                decoratedLog.trace(new Message(context, message).toString(), arg);
            }
        }
    }

    public void trace(String message, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            if (context == null) {
                decoratedLog.trace(message, arg1, arg2);
            } else {
                decoratedLog.trace(new Message(context, message).toString(), arg1, arg2);
            }
        }

    }

    public void trace(String message, Object[] argArray) {
        if (isTraceEnabled()) {
            if (context == null) {
                decoratedLog.trace(message, argArray);
            } else {
                decoratedLog.trace(new Message(context, message).toString(), argArray);
            }
        }
    }

    public boolean isTraceEnabled(Marker marker) {
        return decoratedLog.isTraceEnabled(marker);
    }

    public void trace(Marker marker, String msg) {
        if (isTraceEnabled(marker)) {
            if (context == null) {
                decoratedLog.trace(marker, msg);
            } else {
                decoratedLog.trace(marker, new Message(context, msg).toString());
            }
        }
    }

    public void trace(Marker marker, String message, Object arg) {
        if (isTraceEnabled(marker)) {
            if (context == null) {
                decoratedLog.trace(marker, message, arg);
            } else {
                decoratedLog.trace(marker, new Message(context, message).toString(), arg);
            }
        }
    }

    public void trace(Marker marker, String message, Object arg1, Object arg2) {
        if (isTraceEnabled(marker)) {
            if (context == null) {
                decoratedLog.trace(marker, message, arg1, arg2);
            } else {
                decoratedLog.trace(marker, new Message(context, message).toString(), arg1, arg2);
            }
        }
    }

    public void trace(Marker marker, String message, Object[] argArray) {
        if (isTraceEnabled(marker)) {
            if (context == null) {
                decoratedLog.trace(marker, message, argArray);
            } else {
                decoratedLog.trace(marker, new Message(context, message).toString(), argArray);
            }
        }
    }

    public void trace(Marker marker, String msg, Throwable t) {
        if (isTraceEnabled(marker)) {
            if (context == null) {
                decoratedLog.trace(marker, msg, t);
            } else {
                decoratedLog.trace(marker, new Message(context, msg).toString(), t);
            }
        }
    }

    public void debug(String message, Object arg) {
        if (isDebugEnabled()) {
            if (context == null) {
                decoratedLog.debug(message, arg);
            } else {
                decoratedLog.debug(new Message(context, message).toString(), arg);
            }
        }
    }

    public void debug(String message, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            if (context == null) {
                decoratedLog.debug(message, arg1, arg2);
            } else {
                decoratedLog.debug(new Message(context, message).toString(), arg1, arg2);
            }
        }
    }

    public void debug(String message, Object[] argArray) {
        if (isDebugEnabled()) {
            if (context == null) {
                decoratedLog.debug(message, argArray);
            } else {
                decoratedLog.debug(new Message(context, message).toString(), argArray);
            }
        }
    }

    public boolean isDebugEnabled(Marker marker) {
        return decoratedLog.isDebugEnabled(marker);
    }

    public void debug(Marker marker, String msg) {
        if (isDebugEnabled(marker)) {
            if (context == null) {
                decoratedLog.debug(marker, msg);
            } else {
                decoratedLog.debug(marker, new Message(context, msg).toString());
            }
        }
    }

    public void debug(Marker marker, String message, Object arg) {
        if (isDebugEnabled(marker)) {
            if (context == null) {
                decoratedLog.debug(marker, message, arg);
            } else {
                decoratedLog.debug(marker, new Message(context, message).toString(), arg);
            }
        }
    }

    public void debug(Marker marker, String message, Object arg1, Object arg2) {
        if (isDebugEnabled(marker)) {
            if (context == null) {
                decoratedLog.debug(marker, message, arg1, arg2);
            } else {
                decoratedLog.debug(new Message(context, message).toString(), arg1, arg2);
            }
        }
    }

    public void debug(Marker marker, String message, Object[] argArray) {
        if (isDebugEnabled(marker)) {
            if (context == null) {
                decoratedLog.debug(marker, message, argArray);
            } else {
                decoratedLog.debug(marker, new Message(context, message).toString(), argArray);
            }
        }
    }

    public void debug(Marker marker, String msg, Throwable t) {
        if (isDebugEnabled(marker)) {
            if (context == null) {
                decoratedLog.debug(marker, msg, t);
            } else {
                decoratedLog.debug(marker, new Message(context, msg).toString(), t);
            }
        }
    }

    public void info(String message, Object arg) {
        if (isInfoEnabled()) {
            if (context == null) {
                decoratedLog.info(message, arg);
            } else {
                decoratedLog.info(new Message(context, message).toString(), arg);
            }
        }
    }

    public void info(String message, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            if (context == null) {
                decoratedLog.info(message, arg1, arg2);
            } else {
                decoratedLog.info(new Message(context, message).toString(), arg1, arg2);
            }
        }
    }

    public void info(String message, Object[] argArray) {
        if (isInfoEnabled()) {
            if (context == null) {
                decoratedLog.info(message, argArray);
            } else {
                decoratedLog.info(new Message(context, message).toString(), argArray);
            }
        }
    }

    public boolean isInfoEnabled(Marker marker) {
        return decoratedLog.isInfoEnabled(marker);
    }

    public void info(Marker marker, String msg) {
        if (isInfoEnabled(marker)) {
            if (context == null) {
                decoratedLog.info(marker, msg);
            } else {
                decoratedLog.info(marker, new Message(context, msg).toString());
            }
        }
    }

    public void info(Marker marker, String message, Object arg) {
        if (isInfoEnabled(marker)) {
            if (context == null) {
                decoratedLog.info(marker, message, arg);
            } else {
                decoratedLog.info(marker, new Message(context, message).toString(), arg);
            }
        }
    }

    public void info(Marker marker, String message, Object arg1, Object arg2) {
        if (isInfoEnabled(marker)) {
            if (context == null) {
                decoratedLog.info(marker, message, arg1, arg2);
            } else {
                decoratedLog.info(marker, new Message(context, message).toString(), arg1, arg2);
            }
        }
    }

    public void info(Marker marker, String message, Object[] argArray) {
        if (isInfoEnabled(marker)) {
            if (context == null) {
                decoratedLog.info(marker, message, argArray);
            } else {
                decoratedLog.info(marker, new Message(context, message).toString(), argArray);
            }
        }
    }

    public void info(Marker marker, String msg, Throwable t) {
        if (isInfoEnabled(marker)) {
            if (context == null) {
                decoratedLog.info(marker, msg, t);
            } else {
                decoratedLog.info(marker, new Message(context, msg).toString(), t);
            }
        }
    }

    public void warn(String message, Object arg) {
        if (isWarnEnabled()) {
            if (context == null) {
                decoratedLog.warn(message, arg);
            } else {
                decoratedLog.warn(new Message(context, message).toString(), arg);
            }
        }
    }

    public void warn(String message, Object[] argArray) {
        if (isWarnEnabled()) {
            if (context == null) {
                decoratedLog.warn(message, argArray);
            } else {
                decoratedLog.warn(new Message(context, message).toString(), argArray);
            }
        }
    }

    public void warn(String message, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            if (context == null) {
                decoratedLog.warn(message, arg1, arg2);
            } else {
                decoratedLog.warn(new Message(context, message).toString(), arg1, arg2);
            }
        }
    }

    public boolean isWarnEnabled(Marker marker) {
        return decoratedLog.isWarnEnabled(marker);
    }

    public void warn(Marker marker, String msg) {
        if (isWarnEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(marker, msg);
            } else {
                decoratedLog.warn(marker, new Message(context, msg).toString());
            }
        }
    }

    public void warn(Marker marker, String message, Object arg) {
        if (isWarnEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(marker, message, arg);
            } else {
                decoratedLog.warn(marker, new Message(context, message).toString(), arg);
            }
        }
    }

    public void warn(Marker marker, String message, Object arg1, Object arg2) {
        if (isWarnEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(marker, message, arg1, arg2);
            } else {
                decoratedLog.warn(marker, new Message(context, message).toString(), arg1, arg2);
            }
        }
    }

    public void warn(Marker marker, String message, Object[] argArray) {
        if (isWarnEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(marker, message, argArray);
            } else {
                decoratedLog.warn(marker, new Message(context, message).toString(), argArray);
            }
        }
    }

    public void warn(Marker marker, String msg, Throwable t) {
        if (isWarnEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(marker, msg, t);
            } else {
                decoratedLog.warn(marker, new Message(context, msg).toString(), t);
            }
        }
    }

    public void error(String message, Object arg) {
        if (isErrorEnabled()) {
            if (context == null) {
                decoratedLog.warn(message, arg);
            } else {
                decoratedLog.warn(new Message(context, message).toString(), arg);
            }
        }
    }

    public void error(String message, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            if (context == null) {
                decoratedLog.warn(message, arg1, arg2);
            } else {
                decoratedLog.warn(new Message(context, message).toString(), arg1, arg2);
            }
        }
    }

    public void error(String message, Object[] argArray) {
        if (isErrorEnabled()) {
            if (context == null) {
                decoratedLog.warn(message, argArray);
            } else {
                decoratedLog.warn(new Message(context, message).toString(), argArray);
            }
        }
    }

    public boolean isErrorEnabled(Marker marker) {
        return decoratedLog.isErrorEnabled(marker);
    }

    public void error(Marker marker, String msg) {
        if (isErrorEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(msg);
            } else {
                decoratedLog.warn(marker, new Message(context, msg).toString());
            }
        }
    }

    public void error(Marker marker, String message, Object arg) {
        if (isErrorEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(marker, message, arg);
            } else {
                decoratedLog.warn(marker, new Message(context, message).toString(), arg);
            }
        }
    }

    public void error(Marker marker, String message, Object arg1, Object arg2) {
        if (isErrorEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(marker, message, arg1, arg2);
            } else {
                decoratedLog.warn(marker, new Message(context, message).toString(), arg1, arg2);
            }
        }
    }

    public void error(Marker marker, String message, Object[] argArray) {
        if (isErrorEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(marker, message, argArray);
            } else {
                decoratedLog.warn(marker, new Message(context, message).toString(), argArray);
            }
        }
    }

    public void error(Marker marker, String msg, Throwable t) {
        if (isErrorEnabled(marker)) {
            if (context == null) {
                decoratedLog.warn(marker, msg, t);
            } else {
                decoratedLog.warn(marker, new Message(context, msg).toString(), t);
            }
        }
    }
}
