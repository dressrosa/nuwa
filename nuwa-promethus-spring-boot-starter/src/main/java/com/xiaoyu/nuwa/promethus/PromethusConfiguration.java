package com.xiaoyu.nuwa.promethus;

import javax.annotation.PreDestroy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.xiaoyu.nuwa.promethus.counter.RequestCounter;
import com.xiaoyu.nuwa.promethus.histogram.RedisHistogram;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConfigurationProperties(prefix = "nuwa.promethus")
@Slf4j
public class PromethusConfiguration {

    /**
     * 默认开启
     */
    private boolean open = true;

    private static HTTPServer httpServer;

    private static boolean Inited = false;

    public void setOpen(boolean open) {
        this.open = open;
    }

    public PromethusConfiguration() {
        start();
    }

    public void start() {
        if (!open) {
            return;
        }
        if (Inited) {
            return;
        }
        log.info("start Prometheus init.");
        try {
            httpServer = new HTTPServer(12306);
            doInit();
            log.info("end Prometheus init.");
        } catch (Exception e) {
            log.error("promethus start fail.", e);
        }
        Inited = true;

    }

    private void doInit() {
        // jvm相关指标
        DefaultExports.initialize();
        // com_request_in
        RequestCounter.getInstance().register();
        // redis_request_in
        try {
            if (Thread.currentThread().getContextClassLoader().loadClass("redis.clients.jedis.Jedis") != null) {
                RedisHistogram.getInstance().register();
            }
        } catch (ClassNotFoundException ignore) {
        }
    }

    @PreDestroy
    public void close() {
        if (httpServer != null) {
            log.info("close the promethues httpserver.");
            httpServer.stop();
        }
    }
}
