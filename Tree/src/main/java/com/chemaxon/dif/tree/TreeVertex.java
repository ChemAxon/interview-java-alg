/*
 * Copyright (c) 1998-2016 ChemAxon Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * ChemAxon. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon.
 */
package com.chemaxon.dif.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public abstract class TreeVertex {

    private final Object lock;
    private final TreeVertex parent;
    private final List<TreeVertex> childs = Lists.newCopyOnWriteArrayList();

    /**
     * Constructor for creating the root vertex.
     */
    protected TreeVertex() {
        this.parent = null;
        this.lock = new Object();
    }

    /**
     * Constructor for child vertices.
     * @param parent The parent vertex, cannot be null.
     */
    protected TreeVertex(TreeVertex parent) {
        this.parent = Preconditions.checkNotNull(parent, "The parent cannot be null");
        this.lock = parent.getLock();
    }

    private Object getLock() {
        return lock;
    }

    public void addChildren(TreeVertex v) {
        synchronized (getLock()) {
            Preconditions.checkArgument(equals(v.getParent()), "The child has wrong parent");
            childs.add(v);
        }
    }

    private List<TreeVertex> getChilds() {
        return Collections.unmodifiableList(childs);
    }

    /**
     * Returns the parent vertex in the transformed {@link DFDataTree}.
     *
     * @return The parent vertex.
     */
    public TreeVertex getParent() {
        return parent;
    }

    /**
     * Iterates over the vertices in subtree of this vertex.
     *
     * @param <T> The result type.
     * @param visitor The visitor to use for visiting the vertices in the subtree of this vertex.
     * @param t The strategy to use for visiting the vertices.
     * @return The result of visitSubtree.
     */
    public <T> T visitSubtree(Visitor<T> visitor, Strategy t) {
        synchronized (getLock()) {
            SearchCollection collection = t.getCollection();
            collection.addFirst(this);
            while (!collection.isEmpty()) {
                TreeVertex current = collection.removeFirst();
                T result = visitor.visit(current);
                if (result != null) {
                    return result;
                }
                collection.addAll(current.getChilds());
            }
            return null;
        }
    }

    /**
     * The strategy for visiting the data tree vertices.
     */
    public enum Strategy {
        /**
         * Breadth first visiting of vertices.
         */
        BREADTH_FIRST(new BreadthFirstCollection()),
        /**
         * Depth first visiting of vertices.
         */
        DEPTH_FIRST(new DepthFirstCollection());

        private final SearchCollection collection;

        Strategy(SearchCollection collection) {
            this.collection = collection;
        }

        private SearchCollection getCollection() {
            return collection;
        }

    }

    private interface SearchCollection {

        void addFirst(TreeVertex vertex);

        void addAll(Collection<TreeVertex> vertices);

        TreeVertex removeFirst();

        boolean isEmpty();
    }

    private static final class DepthFirstCollection implements SearchCollection {

        private final LinkedList<TreeVertex> linkedList = Lists.newLinkedList();

        @Override
        public void addFirst(TreeVertex v) {
            linkedList.addFirst(v);
        }

        @Override
        public void addAll(Collection<TreeVertex> vertices) {
            linkedList.addAll(0, vertices);
        }

        @Override
        public TreeVertex removeFirst() {
            return linkedList.removeFirst();
        }

        @Override
        public boolean isEmpty() {
            return linkedList.isEmpty();
        }

    }

    private static final class BreadthFirstCollection implements SearchCollection {

        private final Queue<TreeVertex> queue = Lists.newLinkedList();

        @Override
        public void addFirst(TreeVertex v) {
            queue.add(v);
        }

        @Override
        public void addAll(Collection<TreeVertex> vertices) {
            queue.addAll(vertices);
        }

        @Override
        public TreeVertex removeFirst() {
            return queue.remove();
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    /**
     * Visitor for vertices in data tree.
     *
     * @param <T> The result type of the visitor.
     */
    public interface Visitor<T> {

        /**
         * Visits the vertex of the data tree, if non null result is returned the visitSubtree method stops and returns
         * this result.
         *
         * @param v The vertex.
         * @return The result. Return non null if
         * {@link Tree#visit(com.im.df.impl.db.query.join.Tree.Visitor, com.im.df.impl.db.query.join.Tree.Strategy) }
         * should stop. If non null is returned the visitSubtree will continue based on the {@link Strategy}.
         */
        T visit(TreeVertex v);
    }

}
