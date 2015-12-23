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

package org.kaazing.net.sse.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.net.impl.util.BlockingQueueImpl;
import org.kaazing.net.sse.SseEventReader;
import org.kaazing.net.sse.SseEventType;
import org.kaazing.net.sse.SseException;

public class SseEventReaderImpl extends SseEventReader {
    private static final String _CLASS_NAME = SseEventReaderImpl.class.getName();
    private static final Logger _LOG = Logger.getLogger(_CLASS_NAME);

    private final BlockingQueueImpl<Object>    _sharedQueue;
    private final SseEventSourceImpl           _eventSource;
    
    private SsePayload                         _payload;
    private SseEventType                       _eventType;
    private String                             _eventName;
    private String                             _data;

    public SseEventReaderImpl(SseEventSourceImpl        eventSource,
                              BlockingQueueImpl<Object> sharedQueue) {
        _eventSource = eventSource;
        _sharedQueue = sharedQueue;
        
        _payload = null;
        _eventType = null;
        _eventName = null;
        _data = null;
    }
    
    @Override
    public CharSequence getData() throws IOException {
        if (_payload == null) {
            return null;
        }

        if (_eventType != SseEventType.DATA) {
            String s = "readData() can only be used to read events " +
                       "of type SseEventType.DATA";
            throw new SseException(s);            
        }

        return _data;
    }
    
    @Override
    public String getName() {
        return _eventName;
    }

    @Override
    public SseEventType getType() {
        return _eventType;
    }

    @Override
    public SseEventType next() throws IOException {
        if (_sharedQueue.isDone()) {
            _eventType = SseEventType.EOS;
            return _eventType;
        }
        
        synchronized (this) {
            if (!_eventSource.isConnected()) {
                _eventType = SseEventType.EOS;
                return _eventType;
            }

            try {
                _payload = null;
                _payload = (SsePayload) _sharedQueue.take();
            } 
            catch (InterruptedException ex) {
                _LOG.log(Level.FINE, ex.getMessage());
            }

            if (_payload == null) {
                String s = "Reader has been interrupted maybe the connection " +
                           "is closed";
                // throw new SseException(s);
                _LOG.log(Level.FINE, _CLASS_NAME, s);

                _eventType = SseEventType.EOS;
                return _eventType;
            }
            
            _data = _payload.getData();
            _eventName = _payload.getEventName();
            _eventType = (_payload.getData() == null) ? SseEventType.EMPTY :
                                                        SseEventType.DATA;
        }

        return _eventType;
    }
    
    // ------------------ Package-Private Implementation ----------------------
    // These methods are called from other classes in this package. They are
    // not part of the public API.
    void close() throws IOException {
        _sharedQueue.done();
        _payload = null;
        _eventType = null;
        _data = null;
        _eventName = null;
    }

    void reset() throws IOException {
        _sharedQueue.reset();
        _payload = null;
        _eventType = null;
        _data = null;
        _eventName = null;
    }
    
    // ------------- Currently not being used methods -------------------------
    // This was earlier part of our public API. It's no longer being exposed.
    /**
     * Returns the payload of the event. Use this method to retrieve the 
     * payload only if the event's type is {@link SseEventType.DATA}.
     * <p>
     * If this method is invoked AFTER {@link #next()} method, then it will not
     * block. If this method is invoked before {@link #next()} method, then it 
     * will block till a message is received. If the type of received event is 
     * {@link SseEventType#EMPTY}, then this method will throw an IOException.
     * <p>
     * An IOException is thrown if the connection is closed while blocked. An
     * IOException is thrown if this method is invoked before the connection
     * has been established. 
     * <p>
     * @return CharSequence     event's payload
     * @throws IOException      if the connection is closed; if the received
     *                          event's type is SseEventType.EMPTY; if invoked 
     *                          before connection is established
     */
    @SuppressWarnings("unused")
    private CharSequence readData() throws IOException {
        if (!_eventSource.isConnected()) {
            String s = "Can't read using the MessageReader if the event " +
                       "source is not connected";
            throw new SseException(s);
        }

        synchronized (this) {        
            if (_payload != null) {
                // If we are here, then it means that readData() was invoked 
                // after next(). So, the _payload is already setup and we just
                // have to return the data.
                if (_eventType != SseEventType.DATA) {
                    String s = "readData() can only be used to read events " +
                               "of type SseEventType.DATA";
                    throw new SseException(s);            
                }
                
                // Clear the _payload member variable for the internal state 
                // machine.
                _payload = null;            
                return _data;
            }
            
            // This will block the thread. If we are here, this means that
            // readData() was invoked without a previous invocation of next().
            // So, we invoke next() and ensure that the next message is a text
            // message. Otherwise, throw an exception.
            SseEventType type = next();
            
            if (type != SseEventType.DATA) {
                String s = "readData() can only be used to read events " +
                           "of type SseEventType.DATA";
                throw new SseException(s);            
            }
    
            _data = _payload.getData();
            _eventName = _payload.getEventName();
    
            // Clear the _payload member variable for the internal state machine.
            _payload = null;
            return _data;
        }
    }
}
