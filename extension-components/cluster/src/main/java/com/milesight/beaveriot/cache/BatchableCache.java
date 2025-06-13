package com.milesight.beaveriot.cache;

import java.util.List;

/**
 * @author leon
 */
public interface BatchableCache {

    List<Object> multiGet(Object[] key) ;

    Long multiEvict(Object[] key);

}
