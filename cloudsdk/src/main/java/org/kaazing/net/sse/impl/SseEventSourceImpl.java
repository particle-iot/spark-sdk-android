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
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.net.http.HttpRedirectPolicy;
import org.kaazing.net.impl.util.BlockingQueueImpl;
import org.kaazing.net.sse.SseEventReader;
import org.kaazing.net.sse.SseEventSource;
import org.kaazing.net.sse.SseException;

public class SseEventSourceImpl extends SseEventSource {
    private static final String _CLASS_NAME = SseEventSourceImpl.class.getName();
    private static final Logger _LOG = Logger.getLogger(_CLASS_NAME);
    
    /**
     * Values are CONNECTING = 0, OPEN = 1, CLOSING = 2, and CLOSED = 3;
     */
    enum ReadyState {
        CONNECTING, OPEN, CLOSING, CLOSED;
    }

    private SseEventStream               _eventStream;
    private ReadyState                   _readyState;
    private SseException                 _exception;
    private SseEventReaderImpl           _eventReader;
    private BlockingQueueImpl<Object>    _sharedQueue;
    private URI                          _location;
    private HttpRedirectPolicy           _redirectOption;
    private long                         _retryTimeout = 3000;
    
    public SseEventSourceImpl(URI location) {
        URI    loc = location;
        
        // Map "sse" to "http".
        if (location.getScheme().equalsIgnoreCase("sse")) {
            String fragment = location.getFragment();
            String schemeSpecificPart = location.getSchemeSpecificPart();
            
            if (fragment == null) {
                fragment = "";
            }
            loc = URI.create("http:" + schemeSpecificPart + fragment);
        }
        
        _location = loc;
        _readyState = ReadyState.CLOSED;
        
        // Used by the producer(i.e. the eventSourceListener) and the 
        // consumer(i.e. the SseEventReader).
        _sharedQueue = new BlockingQueueImpl<Object>();

    }

    @Override
    public synchronized void close() throws IOException {
        _LOG.entering(_CLASS_NAME, "close");

        if ((_readyState == ReadyState.CLOSED) ||
            (_readyState == ReadyState.CLOSING)) {
            // Since the WebSocket is already closed/closing, we just bail.
            _LOG.log(Level.FINE, "Event source is not connected");
            return;
        }

        setException(null);
        _readyState = ReadyState.CLOSING;
        
        _eventStream.stop();
        
        _readyState = ReadyState.CLOSED;
        
        cleanupAfterClose();
    }

    @Override
    public synchronized void connect() throws IOException {
        _LOG.entering(_CLASS_NAME, "connect");
        
        if (_readyState != ReadyState.CLOSED) {
            String s = "Event source must be closed before connecting";
            throw new SseException(s);
        }
        
        _eventStream = new SseEventStream(_location.toString());
        _eventStream.setListener(_eventStreamListener);
        _eventStream.setRetryTimeout(_retryTimeout);

        if (_eventReader != null) {
            // Ensure that the reader is reset and ready to block the consumer
            // if no data has been produced.
            _eventReader.reset();
        }

        // Prepare the state for connection.
        _readyState = ReadyState.CONNECTING;
        setException(null);

        // Connect to the event source. Note that it all happens on the same
        // thread. The registered SseEventStreamListener is also invoked as part
        // of this call. In WebSocket, we have to block to synchronize with the
        // other thread that invokes the listener. Here, we don't have to.
        _eventStream.connect();

        // Check if there is any exception that needs to be reported.
        SseException exception = getException();
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public SseEventReader getEventReader() throws IOException {
        if (_readyState != ReadyState.OPEN) {
            String s = "Cannot get the SseEventReader as the event source is not yet connected";
            throw new IOException(s);
        }

        synchronized (this) {
            if (_eventReader != null) {
                return _eventReader;
            }
    
            _eventReader = new SseEventReaderImpl(this, _sharedQueue);
        }
        
        return _eventReader;
    }
    

    @Override
    public HttpRedirectPolicy getFollowRedirect() {
        return _redirectOption;
    }

    @Override
    public long getRetryTimeout() {
        return _retryTimeout;
    }

    @Override
    public void setFollowRedirect(HttpRedirectPolicy redirectOption) {
        _redirectOption = redirectOption;
    }

    @Override
    public void setRetryTimeout(long millis) {
        _retryTimeout = millis;
        
        if (_eventStream != null) {
            _eventStream.setRetryTimeout(millis);
        }
    }

    // ---------------------- Internal Implementation ------------------------
    public SseException getException() {
        return _exception;
    }
    
    public void setException(SseException exception) {
        _exception = exception;
    }
    
    public boolean isConnected() {
        return (_readyState == ReadyState.OPEN);
    }
    
    public boolean isDisconnected() {
        return (_readyState == ReadyState.CLOSED);
    }
    
    public BlockingQueueImpl<Object> getSharedQueue() {
        return _sharedQueue;
    }
    
    // --------------------- Private Methods ---------------------------------
    private synchronized void connectionOpened() {
        _readyState = ReadyState.OPEN;
        
        // Unblock the connect() call so that it can proceed.
        notifyAll();
    }
    
    private void messageArrived(String eventName, String data) {
        if (_readyState != ReadyState.OPEN) {
            // If the connection is closed, then we should be queuing the
            // events/payload.
            return;
        }
        
        synchronized (_sharedQueue) {
            try {
                _sharedQueue.put(new SsePayload(eventName, data));
            } 
            catch (InterruptedException ex) {
                _LOG.log(Level.INFO, ex.getMessage(), ex);
            }
        }
    }
    
    @SuppressWarnings("unused")
    private synchronized void connectionClosed(String  reason) {
        _readyState = ReadyState.CLOSED;
        
        if (reason != null) {
            setException(new SseException(reason));
        }
        
        cleanupAfterClose();

        // Unblock the close() call so that it can proceed.
        notifyAll();
    }
    
    private synchronized void connectionFailed(Exception exception) {
        SseException ex = null;

        if (exception == null) {
            ex = new SseException("Connection Failed");
        }
        else {
            ex = new SseException(exception);
        }

        setException(ex);
        
        _readyState = ReadyState.CLOSED;
        
        cleanupAfterClose();

        // Unblock threads so that they can proceed.
        notifyAll();
    }

    private synchronized void cleanupAfterClose() {
        if (_eventReader != null) {
            // Notify the waiting consumers that the connection is closing.
            try {
                _eventReader.close();
            } 
            catch (IOException ex) {
                _LOG.log(Level.FINE, ex.getMessage(), ex);
            }
        }
        else {
            getSharedQueue().done();
        }
    }

    
    private SseEventStreamListener _eventStreamListener = new SseEventStreamListener() {
        
        @Override
        public void streamOpened() {
            _LOG.entering(_CLASS_NAME, "streamOpened");
            connectionOpened();
            
            /*
            EventSourceEvent event = new EventSourceEvent(this, EventSourceEvent.Type.OPEN);
            for (EventSourceListener listener : listeners) {
                try {
                    listener.onOpen(event);
                } catch (RuntimeException e) {
                    String s = "Application threw an exception during onOpen: "+e.getMessage();
                    _LOG.logp(Level.WARNING, _CLASS_NAME, "onOpen", s, e);
                }
            }
            */
        }
        
        @Override
        public void messageReceived(String eventName, String message) {
            _LOG.entering(_CLASS_NAME, "messageReceived", message);
            messageArrived(eventName, message);
            
            /*
            EventSourceEvent event = new EventSourceEvent(this, EventSourceEvent.Type.MESSAGE, message);
            for (EventSourceListener listener : listeners) {
                try {
                    listener.onMessage(event);
                } catch (RuntimeException e) {
                    LOG.logp(Level.WARNING, CLASS_NAME, "onMessage", "Application threw an exception during onMessage: "+e.getMessage(), e);
                }
            }
            */
        }
        
        @Override
        public void streamErrored(Exception exception) {
            _LOG.entering(_CLASS_NAME, "streamErrored");
            connectionFailed(exception);
            
            /*
            EventSourceEvent event = new EventSourceEvent(this, EventSourceEvent.Type.ERROR);
            for (EventSourceListener listener : listeners) {
                try {
                    listener.onError(event);
                } catch (RuntimeException e) {
                    LOG.logp(Level.WARNING, CLASS_NAME, "onError", "Application threw an exception during onError: "+e.getMessage(), e);
                }
            }
            */
        }
    };    
}
