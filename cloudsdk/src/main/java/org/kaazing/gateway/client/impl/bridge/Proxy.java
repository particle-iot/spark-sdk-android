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

package org.kaazing.gateway.client.impl.bridge;

import java.net.URI;

import org.kaazing.gateway.client.impl.bridge.XoaEvent.XoaEventKind;

/**
 * This class manages the handler id and URI associated with each proxy for the bridge.
 */
public class Proxy {
    private Integer handlerId;
    private URI uri;
    private Object peer;
    private ProxyListener listener;

    public Proxy() {
    }
    
    public URI getUri() {
        return uri;
    }

    void setUri(URI uri) {
        this.uri = uri;
    }
    
    void setListener(ProxyListener listener) {
        this.listener = listener;
    }
    
    public void setHandlerId(Integer handlerId) {
        this.handlerId = handlerId;
    }

    public Integer getHandlerId() {
        return handlerId;
    }

    public void setPeer(Object peer) {
        this.peer = peer;
    }
    
    public Object getPeer() {
        return peer;
    }
    
    void processEvent(XoaEventKind kind, Object[] params) {
        BridgeUtil.processEvent(new XoaEvent(handlerId, kind, params));
    }
    
    void eventReceived(Integer handlerId, XoaEventKind name, Object[] params) {
        listener.eventReceived(this, name, params);
    }

    public String toString() {
        return "[Proxy "+handlerId+"]";
    }
}
