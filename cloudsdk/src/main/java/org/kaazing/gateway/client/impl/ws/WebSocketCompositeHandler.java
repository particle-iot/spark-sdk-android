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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.ws.WebSocketSelectedHandlerImpl.WebSocketSelectedHandlerFactory;
import org.kaazing.gateway.client.impl.wseb.WebSocketEmulatedChannel;
import org.kaazing.gateway.client.impl.wseb.WebSocketEmulatedHandler;
import org.kaazing.gateway.client.impl.wsn.WebSocketNativeChannel;
import org.kaazing.gateway.client.impl.wsn.WebSocketNativeHandler;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
/*
 * WebSocket Handler Chain
 * WebSocket - {CompoisteHandler}  
 *                |- SelctedHandler - NativeHandler (see nativeHandler chain)
 *                   |- SelectedHandler - EmulatedHandler (see emulatedHandler chain)
 * Responsibilities:
 *     a). decide connection strategy
 *             use native first
 *     b). handle fallback
 *             if native failed, fallback to emulated
 *             
 * TODO:
 *         n/a
 */
public class WebSocketCompositeHandler implements WebSocketHandler {    
    private static final String CLASS_NAME = WebSocketCompositeHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private WebSocketHandlerListener handlerListener = createListener();

    static WebSocketSelectedHandlerFactory WEBSOCKET_NATIVE_HANDLER_FACTORY = new WebSocketSelectedHandlerFactory() {
        @Override
        public WebSocketSelectedHandler createSelectedHandler() {
            WebSocketSelectedHandler selectedHandler = new WebSocketSelectedHandlerImpl();
            WebSocketNativeHandler nativeHandler = new WebSocketNativeHandler();
            selectedHandler.setNextHandler(nativeHandler);
            return selectedHandler;
        }
    };
    
    static WebSocketSelectedHandlerFactory WEBSOCKET_EMULATED_HANDLER_FACTORY = new WebSocketSelectedHandlerFactory() {
        @Override
        public WebSocketSelectedHandler createSelectedHandler() {
            WebSocketSelectedHandler selectedHandler = new WebSocketSelectedHandlerImpl();
            WebSocketEmulatedHandler emulatedHandler = new WebSocketEmulatedHandler();
            selectedHandler.setNextHandler(emulatedHandler);
            return selectedHandler;
        }
    };
    
    static interface WebSocketSelectedChannelFactory {
        WebSocketSelectedChannel createChannel(WSURI location);
    }

    private final WebSocketSelectedChannelFactory WEBSOCKET_NATIVE_CHANNEL_FACTORY = new WebSocketSelectedChannelFactory() {
        @Override
        public WebSocketSelectedChannel createChannel(WSURI location) {
            return new WebSocketNativeChannel(location);
        }
    };

    private final WebSocketSelectedChannelFactory WEBSOCKET_EMULATED_CHANNEL_FACTORY = new WebSocketSelectedChannelFactory() {
        @Override
        public WebSocketSelectedChannel createChannel(WSURI location) {
            return new WebSocketEmulatedChannel(location);
        }
    };
    
    static class WebSocketStrategy {
        String nativeEquivalent; // e.g. "ws"
        WebSocketHandler handler;
        WebSocketSelectedChannelFactory channelFactory;
        
        WebSocketStrategy(String nativeEquivalent, WebSocketHandler handler, WebSocketSelectedChannelFactory channelFactory) {
            this.nativeEquivalent = nativeEquivalent;
            this.handler = handler;
            this.channelFactory = channelFactory;
        }
    }

    public static final WebSocketCompositeHandler COMPOSITE_HANDLER = new WebSocketCompositeHandler();

    final Map<String, String[]> strategyChoices = new HashMap<String, String[]>();
    final Map<String, WebSocketStrategy> strategyMap = new HashMap<String, WebSocketStrategy>();

    private WebSocketHandlerListener listener;
    
    /**
     * Creates a WebSocket that opens up a full-duplex connection to the target location on a supported WebSocket provider
     * 
     * @throws Exception
     */
    public WebSocketCompositeHandler() {
        LOG.entering(CLASS_NAME, "<init>");
        
        WebSocketSelectedHandler nativeHandler = WEBSOCKET_NATIVE_HANDLER_FACTORY.createSelectedHandler();
        nativeHandler.setListener(handlerListener);

        WebSocketSelectedHandler emulatedHandler = WEBSOCKET_EMULATED_HANDLER_FACTORY.createSelectedHandler();
        emulatedHandler.setListener(handlerListener);

        strategyChoices.put("ws",  new String[] { "java:ws", "java:wse" });
        strategyChoices.put("wss", new String[] { "java:wss", "java:wse+ssl" });
        strategyChoices.put("wsn", new String[] { "java:ws" });
        strategyChoices.put("wssn", new String[] { "java:wsn" });
        strategyChoices.put("wse", new String[] { "java:wse" });
        strategyChoices.put("wse+ssl", new String[] { "java:wse+ssl" });

        strategyMap.put("java:ws",      new WebSocketStrategy("ws",  nativeHandler,   WEBSOCKET_NATIVE_CHANNEL_FACTORY));
        strategyMap.put("java:wss",     new WebSocketStrategy("wss", nativeHandler,   WEBSOCKET_NATIVE_CHANNEL_FACTORY));
        strategyMap.put("java:wse",     new WebSocketStrategy("ws",  emulatedHandler, WEBSOCKET_EMULATED_CHANNEL_FACTORY));
        strategyMap.put("java:wse+ssl", new WebSocketStrategy("wss", emulatedHandler, WEBSOCKET_EMULATED_CHANNEL_FACTORY));
        strategyMap.put("java:wsn",     new WebSocketStrategy("wss", nativeHandler,   WEBSOCKET_NATIVE_CHANNEL_FACTORY));
    }

    /**
     * Connect the WebSocket object to the remote location
     * @param channel WebSocket channel
     * @param location location of the WebSocket
     * @param protocol protocol spoken over the WebSocket
     */
    @Override
    public void processConnect(WebSocketChannel channel, WSURI location, String[] protocols) {
        LOG.entering(CLASS_NAME, "connect", channel);
        WebSocketCompositeChannel compositeChannel = (WebSocketCompositeChannel)channel;
        
        if (compositeChannel.readyState != ReadyState.CLOSED) {
            LOG.warning("Attempt to reconnect an existing open WebSocket to a different location");
            throw new IllegalStateException("Attempt to reconnect an existing open WebSocket to a different location");
        }

        compositeChannel.readyState = ReadyState.CONNECTING;
        compositeChannel.requestedProtocols = protocols;
        
        String scheme = compositeChannel.getCompositeScheme();
        if (scheme.indexOf(":") >= 0) {
            // qualified scheme: e.g. "java:wse" 
            WebSocketStrategy strategy = strategyMap.get(scheme);
            if (strategy == null) {
                throw new IllegalArgumentException("Invalid connection scheme: "+scheme);
            }
            
            LOG.finest("Turning off fallback since the URL is prefixed with java:");
            compositeChannel.connectionStrategies.add(scheme);
        }
        else {
            String[] connectionStrategies = strategyChoices.get(scheme);
            if (connectionStrategies != null) {
                for (String each : connectionStrategies) {
                    compositeChannel.connectionStrategies.add(each);
                }
            }
            else {
                throw new IllegalArgumentException("Invalid connection scheme: "+scheme);
            }
        }

        fallbackNext(compositeChannel, null);
    }
    
    private void fallbackNext(WebSocketCompositeChannel channel, Exception exception) {
        LOG.entering(CLASS_NAME, "fallbackNext");
        try {
            String strategyName = channel.getNextStrategy();
            if (strategyName == null) {
                if (exception == null) {
                    doClose(channel, false, 1006, null);
                }
                else {
                    doClose(channel, exception);
                }
            }
            else {
                initDelegate(channel, strategyName);
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void initDelegate(WebSocketCompositeChannel channel, String strategyName) {
        WebSocketStrategy strategy = strategyMap.get(strategyName);
        WebSocketSelectedChannelFactory channelFactory = strategy.channelFactory;
        
        WSURI location = channel.getLocation();
        
        WebSocketSelectedChannel selectedChannel = channelFactory.createChannel(location);
        channel.selectedChannel = selectedChannel;
        selectedChannel.setParent(channel);
        selectedChannel.handler = (WebSocketSelectedHandler)strategy.handler;
        selectedChannel.requestedProtocols = channel.requestedProtocols;
        
        selectedChannel.handler.processConnect(channel.selectedChannel, location, channel.requestedProtocols);
    }

    /**
     * Writes the message to the WebSocket.
     * 
     * @param message
     *            String to be written
     * @throws Exception
     *             if contents cannot be written successfully
     */
    @Override
    public void processTextMessage(WebSocketChannel channel, String message) {
        LOG.entering(CLASS_NAME, "send", message);

        WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel;
        if (parent.readyState != ReadyState.OPEN) {
            LOG.warning("Attempt to post message on unopened or closed web socket");
            throw new IllegalStateException("Attempt to post message on unopened or closed web socket");
        }
        
        WebSocketSelectedChannel selectedChannel = parent.selectedChannel;
        selectedChannel.handler.processTextMessage(selectedChannel, message);
    }

    /**
     * Writes the message to the WebSocket.
     * 
     * @param message
     *            WrappedByteBuffer to be written
     * @throws Exception
     *             if contents cannot be written successfully
     */
    @Override
    public void processBinaryMessage(WebSocketChannel channel, WrappedByteBuffer message) {
        LOG.entering(CLASS_NAME, "send", message);
        
        WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel;
        if (parent.readyState != ReadyState.OPEN) {
            LOG.warning("Attempt to post message on unopened or closed web socket");
            throw new IllegalStateException("Attempt to post message on unopened or closed web socket");
        }
        
        WebSocketSelectedChannel selectedChannel = parent.selectedChannel;
        selectedChannel.handler.processBinaryMessage(selectedChannel, message);
    }

    @Override
    public void processAuthorize(WebSocketChannel channel, String authorizeToken) {
         // Currently not used
    }
    
    @Override
    public void setIdleTimeout(WebSocketChannel channel, int timeout) {
         // Currently not used
    }
    
    /**
     * Disconnect the WebSocket
     * 
     * @throws Exception
     *             if the disconnect does not succeed
     */
    @Override
    public void processClose(WebSocketChannel channel, int code, String reason) {
        LOG.entering(CLASS_NAME, "close");

        //2. check current readyState
        WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel;

        // When the connection timeout expires due to network loss, we first
        // invoke doClose() to inform the application immediately. Then, we
        // invoke processClose() to close the connection but it may take a
        // while to return. When doClose() is invoked, readyState is set to
        // CLOSED. However, we do want processClose() to be invoked all the
        // all the way down to close the connection. That's why we are no
        // longer throwing an exception here if readyState is CLOSED.
        
        if (!parent.closing) {
            parent.closing = true;
            parent.readyState = ReadyState.CLOSING;
            
            try {
                WebSocketSelectedChannel selectedChannel = parent.selectedChannel;
                selectedChannel.handler.processClose(selectedChannel, code, reason);                
            }
            catch (Exception e) {
                doClose(parent, false, CloseCommandMessage.CLOSE_ABNORMAL, e.getMessage());
            }
        }
    }

    private WebSocketHandlerListener createListener() {
        return new WebSocketHandlerListener() {
            @Override
            public void connectionOpened(WebSocketChannel channel, String protocol) {
                WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel.getParent();
                parent.setProtocol(protocol);
                doOpen(parent);
            }
    
            @Override
            public void textMessageReceived(WebSocketChannel channel, String message) {
                WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel.getParent();
                listener.textMessageReceived(parent, message);
            }
    
            @Override
            public void binaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer buf) {
                WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel.getParent();
                listener.binaryMessageReceived(parent, buf);
            }

            @Override
            public void connectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason) {
                WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel.getParent();

                // TODO: This is an abstration violation - authenticationReceived should not be exposed on Channel
                if ((parent.readyState == ReadyState.CONNECTING) && 
                    !channel.authenticationReceived              &&
                    !channel.preventFallback) {
                    fallbackNext(parent, null);
                }
                else {
                    doClose(parent, wasClean, code, reason);
                }
            }

            @Override
            public void connectionClosed(WebSocketChannel channel, Exception ex) {
                WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel.getParent();
                
                // TODO: This is an abstration violation - authenticationReceived should not be exposed on Channel
                if ((parent.readyState == ReadyState.CONNECTING) && 
                    !channel.authenticationReceived              &&
                    !channel.preventFallback) {
                    fallbackNext(parent, ex);
                }
                else {
                    if (ex == null) {
                        doClose(parent, false, 1006, null);
                    }
                    else {
                        doClose(parent, ex);
                    }
                }
            }
            
            @Override
            public void connectionFailed(WebSocketChannel channel, Exception ex) {
                WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel.getParent();
                
                // TODO: This is an abstration violation - authenticationReceived should not be exposed on Channel
                if ((parent.readyState == ReadyState.CONNECTING) && 
                    !channel.authenticationReceived              &&
                    !channel.preventFallback) {
                    fallbackNext(parent, ex);
                }
                else {
                    if (ex == null) {
                        doClose(parent, false, 1006, null);
                    }
                    else {
                        doClose(parent, ex);
                    }
                }
            }
    
            @Override
            public void authenticationRequested(WebSocketChannel channel, String location, String challenge) {
                // authenticate should not reach here
            }
    
            @Override
            public void redirected(WebSocketChannel channel, String location) {
                // redirect should not reach here
            }

            @Override
            public void commandMessageReceived(WebSocketChannel channel, CommandMessage message) {
                // ignore
            }
        };
    }
    
    private void doOpen(WebSocketCompositeChannel channel) {
        if (channel.readyState == ReadyState.CONNECTING) {
            channel.readyState = ReadyState.OPEN;
            listener.connectionOpened(channel, channel.getProtocol());
        }
    }

    public void doClose(WebSocketCompositeChannel channel, boolean wasClean, int code, String reason) {
        if (channel.readyState == ReadyState.CONNECTING || channel.readyState == ReadyState.CLOSING || channel.readyState == ReadyState.OPEN) {
            channel.readyState = ReadyState.CLOSED;
            listener.connectionClosed(channel, wasClean, code, reason);
        }
    }

    public void doClose(WebSocketCompositeChannel channel, Exception ex) {
        if (channel.readyState == ReadyState.CONNECTING || channel.readyState == ReadyState.CLOSING || channel.readyState == ReadyState.OPEN) {
            channel.readyState = ReadyState.CLOSED;
            listener.connectionClosed(channel, ex);
        }
    }
    
    public void setListener(WebSocketHandlerListener listener) {
        this.listener = listener;
    }
}
