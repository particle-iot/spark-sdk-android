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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.kaazing.net.http.HttpRedirectPolicy;
import org.kaazing.net.sse.SseEventReader;

public class SseURLConnectionImpl extends SseURLConnection {
    private static final String _CLASS_NAME = SseURLConnectionImpl.class.getName();
    private static final Logger _LOG = Logger.getLogger(_CLASS_NAME);
    
    private SseEventSourceImpl     _eventSource;
    
    public SseURLConnectionImpl(URL url) {
        super(url);
        _eventSource = new SseEventSourceImpl(URI.create(url.toString()));
    }

    @Override
    public void close() throws IOException {
        _eventSource.close();
    }

    @Override
    public void connect() throws IOException {
        _LOG.entering(_CLASS_NAME, "connect");
        _eventSource.connect();
    }

    @Override
    public SseEventReader getEventReader() throws IOException {
        return _eventSource.getEventReader();
    }
    

    @Override
    public HttpRedirectPolicy getFollowRedirect() {
        return _eventSource.getFollowRedirect();
    }

    @Override
    public long getRetryTimeout() {
        return _eventSource.getRetryTimeout();
    }

    @Override
    public void setFollowRedirect(HttpRedirectPolicy option) {
        _eventSource.setFollowRedirect(option);
    }

    @Override
    public void setRetryTimeout(long millis) {
        _eventSource.setRetryTimeout(millis);
    }
    
    // --------------- Unsupported URLConnection Methods ----------------------
    @Override
    public void addRequestProperty(String key, String value) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public int getConnectTimeout() {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public void setConnectTimeout(int timeout) {
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

    @Override
    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("Unsupported Operation");
    }
}
