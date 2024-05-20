package com.xiaoyu.nuwa.promethus;

import com.xiaoyu.nuwa.promethus.counter.RequestCounter;
import com.xiaoyu.nuwa.promethus.histogram.RedisHistogram;

/**
 * 提供采集方法
 */
public class MetricUtils {

    /**
     * 统计com_request_in
     * 
     * @param uri
     */
    public static void comRequestIn(String uri) {
        RequestCounter.getInstance().handle(uri);
    }

    /**
     * 统计redis_request_in,不适合异步
     * 
     * @param uri
     */
    public static void redisRequestIn(long startTime, String command) {
        long cost = System.currentTimeMillis() - startTime;
        RedisHistogram.getInstance().handle(cost, command);
    }
}
