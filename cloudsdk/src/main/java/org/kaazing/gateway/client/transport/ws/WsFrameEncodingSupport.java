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

package org.kaazing.gateway.client.transport.ws;

import java.nio.ByteBuffer;


public class WsFrameEncodingSupport {

    /**
     * Encode WebSocket message as a single frame, with the provided masking value applied.
     */
    public static ByteBuffer rfc6455Encode(WsMessage message, int maskValue) {
        final boolean mask = true;

        boolean fin = true;   // TODO continued frames?

        ByteBuffer buf = message.getBytes();
        
        int remaining = buf.remaining();

        int offset = 2 + (mask ? 4 : 0) + calculateLengthSize(remaining);
        
        ByteBuffer b = ByteBuffer.allocate(offset + remaining);

        int start = b.position();

        byte b1 = (byte) (fin ? 0x80 : 0x00);
        byte b2 = (byte) (mask ? 0x80 : 0x00);

        b1 = doEncodeOpcode(b1, message);
        b2 |= lenBits(remaining);

        b.put(b1).put(b2);

        doEncodeLength(b, remaining);

        if (mask) {
            b.putInt(maskValue);
        }
        //put message data
        b.put(buf);
        
           if ( mask ) {
               b.position(offset);
            mask(b, maskValue);
        }
        
          b.limit(b.position());
           b.position(start);
           return b;
    }

    protected static enum Opcode {
        CONTINUATION(0),
        TEXT(1),
        BINARY(2),
        RESERVED3(3), RESERVED4(4), RESERVED5(5), RESERVED6(6), RESERVED7(7),
        CLOSE(8),
        PING(9),
        PONG(10);

        private int code;

        public int getCode() {
            return this.code;
        }

        Opcode(int code) {
            this.code = code;
        }
        
        static Opcode getById(int id) {
            Opcode result = null;
            for (Opcode temp : Opcode.values())
            {
                if(id == temp.code)
                {
                    result = temp;
                    break;
                }
            }

            return result;
        }
    }

    private static int calculateLengthSize(int length) {
        if (length < 126) {
            return 0;
        } else if (length < 65535) {
            return 2;
        } else {
            return 8;
        }
    }


    /**
     * Encode a WebSocket opcode onto a byte that might have some high bits set.
     *
     * @param b
     * @param message
     * @return
     */
    private static byte doEncodeOpcode(byte b, WsMessage message) {
        switch (message.getKind()) {
        case TEXT: {
            b |= Opcode.TEXT.getCode();
            break;
        }
        case BINARY: {
            b |= Opcode.BINARY.getCode();
            break;
        }
        case PING: {
            b |= Opcode.PING.getCode();
            break;
        }
        case PONG: {
            b |= Opcode.PONG.getCode();
            break;
        }
        case CLOSE: {
            b |= Opcode.CLOSE.getCode();
            break;
        }
        default:
            throw new IllegalArgumentException("Unrecognized frame type: " + message.getKind());
        }
        return b;
    }

    private static byte lenBits(int length) {
        if (length < 126) {
            return (byte) length;
        } else if (length < 65535) {
            return (byte) 126;
        } else {
            return (byte) 127;
        }
    }

    private static void doEncodeLength(ByteBuffer buf, int length) {
        if (length < 126) {
            return;
        } else if (length < 65535) {
            // FIXME? unsigned short
            buf.putShort((short) length);
        } else {
            // Unsigned long (should never have a message that large! really!)
            buf.putLong((long) length);
        }
    }

    /**
     * Performs an in-situ masking of the readable buf bytes.
     * Preserves the position of the buffer whilst masking all the readable bytes,
     * such that the masked bytes will be readable after this invocation.
     *
     * @param buf   the buffer containing readable bytes to be masked.
     * @param mask  the mask to apply against the readable bytes of buffer.
     */
    public static void mask(ByteBuffer buf, int mask) {
        // masking is the same as unmasking due to the use of bitwise XOR.
        unmask(buf, mask);
    }


    /**
     * Performs an in-situ unmasking of the readable buf bytes.
     * Preserves the position of the buffer whilst unmasking all the readable bytes,
     * such that the unmasked bytes will be readable after this invocation.
     *
     * @param buf   the buffer containing readable bytes to be unmasked.
     * @param mask  the mask to apply against the readable bytes of buffer.
     */
    public static void unmask(ByteBuffer buf, int mask) {
        byte b;
        int remainder = buf.remaining() % 4;
        int remaining = buf.remaining() - remainder;
        int end = remaining + buf.position();

        // xor a 32bit word at a time as long as possible
        while (buf.position() < end) {
            int plaintext = buf.getInt(buf.position()) ^ mask;
            buf.putInt(plaintext);
        }

        // xor the remaining 3, 2, or 1 bytes
        switch (remainder) {
        case 3:
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 24) & 0xff));
            buf.put(b);
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 16) & 0xff));
            buf.put(b);
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 8) & 0xff));
            buf.put(b);
            break;
        case 2:
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 24) & 0xff));
            buf.put(b);
            b = (byte) (buf.get(buf.position()) ^ ((mask >> 16) & 0xff));
            buf.put(b);
            break;
        case 1:
            b = (byte) (buf.get(buf.position()) ^ (mask >> 24));
            buf.put(b);
            break;
        case 0:
        default:
                break;
        }
    }
}
