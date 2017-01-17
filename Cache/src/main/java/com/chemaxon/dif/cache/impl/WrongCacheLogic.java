/*
 * Copyright (c) 1998-2017 ChemAxon Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * ChemAxon. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon.
 */
package com.chemaxon.dif.cache.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.chemaxon.dao.spi.DAO;
import com.chemaxon.dif.cache.spi.CacheLogic;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

// TODO what is the problem with this implementation?
public final class WrongCacheLogic<K extends Comparable<K>, V> implements CacheLogic<K, V> {

    @Override
    public Map<K, V> getData(Collection<K> keys, DAO<K, V> dao, ConcurrentLinkedHashMap<K, V> cache) {
        Map<K, V> result = Maps.newHashMap();
        List<K> toLoad = Lists.newArrayList();
        for (K key : keys) {
            V v = cache.get(key);
            if (v != null) {
                result.put(key, v);
            } else {
                toLoad.add(key);
            }
        }

        Map<K, V> data = dao.getData(toLoad);
        cache.putAll(data);
        result.putAll(data);
        return result;
    }

}
