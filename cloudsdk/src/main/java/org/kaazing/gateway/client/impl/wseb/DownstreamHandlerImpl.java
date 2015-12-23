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

package org.kaazing.gateway.client.impl.wseb;

import static org.kaazing.gateway.client.impl.Channel.HEADER_SEQUENCE;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.DecoderInput;
import org.kaazing.gateway.client.impl.http.HttpRequest;
import org.kaazing.gateway.client.impl.http.HttpRequest.Method;
import org.kaazing.gateway.client.impl.http.HttpRequestHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestListener;
import org.kaazing.gateway.client.impl.http.HttpRequestTransportHandler;
import org.kaazing.gateway.client.impl.http.HttpResponse;
import org.kaazing.gateway.client.impl.ws.CloseCommandMessage;
import org.kaazing.gateway.client.impl.ws.WebSocketReAuthenticateHandler;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

/*
 * WebSocket Emulated Handler Chain
 * EmulateHandler  
 *                |- CreateHandler - HttpRequestAuthenticationHandler - HttpRequestRedirectHandler - HttpRequestBridgeHandler
 *                   |- UpstreamHandler - HttpRequestBridgeHandler
 *                |- {DownstreamHandler} - HttpRequestBridgeHandler    
 * Responsibilities:
 *     a). process receiving messages
 */
class DownstreamHandlerImpl implements DownstreamHandler {

    private static final String CLASS_NAME = DownstreamHandlerImpl.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    
    private static String IDLE_TIMEOUT_HEADER = "X-Idle-Timeout";
    
    static DownstreamHandlerFactory FACTORY = new DownstreamHandlerFactory() {
        @Override
        public DownstreamHandler createDownstreamHandler() {
            return new DownstreamHandlerImpl();
        }
    };
    
    private static final int PROXY_MODE_TIMEOUT_MILLIS = 5000;
    static boolean DISABLE_FALLBACK = false;

    private HttpRequestHandler nextHandler;
    private DownstreamHandlerListener listener;

    DownstreamHandlerImpl() {
        LOG.entering(CLASS_NAME, "<init>");

        HttpRequestHandler transportHandler = HttpRequestTransportHandler.DEFAULT_FACTORY.createHandler();
        setNextHandler(transportHandler);
    }

    @Override
    public void processConnect(final DownstreamChannel channel, final HttpURI uri) {
        LOG.entering(CLASS_NAME, "processConnect");
        makeRequest(channel, uri);
    }

    private void makeRequest(final DownstreamChannel channel, final HttpURI uri) {
        LOG.entering(CLASS_NAME, "makeRequest");

        try {
            // Cancel idle timer if running
            stopIdleTimer(channel);
            
            HttpURI requestUri = HttpURI.replaceScheme(uri.getURI(), uri.getScheme().replaceAll("ws", "http"));
            HttpRequest request = HttpRequest.HTTP_REQUEST_FACTORY.createHttpRequest(Method.POST, requestUri, true);
            request.parent = channel;
            channel.outstandingRequests.add(request);

            if (channel.cookie != null) {
                request.setHeader(WebSocketEmulatedHandler.HEADER_COOKIE, channel.cookie);
            }
            
            // Annotate request with sequence number
            request.setHeader(HEADER_SEQUENCE, Long.toString(channel.nextSequence()));
            
            nextHandler.processOpen(request);

            // Note: attemptProxyModeFallback is only set on the channel for http, not https,
            // since attempting detection for HTTPS can also lead to problems if SSL handshake
            // takes more than 5 seconds to complete
            if (!DISABLE_FALLBACK && channel.attemptProxyModeFallback.get()) {
                TimerTask timerTask = new TimerTask() {

                    @Override
                    public void run() {
                        fallbackToProxyMode(channel);
                    }
                };
                Timer t = new Timer("ProxyModeFallback", true);
                t.schedule(timerTask, PROXY_MODE_TIMEOUT_MILLIS);
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            listener.downstreamFailed(channel, e);
        }
    }

    private void fallbackToProxyMode(DownstreamChannel channel) {
        if (channel.attemptProxyModeFallback.get()) {
            LOG.fine("useProxyMode");

            channel.attemptProxyModeFallback.set(false);
            
            HttpURI uri = channel.location;
            if (uri.getQuery() == null || !uri.getQuery().contains(".ki=p")) {
                uri = channel.location.addQueryParameter(".ki=p");
                channel.location = uri;
            }
            
            makeRequest(channel, uri);
        }
    }

    private void reconnectIfNecessary(DownstreamChannel channel) {
        LOG.entering(CLASS_NAME, "reconnectIfNecessary");

        if (channel.closing.get() == true) {
            if (channel.outstandingRequests.size() == 0) {
                LOG.fine("Closing: "+channel);
                listener.downstreamClosed(channel);
            }
            
        }
        else if (channel.reconnecting.compareAndSet(true, false)) {
            // reconnect if necessary
            LOG.fine("Reconnecting: "+channel);
            makeRequest(channel, channel.location);
        } else {
            LOG.fine("Downstream failed: "+channel);
            listener.downstreamFailed(channel, new Exception("Connection closed abruptly"));
        }
    }
    
    //------------------------------Idle Timer Start/Stop/Handler---------------------//
    
    private void startIdleTimer(final DownstreamChannel downstreamChannel, int delayInMilliseconds) {
        LOG.fine("Starting idle timer");
        if (downstreamChannel.idleTimer != null) {
            downstreamChannel.idleTimer.cancel();
            downstreamChannel.idleTimer = null;
        }
        
        downstreamChannel.idleTimer = new Timer();
        downstreamChannel.idleTimer.schedule(new TimerTask() {
            
            @Override
            public void run() {
                idleTimerHandler(downstreamChannel);
            }
            
        }, delayInMilliseconds);
    }
    
    private void idleTimerHandler(DownstreamChannel downstreamChannel) {
        LOG.fine("Idle timer scheduled");
        int idleDuration = (int)(System.currentTimeMillis() - downstreamChannel.lastMessageTimestamp.get());
        if (idleDuration > downstreamChannel.idleTimeout.get()) {
            String message = "idle duration - " + idleDuration + " exceeded idle timeout - " + downstreamChannel.idleTimeout;
            LOG.fine(message);
            Exception exception = new Exception(message);
            listener.downstreamFailed(downstreamChannel, exception);
        }
        else {
            // Reschedule timer
            startIdleTimer(downstreamChannel, downstreamChannel.idleTimeout.get() - idleDuration);
        }
    }
    
    private void stopIdleTimer(DownstreamChannel downstreamChannel) {
        LOG.fine("Stopping idle timer");
        if (downstreamChannel.idleTimer != null) {
            downstreamChannel.idleTimer.cancel();
            downstreamChannel.idleTimer = null;
        }
    }
    //-------------------------------------------------------------------------------//

    DecoderInput<DownstreamChannel> in = new DecoderInput<DownstreamChannel>() {

        @Override
        public WrappedByteBuffer read(DownstreamChannel channel) {
            return channel.buffersToRead.poll();
        }
    };

    //KG-6984 move decoder into DownstreamChannel - persist state information for each websocket downstream 
    //private WebSocketEmulatedDecoder<DownstreamChannel> decoder = new WebSocketEmulatedDecoderImpl<DownstreamChannel>();

    private synchronized void processProgressEvent(DownstreamChannel channel, WrappedByteBuffer buffer) {
        LOG.entering(CLASS_NAME, "processProgressEvent", buffer);
        try {
            
            // update timestamp that is used to record the timestamp of last received message
            channel.lastMessageTimestamp.set(System.currentTimeMillis());
            channel.buffersToRead.add(buffer);

            WebSocketEmulatedDecoderListener<DownstreamChannel> decoderListener = new WebSocketEmulatedDecoderListener<DownstreamChannel>() {

                @Override
                public void messageDecoded(DownstreamChannel channel, WrappedByteBuffer message) {
                    processMessage(channel, message);
                }

                @Override
                public void messageDecoded(DownstreamChannel channel, String message) {
                    processMessage(channel, message);
                }

                @Override
                public void commandDecoded(DownstreamChannel channel, WrappedByteBuffer command) {
                    int commandByte = command.array()[0];
                    if (commandByte == 0x30 && command.array()[1] == 0x31) { //reconnect
                        // KG-5615: Set flag - but do not reconnect until request has loaded
                        LOG.fine("Reconnect command");
                        channel.reconnecting.set(true);
                    }
                    else if (commandByte == 0x30 && command.array()[1] == 0x32) {
                        channel.closing.set(true);

                        // Cancel the idle timer if running
                        stopIdleTimer(channel);
                        
                        //close frame received
                        int code = CloseCommandMessage.CLOSE_NO_STATUS;
                        String reason = null;
                        command.skip(2); //skip first 2 bytes 0x30, 0x32
                        if (command.hasRemaining()) {
                            code = command.getShort();
                        }
                        if (command.hasRemaining()) {
                            reason = command.getString(Charset.forName("UTF-8"));
                        }
                        CloseCommandMessage message = new CloseCommandMessage(code, reason);
                        listener.commandMessageReceived(channel, message);
                    }
                }

                @Override
                public void pingReceived(DownstreamChannel channel) {
                    listener.pingReceived(channel);
                }
            };

            // Prevent multiple threads from entering
            synchronized (channel.decoder) {
                channel.decoder.decode(channel, in, decoderListener);
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            e.printStackTrace();
            
            listener.downstreamFailed(channel, e);
        }
    }

    private void processMessage(DownstreamChannel channel, String message) {
        listener.textMessageReceived(channel, message);
    }

    private void processMessage(DownstreamChannel channel, WrappedByteBuffer message) {
        listener.binaryMessageReceived(channel, message);
    }

    void handleReAuthenticationRequested(DownstreamChannel channel, String location, String challenge) {
        LOG.entering(CLASS_NAME, "handleAuthenticationRequested");

        //handle revalidate event
        String url = channel.location.getScheme() + "://" + channel.location.getURI().getAuthority() + location;
        WebSocketReAuthenticateHandler reAuthHandler = new WebSocketReAuthenticateHandler();
        try {
            WebSocketEmulatedChannel parent = (WebSocketEmulatedChannel)channel.getParent();
            if (parent.redirectUri != null) {
                //this connection has been redirected to cluster member
                url = parent.redirectUri.getScheme() + "://" + parent.redirectUri.getURI().getAuthority() + location;
            }
            WebSocketEmulatedChannel revalidateChannel = new WebSocketEmulatedChannel(parent.getLocation());
            revalidateChannel.redirectUri = parent.redirectUri;
            revalidateChannel.setParent(parent.getParent());
            reAuthHandler.processOpen(revalidateChannel, new HttpURI(url));
        } catch (URISyntaxException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return;
    }

    @Override
    public void processClose(DownstreamChannel channel) {
        LOG.entering(CLASS_NAME, "stop");
        for (HttpRequest request : channel.outstandingRequests) {
            nextHandler.processAbort(request);
        }
    }

    @Override
    public void setNextHandler(HttpRequestHandler handler) {
        this.nextHandler = handler;

        handler.setListener(new HttpRequestListener() {

            @Override
            public void requestReady(HttpRequest request) {
                nextHandler.processSend(request, WrappedByteBuffer.wrap(">|<".getBytes()));
            }

            @Override
            public void requestOpened(HttpRequest request) {
                HttpResponse response = request.getResponse();
                if (response != null) {
                    DownstreamChannel channel = (DownstreamChannel) request.parent;
                    channel.attemptProxyModeFallback.set(false);
                    String idleTimeoutString = response.getHeader(IDLE_TIMEOUT_HEADER);
                    if (idleTimeoutString != null) {
                        int idleTimeout = Integer.parseInt(idleTimeoutString);
                        if (idleTimeout > 0) {
                            
                            // save in milliseconds
                            idleTimeout = idleTimeout * 1000;
                            channel.idleTimeout.set(idleTimeout);
                            channel.lastMessageTimestamp.set(System.currentTimeMillis());
                            startIdleTimer(channel, idleTimeout);   
                        }
                    }
                    listener.downstreamOpened(channel);
                }
            }

            @Override
            public void requestProgressed(HttpRequest request, WrappedByteBuffer payload) {
                DownstreamChannel channel = (DownstreamChannel) request.parent;
                processProgressEvent(channel, payload);
            }

            @Override
            public void requestLoaded(HttpRequest request, HttpResponse response) {
                LOG.entering(CLASS_NAME, "requestLoaded", request);
                DownstreamChannel channel = (DownstreamChannel) request.parent;
                channel.outstandingRequests.remove(request);
                reconnectIfNecessary(channel);
            }

            @Override
            public void requestClosed(HttpRequest request) {
            }

            @Override
            public void errorOccurred(HttpRequest request, Exception exception) {
                LOG.entering(CLASS_NAME, "errorOccurred", request);
                DownstreamChannel channel = (DownstreamChannel) request.parent;
                listener.downstreamFailed(channel, exception);
            }

            @Override
            public void requestAborted(HttpRequest request) {
                LOG.entering(CLASS_NAME, "errorOccurred", request);
            }
        });
    }

    public void setListener(DownstreamHandlerListener listener) {
        this.listener = listener;
    }
}
