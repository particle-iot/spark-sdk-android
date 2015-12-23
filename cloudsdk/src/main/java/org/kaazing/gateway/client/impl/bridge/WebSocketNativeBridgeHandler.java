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

package org.kaazing.gateway.client.impl.bridge;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.bridge.XoaEvent.XoaEventKind;
import org.kaazing.gateway.client.impl.wsn.WebSocketNativeChannel;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
/*
 * WebSocket Native Handler Chain
 * NativeHandler - AuthenticationHandler - HandshakeHandler - ControlFrameHandler - BalanceingHandler - Nodec - {BridgeHandler}
 * Responsibilities:
 *     a). pass client actions over the bridge as events
 *     b). fire events to client when receive events from bridge (see eventReceived function)
 * TODO:
 *         n/a  
 */
public class WebSocketNativeBridgeHandler implements WebSocketHandler, ProxyListener {
    private static final String CLASS_NAME = WebSocketNativeBridgeHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    private WebSocketHandlerListener listener;

    /**
     * WebSocket
     * @throws Exception
     */
    public WebSocketNativeBridgeHandler() {
        LOG.entering(CLASS_NAME, "<init>");
    }

    /**
     * Establishes the websocket connection
     */
    @Override
    public synchronized void processConnect(WebSocketChannel channel, WSURI uri, String[] protocols) {
        LOG.entering(CLASS_NAME, "processConnect", new Object[] { uri, protocols });

        try {
            WebSocketNativeChannel nativeChannel = (WebSocketNativeChannel)channel;
            if (nativeChannel.getProxy() != null) {
                throw new IllegalStateException("Bridge proxy previously set");
            }
            
            Proxy proxy = BridgeUtil.createProxy(uri.getURI(), this);
            proxy.setPeer(channel);
            nativeChannel.setProxy(proxy);
            
            String[] params;
            if (protocols != null) {
                String s = "";
                for (int i=0; i<protocols.length; i++) {
                    if (i>0) {
                        s += ",";
                    }
                    s += protocols[i];
                }
                params = new String[] { "WEBSOCKET", uri.toString(), s, ""};
            } else {
                params = new String[] { "WEBSOCKET", uri.toString() };
            }
            proxy.processEvent(XoaEventKind.CREATE, params);
        }
        catch (Exception e) {
            LOG.log(Level.FINE, "While initializing WebSocket proxy: "+e.getMessage(), e);
            listener.connectionFailed(channel, e);
        }
    }

    /**
     * Set the authorize token for future requests for "Basic" authentication.
     */
    @Override
    public void processAuthorize(WebSocketChannel channel, String authorizeToken) {
        LOG.entering(CLASS_NAME, "processAuthorize");

        WebSocketNativeChannel nativeChannel = (WebSocketNativeChannel)channel;
        Proxy proxy = nativeChannel.getProxy();
        proxy.processEvent(XoaEventKind.AUTHORIZE, new String[] { authorizeToken });
    }

    @Override
    public void processTextMessage(WebSocketChannel channel, String text) {
        WebSocketNativeChannel nativeChannel = (WebSocketNativeChannel)channel;
        Proxy proxy = (Proxy)nativeChannel.getProxy();
        proxy.processEvent(XoaEventKind.POSTMESSAGE, new Object[] { text });
        // throw new Error("Not implemented: Use binary message for wire traffic");
    }
    
    @Override
    public void processBinaryMessage(WebSocketChannel channel, WrappedByteBuffer message) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(message.remaining());
        buffer.put(message.array(), message.arrayOffset(), message.remaining());
        buffer.flip();
        
        WebSocketNativeChannel nativeChannel = (WebSocketNativeChannel)channel;
        Proxy proxy = (Proxy)nativeChannel.getProxy();
        proxy.processEvent(XoaEventKind.POSTMESSAGE, new Object[] { buffer });
    }

    @Override
    public synchronized void processClose(WebSocketChannel channel, int code, String reason) {
        LOG.entering(CLASS_NAME, "processDisconnect");

        WebSocketNativeChannel nativeChannel = (WebSocketNativeChannel)channel;
        Proxy proxy = nativeChannel.getProxy();
        proxy.processEvent(XoaEventKind.DISCONNECT, new String[] {});
    }

    @Override
    public final void eventReceived(Proxy proxy, XoaEventKind eventKind, Object[] params) {
        LOG.entering(CLASS_NAME, "eventReceived", new Object[] { proxy.getHandlerId(), eventKind, params });
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "SOA <-- XOA:" + "id = " + proxy + " name: " + eventKind);
        }
        
        WebSocketNativeChannel channel = (WebSocketNativeChannel)proxy.getPeer();

        switch (eventKind) {
        case OPEN:
            String protocol = (String)params[0];
            listener.connectionOpened(channel, protocol);
            break;
        case CLOSED:
            channel.setProxy(null);
            listener.connectionClosed(channel, false, 1006, ""); //pass default close code and reason here
            break;
        case REDIRECT:
            String redirectUrl = (String)params[0];
            listener.redirected(channel, redirectUrl);
            break;
        case AUTHENTICATE:
            String location = channel.getLocation().toString();
            String challenge = (String)params[0];
            listener.authenticationRequested(channel, location, challenge);
            break;
        case MESSAGE:
            WrappedByteBuffer messageBuffer = WrappedByteBuffer.wrap((java.nio.ByteBuffer) params[0]);
            String messageType = params.length > 1 ? (String)params[1] : null;
            
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, messageBuffer.getHexDump());
            }
            
            if (messageType == null) {
                LOG.severe("Incompatible bridge detected");
                listener.connectionFailed(channel, new IllegalStateException("Incompatible bridge detected"));
            }
            
            if ("TEXT".equals(messageType)) {
                String text = messageBuffer.getString(UTF8);
                listener.textMessageReceived(channel, text);
            }
            else {
                listener.binaryMessageReceived(channel, messageBuffer);
            }
            break;
            
        case ERROR:
            listener.connectionFailed(channel, new IllegalStateException("ERROR event in the native bridge handler"));
            break;
        }
    }

    public void setListener(WebSocketHandlerListener listener) {
        this.listener = listener;
    }

    @Override
    public void setIdleTimeout(WebSocketChannel channel, int timeout) {

    }
}
