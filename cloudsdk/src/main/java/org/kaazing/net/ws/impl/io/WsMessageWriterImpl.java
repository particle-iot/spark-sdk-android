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
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.kaazing.net.ws.WebSocketException;
import org.kaazing.net.ws.WebSocketMessageWriter;
import org.kaazing.net.ws.impl.WebSocketImpl;

public class WsMessageWriterImpl extends WebSocketMessageWriter {
    private WebSocketImpl    _webSocket;
    private boolean          _closed = false;

    public WsMessageWriterImpl(WebSocketImpl    webSocket) {
        _webSocket = webSocket;
    }

    @Override
    public void writeText(CharSequence src) throws IOException {
        if (isClosed()) {
            String s = "Cannot write as the MessageWriter is closed";
            throw new WebSocketException(s);
        }
        try {
            src.toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            String s = "The platform must support UTF-8 encoded text per RFC 6455";
            throw new IOException(s);  
        }

        _webSocket.send(src.toString());
    }

    @Override
    public void writeBinary(ByteBuffer src) throws IOException {
        if (isClosed()) {
            String s = "Cannot write as the MessageWriter is closed";
            throw new WebSocketException(s);
        }

        _webSocket.send(src);
    }
    
    // ----------------- Internal Implementation ----------------------------
    public void close() {
        _closed = true;
        _webSocket = null;
    }
    
    public boolean isClosed() {
        return _closed;
    }
}