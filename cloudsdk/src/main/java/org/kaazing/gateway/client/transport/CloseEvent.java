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

package org.kaazing.gateway.client.transport;

public class CloseEvent extends Event {

    private int code;
    private String reason;
    private boolean wasClean;
    private Exception exception;
    
    public CloseEvent(int code, boolean wasClean, String reason) {
        super(Event.CLOSED);
        this.code = code;
        this.wasClean = wasClean;
        this.reason = reason;
    }
    
    public CloseEvent(Exception exception) {
        super(Event.CLOSED);
        this.exception = exception;
    }
    
    public int getCode() {
        return code;
    }

    public boolean wasClean() {
        return wasClean;
    }

    public String getReason() {
        return reason;
    }

    public Exception getException() {
        return exception;
    }
}
