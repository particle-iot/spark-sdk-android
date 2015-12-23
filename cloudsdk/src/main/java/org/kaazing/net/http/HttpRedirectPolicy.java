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

package org.kaazing.net.http;

import java.net.URI;
import java.util.Comparator;

/**
 * Options for following HTTP redirect requests with response code 3xx.
 */
public enum HttpRedirectPolicy implements Comparator<URI> {
    /**
     * Do not follow HTTP redirects.
     */
    NEVER() {
        @Override
        public int compare(URI current, URI redirect) {
            if ((current == null) || (redirect == null)) {
                String s = "Null URI passed in to compare()";
                throw new IllegalArgumentException(s);
            }

            // URIs don't matter as this option indicates never to follow
            // redirects.
            return -1;
        }
        
        @Override
        public String toString() {
            return "HttpRedirectOption.NEVER";
        }
    },
    
    /**
     * Follow HTTP redirect requests always regardless of the origin, domain, etc.
     */
    ALWAYS() {
        @Override
        public int compare(URI current, URI redirect) {
            if ((current == null) || (redirect == null)) {
                String s = "Null URI passed in to compare()";
                throw new IllegalArgumentException(s);
            }

            // URIs don't matter as this option indicates always to follow
            // redirects.
            return 0;
        }
        
        @Override
        public String toString() {
            return "HttpRedirectOption.ALWAYS";
        }
    },
    
    /**
     * Follow HTTP redirect only if the redirected request is for the same 
     * origin. This implies that both the scheme/protocol and the 
     * <b>authority</b> should match between the current and the redirect URIs. 
     * Note that authority includes the hostname and the port.
     */
    SAME_ORIGIN() {
        @Override
        public int compare(URI current, URI redirect) {
            if ((current == null) || (redirect == null)) {
                String s = "Null URI passed in to compare()";
                throw new IllegalArgumentException(s);
            }

            // Two URIs have same origin if the protocol and authority
            // are the same.
            if (current.getScheme().equalsIgnoreCase(redirect.getScheme()) &&
                current.getAuthority().equalsIgnoreCase(redirect.getAuthority())) {
                return 0;
            }

            return -1;
        }
        
        @Override
        public String toString() {
            return "HttpRedirectOption.SAME_ORIGIN";
        }
    }, 
    
    /**
     * Follow HTTP redirect only if the redirected request is for the same 
     * domain. This implies that both the scheme/protocol and the 
     * <b>hostname</b> should match between the current and the redirect URIs.
     * <p>
     * URIs that satisfy HttpRedirectPolicy.SAME_ORIGIN policy will implicitly
     * satisfy HttpRedirectPolicy.SAME_DOMAIN policy.
     * <p>
     * URIs with identical domains would be ws://production.example.com:8001 and 
     * ws://production.example.com:8002.
     */
    SAME_DOMAIN() {
        @Override
        public int compare(URI current, URI redirect) {
            if (HttpRedirectPolicy.SAME_ORIGIN.compare(current, redirect) == 0) {
                // If the URIs have same origin, then they implicitly have same
                // domain.
                return 0;
            }

            if ((current == null) || (redirect == null)) {
                String s = "Null URI passed in to compare()";
                throw new IllegalArgumentException(s);
            }

            // We should allow redirecting to a more secure scheme from a less
            // secure scheme. For example, we should allow redirecting from 
            // ws -> wss.
            String currScheme = current.getScheme();
            String newScheme = redirect.getScheme();
            if (newScheme.equalsIgnoreCase(currScheme) ||
                newScheme.contains(currScheme)) {
                // Check if the host names are the same between the two URIs.
                String currHost = current.getHost();
                String newHost = redirect.getHost();

                if (currHost.equalsIgnoreCase(newHost)) {
                    return 0;
                }
            }

            return -1;
        }
        
        @Override
        public String toString() {
            return "HttpRedirectOption.SAME_DOMAIN";
        }
    },
    
    /**
     * Follow HTTP redirect only if the redirected request is for a peer-domain.
     * This implies that both the scheme/protocol and the <b>domain</b> should 
     * match between the current and the redirect URIs.
     * <p>
     * URIs that satisfy HttpRedirectPolicy.SAME_DOMAIN policy will implicitly
     * satisfy HttpRedirectPolicy.PEER_DOMAIN policy.
     * <p>
     * To determine if the two URIs have peer-domains, we do the following:
     * <ul>
     *   <li>compute base-domain by removing the token before the first '.' in 
     *       the hostname of the original URI and check if the hostname of the  
     *       redirected URI ends with the computed base-domain
     *  <li>compute base-domain by removing the token before the first '.' in 
     *      the hostname of the redirected URI and check if the hostname of the 
     *      original URI ends with the computed base-domain
     * </ul>
     * <p>
     * If both the conditions are satisfied, then we conclude that the URIs are 
     * for peer-domains. However, if the host in the URI has no '.'(for eg.,
     * ws://localhost:8000), then we just use the entire hostname as the 
     * computed base-domain.
     * <p>
     * If you are using this policy, it is recommended that the number of tokens
     * in the hostname be atleast 2 + number_of_tokens(top-level-domain). For 
     * example, if the top-level-domain(TLD) is "com", then the URIs should have
     * atleast 3 tokens in the hostname. So, ws://marketing.example.com:8001 and
     * ws://sales.example.com:8002 are examples of URIs with peer-domains. Similarly,
     * if the TLD is "co.uk", then the URIs should have atleast 4 tokens in the 
     * hostname. So, ws://marketing.example.co.uk:8001 and 
     * ws://sales.example.co.uk:8002 are examples of URIs with peer-domains.
     */
    PEER_DOMAIN() {
        @Override
        public int compare(URI current, URI redirect) {
            if (HttpRedirectPolicy.SAME_DOMAIN.compare(current, redirect) == 0) {
                // If the domains are the same, then they are peers.
                return 0;
            }

            if ((current == null) || (redirect == null)) {
                String s = "Null URI passed in to compare()";
                throw new IllegalArgumentException(s);
            }

            // We should allow redirecting to a more secure scheme from a less
            // secure scheme. For example, we should allow redirecting from 
            // ws -> wss.
            String currScheme = current.getScheme();
            String newScheme = redirect.getScheme();
            if (newScheme.equalsIgnoreCase(currScheme) ||
                newScheme.contains(currScheme)) {
                String currHost = current.getHost();
                String redirectHost = redirect.getHost();
                String currBaseDomain = getBaseDomain(currHost);
                String redirectBaseDomain = getBaseDomain(redirectHost);
                
                if (currHost.endsWith(redirectBaseDomain) && 
                    redirectHost.endsWith(currBaseDomain)) {
                    return 0;
                }
            }

            return -1;
        }
        
        @Override
        public String toString() {
            return "HttpRedirectOption.PEER_DOMAIN";
        }
        
        // Get the base domain for a given hostname. For example, jms.kaazing.com 
        // will return kaazing.com.
        private String getBaseDomain(String hostname) {
            String[] tokens = hostname.split("\\.");
            
            if (tokens.length <= 2) {
                return hostname;
            }
            
            String baseDomain = "";
            for (int i = 1; i < tokens.length; i++) {
                baseDomain += "." + tokens[i];
            }
            
            return baseDomain;
        }
    },
    
    /**
     * Follow HTTP redirect only if the redirected request is for child-domain 
     * or sub-domain of the original request.
     * <p>
     * URIs that satisfy HttpRedirectPolicy.SAME_DOMAIN policy will implicitly
     * satisfy HttpRedirectPolicy.SUB_DOMAIN policy.
     * <p>
     * To determine if the domain of the redirected URI is sub-domain/child-domain
     * of the domain of the original URI, we check if the hostname of the
     * redirected URI ends with the hostname of the original URI.
     * <p>
     * Domain of the redirected URI ws://benefits.hr.example.com:8002 is a 
     * sub-domain/child-domain of the domain of the original URI 
     * ws://hr.example.com:8001. Note that domain in ws://example.com:9001 is a 
     * sub-domain of the domain in ws://example.com:9001. 
     */
    SUB_DOMAIN() {
        @Override
        public int compare(URI current, URI redirect) {
            if (HttpRedirectPolicy.SAME_DOMAIN.compare(current, redirect) == 0) {
                // If the domains are the same, then one can be a sub-domain
                // of the other.
                return 0;
            }

            if ((current == null) || (redirect == null)) {
                String s = "Null URI passed in to compare()";
                throw new IllegalArgumentException(s);
            }

            // We should allow redirecting to a more secure scheme from a less
            // secure scheme. For example, we should allow redirecting from 
            // ws -> wss.
            String currScheme = current.getScheme();
            String newScheme = redirect.getScheme();
            if (newScheme.equalsIgnoreCase(currScheme) ||
                newScheme.contains(currScheme)) {
                // If the current host is gateway.example.com, and the new
                // is child.gateway.example.com, then allow redirect.
                String currHost = current.getHost();
                String newHost = redirect.getHost();
                
                if (newHost.length() < currHost.length()) {
                    return -1;
                }

                if (newHost.endsWith("." + currHost)) {
                    return 0;
                }                
            }

            return -1;
        }
        
        @Override
        public String toString() {
            return "HttpRedirectOption.SUB_DOMAIN";
        }
    };

    /**
     * Returns 0, if the aspects of current and the redirected URIs match as per
     * the option. Otherwise, -1 is returned.
     * 
     * @param  current     URI of the current request
     * @param  redirect    URI of the redirected request
     * @return 0, for a successful match; otherwise -1 
     */
    @Override
    public abstract int compare(URI current, URI redirect);
    
    @Override
    public abstract String toString();
}