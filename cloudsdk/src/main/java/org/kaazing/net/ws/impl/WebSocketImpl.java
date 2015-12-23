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

package org.kaazing.net.ws.impl;

import static java.util.Collections.unmodifiableCollection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.WebSocketHandlerListener;
import org.kaazing.gateway.client.impl.util.WSCompositeURI;
import org.kaazing.gateway.client.impl.util.WSURI;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeHandler;
import org.kaazing.gateway.client.impl.ws.WebSocketSelectedChannel;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.http.HttpRedirectPolicy;
import org.kaazing.net.impl.util.BlockingQueueImpl;
import org.kaazing.net.impl.util.ResumableTimer;
import org.kaazing.net.ws.WebSocket;
import org.kaazing.net.ws.WebSocketException;
import org.kaazing.net.ws.WebSocketExtension;
import org.kaazing.net.ws.WebSocketExtension.Parameter;
import org.kaazing.net.ws.WebSocketExtension.Parameter.Metadata;
import org.kaazing.net.ws.WebSocketMessageReader;
import org.kaazing.net.ws.WebSocketMessageWriter;
import org.kaazing.net.ws.impl.io.WsInputStreamImpl;
import org.kaazing.net.ws.impl.io.WsMessageReaderAdapter;
import org.kaazing.net.ws.impl.io.WsMessageReaderImpl;
import org.kaazing.net.ws.impl.io.WsMessageWriterImpl;
import org.kaazing.net.ws.impl.io.WsOutputStreamImpl;
import org.kaazing.net.ws.impl.io.WsReaderImpl;
import org.kaazing.net.ws.impl.io.WsWriterImpl;
import org.kaazing.net.ws.impl.spi.WebSocketExtensionFactorySpi;
import org.kaazing.net.ws.impl.spi.WebSocketExtensionHandlerSpi;
import org.kaazing.net.ws.impl.spi.WebSocketExtensionParameterValuesSpi;
import org.kaazing.net.ws.impl.spi.WebSocketExtensionSpi;

public class WebSocketImpl extends WebSocket {
    private static final String _CLASS_NAME = WebSocketImpl.class.getName();
    private static final Logger _LOG = Logger.getLogger(_CLASS_NAME);
    
    // These member variables are final as they will not change once they are 
    // created/set.
    private final Map<String, WsExtensionParameterValuesSpiImpl> _enabledParameters;
    private final Map<String, WsExtensionParameterValuesSpiImpl> _negotiatedParameters;
    private final Map<String, WebSocketExtensionFactorySpi>      _extensionFactories;
    private final WSURI                                          _location;
    private final WebSocketCompositeHandler                      _handler;
    private final WebSocketCompositeChannel                      _channel;

    private Collection<String>           _enabledExtensions;
    private Collection<String>           _negotiatedExtensions;
    private Collection<String>           _supportedExtensions;
    private Collection<String>           _enabledProtocols;
    private String                       _negotiatedProtocol;
    private WsInputStreamImpl            _inputStream;
    private WsOutputStreamImpl           _outputStream;
    private WsReaderImpl                 _reader;
    private WsWriterImpl                 _writer;
    private WsMessageReaderImpl          _messageReader;
    private WsMessageWriterImpl          _messageWriter;
    private BlockingQueueImpl<Object>    _sharedQueue;
    private HttpRedirectPolicy           _followRedirect;
    private ChallengeHandler             _challengeHandler;
    private int                          _connectTimeout = 0;

    private ReadyState                   _readyState;
    private Exception                    _exception;

    /**
     * Values are CONNECTING = 0, OPEN = 1, CLOSING = 2, and CLOSED = 3;
     */
    enum ReadyState {
        CONNECTING, OPEN, CLOSING, CLOSED;
    }

    /**
     * Creates a WebSocket that opens up a full-duplex connection to the target 
     * location on a supported WebSocket provider. Call connect() to establish 
     * the location after adding event listeners.
     * 
     * @param location        URI of the WebSocket service for the connection
     * @throws Exception      if connection could not be established
     */
    public WebSocketImpl(URI                                       location, 
                         Map<String, WebSocketExtensionFactorySpi> extensionFactories)
           throws URISyntaxException {
        this(location, 
             extensionFactories,
             HttpRedirectPolicy.ALWAYS,
             null, 
             null,
             new HashMap<String, WsExtensionParameterValuesSpiImpl>(),
             null,
             0);
    }

    public WebSocketImpl(URI                                            location, 
                         Map<String, WebSocketExtensionFactorySpi>      extensionFactories,
                         HttpRedirectPolicy                             followRedirect,
                         Collection<String>                             enabledExtensions,
                         Collection<String>                             enabledProtocols,
                         Map<String, WsExtensionParameterValuesSpiImpl> enabledParameters,
                         ChallengeHandler                               challengeHandler,
                         int                                            connectTimeout) 
           throws URISyntaxException {
        WSCompositeURI compUri = new WSCompositeURI(location);
        
        _readyState = ReadyState.CLOSED;
        _location = compUri.getWSEquivalent();
        
        _followRedirect = followRedirect;
        _enabledParameters = enabledParameters;
        _negotiatedParameters = new HashMap<String, WsExtensionParameterValuesSpiImpl>();
        _extensionFactories = extensionFactories;
        _challengeHandler = challengeHandler;
        _connectTimeout = connectTimeout;
        
        // Set up the WebCompositeHandler with the listener. Methods on the
        // listener will be invoked from the pipeline. This will allow us to 
        // manage the lifecycle of the WebSocket.
        _handler = WebSocketCompositeHandler.COMPOSITE_HANDLER;
        _handler.setListener(handlerListener);

        // Setup the channel that will represent this instance of the WebSocket.
        _channel = new WebSocketCompositeChannel(compUri);
        _channel.setWebSocket(this);

        if ((_extensionFactories != null) && (_extensionFactories.size() > 0))
        {
            _supportedExtensions = new HashSet<String>();
            _supportedExtensions.addAll(_extensionFactories.keySet());
        }
        
        setEnabledExtensions(enabledExtensions);
        setEnabledProtocols(enabledProtocols);
    }


    @Override
    public synchronized void close() throws IOException {
        close(0, null);
    }

    @Override
    public synchronized void close(int code) throws IOException {
        close(code, null);
    }
    
    @Override
    public synchronized void close(int code, String reason) throws IOException {
        String args = String.format("code = '%d',  reason = '%s'", code, reason);
        _LOG.entering(_CLASS_NAME, "close", args);
        
        if (code != 0) {
            //verify code and reason agaist RFC 6455
            //if code is present, it must equal to 1000 or in range 3000 to 4999
            if (code != 1000 && (code < 3000 || code > 4999)) {
                    throw new IllegalArgumentException("code must equal to 1000 or in range 3000 to 4999");
            }
    
            //if reason is present, it must not be longer than 123 bytes
            if (reason != null && reason.length() > 0) {
                //convert reason to UTF8 string
                try {
                    byte[] reasonBytes = reason.getBytes("UTF8");
                    if (reasonBytes.length > 123) {
                        throw new IllegalArgumentException("Reason is longer than 123 bytes");
                    }
                    reason = new String(reasonBytes, "UTF8");
                    
                } catch (UnsupportedEncodingException e) {
                    _LOG.log(Level.FINEST, e.getMessage(), e);
                    throw new IllegalArgumentException("Reason must be encodable to UTF8");
                }
            }
        }

        if ((_readyState == ReadyState.CLOSED) || (_readyState == ReadyState.CLOSING)) {
            // Since the WebSocket is already closed/closing, we just bail.
            _LOG.log(Level.FINE, "WebSocket is closed or closing");
            return;
        }

        if (_readyState == ReadyState.CONNECTING) {
            _LOG.log(Level.FINE, "WebSocket is connecting");
            _readyState = ReadyState.CLOSED;
            cleanupAfterClose();
            
            // If the close() is called and connection is still being established
            // with WebSocket.connect(), inform the application that the connection
            // failed.
            setException(new WebSocketException("Connection Failed"));
            notifyAll();
            return;
        }

        setException(null);
        _readyState = ReadyState.CLOSING;
        _handler.processClose(_channel, code, reason);
        
        // Block till the WebSocket is closed completely.
        // Sometimes the thread can have a spurious wakeup without getting 
        // notified, interrupted, or timing out. So, we should guard with
        // the WHILE loop.
        while ((_readyState != ReadyState.CLOSED) && (_exception == null)) {
            try {
                wait();
                if (getException() != null) {
                    break;
                }
            } 
            catch (InterruptedException e) {
                throw new WebSocketException(e);
            }
        }
        
        // Check if there is any exception that needs to be reported.
        Exception exception = getException();
        if (exception != null) {
            throw new WebSocketException(exception);
        }

        // At this point, the cleanup after close has already been performed.
    }

    @Override
    public void connect() throws IOException {
        _LOG.entering(_CLASS_NAME, "connect");

        ResumableTimer connectTimer = null;
        String[]       enabledProtocols = null;

        synchronized (this) {
            if (_readyState == ReadyState.OPEN) {
                return;
            }
            else if (_readyState == ReadyState.CONNECTING){
                String s = "WebSocket connection is in progress";
                throw new IllegalStateException(s);
            }
            else if (_readyState == ReadyState.CLOSING) {
                String s = "WebSocket is not in a state to connect at this time";
                throw new IllegalStateException(s);
            }
    
            // Prepare for connecting.
            _readyState = ReadyState.CONNECTING;
            setException(null);
            
            int len = getEnabledProtocols().size();

            if (len > 0) {
                enabledProtocols = getEnabledProtocols().toArray(new String[len]);
            }
            // Used by the producer(i.e. the handlerListener) and the 
            // consumer(i.e. the WebSocketMessageReader).
            _sharedQueue = new BlockingQueueImpl<Object>();
    
            // ### TODO: This might be temporary till we install extensions' 
            //           handler directly in the pipeline.
            _channel.setChallengeHandler(_challengeHandler);
            
            // Setup the channel with the specified characteristics.
            String extensionsHeader = rfc3864FormattedString();
            _channel.setEnabledExtensions(extensionsHeader);
            _channel.setFollowRedirect(_followRedirect);
    
            // If _connectTimeout == 0, then it means there is no timeout.
            if (_connectTimeout > 0) {
                // Create connect timer that is scheduled to run after _connectTimeout
                // milliseconds once it is started. If the connection is not created
                // before the timer expires, then an exception is thrown.
                connectTimer = new ResumableTimer(new Runnable() {
                    @Override
                    public void run() {
                        if (_readyState == ReadyState.CONNECTING) {
                            SocketTimeoutException ex = new SocketTimeoutException("Connection timeout");
                            // Inform the app by raising the CLOSE event.
                            _handler.doClose(_channel, ex);
                            
                            // Try closing the connection all the way down. This may
                            // block when there is a network loss. That's why we are 
                            // first informing the application about the connection 
                            // timeout.
                            _handler.processClose(_channel, 0, "Connection timeout");
                        }
                    }
                }, _connectTimeout, false);
                
                _channel.setConnectTimer(connectTimer);
    
                // Start the connect timer just before we connect to the end-point.
                connectTimer.start();
            }
        }

        // Connect to the end-point. Keep this out of the synchronized blocks
        // that are above and below to avoid deadlock(between connect-timer
        // thread and this thread) when there is a network loss.
        _handler.processConnect(_channel, _location, enabledProtocols);

        synchronized (this) {
            // Block till the WebSocket is opened.
            // Sometimes the thread can have a spurious wakeup without getting 
            // notified, interrupted, or timing out. So, we should guard with
            // the WHILE loop.
            while ((_readyState != ReadyState.OPEN) && (_exception == null)) {
                try {
                    wait();
    
                    if (getException() != null) {
                        break;
                    }
                }
                catch (InterruptedException e) {
                    throw new WebSocketException(e);
                }
            }
        }

        if (connectTimer != null) {
            // Cancel the timer and clear the timer in the channel.
            connectTimer.cancel();
            _channel.setConnectTimer(null);
        }

        // Check if there is any exception that needs to be reported.
        Exception exception = getException();
        if (exception != null) {
            String s = "Connection failed";
            throw new WebSocketException(s, exception);
        }

        // At this point, the _negotiatedProtocol and the _negotiatedExtensions
        // should be set.
        
        // ### TODO: If an enabled extension is successfully negotiated, then
        //           add the corresponding handler to the pipeline.
    }

    @Override
    public ChallengeHandler getChallengeHandler() {
        return _challengeHandler;
    }

    @Override
    public int getConnectTimeout() {
       return _connectTimeout;
    }

    @Override
    public Collection<String> getEnabledExtensions() {
        return (_enabledExtensions == null) ? Collections.<String>emptySet() :
                                              unmodifiableCollection(_enabledExtensions);
    }
    
    @Override
    public <T> T getEnabledParameter(Parameter<T> parameter) {
        String                            extName = parameter.extension().name();
        WsExtensionParameterValuesSpiImpl paramValues = _enabledParameters.get(extName);
        
        if (paramValues == null) {
            return null;
        }
        
        return paramValues.getParameterValue(parameter);
    }

    @Override
    public Collection<String> getEnabledProtocols() {
        return (_enabledProtocols == null) ? Collections.<String>emptySet() :
                                             unmodifiableCollection(_enabledProtocols);
    }

    @Override
    public HttpRedirectPolicy getRedirectPolicy() {
        return _followRedirect;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (_readyState != ReadyState.OPEN) {
            String s = "Cannot create InputStream as the WebSocket is not connected";
            throw new IOException(s);
        }
        
        synchronized (this) {
            if ((_inputStream != null) && !_inputStream.isClosed()) {
                return _inputStream;
            }

            WsMessageReaderAdapter adapter = null;
            adapter = new WsMessageReaderAdapter(getMessageReader());
            _inputStream = new WsInputStreamImpl(adapter);
        }
        
        return _inputStream;
    }

    @Override
    public WebSocketMessageReader getMessageReader() throws IOException {
        if (_readyState != ReadyState.OPEN) {
            String s = "Cannot create MessageReader as the WebSocket is not connected";
            throw new IOException(s);
        }

        synchronized (this) {
            if ((_messageReader != null) && !_messageReader.isClosed()) {
                return _messageReader;
            }
    
            if (_sharedQueue == null) {
                // Used by the producer(i.e. the handlerListener) and the 
                // consumer(i.e. the WebSocketMessageReader).
                _sharedQueue = new BlockingQueueImpl<Object>();
            }

            _messageReader = new WsMessageReaderImpl(this, _sharedQueue);
        }
        
        return _messageReader;
    }

    @Override
    public WebSocketMessageWriter getMessageWriter() throws IOException {
        if (_readyState != ReadyState.OPEN) {
            String s = "Cannot create MessageWriter as the WebSocket is not connected";
            throw new IOException(s);
        }

        synchronized (this) {
            if ((_messageWriter != null) && !_messageWriter.isClosed()) {
                return _messageWriter;
            }
            
            _messageWriter = new WsMessageWriterImpl(this);
        }
        return _messageWriter;
    }

    @Override
    public Collection<String> getNegotiatedExtensions() {
        if (_readyState != ReadyState.OPEN) {
            String s = "Extensions have not been negotiated as the webSocket " +
                       "is not yet connected";
            throw new IllegalStateException(s);
        }
        
        return (_negotiatedExtensions == null) ? Collections.<String>emptySet() :
                                                 unmodifiableCollection(_negotiatedExtensions);
    }

    @Override
    public <T> T getNegotiatedParameter(Parameter<T> parameter) {
        if (_readyState != ReadyState.OPEN) {
            String s = "Extensions have not been negotiated as the webSocket " +
                       "is not yet connected";
            throw new IllegalStateException(s);
        }

        String                            extName = parameter.extension().name();
        WsExtensionParameterValuesSpiImpl paramValues = _negotiatedParameters.get(extName);
        
        if (paramValues == null) {
            return null;
        }
        
        return paramValues.getParameterValue(parameter);
    }

    @Override
    public String getNegotiatedProtocol() {
        if (_readyState != ReadyState.OPEN) {
            String s = "Protocols have not been negotiated as the webSocket " +
                       "is not yet connected";
            throw new IllegalStateException(s);
        }

        return _negotiatedProtocol;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (_readyState != ReadyState.OPEN) {
            String s = "Cannot get the OutputStream as the WebSocket is not yet connected";
            throw new IOException(s);
        }
        
        synchronized (this)
        {
            if ((_outputStream != null) && !_outputStream.isClosed()) {
                return _outputStream;
            }

            _outputStream = new WsOutputStreamImpl(getMessageWriter());
        }

        return _outputStream;
    }

    @Override
    public Reader getReader() throws IOException {
        if (_readyState != ReadyState.OPEN) {
            String s = "Cannot create Reader as the WebSocket is not connected";
            throw new IOException(s);
        }
        
        synchronized (this) {
            if ((_reader != null) && !_reader.isClosed()) {
                return _reader;
            }

            WsMessageReaderAdapter adapter = null;
            adapter = new WsMessageReaderAdapter(getMessageReader());
            _reader = new WsReaderImpl(adapter);
        }
        
        return _reader;
    }

    @Override
    public Collection<String> getSupportedExtensions() {
        return (_supportedExtensions == null) ? Collections.<String>emptySet() :
                                                unmodifiableCollection(_supportedExtensions);
    }

    @Override
    public Writer getWriter() throws IOException {
        if (_readyState != ReadyState.OPEN) {
            String s = "Cannot create Writer as the WebSocket is not yet connected";
            throw new IOException(s);
        }
        
        synchronized (this)
        {
            if ((_writer != null) && !_writer.isClosed()) {
                return _writer;
            }
            
            _writer = new WsWriterImpl(getMessageWriter());
        }

        return _writer;
    }

    @Override
    public void setChallengeHandler(ChallengeHandler challengeHandler) {
        _challengeHandler = challengeHandler;
    }

    @Override
    public void setConnectTimeout(int connectTimeout) {
        if (_readyState != ReadyState.CLOSED) {
            String s = "Connection timeout can be set only when the WebSocket is closed";
            throw new IllegalStateException(s);
        }
        
        if (connectTimeout < 0) {
            throw new IllegalArgumentException("Connect timeout cannot be negative");
        }

        _connectTimeout = connectTimeout;
    }

    @Override
    public void setEnabledExtensions(Collection<String> extensions) {
        if (_readyState != ReadyState.CLOSED) {
            String s = "Extensions can be enabled only when the WebSocket is closed";
            throw new IllegalStateException(s);
        }
        
        if (extensions == null) {
            _enabledExtensions = extensions;
            return;
        }
        
        Collection<String> supportedExtns = getSupportedExtensions();
        for (String extension : extensions) {
            if (!supportedExtns.contains(extension)) {
                String s = String.format("'%s' is not a supported extension", extension);
                throw new IllegalStateException(s);
            }
            
            if (_enabledExtensions == null) {
                _enabledExtensions = new ArrayList<String>();
            }

            _enabledExtensions.add(extension);
        }        
    }

    @Override
    public <T> void setEnabledParameter(Parameter<T> parameter, T value) {
        if (_readyState != ReadyState.CLOSED) {
            String s = "Parameters can be set only when the WebSocket is closed";
            throw new IllegalStateException(s);
        }

        String extensionName = parameter.extension().name();
        
        WsExtensionParameterValuesSpiImpl parameterValues = _enabledParameters.get(extensionName);
        if (parameterValues == null) {
            parameterValues = new WsExtensionParameterValuesSpiImpl();
            _enabledParameters.put(extensionName, parameterValues);
        }
        
        parameterValues.setParameterValue(parameter, value);        
    }

    @Override
    public void setEnabledProtocols(Collection<String> protocols) {
        if (_readyState != ReadyState.CLOSED) {
            String s = "Protocols can be enabled only when the WebSocket is closed";
            throw new IllegalStateException(s);
        }
        
        if ((protocols == null) || protocols.isEmpty()) {
            _enabledProtocols = protocols;
            return;
        }
        
        _enabledProtocols = new ArrayList<String>();

        for (String protocol : protocols) {
            _enabledProtocols.add(protocol);
        }
    }

    @Override
    public void setRedirectPolicy(HttpRedirectPolicy option) {
        _followRedirect = option;
    }

    // ---------------------  Internal Implementation ------------------

    public WebSocketCompositeChannel getCompositeChannel() {
        return _channel;
    }

    public boolean isConnected() {
        return (_readyState == ReadyState.OPEN);
    }
    
    public boolean isDisconnected() {
        return (_readyState == ReadyState.CLOSED);
    }
    
    public Exception getException() {
        return _exception;
    }

    public void setException(Exception exception) {
        _exception = exception;
    }
    
    public synchronized void send(ByteBuffer buf) throws IOException {
        _LOG.entering(_CLASS_NAME, "send", buf);
        
        if (_readyState != ReadyState.OPEN) {
            String s = "Messages can be sent only when the WebSocket is connected";
            throw new WebSocketException(s);
        }

        _handler.processBinaryMessage(_channel, new WrappedByteBuffer(buf));
    }

    public synchronized void send(String message) throws IOException {
        _LOG.entering(_CLASS_NAME, "send", message);

        if (_readyState != ReadyState.OPEN) {
            String s = "Messages can be sent only when the WebSocket is connected";
            throw new WebSocketException(s);
        }

        _handler.processTextMessage(_channel, message);
    }
    
    // --------------------- Private Implementation --------------------------

    private synchronized void connectionOpened(String protocol, 
                                               String extensionsHeader) {
        // ### TODO: Currently, the Gateway is not sending the negotiated
        //           protocol.
        setNegotiatedProtocol(protocol);
        
        // Parse the negotiated extensions and parameters. This can result in 
        // _exception to be setup indicating that there is something wrong
        // while parsing the negotiated extensions and parameters.
        setNegotiatedExtensions(extensionsHeader);
        
        if ((getException() == null) && (_readyState == ReadyState.CONNECTING)) {
            _readyState = ReadyState.OPEN;
        }
        else {
            // The exception can be caused either while parsing the negotiated
            // extensions and parameters or the expiry of the connection timeout.
            // The parsing of negotiated extension can cause an exception if -- 
            // 1) a negotiated extension is not an enabled extension or
            // 2) the type of a negotiated parameter is not String.
            _readyState = ReadyState.CLOSED;
            
            // Inform the Gateway to close the WebSocket.
            _handler.processClose(_channel, 0, null);
        }

        // Unblock the connect() call so that it can proceed.
        notifyAll();
    }
    
    private synchronized void connectionClosed(boolean wasClean, 
                                               int     code, 
                                               String  reason) {
        if (_readyState == ReadyState.CLOSED) {
            return;
        }

        _readyState = ReadyState.CLOSED;
        
        if (!wasClean) {
            if (reason == null) {
                reason = "Connection Failed";
            }
            
            setException(new WebSocketException(code, reason));
        }
        
        cleanupAfterClose();

        // Unblock the close() call so that it can proceed.
        notifyAll();
    }
    
    private synchronized void connectionClosed(Exception ex) {
        if (_readyState == ReadyState.CLOSED) {
            return;
        }

        setException(ex);
        
        _readyState = ReadyState.CLOSED;

        cleanupAfterClose();

        // Unblock the close() call so that it can proceed.
        notifyAll();
    }
    
    private synchronized void connectionFailed(Exception ex) {
        if (_readyState == ReadyState.CLOSED) {
            return;
        }

        if (ex == null) {
            ex = new WebSocketException("Connection Failed");
        }

        setException(ex);
        
        _readyState = ReadyState.CLOSED;
        
        cleanupAfterClose();

        // Unblock threads so that they can proceed.
        notifyAll();        
    }

    private synchronized void cleanupAfterClose() {
        setNegotiatedExtensions(null);
        setNegotiatedProtocol(null);
        _negotiatedParameters.clear();

        // ### TODO: 
        // 1. WsExtensionHandlerSpis that were been added to the pipeline based
        //    on negotiated extensions for this connection should be removed.

        if (_messageReader != null) {
            // Notify the waiting consumers that the connection is closing.
            try {
                _messageReader.close();
            } 
            catch (IOException ex) {
                _LOG.log(Level.FINE, ex.getMessage(), ex);
            }
        }

        if (_sharedQueue != null) {
            _sharedQueue.done();
        }

        if (_inputStream != null) {
            try {
                _inputStream.close();
            } 
            catch (Exception ex) {
                _LOG.log(Level.FINE, ex.getMessage(), ex);
            }
        }

        if (_outputStream != null) {
            try {
                _outputStream.close();
            }
            catch (Exception ex) {
                _LOG.log(Level.FINE, ex.getMessage(), ex);
            }
        }

        if (_reader != null) {
            try {
                _reader.close();
            } 
            catch (Exception ex) {
                _LOG.log(Level.FINE, ex.getMessage(), ex);
            }
        }

        if (_writer != null) {
            try {
                _writer.close();
            } 
            catch (Exception ex) {
                _LOG.log(Level.FINE, ex.getMessage(), ex);
            }
        }

        _messageReader = null;
        _sharedQueue = null;
        _messageWriter = null;
        _inputStream = null;
        _outputStream = null;
        _reader = null;
        _writer = null;
    }

    private String formattedExtension(String                    extensionName, 
                           WebSocketExtensionParameterValuesSpi paramValues) {
        if (extensionName == null) {
            return "";
        }

        WebSocketExtension extension =
                       WebSocketExtension.getWebSocketExtension(extensionName);
        Collection<Parameter<?>> extnParameters = extension.getParameters();
        StringBuffer             buffer = new StringBuffer(extension.name());

        // We are using extnParameters to iterate as we want the ordered list
        // of parameters.
        for (Parameter<?> param : extnParameters) {
            if (param.required()) {
                // Required parameter is not enabled/set.
                String s = String.format("Extension '%s': Required parameter "
                        + "'%s' must be set", extension.name(), param.name());
                if ((paramValues == null) || 
                    (paramValues.getParameterValue(param) == null)) {
                    throw new IllegalStateException(s);
                }
            }
            
            if (paramValues == null) {
                // We should continue so that we can throw an exception if
                // any of the required parameters has not been set.                
                continue;
            }
            
            Object value = paramValues.getParameterValue(param);

            if (value == null) {
                // Non-required parameter has not been set. So, let's continue 
                // to the next one.
                continue;
            }

            if (param.temporal()) {
                // Temporal/transient parameters, even if they are required,
                // are not put on the wire.
                continue;
            }

            if (param.anonymous()) {
                // If parameter is anonymous, then only it's value is put
                // on the wire.
                buffer.append(";").append(value);
                continue;
            }

            // Otherwise, append the name=value pair.
            buffer.append(";").append(param.name()).append("=").append(value);
        }

        return buffer.toString();
    }

    private BlockingQueueImpl<Object> getSharedQueue() {
        return _sharedQueue;
    }
    
    private String rfc3864FormattedString() {
        // Iterate over enabled extensions. Using WebSocketExtensionFactorySpi  
        // for each extension, create a WebSocketExtensionSpi instance for each 
        // of the enabled extensions and pass WebSocketExtensionParameterValuesSpi
        // that should contain the values of the enabled parameters.
        StringBuffer extensionsHeader = new StringBuffer("");
        Map<String, WebSocketExtensionHandlerSpi> handlers = 
                           new HashMap<String, WebSocketExtensionHandlerSpi>();
        for (String extensionName : getEnabledExtensions()) {
            WebSocketExtensionFactorySpi extensionFactory = 
                                         _extensionFactories.get(extensionName);
            WebSocketExtensionParameterValuesSpi paramValues = 
                                         _enabledParameters.get(extensionName);
            
            // ### TODO: We are not setting up the extensions' handler in the 
            //           pipeline at this point.            
            WebSocketExtensionSpi extension = extensionFactory.createWsExtension(paramValues);
            WebSocketExtensionHandlerSpi extHandler = extension.createHandler();
            handlers.put(extensionName, extHandler);
            
            // Get the RFC-3864 formatted string representation of the
            // WebSocketExtension.
            String formatted = formattedExtension(extensionName, paramValues);
            
            if (formatted.length() > 0)  {
                if (extensionsHeader.length() > 0) {
                    // Add the ',' separator between strings representing
                    // different extensions.
                    extensionsHeader.append(",");
                }
                
                extensionsHeader.append(formatted);
            }
        }
        
        return extensionsHeader.toString();
    }

    // Comma separated list of negotiated extensions and parameters based on
    // RFC 3864 format.
    private void setNegotiatedExtensions(String extensionsHeader) {
        if ((extensionsHeader == null) || 
            (extensionsHeader.trim().length() == 0)) {
            _negotiatedExtensions = null;
            return;
        }
        
        String[]     extns = extensionsHeader.split(",");
        List<String> extnNames = new ArrayList<String>(); 
        
        for (String extn : extns) {
            String[]    properties = extn.split(";");
            String      extnName = properties[0].trim();

            if (!getEnabledExtensions().contains(extnName)) {
                String s = String.format("Extension '%s' is not an enabled " +
                        "extension so it should not have been negotiated", extnName);
                setException(new WebSocketException(s));
                return;
            }

            WebSocketExtension extension = 
                            WebSocketExtension.getWebSocketExtension(extnName);
            WsExtensionParameterValuesSpiImpl paramValues = 
                           _negotiatedParameters.get(extnName);
            Collection<Parameter<?>> anonymousParams = 
                           extension.getParameters(Metadata.ANONYMOUS);
            
            // Start from the second(0-based) property to parse the name-value
            // pairs as the first(or 0th) is the extension name.
            for (int i = 1; i < properties.length; i++) {
                String       property = properties[i].trim();
                String[]     pair = property.split("=");
                Parameter<?> parameter = null;
                String       paramValue = null;
                
                if (pair.length == 1) {
                    // We are dealing with an anonymous parameter. Since the
                    // Collection is actually an ArrayList, we are guaranteed to
                    // iterate the parameters in the definition/creation order.
                    // As there is no parameter name, we will just get the next
                    // anonymous Parameter instance and use it for setting the
                    // value. The onus is on the extension implementor to either
                    // use only named parameters or ensure that the anonymous
                    // parameters are defined in the order in which the server
                    // will send them back during negotiation.
                    parameter = anonymousParams.iterator().next();
                    paramValue = pair[0].trim();
                }
                else {
                    parameter = extension.getParameter(pair[0].trim());
                    paramValue = pair[1].trim();
                }
                
                if (parameter.type() != String.class) {
                    String paramName = parameter.name();
                    String s = String.format("Negotiated Extension '%s': " +
                                             "Type of parameter '%s' should be String", 
                                             extnName, paramName);
                    setException(new WebSocketException(s));
                    return;
                }

                if (paramValues == null) {
                    paramValues = new WsExtensionParameterValuesSpiImpl();
                    _negotiatedParameters.put(extnName, paramValues);
                }

                paramValues.setParameterValue(parameter, paramValue);

            }
            extnNames.add(extnName);
        }

        HashSet<String> extnsSet = new HashSet<String>(extnNames);
        _negotiatedExtensions = unmodifiableCollection(extnsSet);
    }
    
    private void setNegotiatedProtocol(String protocol) {
        _negotiatedProtocol = protocol;
    }

    private static final WebSocketHandlerListener handlerListener = new WebSocketHandlerListener() {
        
        @Override
        public void connectionOpened(WebSocketChannel channel, String protocol) {
            _LOG.entering(_CLASS_NAME, "connectionOpened");

            WebSocketCompositeChannel cc = (WebSocketCompositeChannel)channel;
            WebSocketImpl webSocket = (WebSocketImpl) cc.getWebSocket();
            WebSocketSelectedChannel selChan = ((WebSocketCompositeChannel)channel).selectedChannel; 

            synchronized (webSocket) {                
                // ### TODO: Currently, Gateway is not returning the negotiated
                //           protocol.
                // Try parsing the negotiated extensions in the 
                // connectionOpened() method. Only when everything looks good,
                // mark the connection as opened. If a negotiated extension is
                // not in the list of enabled extensions, then we will setup an
                // exception and close down. 
                webSocket.connectionOpened(protocol, 
                                           selChan.getNegotiatedExtensions());
            }
        }
        
        @Override
        public void binaryMessageReceived(WebSocketChannel channel, WrappedByteBuffer buf) {
            _LOG.entering(_CLASS_NAME, "binaryMessageReceived");

            WebSocketCompositeChannel cc = (WebSocketCompositeChannel)channel;
            WebSocketImpl webSocket = (WebSocketImpl) cc.getWebSocket();
            
            synchronized (webSocket) {
                BlockingQueueImpl<Object> sharedQueue = webSocket.getSharedQueue();
                if (sharedQueue != null) {
                    synchronized (sharedQueue) {
                        try {
                            ByteBuffer  payload = buf.getNioByteBuffer();
                            sharedQueue.put(payload);
                        } 
                        catch (InterruptedException ex) {
                            _LOG.log(Level.INFO, ex.getMessage(), ex);
                        }
                    }
                }
            }
        }

        @Override
        public void textMessageReceived(WebSocketChannel channel, String text) {
            _LOG.entering(_CLASS_NAME, "textMessageReceived", text);

            WebSocketCompositeChannel cc = (WebSocketCompositeChannel)channel;
            WebSocketImpl      webSocket = (WebSocketImpl) cc.getWebSocket();
            
            synchronized (webSocket) {
                BlockingQueueImpl<Object> sharedQueue = webSocket.getSharedQueue();
                if (sharedQueue != null) {
                    synchronized (sharedQueue) {
                        try {
                            sharedQueue.put(text);
                        } 
                        catch (InterruptedException ex) {
                            _LOG.log(Level.INFO, ex.getMessage(), ex);
                        }
                    }
                }
            }
        }

        @Override
        public void connectionClosed(WebSocketChannel channel, 
                                     boolean          wasClean, 
                                     int              code, 
                                     String           reason) {
            _LOG.entering(_CLASS_NAME, "connectionClosed");

            WebSocketCompositeChannel cc = (WebSocketCompositeChannel)channel;
            WebSocketImpl webSocket = (WebSocketImpl) cc.getWebSocket();
            
            // Since close() is a blocking call, if there is any thread
            // waiting then we should call webSocket.connectionClosed() to
            // unblock it.
            synchronized (webSocket) {
                webSocket.connectionClosed(wasClean, code, reason);
            }
        }

        @Override
        public void connectionClosed(WebSocketChannel channel, Exception ex) {
            _LOG.entering(_CLASS_NAME, "onError");
            
            WebSocketCompositeChannel cc = (WebSocketCompositeChannel)channel;
            WebSocketImpl webSocket = (WebSocketImpl) cc.getWebSocket();
            
            synchronized (webSocket) {
                webSocket.connectionClosed(ex);
            }
        }
        
        @Override
        public void connectionFailed(WebSocketChannel channel, Exception ex) {
            _LOG.entering(_CLASS_NAME, "onError");
            
            WebSocketCompositeChannel cc = (WebSocketCompositeChannel)channel;
            WebSocketImpl webSocket = (WebSocketImpl) cc.getWebSocket();
            
            synchronized (webSocket) {
                webSocket.connectionFailed(ex);
            }
        }

        @Override
        public void authenticationRequested(WebSocketChannel channel, 
                                            String           location, 
                                            String           challenge) {
            // Should never be fired from WebSocketCompositeHandler
        }

        @Override
        public void redirected(WebSocketChannel channel, String location) {
            // Should never be fired from WebSocketCompositeHandler
        }

        @Override
        public void commandMessageReceived(WebSocketChannel channel, 
                                           CommandMessage   message) {
            // ignore
        }
    };
}
