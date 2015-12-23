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

package org.kaazing.gateway.client.impl.wsn;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.kaazing.gateway.client.impl.bridge.Proxy;
import org.kaazing.gateway.client.impl.ws.WebSocketSelectedChannel;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.transport.ws.WebSocketDelegate;

public class WebSocketNativeChannel extends WebSocketSelectedChannel {

    /* Balancer attributes */
    public WSURI redirectUri;
    public final AtomicInteger balanced = new AtomicInteger(0);
    public final AtomicBoolean reconnecting = new AtomicBoolean(false);
    public final AtomicBoolean reconnected = new AtomicBoolean(false);

    /* Bridge channel */
    private Proxy proxy;
    private WebSocketDelegate delegate;

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }
    
    public Proxy getProxy() {
        return proxy;
    }
    
    public void setDelegate(WebSocketDelegate delegate) {
        this.delegate = delegate;
    }
    
    public WebSocketDelegate getDelegate() {
        return delegate;
    }
    
    public WebSocketNativeChannel(WSURI location) {
        super(location);
    }
}
