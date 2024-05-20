package com.xiaoyu.nuwa.redis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.xiaoyu.nuwa.redis.entity.JedisConfig;
import com.xiaoyu.nuwa.redis.entity.JedisWrapper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;

@Configuration
@ConfigurationProperties(prefix = "nuwa.redis")
@Getter
@Setter
@Order(1)
@Slf4j
public class JedisConfiguration {

    private List<JedisConfig> configs;

    @Bean(name = "jedisPool")
    public List<JedisWrapper> jedisConfig() {
        if (configs == null || configs.isEmpty()) {
            log.debug("No redis config info be found, the init operation will not to do. ");
            return new ArrayList<>(0);
        }
        List<JedisWrapper> wrappers = new ArrayList<>(configs.size());
        for (JedisConfig a : configs) {
            if (!a.isOpen()) {
                continue;
            }
            JedisWrapper wrapper = new JedisWrapper();
            wrapper.setJedisConfig(a);
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMinIdle(a.getMinIdle());
            config.setMaxIdle(a.getMaxIdle());
            config.setMaxTotal(a.getMaxTotal());
            config.setTestOnBorrow(a.getTestOnBorrow());
            config.setTestOnReturn(a.getTestOnReturn());
            config.setTestWhileIdle(a.getTestWhileIdle());
            config.setMaxWaitMillis(a.getMaxWaitMillis());
            config.setTimeBetweenEvictionRunsMillis(a.getTimeBetweenEvictionRunsMillis());
            boolean isSentinel = hasLength(a.getMasterName());
            String password = hasLength(a.getPassword()) ? a.getPassword() : null;
            if (isSentinel) {
                Set<String> sentinels = new HashSet<>(Arrays.asList(a.getHost().split(",")));
                JedisSentinelPool pool = new JedisSentinelPool(a.getMasterName(), sentinels, config,
                        Protocol.DEFAULT_TIMEOUT, password, a.getDb());
                wrapper.setJedisSentinelPool(pool);
            } else {
                String[] hostPortArr = a.getHost().split(":");
                JedisPool pool = new JedisPool(config, hostPortArr[0], Integer.valueOf(hostPortArr[1]),
                        Protocol.DEFAULT_TIMEOUT, password, a.getDb());
                wrapper.setJedisPool(pool);
            }
            wrappers.add(wrapper);
        }
        // 初始化jedisutils
        JedisUtils.setPool(wrappers);
        // 初始化分布式锁脚本
        RedisLock.initScriptSha();
        return wrappers;
    }

    private static boolean hasLength(String str) {
        return str != null && !str.isEmpty();
    }
}
