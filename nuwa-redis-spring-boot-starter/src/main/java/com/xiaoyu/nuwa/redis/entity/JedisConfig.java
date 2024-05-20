package com.xiaoyu.nuwa.redis.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JedisConfig {
    // jedis实例名 默认 default
    private String name = "default";
    // 是否开启
    private boolean open = true;
    // 地址
    private String host;
    // 哨兵模式需要填写master
    private String masterName;
    // 密码
    private String password;
    // 指定db 默认0
    private int db = 0;
    // 最大空闲数
    private int maxIdle = 10;
    // 最小空闲数
    private int minIdle = 2;
    /**
     * 最大连接数
     */
    private int maxTotal = 10;
    // 在borrow一个jedis实例时，是否提前进行validate操作
    private Boolean testOnBorrow = false;
    // 在return给pool时，是否提前进行validate操作
    private Boolean testOnReturn = false;
    // 链接超时等待时间
    private long maxWaitMillis = 1000L;
    // 空闲是检查
    private Boolean testWhileIdle = false;
    // 空闲检查间隔
    private long timeBetweenEvictionRunsMillis = 60_000;
}