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

/**
 * Class representing the HTML 5 DOM event
 */
public class Event {
    /**
     * WebSocket and ByteSocket Events
     */
    public static final String MESSAGE = "message";
    public static final String OPEN = "open";
    public static final String CLOSED = "closed";
    public static final String REDIRECT = "redirect";
    public static final String AUTHENTICATE = "authenticate";

    /**
     * EventSource Events use MESSAGE, OPEN from the list above
     */
    public static final String ERROR = "error";

    /**
     * HttpRequest Events use OPEN and ERROR from the list above
     */
    public static final String READY_STATE_CHANGE = "readystatechange";
    public static final String LOAD = "load";
    public static final String ABORT = "abort";
    public static final String PROGRESS = "progress";

    private static final String[] EMPTY_PARAMS = {};
    Object[] params;
    String type;

    public Event(String type) {
        this(type, EMPTY_PARAMS);
    }

    public Event(String type, Object[] params) {
        this.type = type;
        this.params = params;
    }

    public String getType() {
        return type;
    }

    public Object[] getParams() {
        return params;
    }

    public String toString() {
        String ret = "Event[type:" + type + "{";
        for (Object a : params) {
            ret += a + " ";
        }
        return ret + "}]";
    }
}
