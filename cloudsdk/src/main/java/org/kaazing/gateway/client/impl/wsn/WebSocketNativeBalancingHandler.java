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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerAdapter;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.impl.ws.WebSocketHandshakeObject;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.util.StringUtils;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
import org.kaazing.net.http.HttpRedirectPolicy;
/*
 * WebSocket Native Handler Chain
 * NativeHandler - AuthenticationHandler - HandshakeHandler - ControlFrameHandler - {BalancingHandler} - Codec - BridgeHandler
 * Responsibilities:
 *     a). handle balancer messages
 *             balancer message is the first message after connection is established
 *             if message is "\uf0ff" + 'N' - fire connectionOpen event
 *             if message is "\uf0ff" + 'R' + redirectURl - start reConnect process
 * TODO:
 *       a). server will remove balancer message. instead, server will sent a 'HTTP 301' to redirect client
 *             client needs to change accordingly  
 */
public class WebSocketNativeBalancingHandler extends WebSocketHandlerAdapter {
    private static final String CLASS_NAME = WebSocketNativeBalancingHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Connect to the WebSocket
     * 
     * @throws Exception
     */
    @Override
    public void processConnect(WebSocketChannel channel, WSURI uri, String[] protocols) {
        LOG.entering(CLASS_NAME, "connect", new Object[] { uri, protocols });
        WebSocketNativeChannel wsChannel = (WebSocketNativeChannel)channel;
        wsChannel.balanced.set(0);
        nextHandler.processConnect(channel, uri.addQueryParameter(".kl=Y"), protocols);
    }

    /**
     * Connect to the WebSocket
     * 
     * @throws Exception
     */
    private void reconnect(WebSocketChannel channel, WSURI uri, String protocol) {
        LOG.entering(CLASS_NAME, "reconnect", new Object[] { uri, protocol });

        WebSocketNativeChannel wsChannel = (WebSocketNativeChannel)channel;
        wsChannel.redirectUri = uri;
        
        WebSocketCompositeChannel compChannel = (WebSocketCompositeChannel)channel.getParent();
        HttpRedirectPolicy option = compChannel.getFollowRedirect();
        URI currentURI = channel.getLocation().getURI();
        URI redirectURI = uri.getURI();
        
        // option will be null only for unit tests.
        if ((option != null) && (option.compare(currentURI, redirectURI) != 0)) {
            String s = String.format("%s: Cannot redirect from '%s' to '%s'",
                                     option, currentURI, redirectURI);
            channel.preventFallback = true;
            throw new IllegalStateException(s);
        }
        
        wsChannel.reconnecting.compareAndSet(false, true);
    }

    void handleBinaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer message) {
        LOG.entering(CLASS_NAME, "handleMessageReceived", message);
        WebSocketNativeChannel wsChannel = (WebSocketNativeChannel)channel;
        if (wsChannel.balanced.get() <= 1 && message.remaining() >= 4) {
            byte[] prefix = new byte[3];
            message.mark();
            message.get(prefix);
            String prefixString;
            try {
                prefixString = new String(prefix, "UTF-8");
            }
            catch (UnsupportedEncodingException e1) {
                throw new IllegalStateException(e1);
            }
            if (prefixString.charAt(0) == '\uf0ff') {
                int code = message.get();
                LOG.finest("Balancer code = " + code);
                if (code == 'N') {
                    /* Balancer responded, fire open event */
                    if (wsChannel.balanced.getAndIncrement() == 0) {
                        //first balancer message, fire kaazing handshake
                        listener.connectionOpened(channel, WebSocketHandshakeObject.KAAZING_EXTENDED_HANDSHAKE);
                    }
                    else {
                        //second balancer message, fire open
                        //TODO: how to pass 'real' protocol to client?
                        listener.connectionOpened(channel,"");
                    }
                    return;
                }
                else if (code == 'R') {
                    try {
                        String reconnectLocation = message.getString(UTF8);
                        LOG.finest("Balancer redirect location = " + StringUtils.stripControlCharacters(reconnectLocation));

                        WSURI uri = new WSURI(reconnectLocation);
                        reconnect(channel, uri, channel.getProtocol());
                        nextHandler.processClose(channel, 0, null);
                        return;
                    } 
                    catch (URISyntaxException e) {
                        LOG.log(Level.WARNING, e.getMessage(), e);
                        listener.connectionFailed(channel, e);
                    }
                    catch (Exception e) {
                        LOG.log(Level.WARNING, e.getMessage(), e);
                        listener.connectionFailed(channel, e);
                    }
                }
            }
            else {
                message.reset();
                listener.binaryMessageReceived(wsChannel, message);
            }
        }
        else {
            listener.binaryMessageReceived(channel, message);
        }
    }

    void handleTextMessageReceived(WebSocketChannel channel, String message) {
        LOG.entering(CLASS_NAME, "handleTextMessageReceived", message);
        WebSocketNativeChannel wsChannel = (WebSocketNativeChannel)channel;
        if (wsChannel.balanced.get() <= 1 &&
            message.length() >= 2 &&
            message.charAt(0) == '\uf0ff') {

            int code = message.charAt(1);
            LOG.finest("Balancer code = " + code);
            if (code == 'N') {
                /* Balancer responded, fire open event */
                // NOTE: this will cause OPEN to fire twice on the same channel, but it is currently
                // required because the Gateway sends a balancer message both before and after the
                // Extended Handshake.
                if (wsChannel.balanced.incrementAndGet() == 1) {
                    listener.connectionOpened(channel, WebSocketHandshakeObject.KAAZING_EXTENDED_HANDSHAKE);
                }
                else {
//                    listener.connectionOpened(channel, WebSocketHandshakeObject.KAAZING_EXTENDED_HANDSHAKE);
                    listener.connectionOpened(channel, "");
                }
            }
            else if (code == 'R') {
                try {
                    String reconnectLocation = message.substring(2);
                    LOG.finest("Balancer redirect location = " + StringUtils.stripControlCharacters(reconnectLocation));

                    WSURI uri = new WSURI(reconnectLocation);
                    reconnect(channel, uri, channel.getProtocol());
                    nextHandler.processClose(channel, 0, null);
                } 
                catch (URISyntaxException e) {
                    LOG.log(Level.WARNING, e.getMessage(), e);
                    listener.connectionFailed(channel, e);
                }
                catch (Exception e) {
                    LOG.log(Level.WARNING, e.getMessage(), e);
                    listener.connectionFailed(channel, e);
                }
            }
            else {
                listener.textMessageReceived(channel, message);
            }
        }
        else {
            listener.textMessageReceived(channel, message);
        }
    }
    
    public void setNextHandler(WebSocketHandler handler) {
        this.nextHandler = handler;
        
        handler.setListener(new WebSocketHandlerListener() {

            @Override
            public void connectionOpened(WebSocketChannel channel, String protocol) {
                /* We have to wait until the balancer responds for kaazing gateway */
                if (!WebSocketHandshakeObject.KAAZING_EXTENDED_HANDSHAKE.equals(protocol)) {
                    //Non-kaazing gateway, fire open event
                    WebSocketNativeChannel wsChannel = (WebSocketNativeChannel)channel;
                    wsChannel.balanced.set(2); //turn off balancer message check
                    listener.connectionOpened(channel, protocol);
                }
            }

            @Override
            public void redirected(WebSocketChannel channel, String location) {
                try {
                    LOG.finest("Balancer redirect location = " + StringUtils.stripControlCharacters(location));

                    WSURI uri = new WSURI(location);
                    reconnect(channel, uri, channel.getProtocol());
                    nextHandler.processClose(channel, 0, null);
                } 
                catch (URISyntaxException e) {
                    LOG.log(Level.WARNING, e.getMessage(), e);
                    listener.connectionFailed(channel, e);
                }
                catch (Exception e) {
                    LOG.log(Level.WARNING, e.getMessage(), e);
                    listener.connectionFailed(channel, e);
                }
            }

            @Override
            public void authenticationRequested(WebSocketChannel channel, String location, String challenge) {
                listener.authenticationRequested(channel, location, challenge);
            }

            @Override
            public void binaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer buf) {
                handleBinaryMessageReceived(channel, buf);
            }

            @Override
            public void textMessageReceived(WebSocketChannel channel, String message) {
                handleTextMessageReceived(channel, message);
            }

            @Override
            public void connectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason) {
                WebSocketNativeChannel wsChannel = (WebSocketNativeChannel)channel;
                if (wsChannel.reconnecting.compareAndSet(true, false)) {
                    //balancer redirect, open a new connection to redirectUri
                    wsChannel.reconnected.set(true);
                    
                    // add kaazing protocol header
                    String[] nextProtocols;
                    String[] requestedProtocols = wsChannel.getRequestedProtocols();
                    if (requestedProtocols == null || requestedProtocols.length == 0) {
                        nextProtocols = new String[] { WebSocketHandshakeObject.KAAZING_EXTENDED_HANDSHAKE };
                    }
                    else {
                        nextProtocols = new String[requestedProtocols.length+1];
                        nextProtocols[0] = WebSocketHandshakeObject.KAAZING_EXTENDED_HANDSHAKE;
                        for (int i=0; i<requestedProtocols.length; i++) {
                            nextProtocols[i+1] = requestedProtocols[i];
                        }
                    }
                    
                    processConnect(channel, wsChannel.redirectUri, nextProtocols);
                }
                else {
                    listener.connectionClosed(channel, wasClean, code, reason);
                }
            }

            @Override
            public void connectionClosed(WebSocketChannel channel, Exception ex) {
                listener.connectionClosed(channel, ex);
            }
                
            @Override
            public void connectionFailed(WebSocketChannel channel, Exception ex) {
                if (ex == null) {
                    listener.connectionClosed(channel, false, 0, null);
                }
                else {
                    listener.connectionClosed(channel, ex);
                }
            }

            @Override
            public void commandMessageReceived(WebSocketChannel channel, CommandMessage message) {
                listener.commandMessageReceived(channel, message);
            }
        });
    }
}
