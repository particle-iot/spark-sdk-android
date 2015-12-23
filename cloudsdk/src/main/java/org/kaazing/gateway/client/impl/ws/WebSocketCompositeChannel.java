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

import java.net.URI;
import java.util.LinkedList;
import java.util.List;


//import org.kaazing.gateway.client.html5.WebSocket;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.util.WSCompositeURI;
import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.impl.util.ResumableTimer;

public class WebSocketCompositeChannel extends WebSocketChannel {
    public WebSocketSelectedChannel selectedChannel;

    String[]                        requestedProtocols;

    protected List<String>          connectionStrategies = new LinkedList<String>();
    protected ReadyState            readyState = ReadyState.CLOSED;
    protected boolean               closing = false;

    private Object                  webSocket;
    private String                  compositeScheme;
    private ChallengeHandler        challengeHandler; // This might be temporary till we move off of legacy stuff.
    private ResumableTimer          connectTimer;

    public WebSocketCompositeChannel(WSCompositeURI location) {
        super(location.getWSEquivalent());
        this.compositeScheme = location.getScheme();
    }
    
    public ChallengeHandler getChallengeHandler() {
        return challengeHandler;
    }

    public void setChallengeHandler(ChallengeHandler challengeHandler) {
        this.challengeHandler = challengeHandler;
    }

    public ReadyState getReadyState() {
        return readyState;
    }

    public Object getWebSocket() {
        return webSocket;
    }
    
    public void setWebSocket(Object webSocket) {
        this.webSocket = webSocket;
    }
    
    public String getOrigin() {
        URI uri = getLocation().getURI();
        return uri.getScheme()+"://"+uri.getHost()+":"+uri.getPort();
    }
    
    public URI getURL() {
        return getLocation().getURI();
    }
    
    public String getCompositeScheme() {
        return compositeScheme;
    }
    
    public String getNextStrategy() {
        if (connectionStrategies.isEmpty()) {
            return null;
        }
        else {
            return connectionStrategies.remove(0);
        }
    }

    public synchronized ResumableTimer getConnectTimer() {
        return connectTimer;
    }

    public synchronized void setConnectTimer(ResumableTimer connectTimer) {
        if (this.connectTimer != null) {
            this.connectTimer.cancel();
            this.connectTimer = null;
        }

        this.connectTimer = connectTimer;
    }
}
