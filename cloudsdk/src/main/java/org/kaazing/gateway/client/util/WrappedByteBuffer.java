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

import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * WrappedByteBuffer provides an automatically expanding byte buffer for ByteSocket.
 */
public class WrappedByteBuffer {
    /* Invariant: 0 <= mark <= position <= limit <= capacity */

    static int INITIAL_CAPACITY = 128;

    java.nio.ByteBuffer _buf;
    private final int _minimumCapacity;
    private boolean autoExpand = true;
    private boolean _isBigEndian = true;

    /**
     * The order property indicates the endianness of multibyte integer types in the buffer.
     * Defaults to ByteOrder.BIG_ENDIAN;
     */
    public WrappedByteBuffer() {
        this(INITIAL_CAPACITY);
    }

    WrappedByteBuffer(int capacity) {
        this(java.nio.ByteBuffer.allocate(capacity));
    }

    public WrappedByteBuffer(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    WrappedByteBuffer(byte[] bytes, int offset, int length) {
        this(java.nio.ByteBuffer.wrap(bytes, offset, length));
    }

    public WrappedByteBuffer(java.nio.ByteBuffer buf) {
        _buf = buf;
        _minimumCapacity = buf.capacity();
    }

    public java.nio.ByteBuffer getNioByteBuffer() {
        return _buf;
    }

    /**
     * Sets whether the buffer is auto-expanded when bytes are added at the limit.
     */
    public final WrappedByteBuffer setAutoExpand(boolean autoExpand) {
        this.autoExpand = autoExpand;
        return this;
    }

    /**
     * Return true if the buffer is auto-expanded when bytes are added at the limit.
     * 
     * @return whether the buffer is auto-expanded.
     */
    public final boolean isAutoExpand() {
        return autoExpand;
    }

    /**
     * Return underlying byte array offset
     * 
     * @return underlying byte array offset
     */
    public final int arrayOffset() {
        int ao = _buf.arrayOffset();
        return ao;
    }

    /**
     * Return underlying byte array
     * 
     * @return returns the contents of the buffer as a byte array
     */
    public final byte[] array() {
        byte[] a = _buf.array();
        return a;
    }

    /**
     * Allocates a new WrappedByteBuffer instance.
     * 
     * @param capacity
     *            the maximum buffer capacity
     * 
     * @return the allocated WrappedByteBuffer
     */
    public static WrappedByteBuffer allocate(int capacity) {
        return new WrappedByteBuffer(capacity);
    }

    /**
     * Wraps a byte array as a new WrappedByteBuffer instance.
     * 
     * @param bytes
     *            an array of byte-sized numbers
     * 
     * @return the bytes wrapped as a WrappedByteBuffer
     */
    public static WrappedByteBuffer wrap(byte[] bytes) {
        return new WrappedByteBuffer(bytes);
    }

    /**
     * Wraps a byte array as a new WrappedByteBuffer instance.
     * 
     * @param bytes
     *            an array of byte-sized numbers
     * 
     * @return the bytes wrapped as a WrappedByteBuffer
     */
    public static WrappedByteBuffer wrap(byte[] bytes, int offset, int length) {
        return new WrappedByteBuffer(bytes, offset, length);
    }

    /**
     * Wraps a java.nio.ByteBuffer as a new WrappedByteBuffer instance.
     * 
     * @param bytes
     *            an array of byte-sized numbers
     * 
     * @return the bytes wrapped as a WrappedByteBuffer
     */
    public static WrappedByteBuffer wrap(java.nio.ByteBuffer in) {
        return new WrappedByteBuffer(in);
    }

    /**
     * Returns the ByteOrder of the buffer
     * 
     * @return
     */
    public ByteOrder order() {
        ByteOrder bo = _buf.order();
        return bo;
    }

    /**
     * Set the byte order for this buffer
     * 
     * @param o ByteOrder
     * 
     * @return the ByteOrder specified
     *
     * @deprecated Will return WrappedByteBuffer in future releases to match java.nio.ByteBuffer
     */
    public ByteOrder order(ByteOrder o) {
        // TODO: In the next release, return WrappedByteBuffer - use return _buf.order(o)
        _isBigEndian = "BIG_ENDIAN".equals(o.toString());
        _buf.order(o);
        return o;
    }

    /**
     * Marks a position in the buffer.
     * 
     * @return the buffer
     * 
     * @see WrappedByteBuffer#reset
     */
    public WrappedByteBuffer mark() {
        _buf.mark();
        return this;
    }

    /**
     * Resets the buffer position using the mark.
     * 
     * @throws Exception
     *             if the mark is invalid
     * 
     * @return the buffer
     * 
     * @see WrappedByteBuffer#mark
     */
    public WrappedByteBuffer reset() {
        _buf = (java.nio.ByteBuffer) _buf.reset();
        return this;
    }

    private static final int max(int a, int b) {
        return a > b ? a : b;
    }

    /**
     * Compacts the buffer by removing leading bytes up to the buffer position, and decrements the limit and position values
     * accordingly.
     * 
     * @return the buffer
     */
    public WrappedByteBuffer compact() {
        int remaining = remaining();
        int capacity = capacity();

        if (capacity == 0) {
            return this;
        }

        if (remaining <= capacity >>> 2 && capacity > _minimumCapacity) {
            int newCapacity = capacity;
            int minCapacity = max(_minimumCapacity, remaining << 1);
            for (;;) {
                if (newCapacity >>> 1 < minCapacity) {
                    break;
                }
                newCapacity >>>= 1;
            }

            newCapacity = max(minCapacity, newCapacity);

            if (newCapacity == capacity) {
                if (_buf.remaining() == 0) {
                    _buf.position(0);
                    _buf.limit(_buf.capacity());                    
                }
                else {
                    java.nio.ByteBuffer dup = _buf.duplicate();
                    _buf.position(0);
                    _buf.limit(_buf.capacity());
                    _buf.put(dup);
                }
                return this;
            }

            // Shrink and compact:
            // // Save the state.
            ByteOrder bo = order();

            // // Sanity check.
            if (remaining > newCapacity) {
                throw new IllegalStateException("The amount of the remaining bytes is greater than " + "the new capacity.");
            }

            // // Reallocate.
            java.nio.ByteBuffer oldBuf = _buf;
            java.nio.ByteBuffer newBuf = java.nio.ByteBuffer.allocate(newCapacity);
            newBuf.put(oldBuf);
            _buf = newBuf;

            // // Restore the state.
            _buf.order(bo);
        } else {
            _buf.compact();
        }

        return this;
    }

    /**
     * Duplicates the buffer by reusing the underlying byte array but with independent position, limit and capacity.
     * 
     * @return the duplicated buffer
     */
    public WrappedByteBuffer duplicate() {
        return WrappedByteBuffer.wrap(_buf.duplicate());
    }

    /**
     * Fills the buffer with a repeated number of zeros.
     * 
     * @param size
     *            the number of zeros to repeat
     * 
     * @return the buffer
     */
    public WrappedByteBuffer fill(int size) {
        _autoExpand(size);
        while (size-- > 0) {
            _buf.put((byte) 0);
        }
        return this;
    }

    /**
     * Fills the buffer with a specific number of repeated bytes.
     * 
     * @param b
     *            the byte to repeat
     * @param size
     *            the number of times to repeat
     * 
     * @return the buffer
     */
    public WrappedByteBuffer fillWith(byte b, int size) {
        _autoExpand(size);
        while (size-- > 0) {
            _buf.put(b);
        }
        return this;
    }

    /**
     * Returns the index of the specified byte in the buffer.
     * 
     * @param b
     *            the byte to find
     * 
     * @return the index of the byte in the buffer, or -1 if not found
     */
    public int indexOf(byte b) {
        if (_buf.hasArray()) {
            byte[] array = _buf.array();
            int arrayOffset = _buf.arrayOffset();
            int startAt = arrayOffset + position();
            int endAt = arrayOffset + limit();

            for (int i = startAt; i < endAt; i++) {
                if (array[i] == b) {
                    return i - arrayOffset;
                }
            }
            return -1;
        } else {
            int startAt = _buf.position();
            int endAt = _buf.limit();

            for (int i = startAt; i < endAt; i++) {
                if (_buf.get(i) == b) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Puts a single byte number into the buffer at the current position.
     * 
     * @param v
     *            the single-byte number
     * 
     * @return the buffer
     */
    public WrappedByteBuffer put(byte v) {
        _autoExpand(1);
        _buf.put(v);
        return this;
    }

    /**
     * Puts segment of a single-byte array into the buffer at the current position.
     * 
     * @param v
     *            the source single-byte array
     * @offset
     *               the start position of source array
     * @length
     *               the length to put into WrappedByteBuffer
     * @return the buffer
     */
    public WrappedByteBuffer put(byte[] v, int offset, int length) {
        _autoExpand(length);
        for (int i = 0; i < length; i++) {
            _buf.put(v[offset + i]);
        }
        return this;
    }
    
    /**
     * Puts a single byte number into the buffer at the specified index.
     * 
     * @param index the index
     * @param v the byte
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putAt(int index, byte v) {
        _checkForWriteAt(index, 1);
        _buf.put(index, v);
        return this;
    }
    
    /**
     * Puts a unsigned single-byte number into the buffer at the current position.
     * 
     * @param v the unsigned byte as an int
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putUnsigned(int v) {
        byte b = (byte)(v & 0xFF);
        return this.put(b);
    }
    
    /**
     * Puts an unsigned single byte into the buffer at the specified position.
     * 
     * @param v the unsigned byte as an int
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putUnsignedAt(int index, int v) {
        _checkForWriteAt(index, 1);
        byte b = (byte)(v & 0xFF);
        return this.putAt(index, b);
    }

    /**
     * Puts a two-byte short into the buffer at the current position.
     * 
     * @param v the two-byte short value
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putShort(short v) {
        _autoExpand(2);
        _buf.putShort(v);
        return this;
    }

    /**
     * Puts a two-byte short into the buffer at the specified index.
     * 
     * @param index the index
     * @param v the two-byte short
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putShortAt(int index, short v) {
        _checkForWriteAt(index, 2);
        _buf.putShort(index, v);
        return this;
    }

    /**
     * Puts a two-byte unsigned short into the buffer at the current position.
     * 
     * @param v the two-byte short value
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putUnsignedShort(int v) {
        this.putShort((short)(v & 0xFFFF));
        return this;
    }

    /**
     * Puts an unsigned two-byte unsigned short into the buffer at the position specified.
     * 
     * @param index the index
     * @param v the short value
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putUnsignedShortAt(int index, int v) {
        _checkForWriteAt(index, 2);
        this.putShortAt(index, (short)(v & 0xFFFF));
        return this;
    }

    /**
     * Puts a four-byte int into the buffer at the current position.
     * 
     * @param v the four-byte int
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putInt(int v) {
        _autoExpand(4);
        _buf.putInt(v);
        return this;
    }

    /**
     * Puts a four-byte int into the buffer at the specified index.
     * 
     * @param index the index
     * @param v the four-byte int
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putIntAt(int index, int v) {
        _checkForWriteAt(index, 4);
        _buf.putInt(index, v);
        return this;
    }

    /**
     * Puts a four-byte array into the buffer at the current position.
     * 
     * @param v the four-byte int
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putUnsignedInt(long value) {
        this.putInt((int)value & 0xFFFFFFFF);
        return this;
    }

    /**
     * Puts an unsigned four-byte array into the buffer at the specified index.
     * 
     * @param index the index
     * @param v the four-byte int
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putUnsignedIntAt(int index, long value) {
        _checkForWriteAt(index, 4);
        this.putIntAt(index, (int)value & 0xFFFFFFFF);
        return this;
    }

    /**
     * Puts an eight-byte long into the buffer at the current position.
     * 
     * @param v the eight-byte long
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putLong(long v) {
        _autoExpand(8);
        _buf.putLong(v);
        return this;
    }

    /**
     * Puts an eight-byte long into the buffer at the specified index.
     * 
     * @param index the index
     * @param v the eight-byte long
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putLongAt(int index, long v) {
        _checkForWriteAt(index, 8);
        _buf.putLong(index, v);
        return this;
    }

    /**
     * Puts a string into the buffer at the current position, using the character set to encode the string as bytes.
     * 
     * @param v
     *            the string
     * @param cs
     *            the character set
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putString(String v, Charset cs) {
        java.nio.ByteBuffer strBuf = cs.encode(v);
        _autoExpand(strBuf.limit());
        _buf.put(strBuf);
        return this;
    }

    /**
     * Puts a string into the buffer at the specified index, using the character set to encode the string as bytes.
     * 
     * @param fieldSize
     *            the width in bytes of the prefixed length field
     * @param v
     *            the string
     * @param cs
     *            the character set
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putPrefixedString(int fieldSize, String v, Charset cs) {
        if (fieldSize == 0) {
            return this;
        }
        boolean utf16 = cs.name().startsWith("UTF-16");

        if (utf16 && (fieldSize == 1)) {
            throw new IllegalArgumentException("fieldSize is not even for UTF-16 character set");
        }

        java.nio.ByteBuffer strBuf = cs.encode(v);
        _autoExpand(fieldSize + strBuf.limit());

        int len = strBuf.remaining();
        switch (fieldSize) {
        case 1:
            put((byte) len);
            break;
        case 2:
            putShort((short) len);
            break;
        case 4:
            putInt(len);
            break;
        default:
            throw new IllegalArgumentException("Illegal argument, field size should be 1,2 or 4 and fieldSize is: " + fieldSize);
        }

        _buf.put(strBuf);
        return this;
    }

    /**
     * Puts a single-byte array into the buffer at the current position.
     * 
     * @param v
     *            the single-byte array
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putBytes(byte[] b) {
        _autoExpand(b.length);
        _buf.put(b);
        return this;
    }

    /**
     * Puts a single-byte array into the buffer at the specified index.
     * 
     * @param index the index
     * @param v the single-byte array
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putBytesAt(int index, byte[] b) {
        _checkForWriteAt(index, b.length);
        int pos = _buf.position();
        _buf.position(index);
        _buf.put(b, 0, b.length);
        _buf.position(pos);
        return this;
    }

    /**
     * Puts a buffer into the buffer at the current position.
     * 
     * @param v the WrappedByteBuffer
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putBuffer(WrappedByteBuffer v) {
        _autoExpand(v.remaining());
        _buf.put(v._buf);
        return this;
    }

    /**
     * Puts a buffer into the buffer at the specified index.
     * 
     * @param index the index
     * @param v the WrappedByteBuffer
     * 
     * @return the buffer
     */
    public WrappedByteBuffer putBufferAt(int index, WrappedByteBuffer v) {
        // TODO: I believe this method incorrectly moves the position!
        // Can't change it without a code analysis - and this is a public API!
        int pos = _buf.position();
        _buf.position(index);
        _buf.put(v._buf);
        _buf.position(pos);
        return this;
    }

    /**
     * Returns a single byte from the buffer at the current position.
     * 
     * @return the byte
     */
    public byte get() {
        _checkForRead(1);
        byte v = _buf.get();
        return v;
    }

    /**
     * @see java.nio.ByteBuffer#get(byte[], int, int)
     */
    public WrappedByteBuffer get(byte[] dst, int offset, int length) {
        _checkForRead(length);
        for (int i = 0; i < length; i++) {
            dst[offset + i] = _buf.get();
        }
        return this;
    }

    /**
     * @see java.nio.ByteBuffer#get(byte[], int, int)
     */
    public WrappedByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Returns a byte from the buffer at the specified index.
     * 
     * @param index the index
     * 
     * @return the byte
     */
    public byte getAt(int index) {
        _checkForReadAt(index,1);
        byte v = _buf.get(index);
        return v;
    }
    
    /**
     * Returns an unsigned byte from the buffer at the current position.
     *
     * @return the unsigned byte as an int
     */
    public int getUnsigned() {
        _checkForRead(1);
        int val = ((int) (_buf.get() & 0xff));
        return val;
    }
    /**
     * Returns an unsigned byte from the buffer at the specified index.
     * 
     * @param index the index
     * 
     * @return the unsigned byte as an int
     */
    public int getUnsignedAt(int index) {
        _checkForReadAt(index, 1);
        int val = ((int) (_buf.get(index) & 0xff));
        return val;
    }
    /**
     * Returns a byte array of length size from the buffer from current position
     * @param size
     * @return a new byte array with bytes read from the buffer
     */
    public byte[] getBytes(int size){ 
        _checkForRead(size);
        byte[] dst = new byte[size];
       _buf.get(dst, 0, size);
       return dst;
    }
    /**
     * Returns a byte array of length size from the buffer starting from the specified position.
     * @param index the index
     * @param size the size of the buffer to be returned
     * @return a new byte array with bytes read from the buffer
     */
    public byte[] getBytesAt(int index,int size){ 
        _checkForReadAt(index,size);
       byte[] dst = new byte[size];
       int i=0;
       int j=index;
       while (i<size) {
           dst[i++] =  _buf.get(j++);
       }
       return dst;
    }
    
    /**
     * Returns a two-byte short from the buffer at the current position.
     * 
     * @return the two-byte short
     */
    public short getShort() {
        _checkForRead(2);
        short v = _buf.getShort();
        return v;
    }

    /**
     * Returns a two-byte short from the buffer at the specified index.
     * 
     * @param index the index
     * 
     * @return the two-byte short
     */
    public short getShortAt(int index) {
        _checkForReadAt(index, 2);
        short v = _buf.getShort(index);
        return v;
    }

    /**
     * Returns an two-byte unsigned short from the buffer at the current position
     * 
     * @return the two-byte unsigned short
     */
    public int getUnsignedShort() {
        _checkForRead(2);
        int val = (_buf.getShort() & 0xffff);
        return val;
    }

    /**
     * Returns an two-byte unsigned short from the buffer at the specified index.
     * 
     * @param index the index
     * 
     * @return the two-byte unsigned short
     */
    public int getUnsignedShortAt(int index) {
        _checkForReadAt(index,2);
        int val = (_buf.getShort(index) & 0xffff);
        return val;
    }

    /**
     * Returns an unsigned three-byte medium int from the buffer at the current position
     *
     * @return the three-byte medium int as an int
     */
    public int getUnsignedMediumInt() {
        int b1 = getUnsigned();
        int b2 = getUnsigned();
        int b3 = getUnsigned();
        if (_isBigEndian) {
            return (b1<<16) | (b2<<8) | b3;
        }
        else {
            return (b3<<16) | (b2<<8) | b1;
        }
    }

    /**
     * Returns a four-byte int from the buffer at the current position.
     * 
     * @return the four-byte int
     */
    public int getInt() {
        _checkForRead(4);
        int v = _buf.getInt();
        return v;
    }

    /**
     * Returns a four-byte int from the buffer at the specified index.
     * 
     * @param index the index
     * 
     * @return the four-byte int
     */
    public int getIntAt(int index) {
        _checkForReadAt(index,4);
        int v = _buf.getInt(index);
        return v;
    }
    
    /**
     * Returns an unsigned four-byte int from the buffer at the current position
     *
     * @return the four-byte unsigned int as a long
     */
    public long getUnsignedInt() {
        _checkForRead(4);
        long val = (long) (_buf.getInt() & 0xffffffffL);
        return val;
    }
    
    /**
     * Returns an unsigned four-byte int from the buffer at the specified index
     *
     * @param index the index
     *
     * @return the four-byte unsigned int as a long
     */
    public long getUnsignedIntAt(int index) {
        _checkForReadAt(index,4);
        long val = (long) (_buf.getInt(index) & 0xffffffffL);
        return val;
    }
    
    /**
     * Returns a eight-byte long from the buffer at the current position.
     * 
     * @return the eight-byte long
     */
    public long getLong() {
        _checkForRead(8);
        long v = _buf.getLong();
        return v;
    }

    /**
     * Returns an eight-byte long from the buffer at the specified index.
     * 
     * @param index the index
     * 
     * @return the eight-byte long
     */
    public long getLongAt(int index) {
        _checkForReadAt(index,8);
        long v = _buf.getLong(index);
        return v;
    }

    /**
     * Returns a length-prefixed string from the buffer at the current position.
     * 
     * @param fieldSize the width in bytes of the prefixed length field
     * @param cs the character set
     * 
     * @return the length-prefixed string
     */
    public String getPrefixedString(int fieldSize, Charset cs) {
        
        int len = 0;
        switch (fieldSize) {
        case 1:
            len = _buf.get();
            break;
        case 2:
            len = _buf.getShort();
            break;
        case 4:
            len = _buf.getInt();
            break;
        default:
            throw new IllegalArgumentException("Illegal argument, field size should be 1,2 or 4 and fieldSize is: " + fieldSize);
        }

        if (len == 0) {
            return "";
        }

        int oldLimit = _buf.limit();
        try {
            _buf.limit(_buf.position() + len);
            byte[] bytes = new byte[len];
            _buf.get(bytes, 0, len);
            String retVal = cs.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            return retVal;
        } finally {
            _buf.limit(oldLimit);
        }
    }

    /**
     * Returns a null-terminated string from the buffer at the current position. If the end of buffer if reached before
     * discovering a null terminator byte, then the decoded string includes all bytes up to the end of the buffer.
     * 
     * @param cs the character set
     * 
     * @return the null-terminated string
     */
    public String getString(Charset cs) {
        // find indexof \0 and create a string for the charset
        int nullAt = indexOf((byte) 0);
        boolean utf16 = cs.name().startsWith("UTF-16");
        if (utf16) {
            int oldPos = _buf.position();
            boolean nullFound = false;
            while (!nullFound && (nullAt != -1)) {
                if (_buf.get(nullAt + 1) == 0) {
                    nullFound = true;
                    break;
                }
                _buf.position(nullAt + 1);
                nullAt = indexOf((byte) 0);
            }
            if (!nullFound) {
                throw new IllegalStateException("The string being read is not UTF-16");
            }
            _buf.position(oldPos);
        }

        int newLimit;
        if (nullAt != -1) {
            newLimit = nullAt;
        } else {
            newLimit = _buf.limit();
        }
        int numBytes = newLimit - _buf.position();
        int oldLimit = _buf.limit();
        try {
            _buf.limit(newLimit);
            byte[] bytes = new byte[numBytes];
            _buf.get(bytes, 0, numBytes);
            String retVal = cs.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            return retVal;
        } finally {
            _buf.limit(oldLimit);
            if (nullAt != -1) {
                _buf.get();
                if (utf16) {
                    _buf.get();
                }
            }
        }
    }

    /**
     * Returns a sliced buffer, setting the position to zero, and decrementing the limit accordingly.
     * 
     * @return the sliced buffer
     */
    public WrappedByteBuffer slice() {
        return WrappedByteBuffer.wrap(_buf.slice());
    }

    /**
     * Flips the buffer. The limit is set to the current position, the position is set to zero, and the mark is reset.
     * 
     * @return the flipped buffer
     */
    public WrappedByteBuffer flip() {
        _buf = (java.nio.ByteBuffer) _buf.flip();
        return this;
    }

    /**
     * Rewinds the buffer. The position is set to zero and the mark is reset.
     * 
     * @return the buffer
     */
    public WrappedByteBuffer rewind() {
        _buf = (java.nio.ByteBuffer) _buf.rewind();
        return this;
    }

    /**
     * Clears the buffer. The position is set to zero, the limit is set to the capacity and the mark is reset.
     * 
     * @return the buffer
     */
    public WrappedByteBuffer clear() {
        _buf = (java.nio.ByteBuffer) _buf.clear();
        return this;
    }

    /**
     * Returns the number of bytes remaining from the current position to the limit.
     * 
     * @return the number of bytes remaining
     */
    public int remaining() {
        int rem = _buf.remaining();
        return rem;
    }

    /**
     * Returns the capacity of this buffer
     * 
     * @return the number of bytes it can hold
     */
    public int capacity() {
        int cap = _buf.capacity();
        return cap;
    }

    /**
     * Returns the current position in this buffer
     * 
     */
    public int position() {
        int pos = _buf.position();
        return pos;
    }

    /**
     * Sets the current position in this buffer
     * 
     * @return returns this buffer
     */
    public WrappedByteBuffer position(int position) {
        _autoExpandAt(position, 0);
        _buf.position(position);
        return this;
    }

    /**
     * Returns the capacity of this buffer
     * 
     * @return the number of bytes it can hold
     */
    public int limit() {
        return _buf.limit();
    }

    /**
     * Sets the limit for this buffer
     * 
     * @return returns this buffer
     */
    public WrappedByteBuffer limit(int limit) {
        _autoExpandAt(limit, 0);
        _buf.limit(limit);
        return this;
    }

    /**
     * Returns true if this buffer has remaining bytes, false otherwise.
     * 
     * @return whether this buffer has remaining bytes
     */
    public boolean hasRemaining() {
        boolean rem = _buf.hasRemaining();
        return rem;
    }

    /**
     * Skips the specified number of bytes from the current position.
     * 
     * @param size the number of bytes to skip
     * 
     * @return the buffer
     */
    public WrappedByteBuffer skip(int size) {
        _autoExpand(size);
        _buf.position(_buf.position() + size);
        return this;
    }

    /**
     * Returns a hex dump of this buffer.
     * 
     * @return the hex dump
     */
    public String getHexDump() {
        if (_buf.position() == _buf.limit()) {
            return "empty";
        }

        StringBuilder hexDump = new StringBuilder();
        for (int i = _buf.position(); i < _buf.limit(); i++) {
            hexDump.append(Integer.toHexString(_buf.get(i)&0xFF)).append(' ');
        }
        return hexDump.toString();
    }

    /**
     * Returns the string representation of this buffer.
     * 
     * @return the string representation
     */
    public String toString() {
        return getClass().getName() + "[position:" + position() + " limit: " + limit() + " capacity:" + capacity() + "]";
    }

    /**
     * @see java.nio.ByteBuffer#equals(Object obj)
     * @return true if, and only if, this buffer is equal to the given object
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof WrappedByteBuffer)) {
            return false;
        }
        WrappedByteBuffer that = (WrappedByteBuffer) obj;
        return this._buf.equals(that._buf);
    }

    @Override
    public int hashCode() {
        return _buf.hashCode();
    }

    /**
     * Expands the buffer to support the expected number of remaining bytes after the current position.
     * 
     * @param expectedRemaining
     *            the expected number of remaining bytes
     * 
     * @return the buffer
     */
    WrappedByteBuffer expand(int expectedRemaining) {
        return expandAt(_buf.position(), expectedRemaining);
    }

    /**
     * Expands the buffer to support the expected number of remaining bytes at the specified index.
     * 
     * @param index the index
     * @param expectedRemaining the expected number of remaining bytes
     * 
     * @return the buffer
     */
    WrappedByteBuffer expandAt(int i, int expectedRemaining) {
        if ((i + expectedRemaining) <= _buf.limit()) {
            return this;
        } else {
            if ((i + expectedRemaining) <= _buf.capacity()) {
                _buf.limit(i + expectedRemaining);
            } else {
                // reallocate the underlying byte buffer and keep the original buffer
                // intact. The resetting of the position is required because, one
                // could be in the middle of a read of an existing buffer, when they
                // decide to over write only few bytes but still keep the remaining
                // part of the buffer unchanged.
                int newCapacity = _buf.capacity()
                        + ((expectedRemaining > INITIAL_CAPACITY) ? expectedRemaining : INITIAL_CAPACITY);
                java.nio.ByteBuffer newBuffer = java.nio.ByteBuffer.allocate(newCapacity);
                _buf.flip();
                newBuffer.put(_buf);
                _buf = newBuffer;
            }
        }

        return this;
    }

    private void _autoExpandAt(int i, int expectedRemaining) {
        if (autoExpand) {
            expandAt(i, expectedRemaining);
        }
    }

    private void _autoExpand(int expectedRemaining) {
        if (autoExpand) {
            expand(expectedRemaining);
        }
    }

    private void _checkForRead(int expected) {
        int end = _buf.position() + expected;
        if (end > _buf.limit()) {
            throw new BufferUnderflowException();
        }
    }

    private void _checkForReadAt(int index, int expected) {
        int end = index + expected;
        if (index < 0 || end > _buf.limit()) {
            throw new IndexOutOfBoundsException();
        }
    }

    private void _checkForWriteAt(int index, int expected) {
        int end = index + expected;
        if (index < 0 || end > _buf.limit()) {
            throw new IndexOutOfBoundsException();
        }
    }
}
