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

package org.kaazing.net.impl.auth;

import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.auth.ChallengeRequest;
import org.kaazing.net.auth.ChallengeResponse;
import org.kaazing.net.auth.DispatchChallengeHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The DefaultDispatchChallengeHandler is responsible for defining and using appropriate challenge handlers when challenges
 * arrive from specific URI locations.  This allows clients to use specific challenge handlers to handle specific
 * types of challenges at different URI locations.
 * <p/>
 */
public class DefaultDispatchChallengeHandler extends DispatchChallengeHandler {
// ------------------------------ FIELDS ------------------------------

    static final String SCHEME_URI = "^(.*)://(.*)";
    static final Pattern SCHEME_URI_PATTERN = Pattern.compile(SCHEME_URI);

    enum UriElement {
        HOST,
        USERINFO,
        PORT,
        PATH
    }
    private Node<ChallengeHandler, UriElement> rootNode;

    public void clear() {
        rootNode = new Node<ChallengeHandler, UriElement>();
    }

    @Override
    public boolean canHandle(ChallengeRequest challengeRequest) {
        return lookup(challengeRequest) != null;
    }

    @Override
    public ChallengeResponse handle(ChallengeRequest challengeRequest) {
        ChallengeHandler challengeHandler = lookup(challengeRequest);
        if (challengeHandler == null) {
            return null;
        }
        return challengeHandler.handle(challengeRequest);
    }




// --------------------------- CONSTRUCTORS ---------------------------

    public DefaultDispatchChallengeHandler() {
        rootNode = new Node<ChallengeHandler, UriElement>();
    }

    @Override
    public DispatchChallengeHandler register(String locationDescription, ChallengeHandler challengeHandler) {
        if (locationDescription == null || locationDescription.length() == 0) {
            throw new IllegalArgumentException("Must specify a location to handle challenges upon.");
        }

        if (challengeHandler == null) {
            throw new IllegalArgumentException("Must specify a handler to handle challenges.");
        }

        addChallengeHandlerAtLocation(locationDescription, challengeHandler);
        return this;
    }

    @Override
    public DispatchChallengeHandler unregister(String locationDescription, ChallengeHandler challengeHandler) {
        if (locationDescription == null || locationDescription.length() == 0) {
            throw new IllegalArgumentException("Must specify a location to un-register challenge handlers upon.");
        }

        if (challengeHandler == null) {
            throw new IllegalArgumentException("Must specify a handler to un-register.");
        }

        delChallengeHandlerAtLocation(locationDescription, challengeHandler);

        return this;
    }

    private void delChallengeHandlerAtLocation(String locationDescription, ChallengeHandler challengeHandler) {
        List<Token<UriElement>> tokens = tokenize(locationDescription);
        Node<ChallengeHandler, UriElement> cursor = rootNode;
        for (Token<UriElement> token : tokens) {
            if (!cursor.hasChild(token.getName(), token.getKind())) {
                return; // silently remove nothing
            } else {
                cursor = cursor.getChild(token.getName());
            }
        }
        cursor.removeValue(challengeHandler);
    }

    private void addChallengeHandlerAtLocation(String locationDescription, ChallengeHandler challengeHandler) {
        List<Token<UriElement>> tokens = tokenize(locationDescription);
        Node<ChallengeHandler, UriElement> cursor = rootNode;

        for (Token<UriElement> token : tokens) {
            if (!cursor.hasChild(token.getName(), token.getKind())) {
                cursor = cursor.addChild(token.getName(), token.getKind());
            } else {
                cursor = cursor.getChild(token.getName());
            }
        }
        cursor.appendValues(challengeHandler);
    }



// -------------------------- OTHER METHODS --------------------------
    /**
     * Locate all challenge handlers to serve the given location.
     *
     * @param location a location
     * @return a collection of {@link ChallengeHandler}s if found registered at a matching location or an empty list if none are found.
     */
    public List<ChallengeHandler> lookup(String location) {
        List<ChallengeHandler> result = Collections.emptyList();
        if (location != null) {
            Node<ChallengeHandler, UriElement> resultNode = findBestMatchingNode(location);
            if (resultNode != null) {
                return resultNode.getValues();
            }
        }
        return result;
    }

    /**
     * Locate a challenge handler factory to serve the given location and challenge type.
     *
     *
     * @param challengeRequest A challenge string from the server.
     * @return a challenge handler registered to handle the challenge at the location,
     *         or <code>null</code> if none could be found.
     */
    ChallengeHandler lookup(ChallengeRequest challengeRequest) {
        ChallengeHandler result = null;
        String location = challengeRequest.getLocation();
        if (location != null) {
            Node<ChallengeHandler,UriElement> resultNode = findBestMatchingNode(location);

            //
            // If we found an exact or wildcard match, try to find a handler
            // for the requested challenge.
            //
            if (resultNode != null) {
                List<ChallengeHandler> handlers = resultNode.getValues();
                if (handlers != null) {
                    for (ChallengeHandler challengeHandler : handlers) {
                        if (challengeHandler.canHandle(challengeRequest)) {
                            result = challengeHandler;
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Return the Node corresponding to ("matching") a location, or <code>null</code> if none can be found.
     *
     * @param location the location at which to find a Node
     * @return the Node corresponding to ("matching") a location, or <code>null</code> if none can be found.
     */
    private Node<ChallengeHandler, UriElement> findBestMatchingNode(String location) {
        List<Token<UriElement>> tokens = tokenize(location);
        int tokenIdx = 0;

        return rootNode.findBestMatchingNode(tokens, tokenIdx);
    }

    /**
     * Tokenize a given string assuming it is like a URL.
     *
     * @param s the string to be parsed as a wildcard-able URI
     * @return the array of tokens of URI parts.
     * @throws IllegalArgumentException when the string cannot be parsed as a wildcard-able URI
     */
    List<Token<UriElement>> tokenize(String s) throws IllegalArgumentException {
        if (s == null || s.length() == 0) {
            return new ArrayList<Token<UriElement>>();
        }

        //
        // Make sure if a scheme is not specified, we default one before we parse as a URI.
        //
        if ( !SCHEME_URI_PATTERN.matcher(s).matches()) {
            s = ("http://")+s;
        }

        //
        // Parse as a URI
        //
        URI uri = URI.create(s);

        //
        // Detect what the scheme is, if any.
        //
        List<Token<UriElement>> result = new ArrayList<Token<UriElement>>(10);
        String scheme = "http";
        if (uri.getScheme() != null) {
            scheme = uri.getScheme();
        }


        //
        // A wildcard-ed hostname is parsed as an authority.
        //
        String host = uri.getHost();
        String parsedPortFromAuthority = null;
        String parsedUserInfoFromAuthority = null;
        String userFromAuthority = null;
        String passwordFromAuthority = null;
        if (host == null) {
            String authority = uri.getAuthority();
            if (authority != null) {
                host = authority;
                int asteriskIdx = host.indexOf("@");
                if ( asteriskIdx >= 0) {
                    parsedUserInfoFromAuthority = host.substring(0, asteriskIdx);
                    host = host.substring(asteriskIdx+1);
                    int colonIdx = parsedUserInfoFromAuthority.indexOf(":");
                    if ( colonIdx >= 0) {
                        userFromAuthority = parsedUserInfoFromAuthority.substring(0, colonIdx);
                        passwordFromAuthority = parsedUserInfoFromAuthority.substring(colonIdx+1);
                    }
                }
                int colonIdx = host.indexOf(":");
                if ( colonIdx >=0 ) {
                    parsedPortFromAuthority = host.substring(colonIdx + 1);
                    host = host.substring(0, colonIdx);
                }
            } else {
                throw new IllegalArgumentException("Hostname is required.");
            }
        }

        //
        // Split the host and reverse it for the tokenization.
        //
        List<String> hostParts = Arrays.asList(host.split("\\."));
        Collections.reverse(hostParts);
        for (String hostPart: hostParts) {
            result.add(new Token<UriElement>(hostPart, UriElement.HOST));
        }

        if (parsedPortFromAuthority != null) {
            result.add(new Token<UriElement>(parsedPortFromAuthority, UriElement.PORT));
        } else if (uri.getPort() > 0) {
            result.add(new Token<UriElement>(String.valueOf(uri.getPort()), UriElement.PORT));
        } else if (getDefaultPort(scheme) > 0) {
            result.add(new Token<UriElement>(String.valueOf(getDefaultPort(scheme)), UriElement.PORT));
        }


        if ( parsedUserInfoFromAuthority != null ) {
            if ( userFromAuthority != null) {
                result.add(new Token<UriElement>(userFromAuthority, UriElement.USERINFO));
            }
            if ( passwordFromAuthority != null ) {
                result.add(new Token<UriElement>(passwordFromAuthority, UriElement.USERINFO));
            }
            if ( userFromAuthority == null && passwordFromAuthority == null) {
                result.add(new Token<UriElement>(parsedUserInfoFromAuthority, UriElement.USERINFO));
            }
        } else if (uri.getUserInfo() != null) {
            String userInfo = uri.getUserInfo();
            int colonIdx = userInfo.indexOf(":");
            if ( colonIdx >= 0) {
                result.add(new Token<UriElement>(userInfo.substring(0, colonIdx), UriElement.USERINFO));
                result.add(new Token<UriElement>(userInfo.substring(colonIdx+1), UriElement.USERINFO));
            } else {
                result.add(new Token<UriElement>(uri.getUserInfo(), UriElement.USERINFO));
            }
        }

        if (isNotBlank(uri.getPath())) {
            String path = uri.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (isNotBlank(path)) {
                for (String p: path.split("/")) {
                    result.add(new Token<UriElement>(p, UriElement.PATH));
                }
            }
        }
        return result;
    }

    int getDefaultPort(String scheme) {
        if ( defaultPortsByScheme.containsKey(scheme.toLowerCase())) {
            return defaultPortsByScheme.get(scheme);
        } else {
            return -1;
        }
    }

    static Map<String, Integer> defaultPortsByScheme = new HashMap<String,Integer>();
    static {
        defaultPortsByScheme.put("http", 80);
        defaultPortsByScheme.put("ws", 80);
        defaultPortsByScheme.put("wss", 443);
        defaultPortsByScheme.put("https", 443);
    }


    private boolean isNotBlank(String s) {
        return s != null && s.length() > 0;
    }

    /**
     * A Node instance has a kind, holds a list of {@link #values} (parameterized type instances),
     * and a sub-tree of nodes called {@link #children}.  It is used as a model for
     * holding typed {@link org.kaazing.net.auth.ChallengeHandler} instances at "locations".
     * <p/>
     * {@link org.kaazing.net.auth.impl.DefaultDispatchChallengeHandler.Node} instances are mutable.  Nodes have {@link #name}s.  One can add children
     * with distinct names, add {@link #values} and recall them.
     * <p/>
     * {@link org.kaazing.net.auth.impl.DefaultDispatchChallengeHandler.Node} instances are considered to be "wildcard" nodes when their name is equal
     * to {@link #getWildcardChar()}.  Nodes are considered to have a wildcard defined if they or
     * any of their children are named {@link #getWildcardChar()}.  Wildcard nodes are treated
     * as matching one or multiple elements during {@link org.kaazing.net.auth.impl.DefaultDispatchChallengeHandler.Node searches}.
     * <p/>
     * The concept of a Node has the following restrictions in the following cases.
     * An {@link IllegalArgumentException} will result in each case.
     * <ol>
     *     <li>One is not permitted to add values to the root node.  Use a wildcard node instead.</li>
     *     <li>One is not permitted to create Node instances with <code>null</code> or empty names.</li>
     * </ol>
     *
     * @private
     */
    static class Node<T, E extends Enum<E>> {

        /**
         * The name of this Node instance.
         * Must not be <code>null</code> or empty.
         */
        private String name;

        /**
         * The parameterized type instances.
         * Optimized for fewer values per node.
         */
        private List<T> values = new ArrayList<T>(3);

        /**
         * An up-link to this Node instance's parent.
         */
        private Node<T, E> parent;

        /**
         * An enumerated value representing the "kind" of this node.
         */
        private E kind;

        /**
         * The down-links to children sub-nodes of this node.
         * Each link is accessed through the child Node's name.
         * This means that child names must be unique.
         */
        private Map<String,Node<T,E>> children = new LinkedHashMap<String, Node<T,E>>();

        /**
         * A method to access the wildcard character.
         * Making this a method rather than a constant will allow
         * this value to change without recompilation of client classes.
         * @return the character assumed to be the wildcard character for this tree.
         */
        public static String getWildcardChar() {
            return "*";
        }

        /**
         * Create a new root node, with a null name and parent.
         */
        Node() {
            this.name=null;
            this.parent=null;
            this.kind = null;
        }


        /**
         * Create a new node with the provided name and parent node.
         * @param name    the name of the node instance to create.
         * @param parent  the parent of the new node to establish the new node's place in the tree.
         * @param kind    the kind of the node instance to create.
         */
        private Node(String name, Node<T,E> parent, E kind) {
            this.name = name;
            this.parent = parent;
            this.kind = kind;
        }

        /**
         * Add a new node with the given name of the given kind to this node.
         * <p/>
         * If an existing child has that name, replace the existing named sub-tree with
         * a new single node with the provided name.
         *
         *
         * @param name the name of the new node
         * @param kind the kind of the new node
         * @return the freshly added Node, for chained calls if needed.
         * @throws IllegalArgumentException if the name is null or empty
         */
        public Node<T,E> addChild(String name, E kind) {
            if ( name == null || name.length() == 0) {
                throw new IllegalArgumentException("A node may not have a null name.");
            }

            Node<T,E> result = new Node<T,E>(name, this, kind);
            children.put(name, result);
            return result;
        }

        /**
         * Return whether this node has a child with the name and kind provided.
         *
         *
         * @param name the name of the child node sought
         * @param kind the kind of the child node sought
         * @return true iff this instance has a child with that name and kind
         */
        public boolean hasChild(String name, E kind) {
            return null != getChild(name) && kind == getChild(name).getKind();
        }

        /**
         * Return the child node instance corresponding to the provided name,
         * or <code>null</code> if no such node can be found.
         *
         *
         * @param name the name of the node sought
         * @return the child node instance corresponding to the provided name,
         * or <code>null</code> if no such node can be found.
         */
        public Node<T,E> getChild(String name) {
                return children.get(name);
        }

        /**
         * Return the distance that this token is away from the root.
         * @return 0 if this node is the root node,
         *           otherwise the number of nodes from this node to the root node including this node.
         */
        public int getDistanceFromRoot() {
            int result = 0;
            Node<T,E> cursor = this;
            while (!cursor.isRootNode()) {
                result++;
                cursor = cursor.getParent();
            }
            return result;
        }

        /**
         * Add the provided values to the current node.
         *
         * @param values the values to add to this node instance
         * @throws IllegalArgumentException when attempting to add values to the root node.
         */
        public void appendValues(T... values) {
            if ( isRootNode() ) {
                throw new IllegalArgumentException("Cannot set a values on the root node.");
            }
            if ( values != null ) {
                this.values.addAll(Arrays.asList(values));
            }
        }

        /**
         * Remove the provided value from the current node.
         * @param value the value to remove from this node instance.
         */
        public void removeValue(T value) {
            if ( isRootNode() ) {
                return;
            }
            this.values.remove(value);
        }

        /**
         * Return the collection of stored values for this node instance.
         * If no values have been stored, we return an empty list.
         * @return the collection of stored values for this node instance; an empty list if none have been stored.
         */
        public List<T> getValues() {
            return values;
        }

        /**
         * Returns whether this node instance contains any values.
         * @return true iff values have been stored in this node.
         */
        public boolean hasValues() {
            return values != null && values.size()>0;
        }

        /**
         * Return a link to the parent instance of this node, or <code>null</code>
         * when invoked on the root node.
         *
         * @return a link to the parent instance of this node, or <code>null</code>
         * when invoked on the root node.
         */
        public Node<T,E> getParent() {
            return parent;
        }

        /**
         * Return the enumerated value from E for this kind of this node, or <code>null</code>
         * when invoked on the root node.
         *
         * @return the enumerated value from E for this kind of this node, or <code>null</code>
         * when invoked on the root node.
         */
        public E getKind() {
            return this.kind;
        }

        /**
         * Is this node the root node?
         * @return true iff this node instance is the root node.
         */
        public boolean isRootNode() {
            return this.parent == null;
        }

        /**
         * Return the name of this node.
         * @return the name of this node.  <code>null</code> iff this node is the root node.
         */
        public String getName() {
            return name;
        }

        /**
         * Has at least one child been added to this node instance explicitly?
         * @return true iff one child been added to this node instance explicitly.
         */
        public boolean hasChildren() {
            return children != null && children.size() > 0;
        }

        /**
         * Return whether this node instance's name is equal to {@link #getWildcardChar()}.
         *
         * @return true iff this node instance's name is equal to {@link #getWildcardChar()}.
         */
        public boolean isWildcard() {
            return name!=null && name.equals(getWildcardChar());
        }



        boolean hasWildcardChild() {
            return hasChildren() && children.keySet().contains(getWildcardChar());
        }

        /**
         * Get a fully qualified name from the root node down to this node.
         * <p/>
         * Implemented as: Walk up to the root, gathering names, then emit a dot-separated list of names.
         * Useful for debugging.
         *
         * @return a fully qualified name from the root node down to this node.
         */
        String getFullyQualifiedName() {
            StringBuilder b = new StringBuilder();
            List<String> name = new ArrayList<String>();
            Node cursor = this;
            while (!cursor.isRootNode()) {
                name.add(cursor.name);
                cursor = cursor.parent;
            }
            Collections.reverse(name);
            for(String s: name) {
                b.append(s).append('.');
            }

            if ( b.length() >= 1 && b.charAt(b.length()-1) == '.') {
                b.deleteCharAt(b.length()-1);
            }

            return b.toString();
        }

        public List<Node<T,E>> getChildrenAsList() {
            return new ArrayList<Node<T,E>>(children.values());
        }

        /**
         * Find the best matching node with respect to the tokens underneath this node.
         * @param tokens     the tokenized location to query.
         * @param tokenIdx   the index into the tokens to commence matching at.
         * @return the best matching node or {@code null} if no matching node could be found.
         */
        Node<T,E> findBestMatchingNode(List<Token<E>> tokens, int tokenIdx) {
            List<Node<T,E>> matches = findAllMatchingNodes(tokens, tokenIdx);

            Node<T,E> resultNode = null;
            int score = 0;
            for (Node<T,E> node : matches) {
                if (node.getDistanceFromRoot() > score) {
                    score = node.getDistanceFromRoot();
                    resultNode = node;
                }
            }
            return resultNode;
        }

        /**
         * Find all matching nodes with respect to the tokens underneath this node.
         * @param tokens     the tokenized location to query.
         * @param tokenIdx   the index into the tokens to commence matching at.
         * @return a collection of all matching nodes, which may be empty if no matching nodes were found.
         */
        private List<Node<T,E>> findAllMatchingNodes(List<Token<E>> tokens, int tokenIdx) {
            List<Node<T,E>> result = new ArrayList<Node<T,E>>();

            //
            // Iterate over this node's children.
            //
            List<Node<T,E>> nodes = this.getChildrenAsList();
            for (Node<T,E> node : nodes) {

                //
                // Do any tokens match the child node?
                //
                int matchResult = node.matches(tokens, tokenIdx);
                if (matchResult < 0) {
                    // The node matched no tokens.
                    continue;
                }
                if (matchResult >= tokens.size()) {
                    // This node matched all remaining tokens.

                    //
                    // Make sure we walk down further wildcard node(s) of the same kind
                    // as the node that matched all tokens and gather all values.
                    //
                    do {
                        if (node.hasValues()) {
                            result.add(node);
                        }
                        if ( node.hasWildcardChild()) {
                            Node<T,E> child = node.getChild(getWildcardChar());
                            if (child.getKind() != getKind()) {
                                node = null;
                            } else {
                                node = child;
                            }

                        } else {
                            node = null;
                        }
                    } while (node != null);

                } else {
                    //
                    // This node matched some of the remaining tokens.
                    // So continue to find matching nodes for the remaining tokens (from matchResult onwards).
                    //
                    result.addAll(node.findAllMatchingNodes(tokens, matchResult));
                }
            }
            return result;
        }

        /**
         * Does this node match one or more tokens starting at tokenIdx?
         * <p/>
         * If not, returns {@code -1}.
         * If so, return the index of the first non-matching token in the provided
         * tokens, or {@code tokens.length} when all tokens in the array match the node.
         *
         * @param tokens    the tokenized lcoation query
         * @param tokenIdx  the index of interest into the tokens
         * @return the index of the first non-matching token starting at tokenIdx,
         *         {@code -1} when this node does not match any tokens starting at tokenIdx,
         *         {@code tokens.length} when this node matches all tokens starting at tokenIdx.
         */
        private int matches(List<Token<E>> tokens, int tokenIdx) {
            // Return no match (-1) for bad token indices
            if (tokenIdx < 0 || tokenIdx >= tokens.size()) {
                return -1;
            }

            // For exact name matches return the next token index
            if (matchesToken(tokens.get(tokenIdx))) {
                return tokenIdx + 1;
            }

            // Return no match (-1) since we are not a wildcard and not an exact match
            if (!this.isWildcard()) {
                return -1;
            } else {

                // Return no match because wildcards match within Node kinds
                if ( this.kind != tokens.get(tokenIdx).getKind()) {
                    return -1;
                }

                do {
                    tokenIdx++;
                } while ( tokenIdx < tokens.size() && this.kind == tokens.get(tokenIdx).getKind());
                return tokenIdx;
            }
        }

        private boolean matchesToken(Token<E> token) {
            return this.getName().equals(token.getName()) &&
                    this.kind == token.getKind();
        }


    }


    static class Token<E extends Enum<E>> {
        E kind;
        String name;

        Token(String name, E element) {
            this.kind = element;
            this.name = name;
        }

        public E getKind() {
            return kind;
        }

        public void setKind(E kind) {
            this.kind = kind;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
