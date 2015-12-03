/**
 * Portions of this file copyright (c) 2007-2014 Kaazing Corporation.
 * All rights reserved.
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.net.sse.impl;

import org.kaazing.net.http.HttpRedirectPolicy;
import org.kaazing.net.impl.util.BlockingQueueImpl;
import org.kaazing.net.sse.SseEventReader;
import org.kaazing.net.sse.SseException;

import java.io.IOException;
import java.net.URI;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.utils.TLog;

import static io.particle.android.sdk.utils.Py.list;


// FIXME: replace this entire SSE stack with something backed by OkHttp.
public class AuthenticatedSseEventSourceImpl extends SseEventSourceImpl {

    private static final String _CLASS_NAME = AuthenticatedSseEventSourceImpl.class.getName();
    private static final TLog _LOG = TLog.get(AuthenticatedSseEventSourceImpl.class);

    enum ReadyState {
        CONNECTING,
        OPEN,
        CLOSING,
        CLOSED
    }

    private final SseEventReaderImpl _eventReader;
    private final BlockingQueueImpl<Object> _sharedQueue;
    private final URI _location;

    private SseEventStream _eventStream;
    private long _retryTimeout = 3000;
    private HttpRedirectPolicy _redirectOption;
    private ReadyState _readyState;
    private SseException _exception;
    private ParticleCloud cloud;

    public AuthenticatedSseEventSourceImpl(URI location, ParticleCloud cloud) {
        super(location);
        this.cloud = cloud;

        URI loc = location;

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
        _sharedQueue = new BlockingQueueImpl<>();
        _eventReader = new SseEventReaderImpl(this, _sharedQueue);
    }

    @Override
    public synchronized void close() throws IOException {
        if (list(ReadyState.CLOSED, ReadyState.CLOSING).contains(_readyState)) {
            // Since the WebSocket is already closed/closing, we just bail.
            _LOG.v("Event source is not connected");
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
        if (_readyState != ReadyState.CLOSED) {
            String s = "Event source must be closed before connecting";
            throw new SseException(s);
        }

        _eventStream = new AuthenticatedSseEventStream(_location.toString(), cloud);
        _eventStream.setListener(_eventStreamListener);
        _eventStream.setRetryTimeout(_retryTimeout);

        // Ensure that the reader is reset and ready to block the consumer
        // if no data has been produced.
        _eventReader.reset();

        // Prepare the state for connection.
        _readyState = ReadyState.CONNECTING;
        setException(null);

        // Connect to the event source. Note that it all happens on the same
        // thread. The registered SseEventStreamListener is also invoked as part
        // of this call.
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
            } catch (InterruptedException ex) {
                _LOG.i(ex.getMessage(), ex);
            }
        }
    }

    @SuppressWarnings("unused")
    private synchronized void connectionClosed(String reason) {
        _readyState = ReadyState.CLOSED;

        if (reason != null) {
            setException(new SseException(reason));
        }

        cleanupAfterClose();

        // Unblock the close() call so that it can proceed.
        notifyAll();
    }

    private synchronized void connectionFailed(Exception exception) {
        SseException ex = (exception == null)
                ? new SseException("Connection Failed")
                : new SseException(exception);
        setException(ex);

        _readyState = ReadyState.CLOSED;

        cleanupAfterClose();

        // Unblock threads so that they can proceed.
        notifyAll();
    }

    private synchronized void cleanupAfterClose() {
        // Notify the waiting consumers that the connection is closing.
        try {
            _eventReader.close();
        } catch (IOException ex) {
            _LOG.v(ex.getMessage(), ex);
        }
    }


    private SseEventStreamListener _eventStreamListener = new SseEventStreamListener() {

        @Override
        public void streamOpened() {
            _LOG.d("entering " + _CLASS_NAME + ".streamOpened");
            connectionOpened();
        }

        @Override
        public void messageReceived(String eventName, String message) {
            _LOG.d("entering " + _CLASS_NAME + ".messageReceived: " + message);
            messageArrived(eventName, message);
        }

        @Override
        public void streamErrored(Exception exception) {
            _LOG.d("entering " + _CLASS_NAME + ".streamErrored");
            connectionFailed(exception);
        }
    };

}
