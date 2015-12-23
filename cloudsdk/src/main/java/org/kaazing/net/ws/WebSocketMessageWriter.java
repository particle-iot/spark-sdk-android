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
 * {@link WebSocketMessageWriter} is used to send binary and text messages. A 
 * reference to {@link WebSocketMessageWriter} is obtained by invoking either 
 * {@link WebSocket#getMessageWriter()} or 
 * {@link WsURLConnection#getMessageWriter() methods after the connection has
 * been established. Trying to get a reference to {@link WebSocketMessageWriter}
 * before the connection is established will result in an IOException. 
 * <p>
 * Once the connection is closed, a new {@link WebSocketMessageReader} should 
 * be obtained using the aforementioned methods after the connection has been
 * established. Using the old reader will result in IOException.
 */
public abstract class WebSocketMessageWriter {

    /**
     * Sends a text message using the specified payload. Trying to write
     * after the underlying connection has been closed will result in an
     * IOException.
     * 
     * @param  src            CharSequence payload of the message
     * @throws IOException    if the connection is not open or if the connection
     *                        has been closed
     */
    public abstract void writeText(CharSequence src) throws IOException;

    /**
     * Sends a binary message using the specified payload.
     * 
     * @param  src            ByteBuffer payload of the message
     * @throws IOException    if the connection is not open or if the connection
     *                        has been closed
     */
    public abstract void writeBinary(ByteBuffer src) throws IOException;
}
