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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.kaazing.gateway.client.impl.Channel;
import org.kaazing.gateway.client.impl.http.HttpRequest;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

class DownstreamChannel extends Channel {
    HttpURI location;
    public String protocol;

    final AtomicBoolean reconnecting = new AtomicBoolean(false);
    final AtomicBoolean closing = new AtomicBoolean(false);
    final AtomicBoolean attemptProxyModeFallback = new AtomicBoolean(false);
    Set<HttpRequest> outstandingRequests = new HashSet<HttpRequest>(5);
    Queue<WrappedByteBuffer> buffersToRead = new LinkedList<WrappedByteBuffer>();
    int nextMessageAt;
    
    //--------Idle Timeout-------------//
    final AtomicInteger idleTimeout = new AtomicInteger();
    final AtomicLong lastMessageTimestamp = new AtomicLong();
    Timer idleTimer = null;
    
    /** Cookie required for auth credentials */
    String cookie;
    
    //KG-6984 move decoder into DownstreamChannel - persist state information for each websocket downstream 
    WebSocketEmulatedDecoder<DownstreamChannel> decoder;
    
    public DownstreamChannel(HttpURI location, String cookie) {
        this(location, cookie, 0);
    }
    
    public DownstreamChannel(HttpURI location, String cookie, long sequence) {
        super(sequence);
        this.cookie = cookie;
        this.location = location;
        this.decoder = new WebSocketEmulatedDecoderImpl<DownstreamChannel>();
        
        attemptProxyModeFallback.set(!location.isSecure());
    }
}