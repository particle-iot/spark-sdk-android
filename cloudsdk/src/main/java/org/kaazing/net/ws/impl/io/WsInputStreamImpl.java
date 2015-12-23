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
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.kaazing.gateway.client.util.WrappedByteBuffer;
import org.kaazing.net.ws.WebSocketException;
import org.kaazing.net.ws.WebSocketMessageType;

public class WsInputStreamImpl extends InputStream {

    private WsMessageReaderAdapter    _adapter;
    private WrappedByteBuffer         _buffer;
    private boolean                   _closed = false;
    
    public WsInputStreamImpl(WsMessageReaderAdapter adapter) throws IOException {
        _adapter = adapter;
    }

    @Override
    public synchronized int available() throws IOException {
        checkStreamClosed();
        
        if (_buffer == null) {
            return 0;
        }
        
        return _buffer.remaining();
    }
    
    @Override
    public void close() throws IOException {
        if (_closed) {
            return;
        }
        
        if (_buffer != null) {
            _buffer.clear();
        }

        _buffer = null;
        _closed = true;
    }
    
    @Override
    public void mark(int readLimit) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }
    
    @Override
    public synchronized int read() throws IOException {
        checkStreamClosed();

        try {
            prepareBuffer();
        }
        catch (IOException ex) {
            WebSocketMessageType type = _adapter.getType();
            if ((type == WebSocketMessageType.EOS) || (type == null)) {
                // End of stream. Return -1 as per the javadoc.
                return -1;
            }

            // InputStream is used to read a text message. 
            String s = "InvalidMessageType: InputStream must be used to only " +
                       "receive binary messages";
            throw new WebSocketException(s, ex);
        }
        
        return _buffer.get();
    }
    
    @Override
    public synchronized int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        checkStreamClosed();

        try {
            prepareBuffer();
        }
        catch (IOException ex) {
            WebSocketMessageType type = _adapter.getType();
            if ((type == WebSocketMessageType.EOS) || (type == null)) {
                // End of stream. Return -1 as per the javadoc.
                return -1;
            }

            // InputStream is used to read a text message. 
            String s = "InvalidMessageType: InputStream must be used to only " +
                       "receive binary messages";
            throw new WebSocketException(s, ex);
        }
        
        int remaining = _buffer.remaining();
        int retval = (remaining < len) ? remaining : len;
        _buffer.get(b, off, retval);
        
        return retval;
    }
    
    @Override
    public void reset() throws IOException {
        checkStreamClosed();
        _buffer.clear();
        _buffer = null;
    }
    
    // ---------------------- Internal Implementation -----------------------
    public boolean isClosed() {
        return _closed;
    }
    
    // ---------------------- Private Methods -------------------------------
    private void checkStreamClosed() throws IOException {
        if (!_closed) {
            return;
        }
        
        String s = "Cannot perform the operation as the InputStream is closed";
        throw new WebSocketException(s);
    }

    private void prepareBuffer() throws IOException {
        if ((_buffer == null) || (!_buffer.hasRemaining())) {
            ByteBuffer byteBuf = _adapter.readBinary();

            if (_buffer == null) {
                _buffer = new WrappedByteBuffer(byteBuf);
            }
            else {
                int pos = _buffer.position();
                int remaining = byteBuf.remaining();
                byte[] bytes = new byte[remaining];
                byteBuf.get(bytes);
                _buffer.putBytes(bytes);
                _buffer.limit(_buffer.position());
                _buffer.position(pos);
            }
        }
    }
}
