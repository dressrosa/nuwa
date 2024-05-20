/**
 * 
 */
package com.xiaoyu.nuwa.redis;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * redis锁 <blockquote>
 *
 * <pre>
 *RedisLock lock = RedisLock.getLock("lock_key");
 * try {
 *  if (lock.lock(10)) {
 *   // do something
 *     }
 * } finally {
 *    lock.unlock();
 *  }
 *        </blockquote>
 * </pre>
 *
 * @author xiaoyu
 *
 *
 */
/**
 * 
 */
@Slf4j
public class RedisLock {

    public static final String LOCK_LUA_SCRIPT = "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then redis.call('expire', KEYS[1], ARGV[2]) return 'true' else return 'false' end";
    public static final String UNLOCK_LUA_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then redis.call('del', KEYS[1])  return 'true' else return 'false' end";

    // 加锁sha1值
    private static String Lock_lua_sha1 = "";
    // 解锁sha1值
    private static String Unlock_lua_sha1 = "";

    private static final String DEFAULT_VALUE = "0";

    private static final String SUCCESS = "true";

    private String lockKey;

    private boolean isLocked = false;

    private RedisLock() {

    }

    private RedisLock(String lockKey) {
        this.lockKey = lockKey;
    }

    /**
     * 创建锁
     *
     * @param lockKey
     * @return
     */
    public static RedisLock getLock(String lockKey) {
        return new RedisLock(lockKey);
    }

 
    /**
     * 初始化枷锁脚本到redis服务器上
     */
    protected static void initScriptSha() {
        try (Jedis jedis = JedisUtils.getRedis()) {
            String sha = jedis.scriptLoad(LOCK_LUA_SCRIPT);
            if (sha != null && !sha.isEmpty()) {
                Lock_lua_sha1 = sha;
            }
            sha = jedis.scriptLoad(UNLOCK_LUA_SCRIPT);
            if (sha != null && !sha.isEmpty()) {
                Unlock_lua_sha1 = sha;
            }
        }
    }

    /**
     * 加锁
     *
     * @param lockSecond 锁定时长 单位秒
     * @return
     */
    public boolean lock(int lockSecond) {
        if (lockSecond < 1) {
            return false;
        }
        try (Jedis jedis = JedisUtils.getRedis()) {
           // Object result = jedis.eval(LOCK_LUA_SCRIPT, Arrays.asList(lockKey),  Arrays.asList(DEFAULT_VALUE, lockSecond + ""));
            Object result = jedis.evalsha(Lock_lua_sha1, Arrays.asList(lockKey),  Arrays.asList(DEFAULT_VALUE, lockSecond + ""));
            isLocked = SUCCESS.equals(result);
            return isLocked;
        }
    }

    /**
     * 加锁 尝试获取锁,如果没获取到,则trySecond内每500ms一直尝试获取
     *
     * @param trySecond
     * @param lockSecond 锁定时长
     * @return
     */
    public boolean tryLock(int trySecond, int lockSecond) {
        if (trySecond < 1) {
            return this.lock(lockSecond);
        }
        boolean flag = this.lock(lockSecond);
        if (flag) {
            return true;
        }
        // 每500ms循环一次检查
        int count = trySecond << 1;
        try {
            for (int i = 0; i < count; i++) {
                TimeUnit.MILLISECONDS.sleep(500);
                flag = this.lock(lockSecond);
                if (flag) {
                    return true;
                }
            }
        } catch (InterruptedException e) {
            log.error("", e);
        }
        return false;
    }

    /**
     * 释放锁
     */
    public void unlock() {
        if (!isLocked) {
            return;
        }
        try (Jedis jedis = JedisUtils.getRedis()) {
           // jedis.eval(UNLOCK_LUA_SCRIPT, Arrays.asList(lockKey), Arrays.asList(DEFAULT_VALUE));
            jedis.evalsha(Unlock_lua_sha1, Arrays.asList(lockKey), Arrays.asList(DEFAULT_VALUE));
        }
    }


}