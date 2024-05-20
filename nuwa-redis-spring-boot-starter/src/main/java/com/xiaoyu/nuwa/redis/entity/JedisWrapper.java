package com.xiaoyu.nuwa.redis.entity;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;

@Setter
@Getter
public class JedisWrapper {
    private JedisConfig jedisConfig;
    private JedisSentinelPool JedisSentinelPool;
    private JedisPool JedisPool;
}