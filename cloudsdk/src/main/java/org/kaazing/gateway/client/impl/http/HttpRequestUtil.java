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

import java.util.logging.Logger;

public class HttpRequestUtil {
    private static final String CLASS_NAME = HttpRequestUtil.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private HttpRequestUtil() {
        LOG.entering(CLASS_NAME, "<init>");
    }

    public static void validateHeader(String header) {
        LOG.entering(CLASS_NAME, "validateHeader", header);
        /*
         * From the XMLHttpRequest spec: http://www.w3.org/TR/XMLHttpRequest/#setrequestheader
         * 
         * For security reasons, these steps should be terminated if the header argument case-insensitively matches one of the
         * following headers:
         * 
         * Accept-Charset Accept-Encoding Connection Content-Length Content-Transfer-Encoding Date Expect Host Keep-Alive Referer
         * TE Trailer Transfer-Encoding Upgrade Via Proxy-* Sec-*
         * 
         * Also for security reasons, these steps should be terminated if the start of the header argument case-insensitively
         * matches Proxy- or Se
         */
        if (header == null || (header.length() == 0)) {
            throw new IllegalArgumentException("Invalid header in the HTTP request");
        }
        String lowerCaseHeader = header.toLowerCase();
        if (lowerCaseHeader.startsWith("proxy-") || lowerCaseHeader.startsWith("sec-")) {
            throw new IllegalArgumentException("Headers starting with Proxy-* or Sec-* are prohibited");
        }
        for (String prohibited : INVALID_HEADERS) {
            if (header.equalsIgnoreCase(prohibited)) {
                throw new IllegalArgumentException("Headers starting with Proxy-* or Sec-* are prohibited");
            }
        }
    }

    private static final String[] INVALID_HEADERS = new String[] { "Accept-Charset", "Accept-Encoding", "Connection",
            "Content-Length", "Content-Transfer-Encoding", "Date", "Expect", "Host", "Keep-Alive", "Referer", "TE", "Trailer",
            "Transfer-Encoding", "Upgrade", "Via" };

}
