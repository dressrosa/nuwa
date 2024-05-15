package com.xiaoyu.nuwa.utils.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存内.适用于key是固定的或变化不多的通用性的缓存信息.<br>
 * 缓存采用的是过期不删除的机制,所以不适用于key是动态变化的,比如userId这种数量太多的.
 */
public class SimpleLocalCacheUtils {

    /**
     * 缓存类型->(缓存key,缓存value)
     */
    private static final Map<String, Map<String, CacheEntity>> cacheMap = new ConcurrentHashMap<>();

    /**
     * 默认过期时间 10分钟
     */
    private static long defaultExpireTime = 600;

    private static final String DefaultCacheType = "default";

    public static void setCache(String cacheKey, Object cacheValue) {
        setCache(DefaultCacheType, cacheKey, cacheValue, -1);
    }

    public static void setCache(String cacheKey, Object cacheValue, long expireSeconds) {
        setCache(DefaultCacheType, cacheKey, cacheValue, expireSeconds);
    }

    public static void setCache(String cacheType, String cacheKey, Object cacheValue, long expireSeconds) {
        Map<String, CacheEntity> map = cacheMap.get(cacheType);
        if (map == null) {
            cacheMap.putIfAbsent(cacheType, new HashMap<>());
        }
        long expire = expireSeconds < 1 ? defaultExpireTime * 1000 : expireSeconds * 1000;
        CacheEntity cache = new CacheEntity(System.currentTimeMillis() + expire, cacheValue);
        cacheMap.get(cacheType).put(cacheKey, cache);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCache(String cacheType, String cacheKey) {
        Map<String, CacheEntity> map = cacheMap.get(cacheType);
        if (map == null) {
            return null;
        }
        CacheEntity cache = map.get(cacheKey);
        if (cache == null) {
            return null;
        }
        if (cache.isExpire()) {
            return null;
        }
        return (T) cache.getValue();
    }

    public static CacheEntity getCacheEntity(String cacheType, String cacheKey) {
        Map<String, CacheEntity> map = cacheMap.get(cacheType);
        if (map == null) {
            return null;
        }
        CacheEntity cache = map.get(cacheKey);
        if (cache == null) {
            return null;
        }
        return cache;
    }

    public static <T> T getCache(String cacheKey) {
        return getCache(DefaultCacheType, cacheKey);
    }

    public static void removeCache(String cacheKey) {
        removeCache(DefaultCacheType, cacheKey);
    }

    public static void removeCache(String cacheType, String cacheKey) {
        Map<String, CacheEntity> map = cacheMap.get(cacheType);
        if (map == null) {
            return;
        }
        map.remove(cacheKey);
    }
}
