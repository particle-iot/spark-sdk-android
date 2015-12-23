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

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.EncoderOutput;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerAdapter;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
/*
 * WebSocket Native Handler Chain
 * NativeHandler - AuthenticationHandler - HandshakeHandler - ControlFrameHandler - BalanceingHandler - {Nodec} - BridgeHandler
 * Responsibilities:
 *     a). encode message to follow WebSocket standard
 */
public class WebSocketNativeCodec extends WebSocketHandlerAdapter {

    private WebSocketNativeEncoder encoder = new WebSocketNativeEncoderImpl();
    private EncoderOutput<WebSocketChannel> out;

    public WebSocketNativeCodec() {
    }
    
    @Override
    public void processBinaryMessage(WebSocketChannel channel, WrappedByteBuffer message) {
        encoder.encodeBinaryMessage(channel, message, out);
    }

    @Override
    public void processTextMessage(WebSocketChannel channel, String message) {
        encoder.encodeTextMessage(channel, message, out);
    }

    @Override
    public void setNextHandler(final WebSocketHandler handler) {
        super.setNextHandler(handler);
        
        out = new EncoderOutput<WebSocketChannel>() {
            @Override
            public void write(WebSocketChannel channel, WrappedByteBuffer buffer) {
                handler.processBinaryMessage(channel, buffer);
            }
        };
        
        // TODO: use WebSocketHandlerListenerAdapter
        nextHandler.setListener(new WebSocketHandlerListener() {
            @Override
            public void connectionOpened(WebSocketChannel channel, String protocol) {
                listener.connectionOpened(channel, protocol);
            }

            @Override
            public void redirected(WebSocketChannel channel, String location) {
                listener.redirected(channel, location);
            }

            @Override
            public void authenticationRequested(WebSocketChannel channel, String location, String challenge) {
                listener.authenticationRequested(channel, location, challenge);
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
        });
    }
}
