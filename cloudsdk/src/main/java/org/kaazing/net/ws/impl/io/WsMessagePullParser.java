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
 * The inspiration of this class is the XmlPullParser. Eventually, we may 
 * decide to expose this class as part of our public APIs. At that time, we
 * should rename it to WebSocketMessagePullParser to conform with the
 * naming convention for publicly exposed classes.
 */
public class WsMessagePullParser {
    private WebSocketMessageReader    _messageReader;
    
    public WsMessagePullParser(WebSocketMessageReader messageReader) {
        _messageReader = messageReader;
    }

    /**
     * Returns the next text message received on this connection. This method 
     * will block till a text message is received. Any binary messages that may
     * arrive will be ignored. A null is returned when the connection is closed.
     * <p>
     * An IOException is thrown if the connection has not been established
     * before invoking this method.
     * 
     * @return CharSequence     the payload of the text message
     * @throws IOException    if the connection has not been established
     */
    public CharSequence nextText() throws IOException {
        WebSocketMessageType msgType = null;
        
        while ((msgType = _messageReader.next()) != WebSocketMessageType.EOS) {
            if (msgType == WebSocketMessageType.TEXT) {
                return _messageReader.getText();
            }
        }

        return null;
    }

    /**
     * Returns the next binary message received on this connection. This method 
     * will block till a binar message is received. Any text messages that may
     * arrive will be ignored. A null is returned when the connection is closed.
     * <p>
     * An IOException is thrown if the connection has not been established
     * before invoking this method.
     * 
     * @return ByteBuffer     the payload of the binary message
     * @throws IOException    if the connection has not been established
     */
    public ByteBuffer nextBinary() throws IOException {
        WebSocketMessageType msgType = null;
        
        while ((msgType = _messageReader.next()) != WebSocketMessageType.EOS) {
            if (msgType == WebSocketMessageType.BINARY) {
                return _messageReader.getBinary();
            }
        }

        return null;
    }
}
