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

package org.kaazing.net.ws.impl.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.kaazing.net.ws.WebSocketMessageReader;
import org.kaazing.net.ws.WebSocketMessageType;

/**
 * This is an internal adapter that will be used by our {@link InputStream} and 
 * {@link Reader}implementations to just invoke {@link #readBinary()} and 
 * {@link #readText()} methods to retrieve binary and text messages 
 * respectively.
 */
public class WsMessageReaderAdapter {
    private WebSocketMessageReader    _messageReader;
    
    public WsMessageReaderAdapter(WebSocketMessageReader messageReader) {
        if (messageReader == null) {
            String s = "Null WebSocketMessageReader passed in";
            throw new IllegalArgumentException(s);
        }
        
        _messageReader = messageReader;
    }

    /**
     * Returns the {@link WebSocketMessageType} of the last received message.
     * 
     * @return WebSocketMessageType
     */
    public WebSocketMessageType getType() {
        return _messageReader.getType();
    }

    /**
     * This method will be used our InputStream implementation to continually
     * retrieve binary messages.
     * 
     * @return ByteBuffer      payload of a binary message
     * @throws IOException     if this method is used to retrieve a text
     *                         message or the connection is closed
     */
    public ByteBuffer readBinary() throws IOException {
        _messageReader.next();        
        return _messageReader.getBinary();
    }

    /**
     * This method will be used our Reader implementation to continually
     * retrieve text messages.
     * 
     * @return CharSequence    payload of a text message
     * @throws IOException     if this method is used to retrieve a binary
     *                         message or the connection is closed
     */
    public CharSequence readText() throws IOException {
        _messageReader.next();        
        return _messageReader.getText();
    }
}
