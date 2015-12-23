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
/*
 * WebSocket Emulated Handler Chain
 * {EmulateHandler}  
 *                |- CreateHandler - HttpRequestAuthenticationHandler - HttpRequestRedirectHandler - HttpRequestBridgeHandler
 *                   |- UpstreamHandler - HttpRequestBridgeHandler
 *                |- DownstreamHandler - HttpRequestBridgeHandler    
 * Responsibilities:
 *     a). process Connect
 *             build handler chain
 *             start createHandler.processOpen
 *             on connect, build upstreamHandler and downstreamHandler
 *     b). process close
 *             send Close frame via upstream
 *             fire connectionClosed event
 * TODO:
 *         n/a  
 */
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandlerAdapter;
import org.kaazing.gateway.client.impl.ws.CloseCommandMessage;
import org.kaazing.gateway.client.impl.ws.ReadyState;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class WebSocketEmulatedHandler extends WebSocketHandlerAdapter {
    
    static final String CLASS_NAME = WebSocketEmulatedHandler.class.getName();
    static final Logger LOG = Logger.getLogger(CLASS_NAME);

    static final String HEADER_CONTENT_TYPE = "Content-Type";
    static final String HEADER_COOKIE = "Cookie";
    static final String HEADER_SET_COOKIE = "Set-Cookie";

    static final Charset UTF_8 = Charset.forName("UTF-8");
    
    static CreateHandlerFactory createHandlerFactory = CreateHandlerImpl.FACTORY;
    static DownstreamHandlerFactory downstreamHandlerFactory = DownstreamHandlerImpl.FACTORY;
    static UpstreamHandlerFactory upstreamHandlerFactory = UpstreamHandlerImpl.FACTORY;
    
    private final CreateHandler createHandler =  createHandlerFactory.createCreateHandler();
    private final UpstreamHandler upstreamHandler =  upstreamHandlerFactory.createUpstreamHandler();
    private final DownstreamHandler downstreamHandler =  downstreamHandlerFactory.createDownstreamHandler();
    
    public WebSocketEmulatedHandler() {
        LOG.entering(CLASS_NAME, "<init>");
        initCreateHandler(createHandler);
        initUpstreamHandler(upstreamHandler);
        initDownstreamHandler(downstreamHandler);
    }
    
    void initCreateHandler(CreateHandler handler) {
        
        handler.setListener(new CreateHandlerListener() {
            
            @Override
            public void createCompleted(CreateChannel channel, HttpURI upstreamUri, HttpURI downstreamUri, String protocol) {
                LOG.entering(CLASS_NAME, "createCompleted");
                
                WebSocketEmulatedChannel parent = (WebSocketEmulatedChannel)channel.getParent();
                parent.createChannel = null;
                parent.setProtocol(protocol);
                
                long nextSequence = channel.nextSequence();
                
                UpstreamChannel upstreamChannel = new UpstreamChannel(upstreamUri, channel.cookie, nextSequence);
                upstreamChannel.setParent(parent);
                parent.upstreamChannel = upstreamChannel;
                
                DownstreamChannel downstreamChannel = new DownstreamChannel(downstreamUri, channel.cookie, nextSequence);
                downstreamChannel.setParent(parent);
                parent.downstreamChannel = downstreamChannel;

                parent.cookie = channel.cookie;

                downstreamHandler.processConnect(parent.downstreamChannel, downstreamUri);
                listener.connectionOpened(parent, protocol);
            }
            
            @Override
            public void createFailed(CreateChannel channel, Exception exception) {
                LOG.entering(CLASS_NAME, "createFailed");
                
                WebSocketEmulatedChannel parent = (WebSocketEmulatedChannel)channel.getParent();
                listener.connectionFailed(parent, exception);
            }
        });
        
    }

    void initUpstreamHandler(UpstreamHandler handler) {
        
        handler.setListener(new UpstreamHandlerListener() {
            
            @Override
            public void upstreamCompleted(UpstreamChannel channel) {
            }
            
            @Override
            public void upstreamFailed(UpstreamChannel channel, Exception exception) {
                if (channel != null && channel.parent != null) {
                    WebSocketEmulatedChannel parent = channel.parent;
                    parent.upstreamChannel = null;
                    doError(parent, exception);
                }
                else {
                    throw new IllegalStateException("WebSocket upstream channel already closed");
                }
            }
            
        });
        
    }
    
    void initDownstreamHandler(DownstreamHandler handler) {
        
        handler.setListener(new DownstreamHandlerListener() {
            
            @Override
            public void downstreamOpened(DownstreamChannel channel) {
            
            }
            
            @Override
            public void binaryMessageReceived(DownstreamChannel channel, WrappedByteBuffer data) {
                WebSocketEmulatedChannel wsebChannel = (WebSocketEmulatedChannel)channel.getParent();
                listener.binaryMessageReceived(wsebChannel, data);
            }

            @Override
            public void textMessageReceived(DownstreamChannel channel, String text) {
                WebSocketEmulatedChannel wsebChannel = (WebSocketEmulatedChannel)channel.getParent();
                listener.textMessageReceived(wsebChannel, text);
            }
            
            @Override
            public void downstreamFailed(DownstreamChannel channel, Exception exception) {
                WebSocketEmulatedChannel wsebChannel = (WebSocketEmulatedChannel)channel.getParent();
                doError(wsebChannel, exception);
            }
            
            @Override
            public void downstreamClosed(DownstreamChannel channel) {
                WebSocketEmulatedChannel wsebChannel = (WebSocketEmulatedChannel)channel.getParent();
                doClose(wsebChannel);
            }

            @Override
            public void commandMessageReceived(DownstreamChannel channel, CommandMessage message) {
                WebSocketEmulatedChannel wsebChannel = (WebSocketEmulatedChannel)channel.getParent();
                if (message instanceof CloseCommandMessage) {
                    //close frame received, save code and reason
                    CloseCommandMessage msg = (CloseCommandMessage) message;
                    wsebChannel.wasCleanClose = true;
                    wsebChannel.closeCode = msg.getCode();
                    wsebChannel.closeReason = msg.getReason();
                    
                    if (wsebChannel.getReadyState() == ReadyState.OPEN) {
                        //server initiated close, echo close command message
                        upstreamHandler.processClose(wsebChannel.upstreamChannel, msg.getCode(), msg.getReason());
                    }
                    
                }
                listener.commandMessageReceived(wsebChannel, message);
            }

            @Override
            public void pingReceived(DownstreamChannel channel) {
                WebSocketEmulatedChannel wsebChannel = (WebSocketEmulatedChannel)channel.getParent();
                
                // Server sent PING, reponse with PONG via upstream handler
                upstreamHandler.processPong(wsebChannel.upstreamChannel);
            }
        });
        
    }

    @Override
    public synchronized void processConnect(WebSocketChannel channel, WSURI location, String[] protocols) {
        LOG.entering(CLASS_NAME, "connect", channel);

        String path = location.getPath();
        if (path.endsWith("/")) {
            // eliminate duplicate slash when appending create suffix
            path = path.substring(0, path.length()-1);
        }
        
        try {
            CreateChannel createChannel = new CreateChannel();
            createChannel.setParent(channel);
            createChannel.setProtocols(protocols);
            HttpURI createUri = HttpURI.replaceScheme(location, location.getHttpEquivalentScheme())
                                       .replacePath(path + "/;e/cbm");
            createHandler.processOpen(createChannel, createUri);
            
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            listener.connectionFailed(channel, e);
        }
    }

    @Override
    public synchronized void processClose(WebSocketChannel channel, int code, String reason) {
        LOG.entering(CLASS_NAME, "processDisconnect");
        WebSocketEmulatedChannel wsebChannel = (WebSocketEmulatedChannel)channel;
        
        // ### TODO: This is temporary till Gateway sends us the CLOSE frame
        //           with code and reason while closing emulated downstream
        //           connection.
        wsebChannel.closeCode = code;
        wsebChannel.closeReason = reason;
        
        upstreamHandler.processClose(wsebChannel.upstreamChannel, code, reason);
        
    }

    @Override
    public void processTextMessage(WebSocketChannel channel, String message) {
        LOG.entering(CLASS_NAME, "processTextMessage", message);
        WebSocketEmulatedChannel wsebChannel = (WebSocketEmulatedChannel)channel;
        upstreamHandler.processTextMessage(wsebChannel.upstreamChannel, message);
    }

    @Override
    public void processBinaryMessage(WebSocketChannel channel, WrappedByteBuffer message) {
        LOG.entering(CLASS_NAME, "processBinaryMessage", message);
        WebSocketEmulatedChannel wsebChannel = (WebSocketEmulatedChannel)channel;
        upstreamHandler.processBinaryMessage(wsebChannel.upstreamChannel, message);
    }
    
    private void doError(WebSocketEmulatedChannel channel, Exception exception)
    {
        LOG.entering(CLASS_NAME, "Error handler. Tearing down WebSocket connection.");
        try
        {
            if (channel.createChannel != null)
            {
                createHandler.processClose(channel.createChannel);
            }

            if (channel.downstreamChannel != null)
            {
                downstreamHandler.processClose(channel.downstreamChannel);
            }
        }
        catch (Exception e)
        {
            LOG.entering(CLASS_NAME, "Exception while tearing down the connection: " + e.getMessage());
        }

        LOG.entering(CLASS_NAME, "Firing Close Event");
        try
        {
            listener.connectionFailed(channel, exception);
        }
        catch (Exception e)
        {
            LOG.entering(CLASS_NAME, "Unhandled exception in Close Event: " + e.getMessage());
        }
    }

    private void doClose(WebSocketEmulatedChannel channel)
    {
        LOG.entering(CLASS_NAME, "Close");
        // TODO: the 'SelectedHandler' was already setting the _readyState on the WebSocketEmulatedChannel to CLOSED
        // Commenting out the below IF statement, because it is NEVER true (since the channel state was already updated

        //if (channel._readyState == WebSocket.OPEN || channel._readyState == WebSocket.CONNECTING)
        //{
            try
            {
                // TODO: why did we set the _readyState to CLOSE?
                // The channel is passed down to the listener (see WebSocketSelectedHandler)
                // and in there (like in Java) the state is set to CLOSE _and_ we continue to
                // close the close/clean-up the connection.
                
                // Setting the state here causes that the _listener.HandleConnectionClosed()
                // is doing nothing!
                
                //channel._readyState = WebSocket.CLOSED;
                if (channel.createChannel != null)
                {
                    createHandler.processClose(channel.createChannel);
                }
                if (channel.downstreamChannel != null)
                {
                    downstreamHandler.processClose(channel.downstreamChannel);
                }
            }
            catch (Exception e)
            {
                LOG.entering(CLASS_NAME, "While closing: " + e.getMessage());
            }

            LOG.entering(CLASS_NAME, "Firing Close Event");

            try
            {
                // ### TODO: Till Gateway supports CLOSE frame, we are going to
                //           workaround with the following hardcoded values.
                channel.wasCleanClose = true;
                if (channel.closeCode == 0) {
                    channel.closeCode = CloseCommandMessage.CLOSE_NO_STATUS;
                }
                
                listener.connectionClosed(channel, channel.wasCleanClose, channel.closeCode, channel.closeReason);
            }
            catch (Exception e)
            {
                LOG.entering(CLASS_NAME, "Unhandled exception in Close Event: " + e.getMessage());
            }
        //}
    }
}
