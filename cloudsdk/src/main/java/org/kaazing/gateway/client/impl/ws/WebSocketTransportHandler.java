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
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerAdapter;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.bridge.WebSocketNativeBridgeHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestHandler;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.impl.wsn.WebSocketNativeDelegateHandler;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class WebSocketTransportHandler extends WebSocketHandlerAdapter {
    
    private static final String CLASS_NAME = WebSocketTransportHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public WebSocketHandler WEB_SOCKET_NATIVE_HANDLER = null;
    public HttpRequestHandler HTTP_REQUEST_HANDLER = null;

    public static boolean useBridge(URI uri) {
        LOG.fine("Determine whether bridge needs to be used");

        try {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                String host = uri.getHost();
                int port = uri.getPort();
                securityManager.checkConnect(host, port);
            }

            LOG.fine("Bypassing the bridge: "+uri);
            return false;
        } catch (Exception e) {
            LOG.fine("Must use bridge: "+uri+": "+e.getMessage());
            return true;
        }
    }
    
    @Override
    public void processConnect(WebSocketChannel channel, WSURI location, String[] protocols) {
        
        WebSocketHandler transportHandler = channel.transportHandler;
        if (useBridge(location.getURI())) {
            transportHandler = new WebSocketNativeBridgeHandler();
        } else {
            transportHandler = new WebSocketNativeDelegateHandler();
        }
        
        channel.transportHandler = transportHandler;

        transportHandler.setListener(new WebSocketHandlerListener() {
            @Override
            public void redirected(WebSocketChannel channel, String location) {
                listener.redirected(channel, location);
            }
            
            @Override
            public void connectionOpened(WebSocketChannel channel, String protocol) {
                listener.connectionOpened(channel, protocol);
            }
            
            @Override
            public void connectionFailed(WebSocketChannel channel, Exception ex) {
                listener.connectionFailed(channel, ex);
            }
            
            @Override
            public void connectionClosed(WebSocketChannel channel, Exception ex) {
                listener.connectionClosed(channel, ex);
            }
            
            @Override
            public void connectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason) {
                listener.connectionClosed(channel, wasClean, code, reason);
            }
            
            @Override
            public void commandMessageReceived(WebSocketChannel channel, CommandMessage message) {
                listener.commandMessageReceived(channel, message);
            }
            
            @Override
            public void textMessageReceived(WebSocketChannel channel, String message) {
                listener.textMessageReceived(channel, message);
            }
            
            @Override
            public void binaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer buf) {
                listener.binaryMessageReceived(channel, buf);
            }
            
            @Override
            public void authenticationRequested(WebSocketChannel channel, String location, String challenge) {
                listener.authenticationRequested(channel, location, challenge);
            }
        });

        transportHandler.processConnect(channel, location, protocols);
    }

    @Override
    public void processAuthorize(WebSocketChannel channel, String authorizeToken) {
        WebSocketHandler transportHandler = channel.transportHandler;
        transportHandler.processAuthorize(channel, authorizeToken);
    }

    @Override
    public void processClose(WebSocketChannel channel, int code, String reason) {
        WebSocketHandler transportHandler = channel.transportHandler;
        transportHandler.processClose(channel, code, reason);
    }

    @Override
    public void processTextMessage(WebSocketChannel channel, String text) {
        WebSocketHandler transportHandler = channel.transportHandler;
        transportHandler.processTextMessage(channel, text);
    }
    
    @Override
    public void processBinaryMessage(WebSocketChannel channel, WrappedByteBuffer buffer) {
        WebSocketHandler transportHandler = channel.transportHandler;
        transportHandler.processBinaryMessage(channel, buffer);
    }

    @Override
    public void setIdleTimeout(WebSocketChannel channel, int timeout) {
        WebSocketHandler transportHandler = channel.transportHandler;
        transportHandler.setIdleTimeout(channel, timeout);      
    }
}
