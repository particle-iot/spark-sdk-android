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
import java.io.Writer;

import org.kaazing.net.ws.WebSocketException;
import org.kaazing.net.ws.WebSocketMessageWriter;

public class WsWriterImpl extends Writer {
    private WsMessageWriterImpl   _writer;
    private StringBuffer          _stringBuffer;
    private boolean               _closed;
    
    public WsWriterImpl(WebSocketMessageWriter writer) {
        _writer = (WsMessageWriterImpl) writer;
        _stringBuffer = new StringBuffer("");
        _closed = false;
    }

    @Override
    public void close() throws IOException {
        if (isClosed()) {
            return;
        }
        
        synchronized (this) {
            if (isClosed()) {
                return;
            }
    
            _closed = true;
            _stringBuffer = null;
            _writer = null;
        }
    }

    @Override
    public void write(char[] cbuf, int offset, int length) throws IOException {
        if (cbuf == null) {
            throw new IllegalArgumentException("Null char array passed to write()");
        }

        if (offset < 0) {
            throw new StringIndexOutOfBoundsException(offset);
        }

        if (length < 0) {
            throw new StringIndexOutOfBoundsException(length);
        }

        if (offset > (cbuf.length - length)) {
            throw new StringIndexOutOfBoundsException(offset + length);
        }
        
        synchronized (this) {
            _checkWriterClosed();
            _stringBuffer.append(cbuf, offset, length);
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (this) {
            _checkWriterClosed();
            
            if (_stringBuffer.length() > 0) {
                try {
                    _stringBuffer.toString().getBytes("UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    String s = "The platform must support UTF-8 encoded text per RFC 6455";
                    throw new IOException(s);  
                }
                _writer.writeText(_stringBuffer.toString());
            }
            
            // We don't want to overwrite the buffer that is making it's way
            // through the pipeline. So, let's create a brand new instance
            // of StringBuffer to deal with future write() invocations.
            _stringBuffer = new StringBuffer("");
        }
    }
    
    // ------------------------ Internal Methods -----------------------------
    public boolean isClosed() {
        return _closed;
    }

    // ------------------------ Private Methods ------------------------------
    private void _checkWriterClosed() throws IOException {
        String s = "Cannot perform the operation on the Writer as it " +
                   "is closed";
        if (_closed ) {
            throw new WebSocketException(s);
        }        
    }
}
