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

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerAdapter;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.auth.AuthenticationUtil;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.impl.ws.WebSocketHandshakeObject;
import org.kaazing.gateway.client.impl.ws.WebSocketReAuthenticateHandler;
import org.kaazing.gateway.client.impl.wseb.WebSocketEmulatedChannel;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.auth.ChallengeRequest;
import org.kaazing.net.auth.ChallengeResponse;
import org.kaazing.net.impl.util.ResumableTimer;

/*
 * WebSocket Native Handler Chain
 * NativeHandler - {AuthenticationHandler} - HandshakeHandler - ControlFrameHandler - BalanceingHandler - Nodec - BridgeHandler
 * Responsibilities:
 *     a). handle authenticationRequested event
 */
public class WebSocketNativeAuthenticationHandler extends WebSocketHandlerAdapter {

    private static final String CLASS_NAME = WebSocketNativeAuthenticationHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
        
    private void handleAuthenticationRequested(WebSocketChannel channel, String location, String challenge) {
        LOG.entering(CLASS_NAME, "handleAuthenticationRequested");

        channel.authenticationReceived = true;

        WSURI                  serverURI;
        WebSocketNativeChannel ch = (WebSocketNativeChannel)channel;
        ResumableTimer         connectTimer = null;

        if (((WebSocketCompositeChannel)channel.getParent()) != null) {
            WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel.getParent();
            connectTimer = parent.getConnectTimer();
            if (connectTimer != null) {
                // Pause the connect timer while the user is providing the credentials.
                connectTimer.pause();
            }
        }

        // get server location
        if (ch.redirectUri != null) {
            //this connection has been redirected
            serverURI = ch.redirectUri;
        }
        else {
            serverURI =  channel.getLocation(); 
        }

        //handle handshake 401 - use original url as ChallengeHandler lookup
        ChallengeRequest challengeRequest = new ChallengeRequest(serverURI.toString(), challenge);
        try {
            channel.challengeResponse = AuthenticationUtil.getChallengeResponse(channel, challengeRequest, channel.challengeResponse);
        } catch (Exception e) {
            clearAuthenticationCredentials(channel);
            doError(channel, e);
            //throw new IllegalStateException("Unexpected error processing challenge: "+challengeRequest, e);
            return;
        }
        char[] authResponse = channel.challengeResponse.getCredentials();
        if (authResponse == null) {
            doError(channel, new IllegalStateException("No response possible for challenge"));
            //throw new IllegalStateException("No response possible for challenge");
            return;
        }
        
        // Resume the connect timer before invoking processAuthorize().
        if (connectTimer != null) {
            connectTimer.resume();
        }

        processAuthorize(channel, String.valueOf(authResponse));
        clearAuthenticationCredentials(channel);
    }

    private void doError(WebSocketChannel channel, Exception exception) {
        LOG.entering(CLASS_NAME, "handleConnectionClosed");
        this.nextHandler.processClose(channel, 1000, null);
        listener.connectionClosed(channel, exception);
    }

    private void clearAuthenticationCredentials(WebSocketChannel channel) {
        ChallengeHandler nextChallengeHandler = null;
        if (channel.challengeResponse != null) {
            nextChallengeHandler = channel.challengeResponse.getNextChallengeHandler();
            channel.challengeResponse.clearCredentials();
            // prevent leak in case challengeResponse below throws an exception
           channel.challengeResponse = null;
        }
        channel.challengeResponse = new ChallengeResponse(null, nextChallengeHandler);
    }

    @Override
    public void setNextHandler(WebSocketHandler handler) {
        super.setNextHandler(handler);
        
        handler.setListener(new WebSocketHandlerListener() {
            
            @Override
            public void connectionOpened(WebSocketChannel channel, String protocol) {
                clearAuthenticationCredentials(channel);
                listener.connectionOpened(channel, protocol);
            }
            
            @Override
            public void redirected(WebSocketChannel channel, String location) {
                clearAuthenticationCredentials(channel);
                listener.redirected(channel, location);
            }
            
            @Override
            public void authenticationRequested(WebSocketChannel channel, String location, String challenge) {
                handleAuthenticationRequested(channel, location, challenge);
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
            public void connectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason) {
                clearAuthenticationCredentials(channel);
                listener.connectionClosed(channel, wasClean, code, reason);
            }
            
            @Override
            public void connectionClosed(WebSocketChannel channel, Exception ex) {
                listener.connectionClosed(channel, ex);
            }

            @Override
            public void connectionFailed(WebSocketChannel channel, Exception ex) {
                clearAuthenticationCredentials(channel);
                listener.connectionFailed(channel, ex);
            }

            @Override
            public void commandMessageReceived(WebSocketChannel channel, CommandMessage message) {
            }
        });
    }
}
