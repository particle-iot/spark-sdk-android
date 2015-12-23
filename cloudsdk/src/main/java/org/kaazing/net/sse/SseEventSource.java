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

package org.kaazing.net.sse;

import java.io.IOException;

import org.kaazing.net.http.HttpRedirectPolicy;

/**
 * SseEventSource provides an implementation of HTML5 Server-sent Events. 
 * Refer to HTML5 EventSource at 
 * {@link http://www.whatwg.org/specs/web-apps/current-work/#server-sent-events} 
 * {@link http://www.whatwg.org/specs/web-apps/current-work/#the-event-source}
 */
public abstract class SseEventSource {
    /**
     * Disconnects with the server. This is a blocking call that returns only 
     * when the shutdown is complete.
     * 
     * @throws IOException    if the disconnect did not succeed
     */
    public abstract void close() throws IOException;

    /**
     * Connects with the server using an end-point. This is a blocking call. The 
     * thread invoking this method will be blocked till a successful connection
     * is established. If the connection cannot be established, then an 
     * IOException is thrown and the thread is unblocked. 
     * 
     * @throws IOException    if the connection cannot be established
     */
    public abstract void connect() throws IOException;

    /**
     * Returns a {@link SseEventReader} that can be used to receive 
     * events based on the {@link SseEventType}. 
     * <p>
     * If this method is invoked before a connection is established successfully,
     * then an IOException is thrown. 
     * 
     * @return SseEventReader   to receive events
     * @throws IOException      if invoked before the connection is opened
     */
    public abstract SseEventReader getEventReader() throws IOException;
    
    /**
     * Returns {@link HttpRedirectPolicy} indicating the policy for 
     * following  HTTP redirects (3xx). The default option is 
     * {@link HttpRedirectPolicy#NONE}.
     * 
     * @return HttpRedirectOption     indicating the 
     */
    public abstract HttpRedirectPolicy getFollowRedirect();

    /**
     * Returns the retry timeout in milliseconds. The default is 3000ms.
     * 
     * @return retry timeout in milliseconds
     */
    public abstract long getRetryTimeout();    

    /**
     * Sets {@link HttpRedirectPolicy} indicating the policy for 
     * following  HTTP redirects (3xx).
     * 
     * @param option     HttpRedirectOption to used for following the
     *                   redirects 
     */
    public abstract void setFollowRedirect(HttpRedirectPolicy option);

    /**
     * Sets the retry timeout specified in milliseconds.
     * 
     * @param millis    retry timeout
     */
    public abstract void setRetryTimeout(long millis);
}
