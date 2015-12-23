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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerAdapter;
import org.kaazing.gateway.client.impl.WebSocketHandlerFactory;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.ws.WebSocketLoggingHandler;
import org.kaazing.gateway.client.impl.ws.WebSocketTransportHandler;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

/*
 * WebSocket Native Handler Chain
 * {NativeHandler} - AuthenticationHandler - HandshakeHandler - ControlFrameHandler - BalanceingHandler - Nodec - BridgeHandler
 * Responsibilities:
 *     a). build up native handler chain
 */
public class WebSocketNativeHandler extends WebSocketHandlerAdapter {
    private static final String CLASS_NAME = WebSocketNativeHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    
    public static WebSocketHandlerFactory TRANSPORT_HANDLER_FACTORY = new WebSocketHandlerFactory() {
        @Override
        public WebSocketHandler createWebSocketHandler() {
            return new WebSocketTransportHandler();
        }
    };
    
    private WebSocketNativeAuthenticationHandler authHandler = new WebSocketNativeAuthenticationHandler();
    private WebSocketNativeHandshakeHandler handshakeHandler = new WebSocketNativeHandshakeHandler();
    private WebSocketNativeBalancingHandler balancingHandler = new WebSocketNativeBalancingHandler();
    // private WebSocketNativeCodec codec = new WebSocketNativeCodec();

    /**
     * WebSocket
     * @throws Exception
     */
    public WebSocketNativeHandler() {
        LOG.entering(CLASS_NAME, "<init>");
        
        authHandler.setNextHandler(handshakeHandler);
        handshakeHandler.setNextHandler(balancingHandler);

        WebSocketHandler transportHandler = TRANSPORT_HANDLER_FACTORY.createWebSocketHandler();
        if (LOG.isLoggable(Level.FINE)) {
            WebSocketLoggingHandler loggingHandler = new WebSocketLoggingHandler();
            loggingHandler.setNextHandler(transportHandler);
            transportHandler = loggingHandler;
        }
        
        balancingHandler.setNextHandler(transportHandler);
        
        nextHandler = authHandler;
        
        // TODO: Use WebSocketHandlerListenerAdapter
        nextHandler.setListener(new WebSocketHandlerListener() {
            
            @Override
            public void connectionOpened(WebSocketChannel channel, String protocol) {
                listener.connectionOpened(channel, protocol);
            }

            @Override
            public void binaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer buf) {
                listener.binaryMessageReceived(channel, buf);
            }

            @Override
            public void textMessageReceived(WebSocketChannel channel, String message) {
                listener.textMessageReceived(channel, message);
            }

            @Override
            public void commandMessageReceived(WebSocketChannel channel, CommandMessage message) {
                listener.commandMessageReceived(channel, message);
            }
            
            @Override
            public void connectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason) {
                listener.connectionClosed(channel, wasClean, code, reason);
            }
            
            @Override
            public void connectionClosed(WebSocketChannel channel, Exception ex) {
                listener.connectionClosed(channel, ex);
            }

            @Override
            public void connectionFailed(WebSocketChannel channel, Exception ex) {
                listener.connectionFailed(channel, ex);
            }
            
            @Override
            public void redirected(WebSocketChannel channel, String location) {
            }
            
            @Override
            public void authenticationRequested(WebSocketChannel channel, String location, String challenge) {
            }
        });
    }

    /**
     * Connect to the WebSocket
     * 
     * @throws Exception
     */
    @Override
    public void processConnect(WebSocketChannel channel, WSURI location, String[] protocols) {
        LOG.entering(CLASS_NAME, "connect", channel);

        nextHandler.processConnect(channel, location, protocols);
    }

    /**
     * The number of bytes queued to be sent
     */
    public int getBufferedAmount() {
        // Payloads are sent immediately in native-protocol mode, and we
        // do not have visibility of any buffering at the TCP layer.
        return 0;
    }
}
