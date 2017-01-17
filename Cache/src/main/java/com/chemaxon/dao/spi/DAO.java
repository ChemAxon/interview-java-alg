/*
 * Copyright (c) 1998-2017 ChemAxon Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * ChemAxon. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon.
 */
package com.chemaxon.dao.spi;

import java.util.Collection;
import java.util.Map;

public interface DAO<K extends Comparable<K>, V> {

    /**
     * Returns the mapping from key to value. The value cannot be null.
     * @param ids IDs
     * @return The mapping. Values cannot be null;
     */
    Map<K, V> getData(Collection<K> ids);

}
