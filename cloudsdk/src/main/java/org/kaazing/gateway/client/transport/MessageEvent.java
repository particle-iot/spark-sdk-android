/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.client.transport;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class MessageEvent extends Event {
    private static final String CLASS_NAME = MessageEvent.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private ByteBuffer data;
    private String origin;
    private String lastEventId;
    private String messageType; // "TEXT" or "BINARY"

    /**
     * Message Event
     * 
     * @param data
     * @param origin
     * @param lastEventId
     * @param type
     */
    public MessageEvent(ByteBuffer data, String origin, String lastEventId, String messageType) {
        super(Event.MESSAGE);
        LOG.entering(CLASS_NAME, "<init>", new Object[]{type, data, origin, lastEventId});
        this.data = data;
        this.origin = origin;
        this.lastEventId = lastEventId;
        this.messageType = messageType;
    }

    public ByteBuffer getData() {
        LOG.exiting(CLASS_NAME, "getData", data);
        return data;
    }

    public String getOrigin() {
        LOG.exiting(CLASS_NAME, "getOrigin", origin);
        return origin;
    }

    public String getLastEventId() {
        LOG.exiting(CLASS_NAME, "getLastEventId", lastEventId);
        return lastEventId;
    }

    /**
     * Return "TEXT" or "BINARY" depending on the message type
     * @return
     */
    public String getMessageType() {
        LOG.exiting(CLASS_NAME, "getMessageType", messageType);
        return messageType;
    }
    
    public String toString() {
        String ret = "MessageEvent [type=" + type + " messageType="+messageType+" data=" + data + " origin " + origin + " lastEventId=" + lastEventId + "{";
        for (Object a : params) {
            ret += a + " ";
        }
        return ret + "}]";
    }
}
