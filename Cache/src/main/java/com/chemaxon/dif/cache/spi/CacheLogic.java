/*
 * Copyright (c) 1998-2017 ChemAxon Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * ChemAxon. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon.
 */
package com.chemaxon.dif.cache.spi;

import java.util.Collection;
import java.util.Map;

import com.chemaxon.dao.spi.DAO;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * Interface for the caching logic. The must use the provided DAO for loading data.
 *
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
public interface CacheLogic<K extends Comparable<K>, V> {

    /**
     * The implementation should not waist with the resources of the DAO.
     *
     * @param keys IDs to retrieve data for.
     * @param dao The DAO to use for retrieving data.
     * @param cache reference for the actual cache. No other code will work with this instance of cache.
     * @return The data.
     */
    Map<K, V> getData(Collection<K> keys, DAO<K, V> dao, ConcurrentLinkedHashMap<K, V> cache);

}
