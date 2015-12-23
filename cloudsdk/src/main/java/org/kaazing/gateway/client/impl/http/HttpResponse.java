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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class HttpResponse {

    private int statusCode = 0;
    private String message;
    private Map<String, String> headers = new HashMap<String, String>();
    private WrappedByteBuffer responseBuffer;
    
    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String setHeader(String header, String value) {
        return headers.put(header, value);
    }
    
    public String getHeader(String header) {
        return headers.get(header);
    }
    
    public String getAllHeaders() {
        StringBuffer buf = new StringBuffer();
        for (Entry<String, String> entry : headers.entrySet()) {
            buf.append(entry.getKey() + ":" + entry.getValue() + "\n");
        }
        return buf.toString();
    }

    public WrappedByteBuffer getBody() {
        return responseBuffer.duplicate();
    }

    public void setBody(WrappedByteBuffer responseBuffer) {
        this.responseBuffer = responseBuffer;
    }
    
    public String toString() {
        String headers = getAllHeaders();
        if (headers != null) {
            headers = "\n" + headers;
        }
        return "[RESPONSE "+getStatusCode()+" "+getMessage()+headers+"]";
    }
}
