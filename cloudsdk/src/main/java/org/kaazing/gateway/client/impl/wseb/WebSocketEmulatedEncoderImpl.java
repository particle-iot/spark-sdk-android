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

package org.kaazing.gateway.client.impl.wseb;

import org.kaazing.gateway.client.impl.EncoderOutput;
import org.kaazing.gateway.client.impl.util.WebSocketUtil;
import org.kaazing.gateway.client.util.StringUtils;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class WebSocketEmulatedEncoderImpl<C> implements WebSocketEmulatedEncoder<C> {

    private static final byte WS_BINARY_FRAME_START = (byte) 0x80;
	private static final byte WS_SPECIFIED_LENGTH_TEXT_FRAME_START = (byte) 0x81;
    
    @Override
    public void encodeBinaryMessage(C channel, WrappedByteBuffer message, EncoderOutput<C> out) {
        int length = message.remaining();

        // The largest frame that can be received is 5 bytes (encoded 32 bit length header + trailing byte)
        WrappedByteBuffer frame = WrappedByteBuffer.allocate(length + 6);
        frame.put(WS_BINARY_FRAME_START);           // write binary type header
        WebSocketUtil.encodeLength(frame, length);  // write length prefix
        frame.putBuffer(message.duplicate());       // write payload
        frame.flip();
        
        out.write(channel, frame);
    }

    @Override
    public void encodeTextMessage(C channel, String message, EncoderOutput<C> out) {
        byte[] payload = StringUtils.getUtf8Bytes(message);
        int length = payload.length;
        // The largest frame that can be received is 5 bytes (encoded 32 bit length header + trailing byte)
        WrappedByteBuffer frame = WrappedByteBuffer.allocate(length + 6);
        frame.put(WS_SPECIFIED_LENGTH_TEXT_FRAME_START);           // write binary type header
        WebSocketUtil.encodeLength(frame, length);  // write length prefix
        frame.putBytes(payload);       // write payload
        frame.flip();

        out.write(channel, frame);
    }
}
