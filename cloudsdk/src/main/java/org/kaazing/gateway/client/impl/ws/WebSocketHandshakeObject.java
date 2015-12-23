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

package org.kaazing.gateway.client.impl.ws;

public class WebSocketHandshakeObject {

    private String name;
    private String escape;
    private HandshakeStatus status;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the escape
     */
    public String getEscape() {
        return escape;
    }

    /**
     * @param escape the escape to set
     */
    public void setEscape(String escape) {
        this.escape = escape;
    }

    /**
     * @return the status
     */
    public HandshakeStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(HandshakeStatus status) {
        this.status = status;
    }

    public enum HandshakeStatus {
        Pending,
        Accepted
    }

    /* Kaazing default objects */
    public final static String KAAZING_EXTENDED_HANDSHAKE = "x-kaazing-handshake";
    public static final String KAAZING_SEC_EXTENSION_IDLETIMEOUT = "x-kaazing-idle-timeout";

}
