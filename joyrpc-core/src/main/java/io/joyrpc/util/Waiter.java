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

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 时间等待
 */
public interface Waiter {

    /**
     * 等待时间
     *
     * @param time     时间
     * @param timeUnit 时间单位
     * @throws InterruptedException
     */
    default void await(long time, TimeUnit timeUnit) throws InterruptedException {
        await(time, timeUnit, null);
    }

    /**
     * 等待时间
     *
     * @param time      时间
     * @param timeUnit  时间单位
     * @param condition 是否要等待
     * @throws InterruptedException
     */
    void await(long time, TimeUnit timeUnit, Supplier<Boolean> condition) throws InterruptedException;

    /**
     * 唤醒
     */
    void wakeup();

    /**
     * 采用对象的await等待
     */
    class MutexWaiter implements Waiter {

        protected Object mutex;

        /**
         * 构造函数
         */
        public MutexWaiter() {
            this(new Object());
        }

        /**
         * 构造函数
         *
         * @param mutex
         */
        public MutexWaiter(final Object mutex) {
            this.mutex = mutex == null ? new Object() : mutex;
        }

        @Override
        public void await(final long time, final TimeUnit timeUnit, final Supplier<Boolean> condition) throws InterruptedException {
            synchronized (mutex) {
                if (condition == null || condition.get()) {
                    mutex.wait(timeUnit.toMillis(time));
                }
            }
        }

        @Override
        public void wakeup() {
            synchronized (mutex) {
                mutex.notifyAll();
            }
        }

    }

    /**
     * 采用线程Sleep等待
     */
    class SleepWaiter implements Waiter {

        @Override
        public void await(final long time, final TimeUnit timeUnit, final Supplier<Boolean> condition) throws InterruptedException {
            if (condition == null || condition.get()) {
                Thread.sleep(timeUnit.toMillis(time));
            }
        }

        @Override
        public void wakeup() {

        }

    }
}
