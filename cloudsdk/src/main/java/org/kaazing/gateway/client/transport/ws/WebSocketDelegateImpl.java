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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.transport.AuthenticateEvent;
import org.kaazing.gateway.client.transport.CloseEvent;
import org.kaazing.gateway.client.transport.ErrorEvent;
import org.kaazing.gateway.client.transport.IoBufferUtil;
import org.kaazing.gateway.client.transport.LoadEvent;
import org.kaazing.gateway.client.transport.MessageEvent;
import org.kaazing.gateway.client.transport.OpenEvent;
import org.kaazing.gateway.client.transport.ProgressEvent;
import org.kaazing.gateway.client.transport.ReadyStateChangedEvent;
import org.kaazing.gateway.client.transport.RedirectEvent;
import org.kaazing.gateway.client.transport.http.HttpRequestDelegate;
import org.kaazing.gateway.client.transport.http.HttpRequestDelegateFactory;
import org.kaazing.gateway.client.transport.http.HttpRequestDelegateImpl;
import org.kaazing.gateway.client.transport.http.HttpRequestDelegateListener;
import org.kaazing.gateway.client.transport.ws.WsMessage.Kind;

public class WebSocketDelegateImpl implements WebSocketDelegate {
    private static final String CLASS_NAME = WebSocketDelegateImpl.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final byte[] GET_BYTES = "GET".getBytes();
    private static final String APPLICATION_PREFIX = "Application ";
    private static final String WWW_AUTHENTICATE = "WWW-Authenticate: ";
    private static final String HTTP_1_1_START = "HTTP/1.1";
    private static final int HTTP_1_1_START_LEN = HTTP_1_1_START.length();
    private static final byte[] HTTP_1_1_START_BYTES = HTTP_1_1_START.getBytes();
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    public static final int CLOSE_NO_STATUS = 1005;
    public static final int CLOSE_ABNORMAL = 1006;
        
    static enum ConnectionStatus {
        START, STATUS_101_READ, CONNECTION_UPGRADE_READ, COMPLETED, ERRORED
    }

    public static enum ReadyState {
        CONNECTING, OPEN, CLOSING, CLOSED;
    }
    
    private static final byte[] HTTP_1_1_BYTES = "HTTP/1.1".getBytes();
    private static final byte[] COLON_BYTES = ":".getBytes();
    private static final byte[] SPACE_BYTES = " ".getBytes();
    private static final byte[] CRLF_BYTES = "\r\n".getBytes();
    private static final String HEADER_ORIGIN = "Origin";
    private static final String HEADER_CONNECTION = "Connection";
    private static final String HEADER_HOST = "Host";
    private static final String HEADER_UPGRADE = "Upgrade";
    private static final String HEADER_PROTOCOL = "Sec-WebSocket-Protocol";
    private static final String HEADER_WEBSOCKET_KEY = "Sec-WebSocket-Key";
    private static final String HEADER_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
    private static final String HEADER_VERSION = "13";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_LOCATION = "Location";
    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String WEB_SOCKET_LOWERCASE = "websocket";
    private static final String HEADER_COOKIE = "Cookie";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    private BridgeSocket socket;
    private boolean stopReaderThread;
    private boolean connectionUpgraded = false;
    private URI url;
    private String origin;
    private URI originUri;
    private String[] requestedProtocols;
    private boolean secure;
    private WebSocketDelegateListener listener;
    protected String cookies = null;
    private String authorize = null;
    private AtomicBoolean closed = new AtomicBoolean(false);
    String websocketKey;
    
    private final long connectTimeout;

    //--------Idle Timeout-------------//
    private final AtomicInteger idleTimeout = new AtomicInteger();
    private final AtomicLong lastMessageTimestamp = new AtomicLong();
    private Timer idleTimer = null;
    
    //WebSocket rfc6455 properties
    private ReadyState readyState = ReadyState.CONNECTING; 
    public ReadyState getReadyState(){
        return readyState;
    }
    
    int bufferedAmount;
    public int getBufferedAmount() {
        return bufferedAmount;
    }
    
    private String secProtocol;
    public String getSecProtocol() {
        return secProtocol;
    }
    
    private String extensions;
    public String getExtensions() {
        return extensions;
    }
    // close event data
    private boolean wasClean = false;
    private int code = CLOSE_ABNORMAL;
    private String reason = "";
    
    HttpRequestDelegateFactory HTTP_REQUEST_DELEGATE_FACTORY = new HttpRequestDelegateFactory() {
        @Override
        public HttpRequestDelegate createHttpRequestDelegate() {
            return new HttpRequestDelegateImpl();
        }
    };

    BridgeSocketFactory BRIDGE_SOCKET_FACTORY = new BridgeSocketFactory() {
        @Override
        public BridgeSocket createSocket(boolean secure) throws IOException {
            return new BridgeSocketImpl(secure);
        }
    };

    /**
     * WebSocket Java API for use in Java Web Start applications
     * 
     * @param url
     *            WebSocket URL location
     * @param origin
     *            the codebase+hostname from the JNLP of the Java Web Start app
     * @param protocol
     *            WebSocket protocol
     * @throws Exception
     */
    public WebSocketDelegateImpl(URI url, URI origin, String[] protocols, long connectTimeout) {
        LOG.entering(CLASS_NAME, "<init>", new Object[] {url, origin, protocols});
        if (origin == null) {
            throw new IllegalArgumentException("Please specify the origin for the WebSocket connection");
        }

        if (url == null) {
            throw new IllegalArgumentException("Please specify the target for the WebSocket connection");
        }
        this.url = url;

        if ((origin.getScheme() == null) || (origin.getHost() == null)) {
            this.origin = "null";
        }
        else {
            String originScheme = origin.getScheme();
            String originHost = origin.getHost();
            int originPort = origin.getPort();
            if (originPort == -1) {
                originPort = (originScheme.equals("https")) ? 443 : 80;
            }
            this.origin = originScheme + "://" + originHost + ":" + originPort;
        }

        this.requestedProtocols = protocols;
        secure = url.getScheme().equalsIgnoreCase("wss");
        this.connectTimeout = connectTimeout;
    }

    
    //------------------------------Idle Timer Start/Stop/Handler---------------------//
    
    private void startIdleTimer(long delayInMilliseconds) {
        LOG.fine("Starting idle timer");
        if (this.idleTimer != null) {
            idleTimer.cancel();
            idleTimer = null;
        }
        
        idleTimer = new Timer("IdleTimer", true);
        idleTimer.schedule(new TimerTask() {
            
            @Override
            public void run() {
                idleTimerHandler();
            }
            
        }, delayInMilliseconds);
    }
    
    private void idleTimerHandler() {
        LOG.fine("Idle timer scheduled");
        long idleDuration = System.currentTimeMillis() - lastMessageTimestamp.get();
        if (idleDuration > idleTimeout.get()) {
            String message = "idle duration - " + idleDuration + " exceeded idle timeout - " + idleTimeout;
            LOG.fine(message);
            handleClose(null);
        }
        else {
            // Reschedule timer
            startIdleTimer(idleTimeout.get() - idleDuration);
        }
    }
    
    private void stopIdleTimer() {
        LOG.fine("Stopping idle timer");
        if (idleTimer != null) {
            idleTimer.cancel();
            idleTimer = null;
        }
    }
    
    @Override
    public void setIdleTimeout(int milliSecond) {
        idleTimeout.set(milliSecond);
        if (milliSecond > 0) {
            // start monitor websocket traffic
            lastMessageTimestamp.set(System.currentTimeMillis());
            startIdleTimer(milliSecond);
        }
        else {
            stopIdleTimer();
        }
    }
    
    //-------------------------------------------------------------------------------//

    public void processOpen() {
        LOG.entering(CLASS_NAME, "processOpen");
        // pre-flight cookies request
        // Lookup the session cookie
        String scheme = this.url.getScheme();
        String host = this.url.getHost();
        int port = this.url.getPort();
        String path = this.url.getPath();
        if (port == -1) {
            port = (scheme.equals("wss")) ? 443 : 80;
        }
        LOG.fine("processOpen: Connecting to "+host+":"+port);

        String cookiesUri = scheme.replace("ws", "http") + "://" + host + ":" + port + path + "/;e/cookies?.krn=" + Double.toString(Math.random()).substring(2);
        String query = this.url.getQuery();
        if (query != null && query.length() > 0) {
            // No need to check to append "?" since a query parameter is added above
            cookiesUri += "&" + query;
        }
        final HttpRequestDelegate cookiesRequest = HTTP_REQUEST_DELEGATE_FACTORY.createHttpRequestDelegate();

        cookiesRequest.setListener(new HttpRequestDelegateListener() {
            
            @Override
            public void opened(OpenEvent event) {
            }
            
            @Override
            public void readyStateChanged(ReadyStateChangedEvent event) {
            }
            
            @Override
            public void progressed(ProgressEvent progressEvent) {
            }
            
            @Override
            public void loaded(LoadEvent event) {
                switch (cookiesRequest.getStatusCode()) {
                case 200:
                case 201:
                    ByteBuffer responseBuf = cookiesRequest.getResponseText();
                    if (responseBuf != null && responseBuf.hasRemaining()) {
                        if (isHTTPResponse(responseBuf)) {
                            try {
                                handleWrappedHTTPResponse(responseBuf);
                                return;
                            }
                            catch (Exception e1) {
                                WebSocketDelegateImpl.this.handleClose(e1);
                                throw new IllegalStateException("Handling wrapped HTTP response failed", e1);
                            }
                        }
                        else {
                            cookies = new String(responseBuf.array(), responseBuf.position(), responseBuf.remaining());
                        }
                    }
                    break;
                case 301:
                case 302:
                case 307:
                    String location = cookiesRequest.getResponseHeader(HEADER_LOCATION);
                    LOG.finest("Redirect to " + location);
                    
                    URI uri;
                    try {
                        uri = new URI(location);
                        String query = uri.getQuery();
                        String newQuery = (query != null ? query + "&" : "") + ".kl=Y";
                        String redirectLocation = uri.getScheme().replace("http", "ws") + "://" + uri.getHost() + ":" + uri.getPort() + uri.getPath() + "?" + newQuery;
                        LOG.finest("Redirect as " + redirectLocation);
                        listener.redirected(new RedirectEvent(redirectLocation));
                    } catch (URISyntaxException e) {
                        LOG.severe("Redirect location invalid: "+location);
                    }
                    return;
                case 401:
                    String wwwAuthenticate = cookiesRequest.getResponseHeader(HEADER_WWW_AUTHENTICATE);
                    listener.authenticationRequested(new AuthenticateEvent(wwwAuthenticate));
                    return;
                default:
                    WebSocketDelegateImpl.this.readyState = ReadyState.CLOSED;
                    String s = "Cookies request: Invalid status code: " + cookiesRequest.getStatusCode();
                    listener.errorOccurred(new ErrorEvent(new IllegalStateException(s)));
                    return;
                }
                nativeConnect();
            }

            private void handleWrappedHTTPResponse(ByteBuffer responseBody) throws Exception {
                LOG.entering(CLASS_NAME, "cookiesRequest.handleWrappedHTTPResponse");
                String[] lines = getLines(responseBody);
                int statusCode = Integer.parseInt(lines[0].split(" ")[1]);
                switch (statusCode) {
                case HttpURLConnection.HTTP_UNAUTHORIZED: // 401
                    String wwwAuthenticate = null;
                    for (int i = 1; i < lines.length; i++) {
                        if (lines[i].startsWith(WWW_AUTHENTICATE)) {
                            wwwAuthenticate = lines[i].substring(WWW_AUTHENTICATE.length());
                            break;
                        }
                    }
                    LOG.finest("cookiesRequest.handleWrappedHTTPResponse: WWW-Authenticate: " + wwwAuthenticate);
                    if (wwwAuthenticate == null || "".equals(wwwAuthenticate)) {
                        LOG.severe("Missing authentication challenge in wrapped HTTP 401 response");
                        throw new IllegalStateException("Missing authentication challenge in wrapped HTTP 401 response");
                    }
                    else if (!wwwAuthenticate.startsWith(APPLICATION_PREFIX)) {
                        LOG.severe("Only Application challenges are supported by the client");
                        throw new IllegalStateException("Only Application challenges are supported by the client");
                    }
                    else {
                        listener.authenticationRequested(new AuthenticateEvent(wwwAuthenticate));
                    }
                    break;
                default:
                    throw new IllegalStateException("Unsupported wrapped response with HTTP status code " + statusCode);
                }
            }
            
            @Override
            public void closed(CloseEvent event) {
            }
            
            @Override
            public void errorOccurred(ErrorEvent event) {
                WebSocketDelegateImpl.this.readyState = ReadyState.CLOSED;
                listener.errorOccurred(new ErrorEvent(event.getException()));
            }
        });

        URL cookiesUrl;
        try {
            cookiesUrl = new URL(cookiesUri);
            cookiesRequest.processOpen("GET", cookiesUrl, this.origin, false, connectTimeout);
            if (authorize != null) {
                cookiesRequest.setRequestHeader(HEADER_AUTHORIZATION, authorize);
            }
            postProcessOpen(cookiesRequest);
            cookiesRequest.processSend(null);
        }
        catch (Exception e1) {
            LOG.severe(e1.toString());
            WebSocketDelegateImpl.this.readyState = ReadyState.CLOSED;
            listener.errorOccurred(new ErrorEvent(e1));
        }
    }

    // Hook for subclasses for testing cookies requests.
    protected void postProcessOpen(HttpRequestDelegate cookiesRequest) {
    }

    private static boolean isHTTPResponse(ByteBuffer buf) {
        boolean isHttpResponse = true;
        if (buf.remaining() >= HTTP_1_1_START_LEN) {
            for (int i = 0; i < HTTP_1_1_START_LEN; i++) {
                if (buf.get(i) != HTTP_1_1_START_BYTES[i]) {
                    isHttpResponse = false;
                    break;
                }
            }
        }
        return isHttpResponse;
    }

    private static String[] getLines(ByteBuffer buf) {
        List<String> lineList = new ArrayList<String>();
        while (buf.hasRemaining()) {
            byte next = buf.get();
            List<Byte> lineText = new ArrayList<Byte>();
            while (next != 13) { // CR
                lineText.add(next);
                if (buf.hasRemaining()) {
                    next = buf.get();
                }
                else {
                    break;
                }
            }
            if (buf.hasRemaining()) {
                next = buf.get(); // should be LF
            }
            byte[] lineTextBytes = new byte[lineText.size()];
            int i = 0;
            for (Byte text : lineText) {
                lineTextBytes[i] = text;
                i++;
            }
            try {
                lineList.add(new String(lineTextBytes, "UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Unrecognized Encoding from the server", e);
            }
        }
        String[] lines = new String[lineList.size()];
        lineList.toArray(lines);
        return lines;
    }

    protected void nativeConnect() {
        LOG.entering(CLASS_NAME, "nativeConnect");
        String host = this.url.getHost();
        int port = this.url.getPort();
        String scheme = this.url.getScheme();

        if (port == -1) {
            port = (scheme.equals("wss")) ? 443 : 80;
        }
        try {
            LOG.fine("WebSocketDelegate.nativeConnect(): Connecting to "+host+":"+port);
            socket = BRIDGE_SOCKET_FACTORY.createSocket(secure);
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            socket.setKeepAlive(true);
            socket.setSoTimeout(0); // continuously read from the socket
        }
        catch (Exception e) {
            LOG.log(Level.FINE, "WebSocketDelegateImpl nativeConnect(): "+e.getMessage(), e);
            // Fire error listener to request to try to emulate it
            WebSocketDelegateImpl.this.readyState = ReadyState.CLOSED;
            listener.errorOccurred(new ErrorEvent(e));
            return;
        }
        negotiateWebSocketConnection(socket);
    }

    private void negotiateWebSocketConnection(BridgeSocket socket) {
        LOG.entering(CLASS_NAME, "negotiateWebSocketConnection", socket);
        try {
            int headerCount = 9 + ((cookies == null) ? 0 : 1);
            String[] headerNames = new String[headerCount];
            String[] headerValues = new String[headerCount];
            int headerIndex = 0;
            headerNames[headerIndex] = HEADER_UPGRADE;
            headerValues[headerIndex++] = WEB_SOCKET_LOWERCASE;
            headerNames[headerIndex] = HEADER_CONNECTION;
            headerValues[headerIndex++] = HEADER_UPGRADE;
            headerNames[headerIndex] = HEADER_HOST;
            headerValues[headerIndex++] = this.url.getAuthority();
            headerNames[headerIndex] = HEADER_ORIGIN;
            headerValues[headerIndex++] = origin;

            headerNames[headerIndex] = HEADER_WEBSOCKET_VERSION;
            headerValues[headerIndex++] = HEADER_VERSION;
            headerNames[headerIndex] = HEADER_WEBSOCKET_KEY;
            if (websocketKey == null) {
                websocketKey = base64Encode(randomBytes(16));
            }
            headerValues[headerIndex++] = websocketKey;

            if (requestedProtocols != null && requestedProtocols.length > 0) {
                headerNames[headerIndex] = HEADER_PROTOCOL;
                
                String value;
                if (requestedProtocols.length == 1) {
                    value = requestedProtocols[0];
                }
                else {
                    value = "";
                    for (int i=0; i<requestedProtocols.length; i++) {
                        if (i>0) {
                            value += ",";
                        }
                        value += requestedProtocols[i];
                    }
                }
                
                headerValues[headerIndex++] = value;
            }

            if (cookies != null) {
                headerNames[headerIndex] = HEADER_COOKIE;
                headerValues[headerIndex++] = cookies;
            }

            if (authorize != null) {
                headerNames[headerIndex] = HEADER_AUTHORIZATION;
                headerValues[headerIndex] = authorize;
            }

            LOG.finer("Origin: " + origin);

            byte[] request = encodeGetRequest(this.url, headerNames, headerValues);

            // SOCKET DEBUGGING: OutputStream out = new LoggingOutputStream(socket.getOutputStream());
            OutputStream out = socket.getOutputStream();
            out.write(request);
            out.flush();

            InputStream in = socket.getInputStream();
            Thread readerThread = new Thread(new SocketReader(in), "WebSocketDelegate socket reader");
            readerThread.setDaemon(true);
            readerThread.start();

        } catch (Exception e) {
            LOG.severe(e.toString());
            handleError(e);
        }
    }

    public byte[] encodeGetRequest(URI requestURI, String[] names, String[] values) {
        
        // Any changes to this method should result in the getEncodeRequestSize method below
        // to get accurate length of the buffer that needs to be allocated.
        
        LOG.entering(CLASS_NAME, "encodeGetRequest", new Object[] {requestURI, names, values});
        int requestSize = getEncodeRequestSize(requestURI, names, values);
        ByteBuffer buf = ByteBuffer.allocate(requestSize);

        // Encode Request line
        buf.put(GET_BYTES);
        buf.put(SPACE_BYTES);
        String path = requestURI.getPath();
        if (requestURI.getQuery() != null) {
            path += "?" + requestURI.getQuery();
        }
        buf.put(path.getBytes());
        buf.put(SPACE_BYTES);
        buf.put(HTTP_1_1_BYTES);
        buf.put(CRLF_BYTES);

        // Encode headers
        for (int i = 0; i < names.length; i++) {
            String headerName = names[i];
            String headerValue = values[i];
            if (headerName != null && headerValue != null) {
                buf.put(headerName.getBytes());
                buf.put(COLON_BYTES);
                buf.put(SPACE_BYTES);
                buf.put(headerValue.getBytes());
                buf.put(CRLF_BYTES);
            }
        }

        // Encoding cookies, content length and content not done here as we
        // don't have it in the initial GET request.

        buf.put(CRLF_BYTES);
        buf.flip();
        return buf.array();
    }

    private int getEncodeRequestSize(URI requestURI, String[] names, String[] values) {
        int size = 0;

        // Encode Request line
        size += GET_BYTES.length;
        size += SPACE_BYTES.length;
        String path = requestURI.getPath();
        if (requestURI.getQuery() != null) {
            path += "?" + requestURI.getQuery();
        }
        size += path.getBytes().length;
        size += SPACE_BYTES.length;
        size += HTTP_1_1_BYTES.length;
        size += CRLF_BYTES.length;

        // Encode headers
        for (int i = 0; i < names.length; i++) {
            String headerName = names[i];
            String headerValue = values[i];
            if (headerName != null && headerValue != null) {
                size += headerName.getBytes().length;
                size += COLON_BYTES.length;
                size += SPACE_BYTES.length;
                size += headerValue.getBytes().length;
                size += CRLF_BYTES.length;
            }
        }

        size += CRLF_BYTES.length;

        LOG.fine("Returning a request size of " + size);
        return size;
    }

    public void processDisconnect() throws IOException {
        processDisconnect((short) 0, null);
    }
    
    public void processDisconnect(short code, String reason) throws IOException {
        LOG.entering(CLASS_NAME, "disconnect");
        //stopReaderThread = true;  --- rfc 6455 donot stop SockectReader, wait for CloseFrame

        //send close frame if webSocket is open
        if (this.readyState == ReadyState.OPEN) {
            this.readyState = ReadyState.CLOSING;
            ByteBuffer data;
            if (code == 0) {
                data = ByteBuffer.allocate(0);
            }
            else {
                //if code is present, it must equal to 1000 or in range 3000 to 4999
                if (code != 1000 && (code < 3000 || code > 4999)) {
                    throw new IllegalArgumentException("code must equal to 1000 or in range 3000 to 4999");
                }
                ByteBuffer reasonBuf = null;
                if (reason != null && reason.length() > 0) {
                    //UTF 8 encode reason
                    Charset cs = Charset.forName("UTF-8");
                    reasonBuf = cs.encode(reason);
                    if (reasonBuf.limit() > 123) {
                        throw new IllegalArgumentException("Reason is longer than 123 bytes");
                    }
                }
                data = ByteBuffer.allocate(2 + (reasonBuf == null ? 0 : reasonBuf.remaining()));
                data.putShort(code);
                
                if (reasonBuf != null) {
                    data.put(reasonBuf);   
                }
                
                data.flip();
            }    
            this.send(WsFrameEncodingSupport.rfc6455Encode(new WsMessage(data, Kind.CLOSE), new Random().nextInt()));
        }
        else if (readyState == ReadyState.CONNECTING) {
            //websocket not open yet, fire close event
            stopReaderThread = true;
            handleClose(null);
        }
        
        // Do not close the underlying socket connection immediately. As per the RFC spec - 
        // After both sending and receiving a Close message, an endpoint considers the WebSocket 
        // connection closed and MUST close the underlying TCP connection.
        // Schedule a timer to close the underlying Socket connection if the CLOSE frame is not 
        // received from the Gateway within 5 seconds.
        Timer t = new Timer("SocketCloseTimer", true);
        t.schedule(new TimerTask() {
			
			@Override
			public void run() {
			    try {
    				if (WebSocketDelegateImpl.this.readyState != ReadyState.CLOSED) {
    				    stopIdleTimer();
    					closeSocket();	
    				}
			    }
			    finally {
				    cancel();
			    }
			}
		}, 5000);
        
        //else do nothing for CLOSING and CLOSED
    }

    public void processAuthorize(String authorize) {
        LOG.entering(CLASS_NAME, "processAuthorize", authorize);
        this.authorize = authorize;
        processOpen();
    }

    @Override
    public void processSend(ByteBuffer data) {
        LOG.entering(CLASS_NAME, "processSend", data);

        //move encoder code to core.java.client.internal, here just send data
        ByteBuffer frame = WsFrameEncodingSupport.rfc6455Encode(new WsMessage(data, Kind.BINARY), new Random().nextInt());
        send(frame);
        // send(data);
    }
    
    @Override
    public void processSend(String data) {
        LOG.entering(CLASS_NAME, "processSend", data);
        ByteBuffer buf = null;
        try {
            buf = ByteBuffer.wrap(data.getBytes("UTF-8"));
        } 
        catch (UnsupportedEncodingException e) {
            // this should not be reached
            String s = "The platform should have already been checked to see if UTF-8 encoding is supported";
            throw new IllegalStateException(s);
        }

        ByteBuffer frame = WsFrameEncodingSupport.rfc6455Encode(new WsMessage(buf, Kind.TEXT), new Random().nextInt());
        send(frame);
        // send(data);
    }
    
    private void send(ByteBuffer frame) {
        LOG.entering(CLASS_NAME, "send", frame);
        if (socket == null) {
            handleError(new IllegalStateException("Socket is null"));
        }

        try {
            // consolidate the frame complete here and then flush
            OutputStream outputStream = socket.getOutputStream();
            int offset = frame.position();
            int len = frame.remaining();
            outputStream.write(frame.array(), offset, len);
            outputStream.flush();
        }
        catch (Exception e) {
            LOG.log(Level.FINE, "While sending: "+e.getMessage(), e);
            handleError(e);
        }
    }

    protected URI getUrl() {
        LOG.exiting(CLASS_NAME, "getUrl", url);
        return url;
    }

    protected URI getOrigin() {
        LOG.exiting(CLASS_NAME, "getOrigin", originUri);
        return originUri;
    }
    
    private void closeSocket() {
        try {
            LOG.log(Level.FINE, "Closing socket");
            
            // Sleep for a tenth-of-second before closing the socket.
            Thread.sleep(100);
            
            if ((socket != null) && (readyState != ReadyState.CLOSED)) {
                socket.close();
            }
        }
        catch (IOException e) {
            LOG.log(Level.FINE, "While closing socket: "+e.getMessage(), e);
        } 
        catch (InterruptedException e) {
            LOG.log(Level.FINE, "While closing socket: "+e.getMessage(), e);
        }
        finally {
            WebSocketDelegateImpl.this.readyState = ReadyState.CLOSED;
            socket = null;
        }
    }
    
    private void handleClose(Exception ex) {
        if (closed.compareAndSet(false, true)) {
            try {
            	stopIdleTimer();
                closeSocket();
            }
            finally {
                if (ex == null) {
                    listener.closed(new CloseEvent(this.code, this.wasClean, this.reason));
                }
                else {
                    listener.closed(new CloseEvent(ex));
                }
            }
        }
    }

    private void handleError(Exception ex) {
        if (closed.compareAndSet(false, true)) {
            try {
                closeSocket();
            }
            finally {
                listener.errorOccurred(new ErrorEvent(ex));
            }
        }
    }
    
    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        Random r = new Random();
        r.nextBytes(bytes);
        return bytes;
    }
    
    private String base64Encode(byte[] bytes) {
        return Base64Util.encode(ByteBuffer.wrap(bytes));

    }

    @Override
    public void setListener(WebSocketDelegateListener listener) {
        this.listener = listener;
    }


    class SocketReader implements Runnable {
        private final String CLASS_NAME = SocketReader.class.getName();

        private static final String HTTP_101_MESSAGE = "HTTP/1.1 101 Web Socket Protocol Handshake";
        private static final String UPGRADE_HEADER = "Upgrade: ";
        private static final int UPGRADE_HEADER_LENGTH = 9; // "Upgrade: ".length();
        private static final String UPGRADE_VALUE = "websocket";
        private static final String CONNECTION_MESSAGE = "Connection: Upgrade";
        private static final String WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
        private static final String WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";
        private static final String WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

        ConnectionStatus state = ConnectionStatus.START;
        Boolean upgradeReceived = false;
        Boolean connectionReceived = false;
        Boolean websocketAcceptReceived = false;

        InputStream inputStream = null;

        public SocketReader(InputStream inputStream) throws IOException {
            LOG.entering(CLASS_NAME, "<init>");
            this.inputStream = inputStream;
        }

        public void run() {
            LOG.entering(CLASS_NAME, "run");
            // TODO: to check for the first 85 bytes of the response instead.
            try {
               while (!stopReaderThread && !connectionUpgraded) {
                    if (state == ConnectionStatus.ERRORED) {
                        throw new IllegalArgumentException("WebSocket Connection upgrade unsuccessful");
                    }
                    String line = readLine(inputStream);

                    line = line.trim();
                    //get WebSocket-Protocol: header
                    if (line.startsWith(WEBSOCKET_EXTENSIONS)) {
                        extensions = line.substring(WEBSOCKET_EXTENSIONS.length() + 1).trim();
                        continue;
                    }
                    if (line.startsWith(WEBSOCKET_PROTOCOL)) {
                        secProtocol = line.substring(WEBSOCKET_PROTOCOL.length() + 1).trim();
                        continue;
                    }

                    if (state != ConnectionStatus.COMPLETED) {
                        processLine(line);
                    }
                    if (state == ConnectionStatus.COMPLETED) {
                        //all headers processed, check all required headers for WebSocket handshake
                        connectionUpgraded = websocketAcceptReceived && upgradeReceived && connectionReceived;
                        if (connectionUpgraded) {
                            // Completely read the WebSocket upgraded response. Now
                            // start doing the WebSocket protocol
                            readyState = ReadyState.OPEN;
                            listener.opened(new OpenEvent(secProtocol));
                            lastMessageTimestamp.set(System.currentTimeMillis());
                        }
                        else {
                            throw new IllegalArgumentException("WebSocket Connection upgrade unsuccessful");
                        }
                        break;
                    }
                } //end of while loop
               
                if (!connectionUpgraded && !stopReaderThread) {
                    throw new IllegalArgumentException("WebSocket Connection upgrade unsuccessful");
                }

                FrameProcessor frameProcessor = new FrameProcessor(new FrameProcessorListener() {
                    @Override
                    public void messageReceived(ByteBuffer buffer, String messageType) {
                        // update timestamp that is used to record the timestamp of last received message
                        lastMessageTimestamp.set(System.currentTimeMillis());
                        if (messageType == "TEXT" || messageType == "BINARY") {
                            // fire message event if readyState == OPEN
                            if (WebSocketDelegateImpl.this.readyState == ReadyState.OPEN) {
                                listener.messageReceived(new MessageEvent(buffer, null, null, messageType));
                            }
                        }
                        else if (messageType == "PING") {
                            //PING received, send PONG
                            ByteBuffer frame = WsFrameEncodingSupport.rfc6455Encode(new WsMessage(buffer, Kind.PONG), new Random().nextInt());
                            WebSocketDelegateImpl.this.send(frame);
                            
                        }
                        else if (messageType == "CLOSE") {
                            WebSocketDelegateImpl.this.wasClean = true;
                            if (buffer.remaining() < 2) {
                                WebSocketDelegateImpl.this.code = CLOSE_NO_STATUS; //no status code was actually present
                            }
                            else {
                                WebSocketDelegateImpl.this.code = buffer.getShort();
                            
                                if (buffer.hasRemaining()) {
                                    WebSocketDelegateImpl.this.reason = UTF8.decode(buffer).toString();
                                }
                            }
                            if (WebSocketDelegateImpl.this.readyState == ReadyState.OPEN) {
                                //close frame received, echo close frame
                                WebSocketDelegateImpl.this.readyState = ReadyState.CLOSING;
                                buffer.flip();
                                WsMessage message = new WsMessage(buffer, Kind.CLOSE);
                                ByteBuffer frame = WsFrameEncodingSupport.rfc6455Encode(message, new Random().nextInt());
                                WebSocketDelegateImpl.this.send(frame);
                            }
                            if (WebSocketDelegateImpl.this.readyState == ReadyState.CONNECTING) {
                                WebSocketDelegateImpl.this.readyState = ReadyState.CLOSING;
                            }
                            
                        }
                        else {
                            //unknown type
                            throw new IllegalArgumentException("Unknown message type: " + messageType);
                        }
                    }
                });

                Exception exception = null;

                try {
                    for (;;) {
                        if (stopReaderThread) {
                            LOG.fine("SocketReader: Stopping reader thread; closing socket");
                            break;
                        }
    
                        if (!frameProcessor.process(inputStream)) {
                            LOG.fine("SocketReader: end of stream");
                            break;
                        }
                    }
                }
                catch (Exception ex) {
                    // ex.printStackTrace();
                    exception = ex;
                }
                finally {
                    handleClose(exception);
                }
            }
            catch (Exception e) {
                LOG.log(Level.FINE, "SocketReader: " + e.getMessage(), e);
                listener.errorOccurred(new ErrorEvent(e));
            }
        }

        private void handleClose(Exception ex) {
            WebSocketDelegateImpl.this.handleClose(ex);
        }

        private String readLine(InputStream reader) throws Exception {
            ByteBuffer input = ByteBuffer.allocate(512);
            int ch;
            while ((ch = reader.read()) != -1) {
                if (!IoBufferUtil.canAccomodate(input, 1)) {
                    // expand in 512 bytes chunks
                    input = IoBufferUtil.expandBuffer(input, 512);
                }
                if (ch == '\n') {
                    input.put((byte)0x00);
                    input.flip();
                    return new String(input.array());
                }
                input.put((byte)ch);
            }
            return "";
        }

        private void processLine(String line) throws Exception {
            LOG.entering(CLASS_NAME, "processLine", line);
            
            switch (state) {
            case START:
                if (line.equals(HTTP_101_MESSAGE)) {
                    state = ConnectionStatus.STATUS_101_READ;
                }
                else {
                    String s = "WebSocket upgrade failed: " + line;
                    LOG.severe(s);
                    state = ConnectionStatus.ERRORED;
                    listener.errorOccurred(new ErrorEvent(new IllegalStateException(s)));
                }
                break;
            case STATUS_101_READ:
                if (line == null || (line.length() == 0)) {
                    //end of header, set to Completed
                    state = ConnectionStatus.COMPLETED;
                }
                else if (line.indexOf(UPGRADE_HEADER) == 0) {
                    upgradeReceived = UPGRADE_VALUE.equalsIgnoreCase(line.substring(UPGRADE_HEADER_LENGTH));
                }
                else if (line.equals(CONNECTION_MESSAGE)) {
                    connectionReceived = true;
                }
                else if (line.indexOf(WEBSOCKET_ACCEPT) == 0) {
                    String hashedKey = AcceptHash(websocketKey);
                    websocketAcceptReceived = hashedKey.equals(line.substring(WEBSOCKET_ACCEPT.length() + 1).trim());
                }
                break;
            case COMPLETED:
                break;
            }
        }
        
        /**
         * Compute the Sec-WebSocket-Accept key (RFC-6455)
         * 
         * @param key
         * @return
         * @throws NoSuchAlgorithmException 
         * @throws Exception
         */
        public String AcceptHash(String key) throws NoSuchAlgorithmException {
            String input = key + WEBSOCKET_GUID;
            
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

            byte[] hash = sha1.digest(input.getBytes());;
            return Base64Util.encode(ByteBuffer.wrap(hash));
        }
    }
}

