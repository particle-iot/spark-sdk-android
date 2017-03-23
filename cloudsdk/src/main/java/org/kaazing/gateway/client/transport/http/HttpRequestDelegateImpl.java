/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.client.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.transport.CloseEvent;
import org.kaazing.gateway.client.transport.ErrorEvent;
import org.kaazing.gateway.client.transport.IoBufferUtil;
import org.kaazing.gateway.client.transport.LoadEvent;
import org.kaazing.gateway.client.transport.OpenEvent;
import org.kaazing.gateway.client.transport.ProgressEvent;
import org.kaazing.gateway.client.transport.ReadyStateChangedEvent;

public class HttpRequestDelegateImpl implements HttpRequestDelegate {
    private static final String CLASS_NAME = HttpRequestDelegateImpl.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    /*
     * XmlHTTPReqeuest states const unsigned short UNSENT = 0; const unsigned short OPENED = 1; const unsigned short HEADERS_RECEIVED = 2; const unsigned short
     * LOADING = 3; const unsigned short DONE = 4;
     */
    private enum State {
        UNSENT, OPENED, HEADERS_RECEIVED, LOADING, DONE
    }

    private State readyState = State.UNSENT;

    private ByteBuffer responseBuffer = ByteBuffer.allocate(5000);
    private ByteBuffer completedResponseBuffer;
    private HttpURLConnection connection = null;
    private HttpRequestDelegateListener listener;

    private int httpResponseCode;

    private StreamReader reader;

    private boolean async;

    public HttpRequestDelegateImpl() {
        LOG.entering(CLASS_NAME, "<init>");
    }

    public final State getReadyState() {
        LOG.exiting(CLASS_NAME, "getReadyState", readyState);
        return readyState;
    }

    public ByteBuffer getResponseText() {
        LOG.entering(CLASS_NAME, "getResponseText");
        switch (readyState) {
            case LOADING:
            case OPENED:
                return responseBuffer.duplicate();
            case DONE:
                return completedResponseBuffer;
        }
        return null;
    }

    public int getStatusCode() {
        LOG.exiting(CLASS_NAME, "getStatusCode", httpResponseCode);
        return httpResponseCode;
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.client.transport.http.HttpRequestDelegate#abort()
     */
    public void processAbort() {
        LOG.entering(CLASS_NAME, "abort");
        if (reader != null) {
            reader.stop();
            reader = null;
        }
    }

    public String getAllResponseHeaders() {
        LOG.entering(CLASS_NAME, "getAllResponseHeaders");
        if (readyState == State.LOADING || readyState == State.DONE) {
            // return all headers from the HttpUrlConnection;
            String headerText = connection.getHeaderFields().toString();
            LOG.exiting(CLASS_NAME, "getAllResponseHeaders", headerText);
            return headerText;
        } else {
            LOG.exiting(CLASS_NAME, "getAllResponseHeaders");
            return null;
        }
    }

    public String getResponseHeader(String header) {
        LOG.entering(CLASS_NAME, "getResponseHeader", header);
        if (readyState == State.LOADING || readyState == State.DONE || readyState == State.HEADERS_RECEIVED) {
            String headerText = connection.getHeaderField(header);
            LOG.exiting(CLASS_NAME, "getResponseHeader", headerText);
            return headerText;
        } else {
            LOG.exiting(CLASS_NAME, "getResponseHeader");
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.client.transport.http.HttpRequestDelegate#open(java.lang.String, java.net.URL, java.lang.String, boolean)
     */
    public void processOpen(String method, URL url, String origin, boolean async, long connectTimeout) throws Exception {
        LOG.entering(CLASS_NAME, "processOpen", new Object[]{method, url, origin, async});

        this.async = async;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout((int) connectTimeout);

        if (!origin.equalsIgnoreCase("null") && !origin.startsWith("privileged")) {
            URL originUrl = new URL(origin);
            origin = originUrl.getProtocol() + "://" + originUrl.getAuthority();
        }

        // connection.addRequestProperty("X-Origin", origin);
        connection.addRequestProperty("Origin", origin);
        setReadyState(State.OPENED);

        listener.opened(new OpenEvent());
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.client.transport.http.HttpRequestDelegate#send(java.nio.ByteBuffer)
     */
    public void processSend(ByteBuffer content) {
        LOG.entering(CLASS_NAME, "processSend", content);
        if (readyState != State.OPENED && readyState != State.HEADERS_RECEIVED) {
            throw new IllegalStateException(readyState + " HttpRequest must be in an OPEN state before " + "invocation of the send() method");
        }
        try {
            if (!async && content != null && content.hasRemaining()) {
                connection.setDoOutput(true);
                connection.setDoInput(true);

                OutputStream out = connection.getOutputStream();
                out.write(content.array(), content.arrayOffset(), content.remaining());
                out.flush();
            }

            connection.connect();
            reader = new StreamReader();
            Thread t = new Thread(reader, "HttpRequestDelegate stream reader");
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            LOG.log(Level.FINE, "While processing http request", e);
            // e.printStackTrace();
            listener.errorOccurred(new ErrorEvent(e));
        }
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.bridge.HttpRequestDelegate#setRequestHeader(java.lang.String, java.lang.String)
     */
    public void setRequestHeader(String header, String value) {
        LOG.entering(CLASS_NAME, "setRequestHeader", new Object[]{header, value});
        HttpRequestUtil.validateHeader(header);
        connection.addRequestProperty(header, value);
    }

    protected void reset() {
        LOG.entering(CLASS_NAME, "reset");
        responseBuffer = null;
        completedResponseBuffer = null;
        setStatus(-1);
        setReadyState(State.UNSENT);
    }

    private void setReadyState(State state) {
        LOG.entering(CLASS_NAME, "setReadyState", state);
        this.readyState = state;
    }

    private void setStatus(int status) {
        LOG.entering(CLASS_NAME, "setStatus", status);
        this.httpResponseCode = status;
    }

    private final class StreamReader implements Runnable {
        private final String CLASS_NAME = StreamReader.class.getName();

        private AtomicBoolean stopped = new AtomicBoolean(false);
        private AtomicBoolean requestCompleted = new AtomicBoolean(false);

        public void run() {
            try {
                run2();
            } catch (Exception e) {
                LOG.log(Level.INFO, e.getMessage(), e);
            }
        }

        void run2() {
            LOG.entering(CLASS_NAME, "run");
            InputStream in;
            try {
                httpResponseCode = connection.getResponseCode();

                // For streaming responses Java returns -1 as the response code
                if (httpResponseCode != -1 && (httpResponseCode < 200 || httpResponseCode == 400 || httpResponseCode == 402 || httpResponseCode == 403 || httpResponseCode == 404)) {
                    Exception ex = new Exception("Unexpected HTTP response code received: code = " + httpResponseCode);
                    listener.errorOccurred(new ErrorEvent(ex));
                    throw ex;
                }
                Map<String, List<String>> headers = connection.getHeaderFields();
                StringBuilder allHeadersBuffer = new StringBuilder();
                int numHeaders = headers.size();
                for (int i = 0; i < numHeaders; i++) {
                    allHeadersBuffer.append(connection.getHeaderFieldKey(i));
                    allHeadersBuffer.append(":");
                    allHeadersBuffer.append(connection.getHeaderField(i));
                    allHeadersBuffer.append("\n");
                }
                String allHeaders = allHeadersBuffer.toString();
                String[] params = new String[]{Integer.toString(State.HEADERS_RECEIVED.ordinal()), Integer.toString(httpResponseCode), connection.getResponseMessage() + "",
                        allHeaders};

                setReadyState(State.HEADERS_RECEIVED);
                listener.readyStateChanged(new ReadyStateChangedEvent(params));

                if (httpResponseCode == 401) {
                    listener.loaded(new LoadEvent(ByteBuffer.allocate(0)));
                    requestCompleted.compareAndSet(false, true);
                    connection.disconnect();
                    return;
                }
                in = connection.getInputStream();

            } catch (IOException e) {
                LOG.severe(e.toString());
                listener.errorOccurred(new ErrorEvent(e));
                return;
            } catch (Exception ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                listener.errorOccurred(new ErrorEvent(ex));
                return;
            }

            try {
                if (in == null) {
                    in = connection.getInputStream();
                    if (in == null) {
                        String s = "Input stream not ready";
                        Exception ex = new RuntimeException(s);
                        listener.errorOccurred(new ErrorEvent(ex));
                        LOG.severe(s);
                        throw ex;
                    }
                }


                byte[] payloadBuffer = new byte[4096]; // read in chunks
                while (!stopped.get()) {
                    int numberOfBytesRead = in.read(payloadBuffer, 0, payloadBuffer.length);
                    if (numberOfBytesRead == -1) {
                        // end of stream, break from loop
                        break;
                    }
                    ByteBuffer payload = ByteBuffer.wrap(payloadBuffer, 0, numberOfBytesRead);
                    if (!async) {
                        // build up the buffer for completed response only for
                        // synchronous requests
                        // expand the buffer if required
                        int payloadSize = payload.remaining();
                        if (!IoBufferUtil.canAccomodate(responseBuffer, payloadSize)) {
                            responseBuffer = IoBufferUtil.expandBuffer(responseBuffer, payloadSize);
                        }
                        responseBuffer.put(payload);
                        // the put above resets the position of payload, flip it so we can reuse it
                        payload.flip();
                    }
                    listener.progressed(new ProgressEvent(payload, 0, 0));
                }

                if (!stopped.get()) {
                    // We want to fire the load event for complete responses
                    // only that are
                    // regular HTTP requests. For streaming request, we expect
                    // the caller
                    // to call abort
                    responseBuffer.flip();
                    completedResponseBuffer = responseBuffer.duplicate();
                    setReadyState(State.DONE);

                    try {
                        listener.loaded(new LoadEvent(completedResponseBuffer));
                    } finally {
                        requestCompleted.compareAndSet(false, true);
                        try {
                            connection.disconnect();
                        } finally {
                            listener.closed(new CloseEvent(1000, true, ""));
                        }
                    }
                }
            } catch (IOException e) {
                LOG.severe(e.toString());
                if (!requestCompleted.get()) {
                    listener.errorOccurred(new ErrorEvent(e));
                }
            } catch (Exception ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                listener.errorOccurred(new ErrorEvent(ex));
            }
        }

        public void stop() {
            LOG.entering(CLASS_NAME, "stop");
            this.stopped.set(true);
        }
    }

    @Override
    public void setListener(HttpRequestDelegateListener listener) {
        this.listener = listener;
    }
}
