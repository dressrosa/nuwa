package com.xiaoyu.nuwa.utils.executor;

import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;

import com.xiaoyu.nuwa.utils.LogUtils;
import com.xiaoyu.nuwa.utils.context.ContextUtil;
import com.xiaoyu.nuwa.utils.enums.LogLabelEnum;

/**
 * callable task
 * 
 * @author xiaoyu
 *
 */
public abstract class ExecutorCTask<V> implements Callable<V> {

    /**
     * 任务在未运行时可通过修改标识来改变状态 <br/>
     * 线程池的队列无法进行修改,所以对于在队列中等待的任务, <br/>
     * 如果再有同性质的任务进来,当前任务需要被标识停止, 等进入线程池运行的时候会忽略
     */
    private volatile boolean needStop;
    /**
     * 标识任务性质 可通过值来标识多个任务是否属于相同性质 <br/>
     * 比如传入用户id,即标识多个任务为同一用户的
     */
    private String runningKey;

    /**
     * 上下文信息
     */
    private Map<String, Object> contextMap;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExecutorCTask)) {
            return false;
        }
        if (StringUtils.isBlank(runningKey)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        ExecutorCTask<V> other = (ExecutorCTask<V>) obj;
        return runningKey.equals(other.runningKey);
    }

    public ExecutorCTask(String runningKey) {
        this.contextMap = ContextUtil.getAll();
        this.runningKey = runningKey;
    }

    public ExecutorCTask() {
        this.contextMap = ContextUtil.getAll();
    }

    public String getRunningKey() {
        return this.runningKey;
    }

    public Map<String, Object> getContextMap() {
        return this.contextMap;
    }

    public void stop() {
        this.needStop = true;
    }

    @Override
    public V call() throws Exception {
        if (needStop) {
            throw new RuntimeException("the task not need to run");
        }
        // 填充上下文信息
        ContextUtil.putAll(contextMap);
        // 填充trace
        LogUtils.initTrace(ContextUtil.getStandardContext(LogLabelEnum.PSPANID.getLabel()),
                ContextUtil.getStandardContext(LogLabelEnum.SPANID.getLabel()),
                ContextUtil.getStandardContext(LogLabelEnum.TRACEID.getLabel()));
        return this.doRun();
    }

    protected static ExecutorCTask<?> getCurrentTask(Runnable r) {
        if (r instanceof ExecutorCTask) {
            return (ExecutorCTask<?>) r;
        }
        @SuppressWarnings("unchecked")
        CommonFutureTask<ExecutorCTask<?>> task = (CommonFutureTask<ExecutorCTask<?>>) r;
        ExecutorCTask<?> callable = (ExecutorCTask<?>) task.callable();
        return callable;
    }

    public abstract V doRun();
}