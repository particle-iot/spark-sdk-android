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

package org.kaazing.net.ws;

import java.io.IOException;

import java.nio.ByteBuffer;

/**
 * {@link WebSocketMessageReader} is used to receive binary and text messages. 
 * A reference to {@link WebSocketMessageReader} can be obtained by using 
 * {@link WebSocket#getMessageReader()} or 
 * {@link WsURLConnection#getMessageReader()} methods only <b>after</b> a
 * connection has been successfully established. {@link WebSocketMessageReader}
 * allows looking at {@link WebSocketMessageType} to figure out whether it's a 
 * text or a binary message so that appropriate getter methods can be 
 * subsequently invoked to retrieve the message.
 * <p>
 * Trying to get a reference to {@link WebSocketMessageReader} before the
 * connection is established will result in an IOException. 
 * <p>
 * Once the connection is closed, a new {@link WebSocketMessageReader} should 
 * be obtained using the aforementioned methods after the connection has been
 * established. Using the old reader will result in IOException.
 */
public abstract class WebSocketMessageReader {
    /**
     * Returns the payload of the last received message. This method should
     * be invoked after {@link #next()} only if the type of the received 
     * message is {@link WebSocketMessageType#BINARY}. This is not a blocking
     * call. 
     * <p>
     * A null is returned if this method is invoked before invoking 
     * {@link #next()} method. 
     * <p>
     * If the type of the last received message is not 
     * {@link WebSocketMessageType#BINARY}, then invoking this method to obtain
     * the payload of the message as ByteBuffer will result in an IOException.
     * <p>
     * @return ByteBuffer      binary payload of the message
     * @throws IOException     if the type of the last received message is not 
     *                         {@link WebSocketMessageType#BINARY}
     */
    public abstract ByteBuffer getBinary() throws IOException;

    /**
     * Returns the payload of the last received message. This method should
     * be invoked after {@link #next()} only if the type of the received 
     * message is {@link WebSocketMessageType#TEXT}. This is not a blocking
     * call. 
     * <p>
     * A null is returned if this method is invoked before invoking 
     * {@link #next()} method. 
     * <p>
     * If the type of the last received message is not 
     * {@link WebSocketMessageType#TEXT}, then invoking this method to obtain
     * the payload of the message as CharSequence will result in an IOException.
     * <p>
     * @return CharSequence    text payload of the message
     * @throws IOException     if the type of the last received message is not 
     *                         {@link WebSocketMessageType#TEXT}
     */
    public abstract CharSequence getText() throws IOException;

    /**
     * Returns the {@link WebSocketMessageType} of the already received message.
     * This method returns a null until the first message is received.  Note 
     * that this is not a blocking call. When connected, if this method is 
     * invoked immediately after {@link #next()}, then they will return the same
     * value.
     * <p>
     * Based on the returned {@link WebSocketMessageType}, appropriate read 
     * methods can be used to receive the message. This method will continue to
     * return the same {@link WebSocketMessageType} till the next message 
     * arrives. When the next message arrives, this method will return the
     * the {@link WebSocketMessageType} associated with that message.
     * <p>

     * @return WebSocketMessageType    WebSocketMessageType.TEXT for a text 
     *                                 message; WebSocketMessageType.BINARY 
     *                                 for a binary message; WebSocketMessageType.EOS
     *                                 if the connection is closed; null before
     *                                 the first message
     */
    public abstract WebSocketMessageType getType();

    /**
     * Invoking this method will cause the thread to block until a message is 
     * received. When the message is received, this method returns the type of
     * the newly received message. Based on the returned 
     * {@link WebSocketMessageType}, appropriate getter methods can be used to 
     * retrieve the binary or text message. When the connection is closed, this
     * method returns {@link WebSocketMessageType#EOS}.
     * <p>
     * An IOException is thrown if this method is invoked before the connection
     * has been established.
     * <p>
     * @return WebSocketMessageType     WebSocketMessageType.TEXT for a text 
     *                         message; WebSocketMessageType.BINARY 
     *                         for a binary message; WebSocketMessageType.EOS
     *                         if the connection is closed
     * @throws IOException     if invoked before the connection is established
     */
    public abstract WebSocketMessageType next() throws IOException;
}
