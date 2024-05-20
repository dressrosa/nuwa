package com.xiaoyu.nuwa.promethus.histogram;

import com.xiaoyu.nuwa.promethus.CustomCollector;

import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector;

/**
 * 统计redis_request_in
 */
public class RedisHistogram implements CustomCollector {

    private SimpleCollector<Histogram.Child> metric = null;

    private static final RedisHistogram instance = new RedisHistogram();

    private RedisHistogram() {

    }

    public static RedisHistogram getInstance() {
        return instance;
    }

    @Override
    public void register() {
        instance.metric = Histogram.build()
                .name(this.name())
                .labelNames(this.labels())
                .help("count the redis request info.")
                .buckets(1, 2, 4, 6, 8, 10, 20, 40, 60, 100, 300, 800, 2000, 5000)
                .register();
    }

    @Override
    public String name() {
        return "redis_request_in";

    }

    @Override
    public String[] labels() {
        String[] arr = { "command" };
        return arr;
    }

    @Override
    public void handle(long cost, String... label) {
        if (metric != null) {
            for (String s : label) {
                if (s != null && !s.isEmpty()) {
                    metric.labels(s).observe(cost);
                }
            }
        }
    }

}
