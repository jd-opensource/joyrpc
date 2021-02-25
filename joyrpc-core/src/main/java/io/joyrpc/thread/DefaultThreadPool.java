package io.joyrpc.thread;

import io.joyrpc.extension.Parametric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static io.joyrpc.constants.Constants.*;

/**
 * 线程池
 */
public class DefaultThreadPool extends ThreadPoolExecutor implements ThreadPool {
    protected static final Logger logger = LoggerFactory.getLogger(DefaultThreadPool.class);
    /**
     * 名称
     */
    protected String name;

    public DefaultThreadPool(String name,
                             int corePoolSize,
                             int maximumPoolSize,
                             long keepAliveTime,
                             TimeUnit unit,
                             BlockingQueue<Runnable> workQueue,
                             ThreadFactory threadFactory,
                             RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.name = name;
    }

    @Override
    public Map<String, Object> dump() {
        Map<String, Object> result = new HashMap(10);
        result.put("min", getCorePoolSize());
        result.put("max", getMaximumPoolSize());
        result.put("current", getPoolSize());
        result.put("active", getActiveCount());
        result.put("queue", getQueue().size());
        return result;
    }

    @Override
    public void configure(final Parametric parametric) {
        Integer core = parametric.getInteger(CORE_SIZE_OPTION.getName());
        if (core != null && core > 0) {
            int old = getCorePoolSize();
            if (core != old) {
                logger.info(String.format("Core pool size of %s is changed from %d to %d", name, old, core));
                setCorePoolSize(core);
            }
        }
        Integer max = parametric.getInteger(MAX_SIZE_OPTION.getName());
        if (max != null && max > 0) {
            int old = getMaximumPoolSize();
            if (max != old) {
                logger.info(String.format("Maximum pool size of %s is changed from %d to %d", name, old, max));
                setMaximumPoolSize(max);
            }
        }
        Long keepAliveTime = parametric.getLong(KEEP_ALIVE_TIME_OPTION.getName());
        if (keepAliveTime != null && keepAliveTime > 0) {
            long old = getKeepAliveTime(TimeUnit.MILLISECONDS);
            if (keepAliveTime != old) {
                logger.info(String.format("Keep alive time of %s is changed from %d(ms) to %d(ms)", name, old, keepAliveTime));
                setKeepAliveTime(keepAliveTime, TimeUnit.MILLISECONDS);
            }
        }
    }
}
