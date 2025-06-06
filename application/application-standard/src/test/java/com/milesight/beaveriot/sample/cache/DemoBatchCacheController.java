package com.milesight.beaveriot.sample.cache;

import com.google.common.collect.Lists;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheEvict;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCachePut;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheable;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCaching;
import com.milesight.beaveriot.entity.po.EntityPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author leon
 */
@Slf4j
@RestController
@RequestMapping("/public/batchcache")
public class DemoBatchCacheController {

    static final String CACHE_NAME = "demo:cache-batch";

    @GetMapping("/putcache")
    @BatchCachePut(cacheNames = CACHE_NAME, key = "#result.id")
    public List<EntityPO> putcache(@RequestParam("keys") String[] keys){
        log.info("putcache:" + Arrays.toString(keys));

        return Arrays.stream(keys).map(key -> {
            EntityPO entityPO = new EntityPO();
            entityPO.setId(Long.parseLong(key));
            entityPO.setKey(key);
            entityPO.setName("name" + key);
            return entityPO;
        }).collect(Collectors.toList());
    }
    @GetMapping("/putcache-array")
    @BatchCachePut(cacheNames = CACHE_NAME, key = "#result.id")
    public EntityPO[] testPutArray(@RequestParam("keys") Collection<String> keysList){
        String[] keys = keysList.toArray(String[]::new);
        log.info("testPutArray:" + Arrays.toString(keys));

        return Arrays.stream(keys).map(key -> {
            EntityPO entityPO = new EntityPO();
            entityPO.setId(Long.parseLong(key));
            entityPO.setKey(key);
            entityPO.setName("name" + key);
            return entityPO;
        }).toArray(EntityPO[]::new);
    }

    @GetMapping("/putcache-map")
    @BatchCachePut(cacheNames = CACHE_NAME, key = "#result.id")
    public Map<String,EntityPO> testPutMap(@RequestParam("keys") Collection<String> keysList){
        String[] keys = keysList.toArray(String[]::new);
        log.info("testPutArray:" + Arrays.toString(keys));

        return testCacheableForMap(keysList);
    }

    @GetMapping("/getcache")
    @BatchCacheable(cacheNames = CACHE_NAME, key = "#p0")
    public List<EntityPO> testCacheable(@RequestParam("keys") Collection<String> keys){
        log.info("testCacheable:" + keys);
        return putcache(keys.toArray(new String[0]));
    }

    @GetMapping("/getcache-map")
    @BatchCacheable(cacheNames = CACHE_NAME, key = "#p0")
    public Map<String, EntityPO> testCacheableForMap(@RequestParam("keys") Collection<String> keys){
        log.info("testCacheable:" + keys);
        return putcache(keys.toArray(new String[0])).stream().map(
                    entityPO -> Map.entry(entityPO.getKey(), entityPO)
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @GetMapping("/evictcache")
    @BatchCacheEvict(cacheNames = CACHE_NAME, key = "#result.id")
    public List<EntityPO> testEvict(@RequestParam("keys") Collection<String> keys){
        log.info("testEvict:" + keys);
        return Lists.newArrayList(testPutArray(keys));
    }

    @GetMapping("/evictcache-before")
    @BatchCacheEvict(cacheNames = CACHE_NAME, key = "#p0", beforeInvocation = true)
    public List<EntityPO> testEvictBefore(@RequestParam("keys") Collection<String> keys){
        log.info("testEvict:" + keys);
        return Lists.newArrayList(testPutArray(keys));
    }


    @GetMapping("/multicache")
    @BatchCaching(
            cacheable = @BatchCacheable(cacheNames = CACHE_NAME, key = "#p0"),
            put = @BatchCachePut(cacheNames = CACHE_NAME, key = "#result.id")
    )
    public List<EntityPO> multicache(@RequestParam("keys") Collection<String> keys){
        log.info("multicache:" + keys);
        return Lists.newArrayList(testPutArray(keys));
    }
}
