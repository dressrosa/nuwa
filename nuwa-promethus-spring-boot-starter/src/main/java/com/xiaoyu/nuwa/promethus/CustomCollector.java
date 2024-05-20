package com.xiaoyu.nuwa.promethus;

public interface CustomCollector {

    void register();

    String name();

    String[] labels();

    default void handle(String... label) {
    }

    /**
     * @param cost  毫秒
     * @param label
     */
    default void handle(long cost, String... label) {
    }
}
