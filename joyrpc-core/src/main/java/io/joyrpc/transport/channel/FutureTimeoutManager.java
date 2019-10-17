package io.joyrpc.transport.channel;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.thread.NamedThreadFactory;
import io.joyrpc.util.Shutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @date: 2019/2/28
 */
public class FutureTimeoutManager implements Runnable {
    protected static final Logger logger = LoggerFactory.getLogger(FutureTimeoutManager.class);
    protected static final FutureTimeoutManager INSTANCE = new FutureTimeoutManager();
    protected ScheduledExecutorService scheduler;
    protected Set<Runnable> runnables = new CopyOnWriteArraySet<>();
    protected AtomicLong time = new AtomicLong();
    protected AtomicLong count = new AtomicLong();

    /**
     * 构造函数
     */
    protected FutureTimeoutManager() {
        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("FutureTimoutManager", true));
        scheduler.scheduleWithFixedDelay(this, 200, 200, TimeUnit.MILLISECONDS);
        Shutdown.addHook(scheduler::shutdown);
    }

    @Override
    public void run() {
        //开始时间
        long start = System.nanoTime();
        //执行任务
        for (Runnable runnable : runnables) {
            runnable.run();
        }
        //统计平均执行时间
        long t = time.addAndGet(System.nanoTime() - start);
        long c = count.incrementAndGet();
        if (c % 100 == 0) {
            long time = t / c / 1000;
            if (time > 100) {
                //超过100纳秒
                logger.info(String.format("future timeout checking run avg time: %d(ns)", time));
            }
        }

    }

    /**
     * 注册
     *
     * @param runnable
     */
    public void register(final Runnable runnable) {
        if (runnable != null) {
            runnables.add(runnable);
        }
    }

    /**
     * 注销
     *
     * @param runnable
     */
    public void deregister(final Runnable runnable) {
        if (runnable != null) {
            runnables.remove(runnable);
        }
    }

}
