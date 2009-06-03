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

package org.apache.james.imap.mailbox.util;

import org.apache.james.imap.mailbox.MessageRange;

public class MessageRangeImpl implements MessageRange {

    private static final int NOT_A_UID = -1;

    private final Type type;

    private final long uidFrom;

    private final long uidTo;

    private MessageRangeImpl(final Type type, final long uidFrom,
            final long uidTo) {
        super();
        this.type = type;
        this.uidFrom = uidFrom;
        this.uidTo = uidTo;
    }

    public Type getType() {
        return type;
    }

    public long getUidFrom() {
        return uidFrom;
    }

    public long getUidTo() {
        return uidTo;
    }

    public static MessageRange oneUid(long uid) {
        MessageRangeImpl result = new MessageRangeImpl(Type.ONE, uid, uid);
        return result;
    }

    public static MessageRange all() {
        MessageRangeImpl result = new MessageRangeImpl(Type.ALL, NOT_A_UID,
                NOT_A_UID);
        return result;
    }

    public static MessageRange uidRange(long from, long to) {
        MessageRangeImpl result;
        if (to == Long.MAX_VALUE || to < from) {
            to = NOT_A_UID;
            result = new MessageRangeImpl(Type.FROM, from, to);
        } else {
            result = new MessageRangeImpl(Type.RANGE, from, to);
        }
        return result;
    }

    public String toString() {
        return "TYPE: " + type + " UID: " + uidFrom + ":" + uidTo;
    }
}
