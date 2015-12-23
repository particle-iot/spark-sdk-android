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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerAdapter;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

/*
 * WebSocket Handler Chain
 * WebSocket - CompoisteHandler  
 *                |- {SelctedHandler} - NativeHandler (see nativeHandler chain)
 *                   |- {SelectedHandler} - EmulatedHandler (see emulatedHandler chain)
 * Responsibilities:
 *     a). change Channel readyState when events are received
 *     b). fire events only necessary 
 *             
 * TODO:
 *         n/a
 */
public class WebSocketSelectedHandlerImpl extends WebSocketHandlerAdapter implements WebSocketSelectedHandler {
    private static final String CLASS_NAME = WebSocketSelectedHandlerImpl.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public static interface  WebSocketSelectedHandlerFactory {
        WebSocketSelectedHandler createSelectedHandler();
    }
    
    static WebSocketSelectedHandlerFactory FACTORY = new WebSocketSelectedHandlerFactory() {
        @Override
        public WebSocketSelectedHandlerImpl createSelectedHandler() {
            return new WebSocketSelectedHandlerImpl();
        }
    };
    
    protected WebSocketHandlerListener listener;
    
    public WebSocketSelectedHandlerImpl() {
        LOG.entering(CLASS_NAME, "<init>");
    }

    /**
     * Establishes the websocket connection
     */
    @Override
    public void processConnect(WebSocketChannel channel, WSURI uri, String[] protocols) {
        LOG.entering(CLASS_NAME, "connect", channel);
        
        if (((WebSocketSelectedChannel)channel).readyState == ReadyState.CLOSED) {
            throw new IllegalStateException("WebSocket is already closed");
        }
        
        nextHandler.processConnect(channel, uri, protocols);
    }
    
    @Override
    public void processClose(WebSocketChannel channel, int code, String reason) {
        LOG.entering(CLASS_NAME, "processDisconnect");
        WebSocketSelectedChannel ch = (WebSocketSelectedChannel)channel;
        if (ch.readyState == ReadyState.OPEN || ch.readyState == ReadyState.CONNECTING) {
            ch.readyState = ReadyState.CLOSING;
            nextHandler.processClose(channel, code, reason);
        }
    }

    public void handleConnectionOpened(WebSocketChannel channel, String protocol) {
        LOG.entering(CLASS_NAME, "handleConnectionOpened");

        WebSocketSelectedChannel selectedChannel = (WebSocketSelectedChannel)channel;
        if (selectedChannel.readyState == ReadyState.CONNECTING) {
            selectedChannel.readyState = ReadyState.OPEN;
            listener.connectionOpened(channel, protocol);
        }
    }


    public void handleBinaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer message) {
        LOG.entering(CLASS_NAME, "handleMessageReceived", message);

        if (((WebSocketSelectedChannel)channel).readyState != ReadyState.OPEN) {
            return;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, message.getHexDump());
        }
        
        listener.binaryMessageReceived(channel, message);
    }

    public void handleTextMessageReceived(WebSocketChannel channel, String message) {
        LOG.entering(CLASS_NAME, "handleTextMessageReceived", message);

        if (((WebSocketSelectedChannel)channel).readyState != ReadyState.OPEN) {
            return;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, message);
        }
        
        listener.textMessageReceived(channel, message);
    }

    protected void handleConnectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason) {
        LOG.entering(CLASS_NAME, "handleConnectionClosed");

        WebSocketSelectedChannel selectedChannel = (WebSocketSelectedChannel)channel;
        if (selectedChannel.readyState != ReadyState.CLOSED) {
            selectedChannel.readyState = ReadyState.CLOSED;
            listener.connectionClosed(channel, wasClean, code, reason);
        }
    }

    protected void handleConnectionClosed(WebSocketChannel channel, Exception ex) {
        LOG.entering(CLASS_NAME, "handleConnectionClosed");

        WebSocketSelectedChannel selectedChannel = (WebSocketSelectedChannel)channel;
        if (selectedChannel.readyState != ReadyState.CLOSED) {
            selectedChannel.readyState = ReadyState.CLOSED;
            listener.connectionClosed(channel, ex);
        }
    }
    
    protected void handleConnectionFailed(WebSocketChannel channel, Exception ex) {
        LOG.entering(CLASS_NAME, "connectionFailed");
        
        WebSocketSelectedChannel selectedChannel = (WebSocketSelectedChannel)channel;
        if (selectedChannel.readyState != ReadyState.CLOSED) {
            selectedChannel.readyState = ReadyState.CLOSED;
            listener.connectionFailed(channel, ex);
        }
    }

    @Override
    public void setNextHandler(WebSocketHandler nextHandler) {
        this.nextHandler = nextHandler;
        
        nextHandler.setListener(new WebSocketHandlerListener() {
            
            @Override
            public void connectionOpened(WebSocketChannel channel, String protocol) {
                handleConnectionOpened(channel, protocol);
            }
            
            @Override
            public void binaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer message) {
                handleBinaryMessageReceived(channel, message);
            }

            @Override
            public void textMessageReceived(WebSocketChannel channel, String message) {
                handleTextMessageReceived(channel, message);
            }
            
            @Override
            public void connectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason) {
                handleConnectionClosed(channel, wasClean, code, reason);
            }
            
            @Override
            public void connectionClosed(WebSocketChannel channel, Exception ex) {
                handleConnectionClosed(channel, ex);
            }
            
            @Override
            public void connectionFailed(WebSocketChannel channel, Exception ex) {
                handleConnectionFailed(channel, ex);
            }
            
            @Override
            public void redirected(WebSocketChannel channel, String location) {
            }
            
            @Override
            public void authenticationRequested(WebSocketChannel channel, String location, String challenge) {
            }

            @Override
            public void commandMessageReceived(WebSocketChannel channel, CommandMessage message) {
            }
            
        });
    }
    
    public void setListener(WebSocketHandlerListener listener) {
        this.listener = listener;
    }

}

