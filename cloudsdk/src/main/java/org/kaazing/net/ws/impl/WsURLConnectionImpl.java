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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.http.HttpRedirectPolicy;
import org.kaazing.net.ws.WebSocketExtension.Parameter;
import org.kaazing.net.ws.WebSocketMessageReader;
import org.kaazing.net.ws.WebSocketMessageWriter;
import org.kaazing.net.ws.WsURLConnection;
import org.kaazing.net.ws.impl.spi.WebSocketExtensionFactorySpi;

public class WsURLConnectionImpl extends WsURLConnection {
    private WebSocketImpl    _webSocket;


    public WsURLConnectionImpl(URL                                       location, 
                               Map<String, WebSocketExtensionFactorySpi> extensionFactories) {
        super(location);
        
        try {
            _webSocket = new WebSocketImpl(location.toURI(), extensionFactories);
        } 
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // -------------------------- WsURLConnection Methods -------------------
    @Override
    public void close() throws IOException {
        _webSocket.close();
    }

    @Override
    public void close(int code) throws IOException {
        _webSocket.close(code);
    }

    @Override
    public void close(int code, String reason) throws IOException {
        _webSocket.close(code, reason);
    }
    
    @Override
    public void connect() throws IOException {
        _webSocket.connect();
    }

    @Override
    public ChallengeHandler getChallengeHandler() {
        return _webSocket.getChallengeHandler();
    }

    @Override
    public int getConnectTimeout() {
        return _webSocket.getConnectTimeout();
    }

    @Override
    public Collection<String> getEnabledExtensions() {
        return _webSocket.getEnabledExtensions();
    }

    @Override
    public <T> T getEnabledParameter(Parameter<T> parameter) {
        return _webSocket.getEnabledParameter(parameter);
    }

    @Override
    public Collection<String> getEnabledProtocols() {
        return _webSocket.getEnabledExtensions();
    }

    @Override
    public HttpRedirectPolicy getRedirectPolicy() {
        return _webSocket.getRedirectPolicy();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return _webSocket.getInputStream();
    }

    @Override
    public WebSocketMessageReader getMessageReader() throws IOException {
        return _webSocket.getMessageReader();
    }

    @Override
    public WebSocketMessageWriter getMessageWriter() throws IOException {
        return _webSocket.getMessageWriter();
    }

    @Override
    public Collection<String> getNegotiatedExtensions() {
        return _webSocket.getNegotiatedExtensions();
    }

    @Override
    public <T> T getNegotiatedParameter(Parameter<T> parameter) {
        return _webSocket.getNegotiatedParameter(parameter);
    }

    @Override
    public String getNegotiatedProtocol() {
        return _webSocket.getNegotiatedProtocol();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return _webSocket.getOutputStream();
    }

    @Override
    public Reader getReader() throws IOException {
        return _webSocket.getReader();
    }

    @Override
    public Collection<String> getSupportedExtensions() {
        return _webSocket.getSupportedExtensions();
    }

    @Override
    public Writer getWriter() throws IOException {
        return _webSocket.getWriter();
    }

    @Override
    public void setChallengeHandler(ChallengeHandler challengeHandler) {
        _webSocket.setChallengeHandler(challengeHandler);
    }

    @Override
    public void setConnectTimeout(int timeout) {
        _webSocket.setConnectTimeout(timeout);
    }

    @Override
    public void setEnabledExtensions(Collection<String> extensions) {
        _webSocket.setEnabledExtensions(extensions);
    }

    @Override
    public void setEnabledProtocols(Collection<String> protocols) {
        _webSocket.setEnabledProtocols(protocols);
    }

    @Override
    public <T> void setEnabledParameter(Parameter<T> parameter, T value) {
        _webSocket.setEnabledParameter(parameter, value);
    }
    
    @Override
    public void setRedirectPolicy(HttpRedirectPolicy option) {
        _webSocket.setRedirectPolicy(option);
    }

    // --------------- Unsupported URLConnection Methods ----------------------
    @Override
    public void addRequestProperty(String key, String value) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public int getReadTimeout() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void setReadTimeout(int timeout) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    // @Override -- Not available in JDK 6.
    public long getContentLengthLong() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public int getContentLength() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public String getContentEncoding() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public long getExpiration() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public long getDate() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public long getLastModified() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public String getHeaderField(String name) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public int getHeaderFieldInt(String name, int Default) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    // @Override -- Not available in JDK 6.
    public long getHeaderFieldLong(String name, long Default) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public long getHeaderFieldDate(String name, long Default) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public String getHeaderFieldKey(int n) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public String getHeaderField(int n) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Object getContent() throws IOException {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getContent(Class[] classes) throws IOException {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Permission getPermission() throws IOException {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public boolean getDoInput() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void setDoInput(boolean doinput) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public boolean getDoOutput() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void setDoOutput(boolean dooutput) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public boolean getAllowUserInteraction() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void setAllowUserInteraction(boolean allowuserinteraction) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public boolean getUseCaches() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void setUseCaches(boolean usecaches) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public long getIfModifiedSince() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void setIfModifiedSince(long ifmodifiedsince) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public boolean getDefaultUseCaches() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void setDefaultUseCaches(boolean defaultusecaches) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public String getRequestProperty(String key) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void setRequestProperty(String key, String value) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }
}
