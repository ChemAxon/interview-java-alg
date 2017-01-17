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
import java.util.Map;

import com.chemaxon.dao.spi.DAO;
import com.chemaxon.dif.cache.spi.CacheLogic;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

public final class NonEffectiveCacheLogic<K extends Comparable<K>, V> implements CacheLogic<K, V>{

    @Override
    public Map<K, V> getData(Collection<K> keys, DAO<K, V> dao, ConcurrentLinkedHashMap<K, V> cache) {
        return dao.getData(keys);
    }

}
