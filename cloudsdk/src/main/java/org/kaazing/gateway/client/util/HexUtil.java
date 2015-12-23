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

package org.kaazing.gateway.client.util;

public class HexUtil {
    
    private static final byte[] FROM_HEX = { 
         0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9,  0,  0,  0,  0,  0,  0,
         0, 10, 11, 12, 13, 14, 15,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
         0, 10, 11, 12, 13, 14, 15,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    };

//    public static final WrappedByteBuffer decode(WrappedByteBuffer encoded) {
//        LOG.entering(CLASS_NAME, "decode", encoded);
//        if (encoded == null) {
//            return null;
//        }
//        
//        byte[] decodedArray = new byte[encoded.remaining() / 2];
//        int decodedArrayOffset = 0;
//        
//        byte[] encodedArray = encoded.array();
//        int encodedArrayOffset = encoded.arrayOffset();
//        int encodedArrayLimit = encodedArrayOffset + encoded.limit();
//
//        for (int i = encodedArrayOffset + encoded.position(); i < encodedArrayLimit;) {
//            decodedArray[decodedArrayOffset++] = (byte)((FROM_HEX[encodedArray[i++]] << 4) | FROM_HEX[encodedArray[i++]]);
//        }
//        WrappedByteBuffer decoded = WrappedByteBuffer.wrap(decodedArray, 0, decodedArrayOffset);
//        LOG.exiting(CLASS_NAME, "decode", decoded);
//        return decoded;
//    }

    public static byte[] decode(byte[] input ) {
        if (input == null) {
            return null;
        }
        byte[] output = new byte[input.length/2+1];
        int decodedArrayOffset = 0;
        for(int i = 0; i < input.length; ) {
           output[decodedArrayOffset++] = (byte)(FROM_HEX[input[i++]] << 4 | FROM_HEX[input[i++]]);
        } 
        return output;
    }

    /**
     * Converts a hex string into an array of bytes.
     * @param s the hex string with two characters for each byte 
     * @return the byte array corresponding to the hex string
     */
    public static byte[] fromHex(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
}
        return value;
    }
}
