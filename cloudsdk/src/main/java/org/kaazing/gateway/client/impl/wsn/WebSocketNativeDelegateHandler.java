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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.transport.AuthenticateEvent;
import org.kaazing.gateway.client.transport.CloseEvent;
import org.kaazing.gateway.client.transport.ErrorEvent;
import org.kaazing.gateway.client.transport.MessageEvent;
import org.kaazing.gateway.client.transport.OpenEvent;
import org.kaazing.gateway.client.transport.RedirectEvent;
import org.kaazing.gateway.client.transport.ws.WebSocketDelegate;
import org.kaazing.gateway.client.transport.ws.WebSocketDelegateImpl;
import org.kaazing.gateway.client.transport.ws.WebSocketDelegateListener;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class WebSocketNativeDelegateHandler implements WebSocketHandler {

    private static final String CLASS_NAME = WebSocketNativeDelegateHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final Charset UTF8 = Charset.forName("UTF-8");

    WebSocketHandlerListener listener;
    
    @Override
    public void setIdleTimeout(WebSocketChannel channel, int timeout) {
        WebSocketNativeChannel wsnChannel = (WebSocketNativeChannel)channel;
        WebSocketDelegate delegate = wsnChannel.getDelegate();
        delegate.setIdleTimeout(timeout);
    }
    
    @Override
    public void processConnect(WebSocketChannel channel, WSURI location, String[] protocols) {
        final WebSocketNativeChannel wsnChannel = (WebSocketNativeChannel)channel;
        final WebSocketCompositeChannel parentChannel = (WebSocketCompositeChannel) wsnChannel.getParent();
        long connectTimeout = 0;
        
        if (parentChannel.getConnectTimer() != null) {
            connectTimeout = parentChannel.getConnectTimer().getDelay();
        }

        URI origin;
        try {
            origin = new URI("privileged://" + getCanonicalHostPort(location.getURI()));
        
            WebSocketDelegate delegate = new WebSocketDelegateImpl(location.getURI(), origin, protocols, connectTimeout);
            wsnChannel.setDelegate(delegate);
            delegate.setListener(new WebSocketDelegateListener() {
                
                @Override
                public void opened(OpenEvent event) {
                    LOG.entering(CLASS_NAME, "opened");
                    String protocol = event.getProtocol();
                    listener.connectionOpened(wsnChannel, protocol);
                }
                
                @Override
                public void closed(CloseEvent event) {
                    LOG.entering(CLASS_NAME, "closed");
                    wsnChannel.setDelegate(null);
                    
                    Exception ex = event.getException();
                    if (ex == null) {
                        listener.connectionClosed(wsnChannel, event.wasClean(), event.getCode(), event.getReason());
                    }
                    else {
                        listener.connectionClosed(wsnChannel, ex);
                    }
                }

                @Override
                public void redirected(RedirectEvent redirectEvent) {
                    LOG.entering(CLASS_NAME, "redirected");
                    String redirectUrl = redirectEvent.getLocation();
                    listener.redirected(wsnChannel, redirectUrl);
                }
                
                @Override
                public void authenticationRequested(AuthenticateEvent authenticateEvent) {
                    LOG.entering(CLASS_NAME, "authenticationRequested");
                    String location = wsnChannel.getLocation().toString();
                    String challenge = authenticateEvent.getChallenge();
                    listener.authenticationRequested(wsnChannel, location, challenge);
                }
                
                @Override
                public void messageReceived(MessageEvent messageEvent) {
                    LOG.entering(CLASS_NAME, "messageReceived");
                    WrappedByteBuffer messageBuffer = WrappedByteBuffer.wrap(messageEvent.getData());
                    String messageType = messageEvent.getMessageType();
                    
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, messageBuffer.getHexDump());
                    }
                    
                    if (messageType == null) {
                        throw new NullPointerException("Message type is null");
                    }
                    
                    if ("TEXT".equals(messageType)) {
                        String text = messageBuffer.getString(UTF8);
                        listener.textMessageReceived(wsnChannel, text);
                    }
                    else {
                        listener.binaryMessageReceived(wsnChannel, messageBuffer);
                    }
                }
                
                @Override
                public void errorOccurred(ErrorEvent event) {
                    listener.connectionFailed(wsnChannel, event.getException());
                }
            });
                        
            delegate.processOpen();
        } catch (URISyntaxException e) {
            LOG.log(Level.FINE, "During connect processing: "+e.getMessage(), e);
            listener.connectionFailed(wsnChannel, e);
        }
    }

    @Override
    public void processClose(WebSocketChannel channel, int code, String reason) {
        WebSocketNativeChannel wsnChannel = (WebSocketNativeChannel)channel;
        try {
            WebSocketDelegate delegate = wsnChannel.getDelegate();
            delegate.processDisconnect((short)code, reason);
        } catch (IOException e) {
            LOG.log(Level.FINE, "During close processing: "+e.getMessage(), e);
            listener.connectionFailed(wsnChannel, e);
        }
    }

    @Override
    public void processAuthorize(WebSocketChannel channel, String authorizeToken) {
        WebSocketNativeChannel wsnChannel = (WebSocketNativeChannel)channel;
        WebSocketDelegate delegate = wsnChannel.getDelegate();
        delegate.processAuthorize(authorizeToken);
    }

    @Override
    public void processBinaryMessage(WebSocketChannel channel, WrappedByteBuffer buffer) {
        WebSocketNativeChannel wsnChannel = (WebSocketNativeChannel)channel;
        WebSocketDelegate delegate = wsnChannel.getDelegate();
        java.nio.ByteBuffer nioBuffer = java.nio.ByteBuffer.wrap(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        delegate.processSend(nioBuffer);
    }

    @Override
    public void processTextMessage(WebSocketChannel channel, String text) {
        WebSocketNativeChannel wsnChannel = (WebSocketNativeChannel)channel;
        WebSocketDelegate delegate = wsnChannel.getDelegate();
        delegate.processSend(text);
        // throw new IllegalStateException("Not implemented");
    }

    @Override
    public void setListener(WebSocketHandlerListener listener) {
        this.listener = listener;
    }
    
    public static String getCanonicalHostPort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            String scheme = uri.getScheme();
            if (scheme.equals("https") || scheme.equals("wss") || scheme.equals("wse+ssl")) {
                port = 443;
            }
            else {
                port = 80;
            }
        }
        return uri.getHost()+":"+port;
    }
}
