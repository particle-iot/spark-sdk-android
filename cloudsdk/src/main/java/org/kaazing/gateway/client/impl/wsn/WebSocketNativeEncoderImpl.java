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

package org.kaazing.gateway.client.impl.wsn;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.EncoderOutput;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.util.WrappedByteBuffer;


public class WebSocketNativeEncoderImpl implements WebSocketNativeEncoder {
    private static final String CLASS_NAME = WebSocketNativeEncoderImpl.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    private static final Charset UTF8 = Charset.forName("UTF-8");

    protected WebSocketNativeEncoderImpl() {
    }
    
    @Override
    public void encodeTextMessage(WebSocketChannel channel, String message, EncoderOutput<WebSocketChannel> out) {
        LOG.entering(CLASS_NAME, "processTextMessage", message);

        WrappedByteBuffer buf = new WrappedByteBuffer();
        buf.putString(message, UTF8);
        buf.flip();
        
        WrappedByteBuffer buffer = encodeRFC6455(buf, false);

        out.write(channel, buffer);
    }

    @Override
    public void encodeBinaryMessage(WebSocketChannel channel, WrappedByteBuffer message, EncoderOutput<WebSocketChannel> out) {
        LOG.entering(CLASS_NAME, "processBinaryMessage", message);
        
        WrappedByteBuffer buffer = encodeRFC6455(message, true);

        out.write(channel, buffer);
    }
    
    private WrappedByteBuffer encodeRFC6455(WrappedByteBuffer buf, boolean isBinary) {
        
        final boolean mask = true;
        int maskValue = new Random().nextInt();
        
        boolean fin = true;   // TODO continued frames?

        int remaining = buf.remaining();

        int offset = 2 + (mask ? 4 : 0) + calculateLengthSize(remaining);
        
        WrappedByteBuffer b = WrappedByteBuffer.allocate(offset + remaining);

        int start = b.position();

        byte b1 = (byte) (fin ? 0x80 : 0x00);
        byte b2 = (byte) (mask ? 0x80 : 0x00);

        b1 = doEncodeOpcode(b1, isBinary);
        b2 |= lenBits(remaining);

        b.put(b1).put(b2);

        doEncodeLength(b, remaining);

        if (mask) {
            b.putInt(maskValue);
        }
        //put message data
        b.putBuffer(buf);
        
           if ( mask ) {
               b.position(offset);
            mask(b, maskValue);
        }
        
          b.limit(b.position());
           b.position(start);
           return b;

    }
    
    private static byte doEncodeOpcode(byte b, boolean isBinary) {
        return (byte) (b | (isBinary ? 0x02 : 0x01));
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
    
    private static int calculateLengthSize(int length) {
        if (length < 126) {
            return 0;
        } else if (length < 65535) {
            return 2;
        } else {
            return 8;
        }
    }
    
    private static void doEncodeLength(WrappedByteBuffer buf, int length) {
        if (length < 126) {
            return;
        } else if (length < 65535) {
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
    public static void mask(WrappedByteBuffer buf, int mask) {
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
    public static void unmask(WrappedByteBuffer buf, int mask) {
        byte b;
        int remainder = buf.remaining() % 4;
        int remaining = buf.remaining() - remainder;
        int end = remaining + buf.position();

        // xor a 32bit word at a time as long as possible
        while (buf.position() < end) {
            int plaintext = buf.getIntAt(buf.position()) ^ mask;
            buf.putInt(plaintext);
        }

        // xor the remaining 3, 2, or 1 bytes
        switch (remainder) {
        case 3:
            b = (byte) (buf.getAt(buf.position()) ^ ((mask >> 24) & 0xff));
            buf.put(b);
            b = (byte) (buf.getAt(buf.position()) ^ ((mask >> 16) & 0xff));
            buf.put(b);
            b = (byte) (buf.getAt(buf.position()) ^ ((mask >> 8) & 0xff));
            buf.put(b);
            break;
        case 2:
            b = (byte) (buf.getAt(buf.position()) ^ ((mask >> 24) & 0xff));
            buf.put(b);
            b = (byte) (buf.getAt(buf.position()) ^ ((mask >> 16) & 0xff));
            buf.put(b);
            break;
        case 1:
            b = (byte) (buf.getAt(buf.position()) ^ (mask >> 24));
            buf.put(b);
            break;
        case 0:
        default:
                break;
        }
        //buf.position(start);
    }
}
