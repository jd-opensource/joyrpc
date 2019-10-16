package io.joyrpc.util;

import java.util.concurrent.TimeUnit;

/**
 * 时间等待
 */
public interface Waiter {

    /**
     * 等待时间
     *
     * @param time
     * @param timeUnit
     * @throws InterruptedException
     */
    void await(long time, TimeUnit timeUnit) throws InterruptedException;

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
        public void await(final long time, final TimeUnit timeUnit) throws InterruptedException {
            synchronized (mutex) {
                mutex.wait(timeUnit.toMillis(time));
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
        public void await(final long time, final TimeUnit timeUnit) throws InterruptedException {
            Thread.sleep(timeUnit.toMillis(time));
        }

        @Override
        public void wakeup() {

        }

    }
}
