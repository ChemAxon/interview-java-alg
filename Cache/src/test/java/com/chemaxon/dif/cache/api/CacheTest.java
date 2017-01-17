/*
 * Copyright (c) 1998-2017 ChemAxon Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * ChemAxon. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon.
 */
package com.chemaxon.dif.cache.api;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.chemaxon.dao.spi.DAO;
import com.chemaxon.dif.cache.impl.WrongCacheLogic;
import com.chemaxon.dif.cache.spi.CacheLogic;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// TODO try to implement a CacheLogic passing all the tests.
/**
 * To change the implementation of {@link CacheLogic} change the method in {@link #createCacheLogic()}.
 */
public class CacheTest {

    private static final Logger LOG = Logger.getLogger(CacheTest.class.getName());

    private static <K extends Comparable<K>, V> CacheLogic<K, V> createCacheLogic() {
        return new WrongCacheLogic<>();
    }

    private static ImmutableList<Integer> createRandomList(Random r, int size, int maxNumber) {
        ImmutableList.Builder<Integer> keys = ImmutableList.builder();
        for (int i = 0; i < size; ++i) {
            keys.add(r.nextInt(maxNumber));
        }
        return keys.build();
    }

    @Test
    public void testCacheOneElement() {
        final FakeDAO dao = FakeDAO.create();
        Cache<Integer, Integer> cache = Cache.create(dao, createCacheLogic(), 10, 10, 1);
        dao.assertDaoTouches("The DAO muct be untoched", 0);

        FakeDAO.runQueryAndAssertData(cache, 1);
        dao.assertDaoTouches("querying ID 1", 1);
        dao.assertKeyTouches("querying ID 1", 1, 1);

        FakeDAO.runQueryAndAssertData(cache, 1);
        dao.assertDaoTouches("querying ID 1, must be present in the cache, no DAO is needed", 1);
        dao.assertKeyTouches("querying ID 1, must be present in the cache, no DAO is needed", 1, 1);
    }

    @Test
    public void testCacheTwoElement() {
        final FakeDAO dao = FakeDAO.create();
        Cache<Integer, Integer> cache = Cache.create(dao, createCacheLogic(), 10, 10, 1);

        FakeDAO.runQueryAndAssertData(cache, 5);
        dao.assertDaoTouches("querying ID 5", 1);
        dao.assertKeyTouches("querying ID 5", 5, 1);

        FakeDAO.runQueryAndAssertData(cache, 5, 15);
        dao.assertDaoTouches("ID 5 is present, ID 15 must be retrieved", 2);
        dao.assertKeyTouches("ID 5 is already present in the cache", 5, 1);
        dao.assertKeyTouches("ID 15 must be retrieved from the DAO", 15, 1);
    }

    @Test
    public void testCacheOverflow() {
        final FakeDAO dao = FakeDAO.create();
        Cache<Integer, Integer> cache = Cache.create(dao, createCacheLogic(), 2, 2, 1);

        FakeDAO.runQueryAndAssertData(cache, 1);
        dao.assertDaoTouches("querying ID 1, must be retrieved from DAO", 1);
        dao.assertKeyTouches("querying ID 1, must be retrieved from DAO", 1, 1);

        FakeDAO.runQueryAndAssertData(cache, 2);
        dao.assertDaoTouches("querying ID 2, must be retrieved from DAO", 2);
        dao.assertKeyTouches("querying ID 2, must be retrieved from DAO", 2, 1);

        FakeDAO.runQueryAndAssertData(cache, 1);
        dao.assertDaoTouches("querying ID 1, must be in cache", 2);
        dao.assertKeyTouches("querying ID 1, must be in cache", 1, 1);

        FakeDAO.runQueryAndAssertData(cache, 3);
        dao.assertDaoTouches("querying ID 3, cache overflow, ID 2 is removed", 3);
        dao.assertKeyTouches("querying ID 3, cache overflow, ID 2 is removed", 3, 1);

        FakeDAO.runQueryAndAssertData(cache, 2);
        dao.assertDaoTouches("querying ID 2, must be retrieved from DAO", 4);
        dao.assertKeyTouches("querying ID 2, must be retrieved from DAO", 2, 2);
    }

    private void runMultithreadedRandomQueries(final FakeDAO dao, int numberOfThreads, Random r)
            throws InterruptedException {
        final Cache<Integer, Integer> cache = Cache.create(dao, createCacheLogic(), 128, 2048, numberOfThreads);

        List<Thread> threads = Lists.newArrayList();
        final List<String> errors = Lists.newCopyOnWriteArrayList();
        for (int i = 0; i < numberOfThreads; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    int queries = 50 + r.nextInt(100);
                    for (int j = 0; j < queries; ++j) {
                        final int size = 100 + r.nextInt(100);
                        List<Integer> keys = createRandomList(r, size, 500);
                        FakeDAO.runQueryAndAssertData(cache, keys);
                        FakeDAO.runQueryAndAssertData(cache, keys);
                    }
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testStressTest() throws InterruptedException {
        final long seed = System.currentTimeMillis();
        final Random r = new Random(seed);
        final int numberOfThreads = 16;
        final FakeDAO dao = FakeDAO.create();
        runMultithreadedRandomQueries(dao, numberOfThreads, r);
    }

    @Test
    public void testNumberOfIDRetrieval() throws InterruptedException {
        final long seed = 800;
        final Random r = new Random(seed);
        final int numberOfThreads = 4;
        for (int i = 0; i < 10; ++i) {
            final FakeDAO dao = new FakeDAO();
            runMultithreadedRandomQueries(dao, numberOfThreads, r);
            int allKeyTouches = dao.allKeyTouches();
            assertTrue(MessageFormat.format(
                    "Too much ID were retrieved from DAO, maximal expected is {0}, actual {1}", 1000, allKeyTouches),
                    allKeyTouches < 1000);
        }
    }

    @Test
    public void testParelellyRetrievedSameObject() throws InterruptedException {
        final long seed = System.currentTimeMillis();
        final Random r = new Random(seed);
        final int numberOfThreads = 16;
        final Set<Integer> idsCurrentlyProcessedByDao = Collections.newSetFromMap(Maps.newConcurrentMap());
        final FakeDAO dao = new FakeDAO() {

            private final Object lock = new Object();

            @Override
            public Map<Integer, Integer> getData(Collection<Integer> ids) {
                synchronized (lock) {
                    Sets.SetView<Integer> idsProcessedMultipleTimes = Sets.intersection(
                            idsCurrentlyProcessedByDao, Sets.newHashSet(ids));
                    Preconditions.checkState(idsProcessedMultipleTimes.isEmpty(),
                            "Retrieving IDs which are currently processed by other thread");
                }

                idsCurrentlyProcessedByDao.addAll(ids);
                Map<Integer, Integer> result = super.getData(ids);

                synchronized (lock) {
                    idsCurrentlyProcessedByDao.removeAll(ids);
                }

                return result;
            }
        };
        runMultithreadedRandomQueries(dao, numberOfThreads, r);
    }

    @Test
    public void testPossibleDeadlockTest() throws InterruptedException {

        final ObjectLock<Integer> lock = new ObjectLock<>();
        final FakeDAO dao = new FakeDAO() {
            @Override
            public Map<Integer, Integer> getData(Collection<Integer> ids) {
                lock.lockAll(ids);
                Map<Integer, Integer> result = super.getData(ids);
                lock.unlockAll(ids);
                return result;
            }
        };
        final Cache<Integer, Integer> cache = Cache.create(dao, createCacheLogic(), 32, 64, 2);
        List<Integer>[] lists = new List[]{
            ImmutableList.of(1, 5, 3, 8, 2, 16, 11, 74, 65, 9),
            ImmutableList.of(74, 13, 51, 16, 8, 42, 5, 1)};

        List<Thread> threads = Lists.newArrayList();
        final CountDownLatch waitForThreadStart = new CountDownLatch(2);
        for (List<Integer> list : lists) {
            Thread thread = new Thread(() -> {
                waitForThreadStart.countDown();
                try {
                    waitForThreadStart.await();
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
                FakeDAO.runQueryAndAssertData(cache, list);
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join(500);
            assertTrue("Deadlock occured while locking the IDs.",
                    thread.getState().equals(Thread.State.TERMINATED));
        }

    }

    private static final class ObjectLock<K extends Comparable<K>> {

        private final ConcurrentMap<K, Lock> locks = Maps.newConcurrentMap();
        private final Object synch = new Object();

        public void lockAll(Collection<K> objects) {
            Set<K> objectsToLock = Sets.newLinkedHashSet(objects);
            for (K k : objectsToLock) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
                Lock l = new ReentrantLock();
                synchronized (synch) {
                    locks.putIfAbsent(k, l);
                    Lock toLock = locks.get(k);
                    toLock.lock();
                }
            }
        }

        public void unlockAll(Collection<K> objects) {
            Set<K> objectsToUnlock = Sets.newLinkedHashSet(objects);
            for (K k : objectsToUnlock) {
                Lock l = locks.get(k);
                l.unlock();
            }
        }

    }

    @Test
    public void testMultiThreadedDaoCall() throws InterruptedException {

        final CountDownLatch waitForDaoCalledFromMultipleThreads = new CountDownLatch(2);
        final FakeDAO dao = new FakeDAO() {
            @Override
            public Map<Integer, Integer> getData(Collection<Integer> ids) {
                waitForDaoCalledFromMultipleThreads.countDown();
                try {
                    waitForDaoCalledFromMultipleThreads.await();
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
                Map<Integer, Integer> result = super.getData(ids);
                return result;
            }
        };
        final Cache<Integer, Integer> cache = Cache.create(dao, createCacheLogic(), 32, 64, 2);
        List<Integer>[] lists = new List[]{
            ImmutableList.of(1),
            ImmutableList.of(2)};

        List<Thread> threads = Lists.newArrayList();
        for (List<Integer> list : lists) {
            Thread thread = new Thread(() -> {
                FakeDAO.runQueryAndAssertData(cache, list);
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join(500);
            assertTrue("The cache does not support DAO access from multiple threads",
                    thread.getState().equals(Thread.State.TERMINATED));
        }
    }

    private static class FakeDAO implements DAO<Integer, Integer> {

        private static void runQueryAndAssertData(Cache<Integer, Integer> cache, Integer... keys) {
            runQueryAndAssertData(null, cache, keys);
        }

        private static void runQueryAndAssertData(Cache<Integer, Integer> cache, List<Integer> keys) {
            runQueryAndAssertData(null, cache, keys);
        }

        private static void runQueryAndAssertData(String msgPrefix, Cache<Integer, Integer> cache, Integer... keys) {
            runQueryAndAssertData(msgPrefix, cache, Lists.newArrayList(keys));
        }

        private static void runQueryAndAssertData(String msgPrefix, Cache<Integer, Integer> cache, List<Integer> keys) {
            Set<Integer> keysSet = Sets.newHashSet(keys);
            Map<Integer, Integer> data = cache.getData(keysSet);
            for (Integer k : keysSet) {
                Preconditions.checkState(k.equals(data.get(k)),
                        calcMsgPrefix(msgPrefix) + "wrong data returned for key " + k);
            }
        }

        private static String calcMsgPrefix(String msgPrefix) {
            String prefix = msgPrefix == null ? "" : msgPrefix + ", ";
            return prefix;
        }

        static FakeDAO create() {
            return new FakeDAO();
        }

        protected FakeDAO() {
        }

        private final AtomicInteger daoTouches = new AtomicInteger();
        private final Map<Integer, AtomicInteger> idTouches = Maps.newConcurrentMap();

        void assertDaoTouches(int touches) {
            assertDaoTouches(null, touches);
        }

        void assertDaoTouches(String msgPrefix, int touches) {
            assertEquals(calcMsgPrefix(msgPrefix) + "wrong number of DAO touches",
                    touches, getDaoTouches());

        }

        void assertKeyTouches(int key, int touches) {
            assertKeyTouches(null, key, touches);
        }

        void assertKeyTouches(String msgPrefix, int key, int touches) {
            assertEquals(calcMsgPrefix(msgPrefix) + "wrong number of touches for key " + key,
                    touches, getKeyTouches(key));
        }

        int allKeyTouches() {
            int result = 0;
            for (AtomicInteger value : idTouches.values()) {
                result += value.get();
            }
            return result;
        }

        private int getDaoTouches() {
            return daoTouches.get();
        }

        private int getKeyTouches(Integer id) {
            return putIfAbsentAndReturn(id).get();
        }

        @Override
        public Map<Integer, Integer> getData(Collection<Integer> ids) {
            LOG.log(Level.INFO, "Retrieving data for IDs {0}", ids);
            if (ids.isEmpty()) {
                return Collections.emptyMap();
            }

            daoTouches.incrementAndGet();

            Map<Integer, Integer> result = Maps.newHashMap();
            for (Integer id : ids) {
                putIfAbsentAndReturn(id).incrementAndGet();
                result.put(id, id);
            }
            return result;
        }

        private AtomicInteger putIfAbsentAndReturn(Integer id) {
            AtomicInteger newValue = new AtomicInteger();
            AtomicInteger result = idTouches.putIfAbsent(id, newValue);
            return result == null ? newValue : result;
        }

    }

}
