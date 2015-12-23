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

package org.kaazing.gateway.client.impl;

import org.kaazing.gateway.client.util.WrappedByteBuffer;


/**
 * Listener for inbound handler messages (coming from the network)
 */
public interface WebSocketHandlerListener {

    /**
     * This method is called when the WebSocket is opened
     * @param channel
     */
    void connectionOpened(WebSocketChannel channel, String protocol);

    /**
     * This method is called when the WebSocket is closed
     * @param channel
     * @param wasClean TODO
     * @param code TODO
     * @param reason TODO
     */
    void connectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason);

    void connectionClosed(WebSocketChannel channel, Exception ex);

    /**
     * This method is called when a connection fails
     * @param channel
     */
    void connectionFailed(WebSocketChannel channel, Exception ex);

    /**
     * This method is called when a redirect response is 
     * @param channel
     * @param location new location for redirect
     */
    void redirected(WebSocketChannel channel, String location);

    /**
     * This method is called when authentication is requested 
     * @param channel
     */
    void authenticationRequested(WebSocketChannel channel, String location, String challenge);

    /**
     * This method is called when a text message is received on the WebSocket channel
     * @param channel
     * @param message
     */
    void textMessageReceived(WebSocketChannel channel, String message);

    /**
     * This method is called when a binary message is received on the WebSocket channel
     * @param channel
     * @param buf
     */
    void binaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer buf);

    /**
     * This method is called when a command message is received on the WebSocket channel
     * @param channel
     * @param message
     */
    void commandMessageReceived(WebSocketChannel channel, CommandMessage message);
}
