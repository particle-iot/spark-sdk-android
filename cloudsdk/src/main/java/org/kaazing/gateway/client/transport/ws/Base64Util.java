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
import java.util.logging.Logger;

/**
 * Internal class. This class manages the Base64 encoding and decoding
 */
public class Base64Util {

//    @FlashNative
//import mx.utils.Base64Encoder;
//    public String encodeBytes(ByteArray bytes) {
//        Base64Encoder encoder=new Base64Encoder();
//        encoder.insertNewLines = false;
//        encoder.encodeBytes(bytes);
//        return encoder.drain();
//    }

    private static final String CLASS_NAME = Base64Util.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final byte[] INDEXED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes();
    private static final byte PADDING_BYTE = (byte) '=';

    @Deprecated
    private Base64Util() {
        LOG.entering(CLASS_NAME, "<init>");
    }

    public static String encode(ByteBuffer decoded) {
        LOG.entering(CLASS_NAME, "encode", decoded);

        int decodedSize = decoded.remaining();
        int effectiveDecodedSize = ((decodedSize+2) / 3) * 3;
        int decodedFragmentSize = decodedSize % 3;

        int encodedArraySize = effectiveDecodedSize / 3 * 4;
        byte[] encodedArray = new byte[encodedArraySize];
        int encodedArrayPosition = 0;

        byte[] decodedArray = decoded.array();
        int decodedArrayOffset = decoded.arrayOffset();
        int decodedArrayPosition = decodedArrayOffset + decoded.position();
        int decodedArrayLimit = decodedArrayOffset + decoded.limit() - decodedFragmentSize;

        while (decodedArrayPosition < decodedArrayLimit) {
            int byte0 = decodedArray[decodedArrayPosition++] & 0xff;
            int byte1 = decodedArray[decodedArrayPosition++] & 0xff;
            int byte2 = decodedArray[decodedArrayPosition++] & 0xff;

            encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
            encodedArray[encodedArrayPosition++] = INDEXED[((byte0 << 4) & 0x30) | ((byte1 >> 4) & 0x0f)];
            encodedArray[encodedArrayPosition++] = INDEXED[((byte1 << 2) & 0x3c) | ((byte2 >> 6) & 0x03)];
            encodedArray[encodedArrayPosition++] = INDEXED[byte2 & 0x3f];
        }

        if (decodedFragmentSize == 1) {
            int byte0 = decodedArray[decodedArrayPosition++] & 0xff;

            encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
            encodedArray[encodedArrayPosition++] = INDEXED[((byte0 << 4) & 0x30)];
            encodedArray[encodedArrayPosition++] = PADDING_BYTE;
            encodedArray[encodedArrayPosition++] = PADDING_BYTE;
        }
        else if (decodedFragmentSize == 2) {
            int byte0 = decodedArray[decodedArrayPosition++] & 0xff;
            int byte1 = decodedArray[decodedArrayPosition++] & 0xff;

            encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
            encodedArray[encodedArrayPosition++] = INDEXED[((byte0 << 4) & 0x30) | ((byte1 >> 4) & 0x0f)];
            encodedArray[encodedArrayPosition++] = INDEXED[(byte1 << 2) & 0x3c];
            encodedArray[encodedArrayPosition++] = PADDING_BYTE;
        }

        return new String(encodedArray);
    }

    public static ByteBuffer decode(String encoded) {
        LOG.entering(CLASS_NAME, "decode", encoded);

        if (encoded == null) {
            return null;
        }

        int length = encoded.length();
        if (length % 4 != 0) {
            throw new IllegalArgumentException("Invalid Base64 Encoded String");
        }

        byte[] encodedArray = encoded.getBytes();
        byte[] decodedArray = new byte[(length / 4 * 3)];
        int decodedArrayOffset = 0;
        int i = 0;
        while (i < length) {
            int char0 = encodedArray[i++];
            int char1 = encodedArray[i++];
            int char2 = encodedArray[i++];
            int char3 = encodedArray[i++];

            int byte0 = mapped(char0);
            int byte1 = mapped(char1);
            int byte2 = mapped(char2);
            int byte3 = mapped(char3);

            decodedArray[decodedArrayOffset++] = (byte) (((byte0 << 2) & 0xfc) | ((byte1 >> 4) & 0x03));
            if (char2 != PADDING_BYTE) {
                decodedArray[decodedArrayOffset++] = (byte) (((byte1 << 4) & 0xf0) | ((byte2 >> 2) & 0x0f));
                if (char3 != PADDING_BYTE) {
                    decodedArray[decodedArrayOffset++] = (byte) (((byte2 << 6) & 0xc0) | (byte3 & 0x3f));
                }
            }
        }
        return ByteBuffer.wrap(decodedArray, 0, decodedArrayOffset);
    }

    private static int mapped(int ch) {
        if ((ch & 0x40) != 0) {
            if ((ch & 0x20) != 0) {
                // a(01100001)-z(01111010) -> 26-51
                assert (ch >= 'a');
                assert (ch <= 'z');
                return (ch - 71);
            } else {
                // A(01000001)-Z(01011010) -> 0-25
                assert (ch >= 'A');
                assert (ch <= 'Z');
                return (ch - 65);
            }
        } else if ((ch & 0x20) != 0) {
            if ((ch & 0x10) != 0) {
                if ((ch & 0x08) != 0 && (ch & 0x04) != 0) {
                    // =(00111101) -> 0
                    assert (ch == '=');
                    return 0;
                } else {
                    // 0(00110000)-9(00111001) -> 52-61
                    assert (ch >= '0');
                    assert (ch <= '9');
                    return (ch + 4);
                }
            } else {
                if ((ch & 0x04) != 0) {
                    // /(00101111) -> 63
                    assert (ch == '/');
                    return 63;
                } else {
                    // +(00101011) -> 62
                    assert (ch == '+');
                    return 62;
                }
            }
        } else {
            LOG.warning("Invalid BASE64 string");
            throw new IllegalArgumentException("Invalid BASE64 string");
        }
    }
}
