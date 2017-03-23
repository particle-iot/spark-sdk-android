/**
 * Parts of this code copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
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

import org.kaazing.gateway.client.impl.http.HttpRequest;
import org.kaazing.gateway.client.impl.http.HttpRequest.Method;
import org.kaazing.gateway.client.impl.http.HttpRequestAuthenticationHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestHandlerFactory;
import org.kaazing.gateway.client.impl.http.HttpRequestListener;
import org.kaazing.gateway.client.impl.http.HttpRequestRedirectHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestTransportHandler;
import org.kaazing.gateway.client.impl.http.HttpResponse;
import org.kaazing.gateway.client.impl.ws.ReadyState;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.particle.android.sdk.cloud.ParticleCloud;

/**
 * ServerSentEvent stream implementation.
 */
public class AuthenticatedSseEventStream extends SseEventStream {

    private static final String MESSAGE = "message";
    private static final String CLASS_NAME = AuthenticatedSseEventStream.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final StringBuffer dataBuffer = new StringBuffer();
    private transient static final Timer timer = new Timer("reconnect", true);

    static final HttpRequestHandlerFactory SSE_HANDLER_FACTORY = () -> {
        HttpRequestAuthenticationHandler authHandler = new HttpRequestAuthenticationHandler();
        HttpRequestRedirectHandler redirectHandler = new HttpRequestRedirectHandler();
        HttpRequestHandler transportHandler = HttpRequestTransportHandler.DEFAULT_FACTORY.createHandler();

        authHandler.setNextHandler(redirectHandler);
        redirectHandler.setNextHandler(transportHandler);

        return authHandler;
    };

    private ReadyState readyState = ReadyState.CONNECTING;
    private String lastEventId = "";
    private boolean aborted = false;
    private boolean errored = false;
    private String sseLocation;
    private long retry = 3000; // same as actionscript implementation
    private boolean immediateReconnect = false;
    private String messageBuffer = "";
    private HttpRequest sseSource;
    private AtomicBoolean progressEventReceived = new AtomicBoolean(false);
    private AtomicBoolean reconnected = new AtomicBoolean(false);
    private HttpRequestHandler sseHandler;
    private SseEventStreamListener listener;
    private final ParticleCloud cloud;
    private String name = MESSAGE;

    public AuthenticatedSseEventStream(String sseLoc, ParticleCloud cloud) throws IOException {
        super(sseLoc);
        LOG.entering(CLASS_NAME, "<init>", sseLoc);

        this.cloud = cloud;

        // Validate the URI.
        URI.create(sseLoc);

        this.sseLocation = sseLoc;

        sseHandler = SSE_HANDLER_FACTORY.createHandler();

        sseHandler.setListener(new EventStreamHttpRequestListener());
    }

    public ReadyState getReadyState() {
        return readyState;
    }

    public void stop() {
        LOG.entering(CLASS_NAME, "stop");
        readyState = ReadyState.CLOSED;
        sseHandler.processAbort(sseSource);
        aborted = true;
    }

    public void connect() throws IOException {
        LOG.entering(CLASS_NAME, "connect");
        if (lastEventId != null && (lastEventId.length() > 0)) {
            sseLocation += (!sseLocation.contains("?") ? "?" : "&") + ".ka=" + lastEventId;
        }

        try {
            HttpURI uri = new HttpURI(this.sseLocation);
            sseSource = new HttpRequest(Method.GET, uri, true);
            sseSource.setHeader("Authorization", "Bearer " + this.cloud.getAccessToken());
            sseHandler.processOpen(sseSource);

            if (!reconnected.get()) {
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        // TODO: Why is this commented out? - no fallback to long polling?

                        // if (!SseEventStream.this.progressEventReceived.get() && readyState != ReadyState.CLOSED) {
                        // if (sseLocation.indexOf("?") == -1) {
                        // sseLocation += "?.ki=p";
                        // }
                        // else {
                        // sseLocation += "&.ki=p";
                        // }
                        // listener.reconnectScheduled = true;
                        // reconnected.set(true);
                        // retry = 0;
                        // try {
                        // connect();
                        // }
                        // catch (IOException e) {
                        // // TODO Auto-generated catch block
                        // e.printStackTrace();
                        // }
                        // }
                    }
                };
                Timer timer = new Timer();
                timer.schedule(timerTask, 3000);
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, e.getMessage(), e);
            doError(e);
        }
    }

    public long getRetryTimeout() {
        return retry;
    }

    public void setRetryTimeout(long millis) {
        retry = millis;
    }

    private synchronized void reconnect() {
        LOG.entering(CLASS_NAME, "reconnect");
        if (readyState != ReadyState.CLOSED) {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        connect();
                    } catch (IOException e) {
                        LOG.log(Level.INFO, e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                }
            };
            timer.schedule(task, retry);
        }
    }

    private synchronized void processProgressEvent(String message) {
        LOG.entering(CLASS_NAME, "processProgressEvent", message);
        String line;
        try {
            messageBuffer = messageBuffer + message;
            String field;
            String value;
            immediateReconnect = false;
            while (!aborted && !errored) {
                line = fetchLineFromBuffer();
                if (line == null) {
                    break;
                }

                if (line.length() == 0 && dataBuffer.length() > 0) {
                    synchronized (dataBuffer) {
                        int dataBufferlength = dataBuffer.length();
                        if (dataBuffer.charAt(dataBufferlength - 1) == '\n') {
                            dataBuffer.replace(dataBufferlength - 1, dataBufferlength, "");
                        }
                        doMessage(name, dataBuffer.toString());
                        dataBuffer.setLength(0);
                    }
                }

                int colonAt = line.indexOf(':');
                if (colonAt == -1) {
                    // no colon, line is field name with empty value
                    field = line;
                    value = "";
                } else if (colonAt == 0) {
                    // leading colon indicates comment line
                    continue;
                } else {
                    field = line.substring(0, colonAt);
                    int valueAt = colonAt + 1;
                    if (line.length() > valueAt && line.charAt(valueAt) == ' ') {
                        valueAt++;
                    }
                    value = line.substring(valueAt);
                }
                // process the field of completed event
                switch (field) {
                    case "event":
                        name = value;
                        break;
                    case "id":
                        this.lastEventId = value;
                        break;
                    case "retry":
                        retry = Integer.parseInt(value);
                        break;
                    case "data":
                        // deliver event if data is specified and non-empty, or name is specified and not "message"
                        if (name != null && name.length() > 0 && !MESSAGE.equals(name)) {
                            dataBuffer.append(value).append("\n");
                        }
                        break;
                    case "location":
                        if (value.length() > 0) {
                            this.sseLocation = value;
                        }
                        break;
                    case "reconnect":
                        immediateReconnect = true;
                        break;
                }
            }

            if (immediateReconnect) {
                retry = 0;
                // this will be done on the load
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, e.getMessage(), e);
            doError(e);
        }
    }

    private String fetchLineFromBuffer() {
        LOG.entering(CLASS_NAME, "fetchLineFromBuffer");
        int lf = this.messageBuffer.indexOf("\n");
        if (lf == -1) {
            lf = this.messageBuffer.indexOf("\r");
        }
        if (lf != -1) {
            String ret = messageBuffer.substring(0, lf);
            messageBuffer = messageBuffer.substring(lf + 1);
            return ret;
        }
        return null;
    }

    private class EventStreamHttpRequestListener implements HttpRequestListener {
        private final String CLASS_NAME = EventStreamHttpRequestListener.class.getName();
        private final Logger LOG = Logger.getLogger(CLASS_NAME);

        boolean reconnectScheduled = false;

        EventStreamHttpRequestListener() {
            LOG.entering(CLASS_NAME, "<init>");
        }

        @Override
        public void requestReady(HttpRequest request) {
        }

        @Override
        public void requestOpened(HttpRequest request) {
            doOpen();
        }

        @Override
        public void requestProgressed(HttpRequest request, WrappedByteBuffer payload) {
            progressEventReceived.set(true);
            String response = payload.getString(UTF_8);
            processProgressEvent(response);
        }

        @Override
        public void requestLoaded(HttpRequest request, HttpResponse response) {
            // for Long polling. If we get an onload we have to
            // reconnect.
            if (readyState != ReadyState.CLOSED) {
                if (immediateReconnect) {
                    retry = 0;
                    if (!reconnectScheduled) {
                        reconnect();
                    }
                }
            }
        }

        @Override
        public void requestAborted(HttpRequest request) {
        }

        @Override
        public void requestClosed(HttpRequest request) {
        }

        @Override
        public void errorOccurred(HttpRequest request, Exception exception) {
            doError(exception);
        }
    }

    private void doOpen() {
        /*
          Only file the event once in the case its already opened,
          Currently, this is being called twice, once when the SSE
          gets connected and then again when the ready state changes.
         */
        if (readyState == ReadyState.CONNECTING) {
            readyState = ReadyState.OPEN;
            listener.streamOpened();
        }
    }

    private void doMessage(String eventName, String data) {
        // messages before OPEN and after CLOSE should not be delivered.
        if (getReadyState() != ReadyState.OPEN) {
            LOG.log(Level.INFO, "event message discarded " + getReadyState().name());
            return;
        }

        listener.messageReceived(eventName, data);
    }

    private void doError(Exception exception) {
        if (getReadyState() == ReadyState.CLOSED) {
            LOG.log(Level.INFO, "event error discarded " + getReadyState().name());
            return;
        }

        // TODO: Set readyState to CLOSED?
        errored = true;
        listener.streamErrored(exception);
    }

    public void setListener(SseEventStreamListener listener) {
        this.listener = listener;
    }
}
