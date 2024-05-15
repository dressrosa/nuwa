package com.xiaoyu.nuwa.utils.executor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 以同一用户同一业务为例<br/>
 * 1.请求a到来,线程池corePoolSize之内,新生成线程请求外部;然后请求b到来,新生成线程请求,但是请求a线程需要停止
 * 2.请求a到来,线程池corePoolSize-maximumPoolSize之内,进入队列排队;然后请求b到来,进入队列排队,但是请求a线程需要停止
 * 3.同2,然后请求c到来,进入队列排队;请求b排队的任务需标识为needStop.
 * 
 * @author xiaoyu
 *
 */
@Slf4j
public class CommonThreadPoolExecutor extends ThreadPoolExecutor {

    public CommonThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    private boolean needInterrupt;

    public boolean isNeedInterrupt() {
        return needInterrupt;
    }

    public void setNeedInterrupt(boolean needInterrupt) {
        this.needInterrupt = needInterrupt;
    }

    /**
     * 单用户单业务类型只有一个任务在运行
     */
    private ConcurrentHashMap<String, Thread> runningKeyMap = new ConcurrentHashMap<>();

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        if (!needInterrupt) {
            return;
        }
        String runKey = null;
        if (r instanceof ExecutorCTask) {
            ExecutorCTask<?> task = ExecutorCTask.getCurrentTask(r);
            if (task != null) {
                runKey = task.getRunningKey();
            }
        } else if (r instanceof ExecutorRTask) {
            ExecutorRTask task = ExecutorRTask.getCurrentTask(r);
            if (task != null) {
                runKey = task.getRunningKey();
            }
        }
        if (StringUtils.isNotBlank(runKey)) {
            interruptThread(runKey);
            runningKeyMap.putIfAbsent(runKey, t);
        }
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (!needInterrupt) {
            return;
        }
        // FutureTask会在结束的时候情况置空callable,这里r=null无法使用submit的方式执行线程 因此改用InsFutureTask
        super.afterExecute(r, t);
        String runKey = null;
        if (r instanceof ExecutorCTask) {
            ExecutorCTask<?> task = ExecutorCTask.getCurrentTask(r);
            if (task != null) {
                runKey = task.getRunningKey();
            }
        } else if (r instanceof ExecutorRTask) {
            ExecutorRTask task = ExecutorRTask.getCurrentTask(r);
            if (task != null) {
                runKey = task.getRunningKey();
            }
        }
        if (StringUtils.isNotBlank(runKey)) {
            synchronized (runKey.intern()) {
                runningKeyMap.remove(runKey);
            }
        }
    }

    public <V> Future<V> submit(ExecutorCTask<V> task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        if (needInterrupt) {
            // 还在排队的task直接设置无法运行 已经在运行的task直接interrupt
            BlockingQueue<Runnable> qu = super.getQueue();
            qu.forEach(r -> {
                ExecutorCTask<?> t = ExecutorCTask.getCurrentTask(r);
                if (t != null && task.equals(t)) {
                    t.stop();
                }
            });
            interruptThread(task.getRunningKey());
        }
        CommonFutureTask<V> ftask = new CommonFutureTask<V>(task);
        super.execute(ftask);
        return ftask;
    }

    public void execute(ExecutorRTask task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        if (needInterrupt) {
            // 还在排队的task直接设置无法运行 已经在运行的task直接interrupt
            BlockingQueue<Runnable> qu = super.getQueue();
            qu.forEach(r -> {
                ExecutorRTask t = ExecutorRTask.getCurrentTask(r);
                if (t != null && task.equals(t)) {
                    t.stop();
                }
            });
            interruptThread(task.getRunningKey());
        }
        super.execute(task);
    }

    private void interruptThread(String runningKey) {
        if (StringUtils.isBlank(runningKey)) {
            return;
        }
        Thread predecessor = runningKeyMap.get(runningKey);
        if (predecessor != null) {
            synchronized (runningKey.intern()) {
                predecessor = runningKeyMap.get(runningKey);
                if (predecessor != null) {
                    log.debug("thread be interupted by runningKey->{}", runningKey);
                    // 这里只提供线程中断 ,建议线程任务里面使用可中断的io模式
                    predecessor.interrupt();
                }
            }
        }
    }

}