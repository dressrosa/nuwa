package com.xiaoyu.nuwa.promethus.counter;

import com.xiaoyu.nuwa.promethus.CustomCollector;

import io.prometheus.client.Counter;
import io.prometheus.client.SimpleCollector;

/**
 * 统计com_request_in
 */
public class RequestCounter implements CustomCollector {

    private SimpleCollector<Counter.Child> metric;

    private static final RequestCounter instance = new RequestCounter();

    private RequestCounter() {

    }

    public static RequestCounter getInstance() {
        return instance;
    }

    @Override
    public void register() {
        instance.metric = Counter.build()
                .name(this.name())
                .labelNames(this.labels())
                .help("count the request uri.")
                .register();
    }

    @Override
    public String name() {
        return "com_request_in";

    }

    @Override
    public String[] labels() {
        String[] arr = { "uri" };
        return arr;
    }

    @Override
    public void handle(String... label) {
        if (metric != null) {
            for (String s : label) {
                if (s != null && !s.isEmpty()) {
                    metric.labels(s).inc();
                }
            }
        }
    }

}
