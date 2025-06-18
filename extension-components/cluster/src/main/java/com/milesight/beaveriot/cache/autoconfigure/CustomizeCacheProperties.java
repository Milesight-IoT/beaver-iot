package com.milesight.beaveriot.cache.autoconfigure;

import com.milesight.beaveriot.base.constants.StringConstant;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author leon
 */
@Data
@ConfigurationProperties(prefix = "spring.cache")
public class CustomizeCacheProperties {

    public static final String DEFAULT_CACHE = "defaultName";

    /**
     * Exactly specify the TTL of the Redis Key
     */
    private RedisConfig redis = new RedisConfig();

    private Specs specs = new Specs();


    @Data
    public static class Specs {

        /**
         * Default expiration time (unit s) of the cache.
         */
        @Nullable
        public Duration timeToLive;

        /**
         * Precisely set the expiration time (unit s) of the corresponding cache, and use timeToLive global configuration when it is not configured. Support prefix matching, for example: demo:key1:*
         */
        public Map<String, Duration> timeToLives = new LinkedHashMap<>();

        @Nullable
        public Duration getMatchTimeToLive(String key){
            //Since timeToLives may have a prefix match, take the prefix[]
            String matchKey = timeToLives.keySet().stream().filter(k->{
                String unwrapKey = unwrapPrefixAndSubfix(k);
                if(unwrapKey.endsWith(StringConstant.STAR)){
                    return key.startsWith(unwrapKey.substring(0, unwrapKey.length() - 1));
                }else{
                    return key.equals(unwrapKey);
                }
            }).findFirst().orElse(null);
            return matchKey == null ? timeToLive : timeToLives.get(matchKey);
        }

        public static String unwrapPrefixAndSubfix(String key) {
            if(key.startsWith(StringConstant.BRACKET_START) && key.endsWith(StringConstant.BRACKET_END)){
                return key.substring(1, key.length() - 1);
            }else{
                return key;
            }
        }
    }
    @Data
    public static class RedisConfig{

        public String valueSerializerClass;

    }

}
