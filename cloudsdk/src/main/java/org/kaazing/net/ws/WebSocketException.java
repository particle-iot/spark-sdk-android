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
import java.util.HashMap;
import java.util.Map;

public class WebSocketException extends IOException {
    private static final long serialVersionUID = 1L;
    private static final Map<Integer, String> _STATUS_CODES;
    
    static {
        _STATUS_CODES = new HashMap<Integer, String>();
        _STATUS_CODES.put(1000, "Connection has been closed normally");
        _STATUS_CODES.put(1001, "End-point is going away");
        _STATUS_CODES.put(1002, "Connection terminated due to protocol error");
        _STATUS_CODES.put(1003, "Connection terminated due to incorrect message type");
        _STATUS_CODES.put(1004, "Reserved for future use");
        _STATUS_CODES.put(1005, "No status code was present");
        _STATUS_CODES.put(1006, "Connection was closed abnormally, e.g., without sending or receiving a Close control frame.");
        _STATUS_CODES.put(1007, "Connection terminated due to inconsistency between the data and the message type");
        _STATUS_CODES.put(1008, "Connection terminated as the received a message violates the policy");
        _STATUS_CODES.put(1009, "Connection terminated as the received message is too big to process");
        _STATUS_CODES.put(1010, "Connection terminated by the client because an extension could not be negotiated with the server during the handshake");
        _STATUS_CODES.put(1011, "Connection terminated by the server because of an unexpected condition");
        _STATUS_CODES.put(1015, "Connection was closed due to a failure to perform a TLS handshake");
    }

    private int     _code = 0;

    public WebSocketException(String reason) {
        super(reason);
    }
    
    public WebSocketException(Exception ex) {
        super(ex);
    }

    public WebSocketException(String reason, Exception ex) {
        super(reason, ex);
    }
    
    public WebSocketException(int code, String reason) {
        super((_STATUS_CODES.get(code) == null) ? reason : _STATUS_CODES.get(code));
        _code = code;
    }
    
    public WebSocketException(int code, String reason, Exception ex) {
        super((_STATUS_CODES.get(code) == null) ? reason : _STATUS_CODES.get(code),
              ex);
        _code = code;
    }
    
    public int getCode() {
        return _code;
    }
    
    public String getReason() {
        String s = _STATUS_CODES.get(_code);
        if (s == null) {
            return super.getMessage();
        }
        
        return s;
    }
}
