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

import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerAdapter;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class WebSocketLoggingHandler extends WebSocketHandlerAdapter {
    
    private static final String CLASS_NAME = WebSocketLoggingHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    @Override
    public synchronized void processAuthorize(WebSocketChannel channel, String authorizeToken) {
        LOG.fine("->AUTHORIZE: "+channel+" "+authorizeToken);
        super.processAuthorize(channel, authorizeToken);
    }

    @Override
    public void processConnect(WebSocketChannel channel, WSURI location, String[] protocols) {
        LOG.fine("->CONNECT: "+channel+" "+location+" "+toString(protocols));
        super.processConnect(channel, location, protocols);
    }

    @Override
    public synchronized void processClose(WebSocketChannel channel, int code, String reason) {
        LOG.fine("->CLOSE: "+channel);
        super.processClose(channel, code, reason);
    }

    @Override
    public void processTextMessage(WebSocketChannel channel, String text) {
        LOG.fine("->TEXT: "+channel+" "+text);
        super.processTextMessage(channel, text);
    }
    
    @Override
    public void processBinaryMessage(WebSocketChannel channel, WrappedByteBuffer buffer) {
        LOG.fine("->BINARY: "+channel+" "+buffer.getHexDump());
        super.processBinaryMessage(channel, buffer);
    }
    
    @Override
    public void setNextHandler(WebSocketHandler nextHandler) {
        super.setNextHandler(nextHandler);
        
        nextHandler.setListener(new WebSocketHandlerListener() {
            @Override
            public void redirected(WebSocketChannel channel, String location) {
                LOG.fine("<-REDIRECTED: "+channel+" "+location);
                listener.redirected(channel, location);
            }
            
            @Override
            public void connectionOpened(WebSocketChannel channel, String protocol) {
                LOG.fine("<-OPENED: "+channel+" "+protocol);
                listener.connectionOpened(channel, protocol);
            }
            
            @Override
            public void connectionFailed(WebSocketChannel channel, Exception ex) {
                LOG.fine("<-FAILED: "+channel);
                listener.connectionFailed(channel, ex);
            }
            
            @Override
            public void connectionClosed(WebSocketChannel channel, Exception ex) {
                LOG.fine("<-CLOSED: "+channel);
                listener.connectionClosed(channel, ex);
            }
            
            @Override
            public void connectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason) {
                LOG.fine("<-CLOSED: "+channel+" "+wasClean+" "+code+": "+reason);
                listener.connectionClosed(channel, wasClean, code, reason);
            }
            
            @Override
            public void commandMessageReceived(WebSocketChannel channel, CommandMessage message) {
                LOG.fine("<-COMMAND: "+channel+" "+message);
                listener.commandMessageReceived(channel, message);
            }
            
            @Override
            public void textMessageReceived(WebSocketChannel channel, String message) {
                LOG.fine("<-TEXT: "+channel+" "+message);
                listener.textMessageReceived(channel, message);
            }
            
            @Override
            public void binaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer buf) {
                LOG.fine("<-BINARY: "+channel+" "+buf.getHexDump());
                listener.binaryMessageReceived(channel, buf);
            }
            
            @Override
            public void authenticationRequested(WebSocketChannel channel, String location, String challenge) {
                LOG.fine("<-AUTHENTICATION REQUESTED: "+channel+" "+location+" Challenge:"+challenge);
                listener.authenticationRequested(channel, location, challenge);
            }
        });
    }
    
    private String toString(String[] protocols) {
        if (protocols == null) {
            return "-";
        }
        else if (protocols.length == 1) {
            return protocols[0];
        }
        else {
            StringBuilder builder = new StringBuilder(100);
            for (int i=0; i<protocols.length; i++) {
                if (i>0) {
                    builder.append(",");
                }
                builder.append(protocols[i]);
            }
            return builder.toString();
        }
    }
}
