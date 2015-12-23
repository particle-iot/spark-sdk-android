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

import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class WebSocketHandlerAdapter implements WebSocketHandler {

    protected WebSocketHandler nextHandler;
    protected WebSocketHandlerListener listener;
    
    /**
     * Process connect request to uri and protocol specified
     */
    @Override
    public void processConnect(WebSocketChannel channel, WSURI location, String[] protocols) {
        nextHandler.processConnect(channel, location, protocols);
    }
    
    /**
     * Send authorize token to the Gateway
     */
    @Override
    public synchronized void processAuthorize(WebSocketChannel channel, String authorizeToken) {
        nextHandler.processAuthorize(channel, authorizeToken); 
    }

    @Override
    public void processTextMessage(WebSocketChannel channel, String text) {
        nextHandler.processTextMessage(channel, text);
    }
    
    @Override
    public void processBinaryMessage(WebSocketChannel channel, WrappedByteBuffer buffer) {
        nextHandler.processBinaryMessage(channel, buffer);
    }
    
    /**
     * Disconnect the WebSocket
     */
    @Override
    public synchronized void processClose(WebSocketChannel channel, int code, String reason) {
        nextHandler.processClose(channel, code, reason);        
    }
    @Override
    public void setListener(WebSocketHandlerListener listener) {
        this.listener = listener;
    }
    
    @Override
    public synchronized void setIdleTimeout(WebSocketChannel channel, int timeout) {
        nextHandler.setIdleTimeout(channel, timeout);        
    }
    
    public void setNextHandler(WebSocketHandler handler) {
        this.nextHandler = handler;
    }
}
