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

package org.kaazing.gateway.client.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.net.http.HttpRedirectPolicy;

public class WebSocketChannel extends Channel {    
    static volatile int         nextId = 1;

    public WebSocketHandler     transportHandler;
    public StringBuilder        handshakePayload;

    final int                   id;

    protected int               bufferedAmount = 0;

    private WSURI               location;
    private String              selectedProtocol;
    private String              negotiatedExtensions;
    private String              enabledExtensions;
    private HttpRedirectPolicy  followRedirect;

    public WebSocketChannel(WSURI location) {
        this.id = nextId++;
        
        this.location = location;
        this.handshakePayload = new StringBuilder();
    }

    /**
     * The number of bytes queued to be sent
     */
    public int getBufferedAmount() {
        return this.bufferedAmount;
    }

    public void setLocation(WSURI location) {
        this.location = location;
    }

    public WSURI getLocation() {
        return location;
    }

    public String getEnabledExtensions() {
        return enabledExtensions;
    }
    
    public void setEnabledExtensions(String extensions) {
        this.enabledExtensions = extensions;
    }

    public String getNegotiatedExtensions() {
        return negotiatedExtensions;
    }
    
    public void setNegotiatedExtensions(String extensions) {
        this.negotiatedExtensions = extensions;
    }

    public void setProtocol(String protocol) {
        this.selectedProtocol = protocol;
    }

    public String getProtocol() {
        return selectedProtocol;
    }
    
    public HttpRedirectPolicy getFollowRedirect() {
        return followRedirect;
    }
    
    public void setFollowRedirect(HttpRedirectPolicy redirectOption) {
        this.followRedirect = redirectOption;
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        if (className == null) {
            className = WebSocketChannel.class.getSimpleName();
        }
        return "["+className+" "+id+": "+location + "]";
    }
}