/*
 * Copyright (c) 1998-2017 ChemAxon Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * ChemAxon. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon.
 */
package com.chemaxon.dif.cache.api;


import java.util.Collection;
import java.util.Map;

import com.chemaxon.dao.spi.DAO;
import com.chemaxon.dif.cache.spi.CacheLogic;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

public final class Cache<K extends Comparable<K>, V> {

    /**
     * 
     * @param <K> The key type
     * @param <V> The value type
     * @param dao DAO for retrieving data
     * @param logic The caching logic
     * @param initialSize The initial size of the cache.
     * @param maximumSize The maximum size of the cache.
     * @param concurrencyLevel The estimated number of concurrently updating threads
     * @return 
     */
    public static <K extends Comparable<K>, V> Cache<K, V> create(DAO<K, V> dao,
            CacheLogic<K, V> logic,
            int initialSize,
            int maximumSize,
            int concurrencyLevel) {
        return new Cache<>(dao, logic, initialSize, maximumSize, concurrencyLevel);
    }

    private final ConcurrentLinkedHashMap<K, V> cache;
    private final DAO<K, V> dao;
    private final CacheLogic<K, V> logic;

    private Cache(DAO<K, V> dao,
            CacheLogic<K, V> logic,
            int initialSize,
            int maximumSize,
            int concurrencyLevel) {
        this.dao = dao;
        this.logic = logic;
        this.cache = new ConcurrentLinkedHashMap.Builder<K, V>()
                .initialCapacity(initialSize)
                .maximumWeightedCapacity(maximumSize)
                .concurrencyLevel(concurrencyLevel)
                .build();
        
    }

    public Map<K, V> getData(Collection<K> ids) {
        return logic.getData(ids, dao, cache);
    }

}
