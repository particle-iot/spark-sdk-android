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
import java.io.Reader;
import java.nio.CharBuffer;

import org.kaazing.net.ws.WebSocketException;
import org.kaazing.net.ws.WebSocketMessageType;

public class WsReaderImpl extends Reader {
    private WsMessageReaderAdapter    _adapter;
    private CharBuffer                _charBuffer;
    private boolean                   _closed = false;
    
    public WsReaderImpl(WsMessageReaderAdapter  adapter) throws IOException {
        _adapter = adapter;
    }

    @Override
    public void close() throws IOException {
        if (_closed) {
            return;
        }
        
        if (_charBuffer != null) {
            _charBuffer.clear();
        }

        _charBuffer = null;
        _closed = true;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        checkStreamClosed();
        super.mark(readAheadLimit);
    }

    @Override
    public int read() throws IOException {
        checkStreamClosed();
        return super.read();
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        checkStreamClosed();
        return super.read(cbuf);
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        checkStreamClosed();
        return super.read(target);
    }

    @Override
    public synchronized int read(char[] cbuf, int off, int len) 
           throws IOException {
        checkStreamClosed();

        // If the buffer doesn't have data, then get the message and populate 
        // the buffer with it.
        if ((_charBuffer == null) || (_charBuffer.remaining() == 0)) {
            try {
                CharSequence text = _adapter.readText();
                _charBuffer = CharBuffer.wrap(((String)text).toCharArray());
            }
            catch (IOException ex) {
                WebSocketMessageType type = _adapter.getType();
                if ((type == WebSocketMessageType.EOS) || (type == null)) {
                    // End of stream. Return -1 as per the javadoc.
                    return -1;
                }
               
                // Reader is used to read a binary message. 
                String s = "Invalid message type: Reader must be used to " +
                           "only receive text messages";
                throw new WebSocketException(s, ex);
            }
        }

        // Use the remaining and the passed in length to decide how much can
        // be copied over into the passed in array.
        int remaining = _charBuffer.remaining();
        int retval = (remaining < len) ? remaining : len;

        _charBuffer.get(cbuf, off, retval);
        return retval;
    }

    @Override
    public boolean ready() throws IOException {
        checkStreamClosed();
        
        if ((_charBuffer == null) || !_charBuffer.hasRemaining()) {
            return false;
        }

        return true;
    }

    @Override
    public void reset() throws IOException {
        checkStreamClosed();
        super.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        checkStreamClosed();
        return super.skip(n);
    }
    
    // -------------------- Internal Implementation -------------------------
    public boolean isClosed() {
        return _closed;
    }
    
    // ---------------------- Private Methods -------------------------------
    private void checkStreamClosed() throws IOException {
        if (!_closed) {
            return;
        }
        String s = "Cannot perform the operation as the Reader is closed";
        throw new WebSocketException(s);
    }
}
