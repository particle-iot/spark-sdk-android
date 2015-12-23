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

package org.kaazing.net.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.http.HttpRedirectPolicy;
import org.kaazing.net.ws.WebSocketExtension.Parameter;

/**
 * A URLConnection with support for WebSocket-specific features. See
 * {@link http://www.w3.org/TR/websockets/} for details.
 * <p>
 * Each WsURLConnection provides bi-directional communications for text and 
 * binary messaging via the Kaazing Gateway.
 * <p>
 * An instance of {@link WsURLConnection} is created as shown below:
 * <pre>
 * {@code 
 *     URL location = URLFactory.create("ws://<hostname>:<port>/<serviceName>");
 *     URLConnection connection = location.openConnection();
 *     WsURLConnection wsConnection = (WsURLConnection)connection;
 * }
 * </pre>
 */
public abstract class WsURLConnection extends URLConnection {
    
    protected WsURLConnection(URL url) {
        super(url);
    }

    /**
     * Disconnects with the server. This is a blocking call that returns only 
     * when the shutdown is complete.
     * 
     * @throws IOException    if the disconnect did not succeed
     */
    public abstract void close() throws IOException;

    /**
     * Disconnects with the server with code. This is a blocking
     * call that returns only when the shutdown is complete.
     * 
     * @param code                         the error code for closing
     * @throws IOException                 if the disconnect did not succeed
     * @throws IllegalArgumentException    if the code isn't 1000 or out of 
     *                                     range 3000 - 4999.
     */
    public abstract void close(int code) throws IOException;
    
    /**
     * Disconnects with the server with code and reason. This is a blocking
     * call that returns only when the shutdown is complete.
     * 
     * @param code                         the error code for closing
     * @param reason                       the reason for closing
     * @throws IOException                 if the disconnect did not succeed
     * @throws IllegalArgumentException    if the code isn't 1000 or out of 
     *                                     range 3000 - 4999 OR if the reason
     *                                     is more than 123 bytes
     */
    public abstract void close(int code, String reason) 
           throws IOException;

    /**
     * Connects with the server using an end-point. This is a blocking call. The 
     * thread invoking this method will be blocked till a successful connection
     * is established. If the connection cannot be established, then an 
     * IOException is thrown and the thread is unblocked. 
     * 
     * @throws IOException    if the connection cannot be established
     */
    @Override 
    public abstract void connect() throws IOException;

    /**
     * Gets the {@link ChallengeHandler} that is used during authentication 
     * both at the connect-time as well as at subsequent revalidation-time that
     * occurs at regular intervals. 
     * 
     * @return ChallengeHandler
     */
    public abstract ChallengeHandler getChallengeHandler();
    
    /**
     * Gets the connect timeout in milliseconds. Default connect timeout is
     * zero milliseconds.
     * 
     * @return connect timeout value in milliseconds
     */
    @Override
    public abstract int getConnectTimeout();

    /**
     * Gets the names of all the extensions that have been enabled for this 
     * connection. The enabled extensions are negotiated between the client
     * and the server during the handshake. The names of the negotiated 
     * extensions can be obtained using {@link #getNegotiatedExtensions()} API. 
     * An empty Collection is returned if no extensions have been enabled for 
     * this connection. The enabled extensions will be a subset of the 
     * supported extensions.
     * 
     * @return Collection<String>     names of the enabled extensions for this 
     *                                connection
     */
    public abstract Collection<String> getEnabledExtensions();

    /**
     * Gets the value of the specified {@link Parameter} defined in an enabled 
     * extension. If the parameter is not defined for this connection but a 
     * default value for the parameter is set using the method
     * {@link WebSocketFactory#setDefaultParameter(Parameter, Object)},
     * then the default value is returned.
     * <p>
     * Setting the parameter value when the connection is successfully
     * established will result in an IllegalStateException.
     * </p>
     * @param <T>          Generic type of the value of the Parameter
     * @param parameter    Parameter whose value needs to be set
     * @return the value of the specified parameter
     * @throw IllegalStateException   if this method is invoked after connect()
     */
    public abstract <T> T getEnabledParameter(Parameter<T> parameter);

    /**
     * Gets the names of all the protocols that are enabled for this 
     * connection. Returns an empty Collection if protocols are not enabled.
     * 
     * @return Collection<String>     supported protocols by this connection
     */
    public abstract Collection<String> getEnabledProtocols();

    /**
     * Returns {@link HttpRedirectPolicy} indicating the policy for 
     * following  HTTP redirects (3xx).
     * 
     * @return HttpRedirectOption     indicating the 
     */
    public abstract HttpRedirectPolicy getRedirectPolicy();

    /**
     * Returns the {@link InputStream} to receive <b>binary</b> messages. The 
     * methods on {@link InputStream} will block till the message arrives. The
     * {@link InputStream} must be used to only receive <b>binary</b> 
     * messages.
     * <p>
     * An IOException is thrown if this method is invoked when the connection 
     * has not been established. Receiving a text message using the 
     * {@link InputStream} will result in an IOException.
     * <p>
     * Once the connection is closed, a new {@link InputStream} should be
     * obtained using this method after the connection has been established.
     * Using the old InputStream will result in an IOException.
     * <p>
     * @return InputStream    to receive binary messages
     * @throws IOException    if the method is invoked before the connection is
     *                        successfully opened; if a text message is being
     *                        read using the InputStream
     */
    @Override
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Returns a {@link WebSocketMessageReader} that can be used to receive 
     * <b>binary</b> and <b>text</b> messages based on the 
     * {@link WebSocketMessageType}. 
     * <p>
     * If this method is invoked before a connection is established successfully,
     * then an IOException is thrown. 
     * <p>
     * Once the connection is closed, a new {@link WebSocketMessageReader} 
     * should be obtained using this method after the connection has been
     * established. Using the old WebSocketMessageReader will result in an
     * IOException.
     * <p>
     * @return WebSocketMessageReader   to receive binary and text messages
     * @throws IOException       if invoked before the connection is opened
     */
    public abstract WebSocketMessageReader getMessageReader() throws IOException;
    
    /**
     * Returns a {@link WebSocketMessageWriter} that can be used to send 
     * <b>binary</b> and <b>text</b> messages. 
     * <p>
     * If this method is invoked before a connection is established 
     * successfully, then an IOException is thrown. 
     * <p>
     * Once the connection is closed, a new {@link WebSocketMessageWriter} 
     * should be obtained using this method after the connection has been
     * established. Using the old WebSocketMessageWriter will result in an
     * IOException.
     * <p>
     * @return WebSocketMessageWriter   to send binary and text messages
     * @throws IOException       if invoked before the connection is opened
     */
    public abstract WebSocketMessageWriter getMessageWriter() throws IOException;

    /**
     * Gets names of all the enabled extensions that have been successfully 
     * negotiated between the client and the server during the initial
     * handshake. 
     * <p>
     * Returns an empty Collection if no extensions were negotiated between the
     * client and the server. The negotiated extensions will be a subset of the
     * enabled extensions.
     * <p>
     * If this method is invoked before a connection is successfully established,
     * an IllegalStateException is thrown.
     * 
     * @return Collection<String>      successfully negotiated using this 
     *                                 connection
     * @throws IllegalStateException   if invoked before the {@link #connect()}
     *                                 completes
     */
    public abstract Collection<String> getNegotiatedExtensions();

    /**
     * Returns the value of the specified {@link Parameter} of a negotiated 
     * extension.
     * <p>
     * If this method is invoked before the connection is successfully
     * established, an IllegalStateException is thrown.
     * 
     * @param <T>          parameter type
     * @param parameter    parameter of a negotiated extension
     * @return T           value of the specified parameter
     * @throws IllegalStateException   if invoked before the {@link #connect()}
     *                                 completes
     */
    public abstract <T> T getNegotiatedParameter(Parameter<T> parameter);

    /**
     * Gets the protocol that the client and the server have successfully 
     * negotiated. 
     * <p>
     * If this method is invoked before the connection is successfully 
     * established, an IllegalStateException is thrown.
     * <p>
     * @return protocol                negotiated by the client and the server
     * @throws IllegalStateException   if invoked before the {@link #connect()}
     *                                 completes
     */
    public abstract String getNegotiatedProtocol();
    
    /**
     * Returns the {@link OutputStream} to send <b>binary</b> messages. The 
     * message is put on the wire only when {@link OutputStream#flush()} is
     * invoked. 
     * <p>
     * If this method is invoked before {@link #connect()} is complete, an 
     * IOException is thrown.
     * <p>
     * Once the connection is closed, a new {@link OutputStream} should 
     * be obtained using this method after the connection has been
     * established. Using the old OutputStream will result in IOException.
     * <p>
     * @return OutputStream    to send binary messages
     * @throws IOException     if the method is invoked before the connection is
     *                         successfully opened
     */
    @Override
    public abstract OutputStream getOutputStream() throws IOException;

    /**
     * Returns a {@link Reader} to receive <b>text</b> messages from this 
     * connection. This method should be used to only to receive <b>text</b> 
     * messages. Methods on {@link Reader} will block till a message arrives.
     * <p>
     * If the Reader is used to receive <b>binary</b> messages, then an
     * IOException is thrown.
     * <p>
     * If this method is invoked before a connection is established 
     * successfully, then an IOException is thrown. 
     * <p> 
     * Once the connection is closed, a new {@link Reader} should be obtained 
     * using this method after the connection has been established. Using the 
     * old Reader will result in an IOException.
     * <p>
     * @return Reader         used to receive text messages from this connection
     * @throws IOException    if the method is invoked before the connection is
     *                        successfully opened
     */
    public abstract Reader getReader() throws IOException;

    /**
     * Returns the names of extensions that have been discovered for this
     * connection. An empty Collection is returned if no extensions were 
     * discovered for this connection.
     *
     * @return Collection<String>    extension names discovered for this 
     *                               connection
     */
    public abstract Collection<String> getSupportedExtensions();
    
    /**
     * Returns a {@link Writer} to send <b>text</b> messages from this 
     * connection. The message is put on the wire only when 
     * {@link Writer#flush()} is invoked.
     * <p>
     * An IOException is thrown if this method is invoked when the connection 
     * has not been established.
     * <p>
     * Once the connection is closed, a new {@link Writer} should be obtained 
     * using this method after the connection has been established. Using the 
     * old Writer will result in an IOException.
     * <p>
     * @return Writer          used to send text messages from this connection
     * @throws IOException     if the method is invoked before the connection is
     *                         successfully opened
     */
    public abstract Writer getWriter() throws IOException;

    /**
     * Sets the connect timeout in milliseconds. The timeout will expire if 
     * there is no exchange of packets(for example, 100% packet loss) while 
     * establishing the connection. A timeout value of zero indicates 
     * no timeout.
     * 
     * @param connectTimeout    timeout value in milliseconds
     * @throws IllegalStateException   if the connection timeout is being set
     *                                 after the connection has been established
     * @throws IllegalArgumentException   if connectTimeout is negative
     */
    @Override
    public abstract void setConnectTimeout(int connectTimeout);

    /**
     * Sets the {@link ChallengeHandler} that is used during authentication
     * both at the connect-time as well as at subsequent revalidation-time that
     * occurs at regular intervals. 
     * 
     * @param challengeHandler   ChallengeHandler used for authentication
     */
    public abstract void setChallengeHandler(ChallengeHandler challengeHandler);

    /**
     * Registers the names of all the extensions that must be negotiated between
     * the client and the server during the handshake. This method must be 
     * invoked before invoking the {@link #connect()} method. The 
     * enabled extensions should be a subset of the supported extensions. Only
     * the extensions that are explicitly enabled are put on the wire even
     * though there could be more supported extensions on this connection.
     * <p>
     * If this method is invoked after connection is successfully established,  
     * an IllegalStateException is thrown. If an enabled extension is not
     * discovered as a supported extension, then IllegalStateException is thrown.
     * <p>
     * @param extensions    list of extensions to be negotiated with the server  
     *                      during the handshake
     * @throw IllegalStateException   if this method is invoked after successful
     *                                connection or any of the specified
     *                                extensions is not a supported extension
     */
    public abstract void setEnabledExtensions(Collection<String> extensions);

    /**
     * Sets the value of the specified {@link Parameter} defined in an enabled 
     * extension. The application developer should set the extension parameters
     * of the enabled extensions before invoking the {@link #connect()} method.
     * <p>
     * Setting the parameter value when the connection is successfully
     * established will result in an IllegalStateException.
     * </p>
     * If the parameter has a default value that was specified using
     * {@link WebSocketFactory#setDefaultParameter(Parameter, Object)},
     * then setting the same parameter using this method will override the
     * default value.
     * <p>
     * @param <T>          extension parameter type
     * @param parameter    Parameter whose value needs to be set
     * @param value        of the specified parameter
     * @throw IllegalStateException   if this method is invoked after connect()
     */
    public abstract <T> void setEnabledParameter(Parameter<T> parameter, T value);

    /**
     * Registers the protocols to be negotiated with the server during the
     * handshake. This method must be invoked before {@link #connect()} is 
     * called.
     * <p>
     * If this method is invoked after a connection has been successfully 
     * established, an IllegalStateException is thrown.
     * <p>
     * @param extensions    list of extensions to be negotiated with the server  
     *                      during the handshake
     * @throw IllegalStateException   if this method is invoked after connect()
     */
    public abstract void setEnabledProtocols(Collection<String> protocols);
    
    /**
     * Sets {@link HttpRedirectPolicy} indicating the policy for 
     * following  HTTP redirects (3xx).
     * 
     * @param option     HttpRedirectOption to used for following the
     *                   redirects 
     */
    public abstract void setRedirectPolicy(HttpRedirectPolicy option);
}
