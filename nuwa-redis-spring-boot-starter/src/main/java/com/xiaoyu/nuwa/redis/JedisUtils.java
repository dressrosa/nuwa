package com.xiaoyu.nuwa.redis;

import java.util.ArrayList;
import java.util.List;

import com.xiaoyu.nuwa.promethus.MetricUtils;
import com.xiaoyu.nuwa.redis.entity.JedisWrapper;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * 
 * 提供jedis常用默认工具类
 * 
 * @author xiaoyu
 * 
 */
@Slf4j
public class JedisUtils {

    private static List<JedisWrapper> Wrappers = new ArrayList<>();

    protected static void setPool(List<JedisWrapper> wappers) {
        if (wappers != null && !wappers.isEmpty()) {
            Wrappers.addAll(wappers);
        }
    }

    /**
     * 返回默认的redis
     * 
     * @return
     */
    public static Jedis getRedis() {
        return getRedis("default");
    }

    /**
     * 返回指定的redis
     * 
     * @param name
     * @return
     */
    public static Jedis getRedis(String name) {
        List<JedisWrapper> wrappers = Wrappers;
        int size = wrappers.size();
        if (size == 0) {
            log.warn("not found any redis instance,please check your config.");
            return null;
        }
        if (size == 1) {
            return doGetRedis(Wrappers.get(0));
        }
        for (JedisWrapper w : Wrappers) {
            if (w.getJedisConfig().getName().equals(name)) {
                return doGetRedis(w);
            }
        }
        return null;
    }

    private static Jedis doGetRedis(JedisWrapper wrapper) {
        long start = System.currentTimeMillis();
        Jedis jedis = null;
        try {
            if (wrapper.getJedisSentinelPool() != null) {
                jedis = wrapper.getJedisSentinelPool().getResource();
            } else {
                jedis = wrapper.getJedisPool().getResource();
            }
        } finally {
            // promethus
            MetricUtils.redisRequestIn(start, "getRedis");
        }
        return jedis;
    }

    public static boolean exists(String key) {
        try (Jedis jedis = getRedis()) {
            boolean result = jedis.exists(key);
            return result;
        }
    }

    public static String get(String key) {
        try (Jedis jedis = getRedis()) {
            String result = jedis.get(key);
            return result;
        }
    }

    public static long del(String key) {
        try (Jedis jedis = getRedis()) {
            long result = jedis.del(key);
            return result;
        }
    }

    public static String setex(String key, String value, int cacheSeconds) {
        try (Jedis jedis = getRedis()) {
            String result = jedis.setex(key, cacheSeconds, value);
            return result;
        }
    }

    public static long setnx(String key, String value) {
        try (Jedis jedis = getRedis()) {
            long result = jedis.setnx(key, value);
            return result;
        }
    }

    public static long incr(String key) {
        try (Jedis jedis = getRedis()) {
            long result = jedis.incr(key);
            return result;
        }
    }

}
