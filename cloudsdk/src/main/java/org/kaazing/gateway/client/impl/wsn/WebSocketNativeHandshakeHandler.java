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

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandler;
import org.kaazing.gateway.client.impl.WebSocketHandlerAdapter;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.impl.ws.ReadyState;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.impl.ws.WebSocketHandshakeObject;
import org.kaazing.gateway.client.impl.ws.WebSocketSelectedChannel;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

/*
 * WebSocket Native Handler Chain
 * NativeHandler - AuthenticationHandler - {HandshakeHandler} - ControlFrameHandler - BalanceingHandler - Nodec - BridgeHandler
 * Responsibilities:
 *         a). handle kaazing handshake
 *             if response protocol is "x-kaazing-handshake", start handshake process
 *             otherwise, fire connectionOpened event
 *      b). process 401
 *             if response is enveloped 401 challenge, fire a authenticationRequested event
 * TODO:
 *         a). add more hand shake objects in the future 
 */
public class WebSocketNativeHandshakeHandler extends WebSocketHandlerAdapter {

    private static final String CLASS_NAME = WebSocketNativeHandshakeHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    
    private static final byte[] GET_BYTES = "GET".getBytes();
    private static final byte[] HTTP_1_1_BYTES = "HTTP/1.1".getBytes();
    private static final byte[] COLON_BYTES = ":".getBytes();
    private static final byte[] SPACE_BYTES = " ".getBytes();
    private static final byte[] CRLF_BYTES = "\r\n".getBytes();
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String HEADER_PROTOCOL = "WebSocket-Protocol";
    private static final String HEADER_SEC_PROTOCOL = "Sec-WebSocket-Protocol";
    private static final String HEADER_SEC_EXTENSIONS = "Sec-WebSocket-Extensions";

    
    public WebSocketNativeHandshakeHandler() {
    }

    @Override
    public void processConnect(WebSocketChannel channel, WSURI uri, String[] protocols) {
        LOG.entering(CLASS_NAME, "connect", new Object[]{uri, protocols});
        // add kaazing protocol header
        String[] nextProtocols;
        if (protocols == null || protocols.length == 0) {
            nextProtocols = new String[] { WebSocketHandshakeObject.KAAZING_EXTENDED_HANDSHAKE };
        }
        else {
            nextProtocols = new String[protocols.length+1];
            nextProtocols[0] = WebSocketHandshakeObject.KAAZING_EXTENDED_HANDSHAKE;
            for (int i=0; i<protocols.length; i++) {
                nextProtocols[i+1] = protocols[i];
            }
        }
        nextHandler.processConnect(channel, uri, nextProtocols);
    }

    /**
     * Send authorize token to the Gateway
     */
    @Override
    public void processAuthorize(WebSocketChannel channel, String authorizeToken) {
        sendHandshakePayload(channel, authorizeToken);
    }

    @Override
    public void setNextHandler(final WebSocketHandler handler) {
        super.setNextHandler(handler);

        nextHandler.setListener(new WebSocketHandlerListener() {

            @Override
            public void connectionOpened(WebSocketChannel channel, String supportProtocol) {
                //check response for "x-kaazing-handshake protocol"
                if (WebSocketHandshakeObject.KAAZING_EXTENDED_HANDSHAKE.equals(supportProtocol)) {
                    sendHandshakePayload(channel, null);
                } else {
                    listener.connectionOpened(channel, channel.getProtocol());
                }
            }

            @Override
            public void redirected(WebSocketChannel channel, String location) {
                listener.redirected(channel, location);
            }

            @Override
            public void authenticationRequested(WebSocketChannel channel, String location, String challenge) {
                listener.authenticationRequested(channel, location, challenge);
            }

            @Override
            public void binaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer buf) {
                WebSocketSelectedChannel selectedChannel = (WebSocketSelectedChannel) channel;
                if (selectedChannel.getReadyState() == ReadyState.OPEN) {
                    listener.binaryMessageReceived(channel, buf);
                } else {
                    handleHandshakeMessage(channel, buf);
                }
            }

            @Override
            public void textMessageReceived(WebSocketChannel channel, String message) {
                listener.textMessageReceived(channel, message);

            }

            @Override
            public void commandMessageReceived(WebSocketChannel channel, CommandMessage message) {
                listener.commandMessageReceived(channel, message);
            }

            @Override
            public void connectionClosed(WebSocketChannel channel, boolean wasClean, int code, String reason) {
                listener.connectionClosed(channel, wasClean, code, reason);
            }

            @Override
            public void connectionClosed(WebSocketChannel channel, Exception ex) {
                listener.connectionClosed(channel, ex);
            }
            
            @Override
            public void connectionFailed(WebSocketChannel channel, Exception ex) {
                listener.connectionClosed(channel, false, 0, null);
            }
        });
    }

    protected static String[] getLines(String payload) {
        List<String> lineList = new ArrayList<String>();
        int i=0;
        
        while (i < payload.length()) {
            int endOfLine = payload.indexOf(13, i);
            String nextLine;
            if (endOfLine >= 0) {
                if (payload.charAt(endOfLine+1) != (char)10) {
                    throw new IllegalArgumentException("Invalid payload");
                }
            }
            else {
                endOfLine = payload.length();
            }
            
            nextLine = payload.substring(i, endOfLine);
            lineList.add(nextLine);
            i = endOfLine+2;
        }
        
        String[] lines = new String[lineList.size()];
        lineList.toArray(lines);
        return lines;
    }
    
    private void handleHandshakeMessage(WebSocketChannel channel, WrappedByteBuffer buf) {
        String s = buf.getString(Charset.forName("UTF-8"));
        handleHandshakeMessage(channel, s);
    }
    
    private void handleHandshakeMessage(WebSocketChannel channel, String message) {
        
        channel.handshakePayload.append(message);
        
        if (message.length() > 0) {
            // Continue reading until an empty message is received.
            // wait for more messages
            return;
        }
        
        String[] lines = getLines(channel.handshakePayload.toString());
        channel.handshakePayload.setLength(0);
        
        String httpCode = "";
        //parse the message for embedded http response, should read last one if there are more than one HTTP header
        for (int i = lines.length - 1; i >= 0; i--) {
            if (lines[i].startsWith("HTTP/1.1")) { //"HTTP/1.1 101 ..."
                String[] temp = lines[i].split(" ");
                httpCode = temp[1];
                break;
            }
        }

        if ("101".equals(httpCode)) {
            //handshake completed, websocket Open

            String extensionsHeader = "";
            String negotiatedextensions = "";
            for (String line :lines) {
                if (line != null && line.startsWith(HEADER_SEC_PROTOCOL)) {
                    String protocol = line.substring(HEADER_SEC_PROTOCOL.length() + 1).trim();
                    channel.setProtocol(protocol);
                }

                if (line != null && line.startsWith(HEADER_SEC_EXTENSIONS)) {
                    // Get Protocol and extensions  - note: extensions may contains multiple entries
                    // concatenate extensions to one line, separated by ","
                    extensionsHeader += (extensionsHeader == "" ? "" : ",") + line.substring(HEADER_SEC_EXTENSIONS.length() + 1).trim();
                }
            }
            
            // Parse extensions header
            if (extensionsHeader.length() > 0) {
                String[] extensions = extensionsHeader.split(",");
                for (String extension : extensions) {
                    String[] tmp = extension.split(";");
                    String extName = tmp[0].trim();
                    if (extName.equals(WebSocketHandshakeObject.KAAZING_SEC_EXTENSION_IDLETIMEOUT)) {
                        //idle timeout extension supported by server
                        try {
                             //x-kaazing-idle-timeout extension, parameter = "timeout=10000"
                            int timeout = Integer.parseInt(tmp[1].trim().substring(8));
                            if (timeout > 0) {
                                nextHandler.setIdleTimeout(channel, timeout);
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Cannot find timeout parameter in x-kaazing-idle-timeout extension: " + extension);
                        }
                        // x-kaazing-idle-timeout extension is internal extension, do not add to negotiated extensions
                        continue;
                    }
                    
                    negotiatedextensions += (negotiatedextensions == "" ? extension : ("," + extension));
                }
                if (negotiatedextensions.length() > 0) {
                    channel.setNegotiatedExtensions(negotiatedextensions);
                }
            }
        } 
        else if ("401".equals(httpCode)) {
            //receive HTTP/1.1 401 from server, pass event to Authentication handler
            String challenge = "";
            for (String line : lines) {
                if (line.startsWith(HEADER_WWW_AUTHENTICATE)) {
                    challenge = line.substring(HEADER_WWW_AUTHENTICATE.length()+ 1).trim();
                    break;
                }
            }
            listener.authenticationRequested(channel, channel.getLocation().toString(), challenge);
        } 
        else {
            // Error during handshake, close connect, report connectionFailed
            listener.connectionFailed(channel, 
                                      new IllegalStateException("Error during handshake. HTTP Status Code: " + httpCode));
        }
    }

    private void sendHandshakePayload(WebSocketChannel channel, String authToken) {
        
        String[] headerNames = new String[4];
        String[] headerValues = new String[4];
        headerNames[0] = HEADER_PROTOCOL;
        headerValues[0] = null;  //for now use Sec-Websockect-Protocol header instead
        headerNames[1] = HEADER_SEC_PROTOCOL;
        headerValues[1] = channel.getProtocol();  //now send the Websockect-Protocol
        
        //KG-9978 add x-kaazing-idle-timeout extension
        headerNames[2] = HEADER_SEC_EXTENSIONS;
        headerValues[2] = WebSocketHandshakeObject.KAAZING_SEC_EXTENSION_IDLETIMEOUT;
        
        String enabledExtensions = ((WebSocketCompositeChannel)channel.getParent()).getEnabledExtensions();
        if ((enabledExtensions != null) && (enabledExtensions.trim().length() != 0)) {
            headerValues[2] += "," + enabledExtensions;
        }
        
        headerNames[3] = HEADER_AUTHORIZATION;
        headerValues[3] = authToken;  //send authorization token

        byte[] payload = encodeGetRequest(channel.getLocation().getURI(), headerNames, headerValues);
        nextHandler.processBinaryMessage(channel, new WrappedByteBuffer(payload));
    }

    private byte[] encodeGetRequest(URI requestURI, String[] names, String[] values) {

        // Any changes to this method should result in the getEncodeRequestSize method below
        // to get accurate length of the buffer that needs to be allocated.

        LOG.entering(CLASS_NAME, "encodeGetRequest", new Object[]{requestURI, names, values});
        int requestSize = getEncodeRequestSize(requestURI, names, values);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(requestSize);

        // Encode Request line
        buf.put(GET_BYTES);
        buf.put(SPACE_BYTES);
        String path = requestURI.getPath(); // + "?.kl=Y&.kv=10.05";
        if (path.length() == 0) {
            path = "/";
        }
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
        String path = requestURI.getPath(); // + "?.kl=Y&.kv=10.05";
        if (path.length() == 0) {
            path = "/";
        }
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
}
