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

package org.kaazing.gateway.client.impl.http;

import org.kaazing.gateway.client.util.WrappedByteBuffer;

public interface HttpRequestListener {
    
    /** Invoked when the request is ready to send data upstream */
    void requestReady(HttpRequest request);

    /** Invoked when the response status code and headers are available */
    void requestOpened(HttpRequest request);

    /** Invoked when streamed data is received on the response */
    void requestProgressed(HttpRequest request, WrappedByteBuffer payload);

    /** Invoked when the response has completed and all data is available */
    void requestLoaded(HttpRequest request, HttpResponse response);

    /** Invoked when the request has been aborted */
    void requestAborted(HttpRequest request);

    /** Invoked when the request has closed and no longer valid */
    void requestClosed(HttpRequest request);

    /** Invoked when an error has occurred while processing the request or response */
    void errorOccurred(HttpRequest request, Exception exception);

}
