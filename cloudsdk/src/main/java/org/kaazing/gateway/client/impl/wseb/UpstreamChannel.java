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

package org.kaazing.gateway.client.impl.wseb;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.kaazing.gateway.client.impl.Channel;
import org.kaazing.gateway.client.impl.http.HttpRequest;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

class UpstreamChannel extends Channel {
    HttpURI location;
    String cookie;

    ConcurrentLinkedQueue<WrappedByteBuffer> sendQueue = new ConcurrentLinkedQueue<WrappedByteBuffer>();
    AtomicBoolean sendInFlight = new AtomicBoolean(false);
    HttpRequest request;
    
    WebSocketEmulatedChannel parent;
    
    public UpstreamChannel(HttpURI location, String cookie) {
        this(location, cookie, 0);
    }
    
    UpstreamChannel(HttpURI location, String cookie, long sequence) {
        super(sequence);
        this.location = location;
        this.cookie = cookie;
    }
}
