package com.xiaoyu.nuwa.utils.executor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 请求线程池
 * 
 * @author xiaoyu
 *
 */
@Slf4j
public class CommonExecutors {

    private static final ConcurrentHashMap<String, CommonThreadPoolExecutor> executorMap = new ConcurrentHashMap<>();

    private static final int cupSize = Runtime.getRuntime().availableProcessors();

    private static final int DefaultMinThreadSize = cupSize;
    private static final int DefaultMaxThreadSize = cupSize;

    /**
     * 标识线程池停止 不再接收生成新的线程池
     */
    private static volatile boolean shutdowning = false;

    // 大量qps会集中
    // 不同业务各自采用一个线程池 保证业务隔离干扰
    // 可动态调节线程池参数
    // io型 Nthreads = Ncpu x Ucpu x (1 + W/C)，其中
    // Ncpu = CPU核心数
    // Ucpu = CPU使用率，0~1
    // W/C = 等待时间与计算时间的比率
    // 8*0.6*(1+6000/50) = 580
    public static CommonThreadPoolExecutor getExecutor(String executorName, String bizType) {
        String key = getKey(executorName, bizType);
        if (!executorMap.containsKey(key) && !shutdowning) {
            executorMap.putIfAbsent(key, createExecutor(executorName, bizType));
        }
        CommonThreadPoolExecutor exe = executorMap.get(key);
        if (exe == null) {
            throw new NullPointerException();
        }
        return exe;
    }

    private static CommonThreadPoolExecutor createExecutor(String executorName, String bizType) {
        String key = getKey(executorName, bizType);
        CommonThreadPoolExecutor executor = new CommonThreadPoolExecutor(DefaultMinThreadSize, DefaultMaxThreadSize,
                100L, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, key + "_thread_" + r.hashCode());
                    }
                }, new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        throw new RuntimeException("the task be rejected");
                    }
                });
        return executor;
    }

    /**
     * 
     * 调整线程池属性
     * 
     * @param executorName
     * @param bizType
     * @param paramType
     * @param paramValue
     */
    public static String adjustExecutor(String executorName, String bizType, String paramType, int paramValue) {
        String key = getKey(executorName, bizType);
        CommonThreadPoolExecutor executor = executorMap.get(key);
        if (executor == null) {
            return "execuotor not exist";
        }
        return doAdjustExecutor(executor, paramType, paramValue);
    }

    private static String doAdjustExecutor(CommonThreadPoolExecutor executor, String paramType, int paramValue) {
        if ("corePoolSize".equals(paramType)) {
            int old = executor.getCorePoolSize();
            if (old != paramValue) {
                executor.setCorePoolSize(paramValue);
            }
            return "old:" + old + ";new:" + paramValue;
        } else if ("maximumPoolSize".equals(paramType)) {
            int old = executor.getMaximumPoolSize();
            if (old != paramValue) {
                executor.setMaximumPoolSize(paramValue);
            }
            return "old:" + old + ";new:" + paramValue;
        } else if ("prestartCoreThread".equals(paramType)) {
            int f = 0;
            for (int i = 0; i < paramValue; i++) {
                f = i;
                if (!executor.prestartCoreThread()) {
                    break;
                }
            }
            return "need:" + paramValue + ";success:" + f;
        } else if ("needInterrupt".equals(paramType)) {
            boolean old = executor.isNeedInterrupt();
            executor.setNeedInterrupt(paramValue > 0);
            return "old:" + old + ";new:" + (paramValue > 0);
        }
        return "paramType[" + paramType + "] not exist";
    }

    private static String getKey(String executorName, String bizType) {
        return executorName + "_" + bizType;
    }

    @PreDestroy
    public void shutdown() {
        shutdowning = true;
        log.info("start shutdown executor.");
        executorMap.values().forEach(a -> {
            try {
                a.shutdown();
            } catch (Exception e) {
                log.error("", e);
            }
        });
        executorMap.clear();
        shutdowning = false;
        log.info("complete shutdown executor.");
    }

    public static String allStat() {
        StringBuilder sb = new StringBuilder();
        executorMap.entrySet().forEach(en -> {
            sb.append(en.getKey() + ":statistc(" + statistic(en.getKey()) + ")\n");
        });
        String re = sb.toString();
        if (StringUtils.isBlank(re)) {
            return "no data";
        }
        return re;
    }

    public static String statistic(String executorName, String bizType) {
        return statistic(getKey(executorName, bizType));
    }

    private static String statistic(String key) {
        CommonThreadPoolExecutor exe = executorMap.get(key);
        if (exe == null) {
            return "executor not exist";
        }
        int corePoolSize = exe.getCorePoolSize();
        int maximumPoolSize = exe.getMaximumPoolSize();
        int activeCount = exe.getActiveCount();
        int queueSize = exe.getQueue().size();
        boolean needInterrupt = exe.isNeedInterrupt();
        String stat = "corePoolSize:" + corePoolSize + ";" + "maximumPoolSize:" + maximumPoolSize + ";" + "activeCount:"
                + activeCount + ";" + "queueSize:" + queueSize + ";" + "needInterrupt:" + needInterrupt;
        return stat;
    }
}