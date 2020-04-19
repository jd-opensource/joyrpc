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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 系统时钟，替换 {@link System#currentTimeMillis()} ，防止CPU切换频繁
 * <p/>
 */
public class SystemClock {

    protected static final SystemClock instance = new SystemClock();
    // 精度(毫秒)
    protected long precision;
    // 当前时间
    protected volatile long now;
    // 调度任务
    protected ScheduledExecutorService scheduler;

    public static SystemClock getInstance() {
        return instance;
    }

    public SystemClock() {
        this(1L);
    }

    public SystemClock(long precision) {
        this.precision = precision;
        now = System.currentTimeMillis();
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SystemClock");
            thread.setDaemon(true);
            return thread;
        });
        //TODO 时钟调整问题
        scheduler.scheduleAtFixedRate(() -> {
            now = System.currentTimeMillis();
        }, precision, precision, TimeUnit.MILLISECONDS);
    }

    public long getTime() {
        return now;
    }

    public long precision() {
        return precision;
    }

    /**
     * 获取当前时钟
     *
     * @return 当前时钟
     */
    public static long now() {
        return instance.getTime();
    }

}
