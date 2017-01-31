/*
 * Copyright (c) 1998-2017 ChemAxon Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * ChemAxon. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon.
 */
package com.chemaxon.dif.tree;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// TODO write test for verifying that BREADTH_FIRST and DEPTH_FIRST visiting of subtree works correctly.
// Fix the logic in TreeVertex to pass testConcurrencyProblem
public class TreeVertexTest {

    private static final Logger LOG = Logger.getLogger(TreeVertexTest.class.getName());

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testBreadthFirstLogic() throws InterruptedException {
        fail("Must be implemented");
    }

    @Test
    public void testDepthFirst() throws InterruptedException {
        fail("Must be implemented");
    }

    @Test
    public void testConcurrencyProblem() throws InterruptedException {

        final VertexImpl root1 = VertexImpl.createRoot();
        VertexImpl.createChild(root1);
        VertexImpl.createChild(root1);

        VertexImpl root2 = VertexImpl.createRoot();
        VertexImpl.createChild(root2);
        VertexImpl.createChild(root2);

        final CountDownLatch tree1ProcessesFirstChild = new CountDownLatch(1);
        final CountDownLatch tree2ProcessesFirstChild = new CountDownLatch(1);
        final CountDownLatch tree1Fails = new CountDownLatch(1);

        final AtomicBoolean sameTree = new AtomicBoolean(true);

        Thread t1 = new Thread(() -> root1.visitSubtree(new Visitor() {
            @Override
            public void visit(TreeVertex v, int cnt) {
                switch (cnt) {
                    case 1:
                        tree1ProcessesFirstChild.countDown();
                        await(tree2ProcessesFirstChild);
                        break;
                    case 2:
                        TreeVertex parent = v.getParent();
                        sameTree.compareAndSet(true, parent.equals(root1));
                        tree1Fails.countDown();
                        break;
                }
            }
        }, TreeVertex.Strategy.BREADTH_FIRST));
        Thread t2 = new Thread(() -> {
            await(tree1ProcessesFirstChild);
            root2.visitSubtree(new Visitor() {
                @Override
                public void visit(TreeVertex v, int cnt) {
                    if (cnt == 2) {
                        tree2ProcessesFirstChild.countDown();
                        await(tree1Fails);
                    }
                }
            }, TreeVertex.Strategy.BREADTH_FIRST);
        });

        final List<Thread> threads = Lists.newArrayList();
        threads.add(t1);
        threads.add(t2);

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join(500);
            assertTrue("Problem with multithreaded execution",
                    thread.getState().equals(Thread.State.TERMINATED));
        }

        Assert.assertTrue("Visiting vertex from another tree.", sameTree.get());
    }

    @Test
    public void testMultiThreadedAccsess() throws InterruptedException {

        final VertexImpl[] trees = new VertexImpl[] {VertexImpl.createRoot(), VertexImpl.createRoot()};
        final CountDownLatch multithreadedAccess = new CountDownLatch(2);
        final List<Thread> threads = Lists.newArrayList();
        for (final VertexImpl tree : trees) {
            Thread t = new Thread(() -> tree.visitSubtree(
                    (TreeVertex.Visitor<Void>) v -> {
                        multithreadedAccess.countDown();
                        await(multithreadedAccess);
                        return null;
                    }, TreeVertex.Strategy.DEPTH_FIRST));
            threads.add(t);
            t.start();
        }

        for (Thread thread : threads) {
            thread.join(500);
            assertTrue("Multithreading is not supported",
                    thread.getState().equals(Thread.State.TERMINATED));
        }
    }

    private static abstract class Visitor implements TreeVertex.Visitor<Void> {

        private final AtomicInteger cnt = new AtomicInteger();

        @Override
        public final Void visit(TreeVertex v) {
            visit(v, cnt.getAndIncrement());
            return null;
        }

        abstract void visit(TreeVertex v, int cnt);

    }

    private static final class VertexImpl extends TreeVertex {

        static VertexImpl createRoot() {
            return new VertexImpl();
        }

        static VertexImpl createChild(VertexImpl v) {
            VertexImpl result = new VertexImpl(v);
            v.addChildren(result);
            return result;
        }


        private VertexImpl() {
        }

        private VertexImpl(VertexImpl parent) {
            super(parent);
        }

    }

}
