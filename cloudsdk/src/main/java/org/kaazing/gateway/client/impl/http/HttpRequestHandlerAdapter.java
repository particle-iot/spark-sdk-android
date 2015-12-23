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

package org.kaazing.gateway.client.impl.http;

import org.kaazing.gateway.client.impl.Channel;
import org.kaazing.gateway.client.impl.ws.ReadyState;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class HttpRequestHandlerAdapter implements HttpRequestHandler {

    protected HttpRequestHandler nextHandler;
    protected HttpRequestListener listener;
    
    @Override
    public void processOpen(HttpRequest request) {
        nextHandler.processOpen(request);
    }

    @Override
    public void processSend(HttpRequest request, WrappedByteBuffer buffer) {
        nextHandler.processSend(request, buffer);
    }

    @Override
    public void processAbort(HttpRequest request) {
        nextHandler.processAbort(request);
    }

    @Override
    public void setListener(HttpRequestListener listener) {
        this.listener = listener;
    }

    public void setNextHandler(HttpRequestHandler handler) {
        this.nextHandler = handler;
    }
    

    public Channel getWebSocketChannel(HttpRequest request) {
        if (request.parent != null) {
            return request.parent.getParent();
        }
        else {
            return null;
        }
    }
    
    // return true if WebSocket connection is closing or closed
    // parameter: channel - WebSockectEmulatedChannel
    public boolean isWebSocketClosing(HttpRequest request) {
        Channel channel = getWebSocketChannel(request);
        if (channel != null && channel.getParent() != null) {
            WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel.getParent();
            if (parent != null) {
                return parent.getReadyState() == ReadyState.CLOSED || parent.getReadyState() == ReadyState.CLOSING;
            }
        }
        return false;
    }
}
