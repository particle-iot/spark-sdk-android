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

package org.kaazing.gateway.client.util;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class StringUtils {
    private static final Map<CharSequence, CharSequence> BASIC_ESCAPE = new HashMap<CharSequence, CharSequence>();

    static {
        BASIC_ESCAPE.put("\"", "&quot;"); // " - double-quote
        BASIC_ESCAPE.put("&", "&amp;"); // & - ampersand
        BASIC_ESCAPE.put("<", "&lt;"); // < - less-than
        BASIC_ESCAPE.put(">", "&gt;"); // > - greater-than
    };

    public static byte[] getUtf8Bytes(String input) {
        if (input == null)
            return null;
        try {
            return input.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Strip a String of it's ISO control characters.
     * 
     * @param value The String that should be stripped.
     * @return {@code String} A new String instance with its hexadecimal control characters replaced by a space. Or the
     * unmodified String if it does not contain any ISO control characters.
     */
    public static String stripControlCharacters(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String value = replaceEntities(rawValue);

        boolean hasControlChars = false;
        for (int i = value.length() - 1; i >= 0; i--) {
            if (Character.isISOControl(value.charAt(i))) {
                hasControlChars = true;
                break;
            }
        }

        if (!hasControlChars) {
            return value;
        }

        StringBuilder buf = new StringBuilder(value.length());
        int i = 0;

        // Skip initial control characters (i.e. left trim)
        for (; i < value.length(); i++) {
            if (!Character.isISOControl(value.charAt(i))) {
                break;
            }
        }

        // Copy non control characters and substitute control characters with
        // a space. The last control characters are trimmed.
        boolean suppressingControlChars = false;
        for (; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                suppressingControlChars = true;
                continue;
            } else {
                if (suppressingControlChars) {
                    suppressingControlChars = false;
                    buf.append(' ');
                }
                buf.append(value.charAt(i));
            }
        }

        return buf.toString();
    }

    private static String replaceEntities(String value) {
        if (value == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            String c = String.valueOf(Character.toChars(Character.codePointAt(value, i)));
            CharSequence escapedEntity = BASIC_ESCAPE.get(c);

            if (escapedEntity == null) {
                sb.append(c);
            } else {
                sb.append(escapedEntity);
            }
        }

        return sb.toString();
    }
}
