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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.kaazing.net.ws.WebSocketException;
import org.kaazing.net.ws.WebSocketMessageWriter;

public class WsOutputStreamImpl extends OutputStream {
    private WsMessageWriterImpl    _writer;
    private ByteArrayOutputStream  _byteStream;
    private boolean                _streamClosed;
    
    public WsOutputStreamImpl(WebSocketMessageWriter writer) {
        _writer = (WsMessageWriterImpl) writer;
        _byteStream = new ByteArrayOutputStream();
        _streamClosed = false;
    }
    
    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (isClosed()) {
                return;
            }
            
            _streamClosed = true;
            _byteStream.close(); 
            _byteStream = null;
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (this) {
            _checkStreamClosed();

            if (_byteStream.size() > 0) {
                byte[] bytes = _byteStream.toByteArray();
                _writer.writeBinary(ByteBuffer.wrap(bytes));
                
                _byteStream.reset();
            }
        }
    }

    @Override
    public void write(int b) throws IOException {        
        synchronized (this) {
            _checkStreamClosed();

            // The general contract for write(int) is that one byte is written to 
            // the output stream. The byte to be written is the eight low-order
            // bits of the argument b. The 24 high-order bits of b are ignored.
            byte a = (byte)(b & 0xff);
            _byteStream.write(a);
        }
    }

    // ------------------------ Internal Methods -----------------------------
    public boolean isClosed() {
        return _streamClosed;
    }
    
    // ------------------------ Private Methods ------------------------------
    private void _checkStreamClosed() throws IOException {
        String s = "Cannot perform the operation on the OutputStream as it " +
                   "is closed";
        if (_streamClosed ) {
            throw new WebSocketException(s);
        }        
    }
}
