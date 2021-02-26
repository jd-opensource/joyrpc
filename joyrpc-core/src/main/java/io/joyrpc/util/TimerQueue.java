package io.joyrpc.util;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.joyrpc.util.Timer.timer;

/**
 * 基于时间轮的任务队列，确保任务顺序执行
 */
public class TimerQueue implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(TimerQueue.class);

    /**
     * 名称
     */
    protected final String name;
    /**
     * 任务队列
     */
    protected final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    /**
     * 任务执行的拥有者
     */
    protected final AtomicBoolean owner = new AtomicBoolean();
    /**
     * 启动标识
     */
    protected final AtomicBoolean started = new AtomicBoolean(false);

    public TimerQueue(String name) {
        this.name = name;
    }

    /**
     * 打开
     */
    public void open() {
        started.compareAndSet(false, true);
    }

    @Override
    public void close() {
        started.compareAndSet(true, false);
    }

    /**
     * 是否打开状态
     *
     * @return 打开状态标识
     */
    public boolean isOpened() {
        return started.get();
    }

    /***
     * 添加任务，启动定时任务顺序执行，确保只有一个定时任务再执行，避免并发锁
     * @param task 任务
     */
    public void offer(final Runnable task) {
        if (task != null) {
            tasks.offer(task);
        }
        if (isOpened() && !tasks.isEmpty() && owner.compareAndSet(false, true)) {
            //添加定时任务
            timer().add(name, SystemClock.now(), () -> {
                //遍历任务执行
                Runnable runnable;
                while ((runnable = tasks.poll()) != null && isOpened()) {
                    //捕获异常，避免运行时
                    try {
                        runnable.run();
                    } catch (Throwable e) {
                        logger.error("Error occurs while running task . caused by " + e.getMessage(), e);
                    }
                }
                //清空任务标识
                owner.set(false);
                //再次进行判断，防止并发在清空标识之前放入了新的任务
                offer(null);
            });
        }
    }
}
