package com.milesight.beaveriot.sample.cache;

import com.google.common.collect.Lists;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheEvict;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCachePut;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheable;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCaching;
import com.milesight.beaveriot.base.annotations.cacheable.CacheKeys;
import com.milesight.beaveriot.entity.po.EntityPO;
import lombok.extern.slf4j.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.context.constants.CacheKeyConstants.TENANT_PREFIX;

/**
 * @author leon
 */
@Slf4j
@RestController
@RequestMapping("/public/batchcache")
public class DemoBatchCacheController {

    static final String CACHE_NAME = "demo:cache-batch";
    @GetMapping("/putcache")
    @BatchCachePut(cacheNames = CACHE_NAME, key = "#p0" , keyPrefix = TENANT_PREFIX)
    public List<EntityPO> putcache(@RequestParam("keys") Collection<String> keys){
        log.info("putcache:" + Arrays.toString(keys.toArray()));

        return keys.stream().map(key -> {
            EntityPO entityPO = new EntityPO();
            entityPO.setId(Long.parseLong(key));
            entityPO.setKey(key);
            entityPO.setName("name" + key);
            return entityPO;
        }).collect(Collectors.toList());
    }
    @GetMapping("/putcache-array")
    @BatchCachePut(cacheNames = CACHE_NAME, key = "#result.![id]", keyPrefix = TENANT_PREFIX)
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
    @BatchCachePut(cacheNames = CACHE_NAME, key = "#result.![key]", keyPrefix = TENANT_PREFIX)
    public Map<String,EntityPO> testPutMap(@RequestParam("keys") List<String> keysList){
        String[] keys = keysList.toArray(String[]::new);
        log.info("testPutArray:" + Arrays.toString(keys));

        return testCacheableForMap(keysList, null);
    }

    @GetMapping("/getcache")
    @BatchCacheable(cacheNames = CACHE_NAME, key = "#p0", keyPrefix = TENANT_PREFIX)
    public List<EntityPO> testCacheable(@RequestParam("keys") List<String> keys){
        log.info("testCacheable:" + keys);
        return putcache(keys);
    }

    @PostMapping("/getcache-map")
    @BatchCacheable(cacheNames = CACHE_NAME, key = "#p0", keyPrefix = TENANT_PREFIX)
    public Map<String, EntityPO> testCacheableForMap(@CacheKeys @RequestParam("keys") List<String> keys, @RequestBody String body){
        log.info("testCacheable:{}, body:{}" , keys, body );
        return putcache(keys).stream().map(
                    entityPO -> Map.entry(entityPO.getKey(), entityPO)
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @PostMapping("/getcache-map-obj")
    @BatchCacheable(cacheNames = CACHE_NAME, key = "#p0.![key]", keyPrefix = TENANT_PREFIX)
    public Map<String, EntityPO> testCacheableForMapObj(@CacheKeys @RequestParam("keys") List<EntityPO> keys, @RequestBody String body){
        log.info("testCacheable:{}, body:{}" , keys, body );
        return putcache(keys.stream().map(item->item.getKey()).toList()).stream().map(
                entityPO -> Map.entry(entityPO.getKey(), entityPO)
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @GetMapping("/evictcache")
    @BatchCacheEvict(cacheNames = CACHE_NAME, key = "#result.![id]", keyPrefix = TENANT_PREFIX)
    public List<EntityPO> testEvict(@RequestParam("keys") Collection<String> keys){
        log.info("testEvict:" + keys);
        return Lists.newArrayList(testPutArray(keys));
    }

    @GetMapping("/evictcache-before")
    @BatchCacheEvict(cacheNames = CACHE_NAME, key ="#p0" , beforeInvocation = true, keyPrefix = TENANT_PREFIX)
    public List<EntityPO> testEvictBefore(@RequestParam("keys") Collection<String> keys){
        log.info("testEvict:" + keys);
        return Lists.newArrayList(testPutArray(keys));
    }

    @PostMapping("/evictcache-before2")
    @BatchCacheEvict(cacheNames = CACHE_NAME, key = "#p0.![key]", beforeInvocation = true, keyPrefix = TENANT_PREFIX)
    public List<EntityPO> testEvictBefore2(@RequestBody Collection<EntityPO> keys, Object body){
        log.info("testEvict:" + keys);
        return null;
    }



    @GetMapping("/multicache")
    @BatchCaching(
            cacheable = @BatchCacheable(cacheNames = CACHE_NAME, key = "#p0"),
            put = @BatchCachePut(cacheNames = CACHE_NAME, key = "#result.![id + 'aaa']")
    )
    public List<EntityPO> multicache(@RequestParam("keys") Collection<String> keys){
        log.info("multicache:" + keys);
        return Lists.newArrayList(testPutArray(keys));
    }
}
