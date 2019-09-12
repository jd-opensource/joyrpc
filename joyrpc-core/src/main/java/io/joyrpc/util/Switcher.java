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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 开关
 */
public class Switcher {
    protected static final Logger logger = LoggerFactory.getLogger(Switcher.class);
    //启动标识
    protected AtomicBoolean started;
    /**
     * 打开的Future，用于异步方法重入
     */
    protected CompletableFuture openFuture;
    /**
     * 关闭的Future，用于异步方法重入
     */
    protected CompletableFuture closeFuture;
    /**
     * 读锁
     */
    protected Lock readLock;
    /**
     * 写锁
     */
    protected Lock writeLock;
    protected LockExecution reader;
    protected LockExecution writer;

    public Switcher() {
        this(new ReentrantReadWriteLock());
    }

    public Switcher(final ReadWriteLock lock) {
        this(lock.readLock(), lock.writeLock(), new AtomicBoolean());
    }

    public Switcher(final Lock readLock, final Lock writeLock) {
        this(readLock, writeLock, new AtomicBoolean());
    }

    public Switcher(final Lock readLock, final Lock writeLock, final AtomicBoolean started) {
        this.started = started == null ? new AtomicBoolean() : started;
        this.readLock = readLock;
        this.writeLock = writeLock;
        this.reader = new LockExecution(readLock, started);
        this.writer = new LockExecution(writeLock, started);
    }

    /**
     * 启动
     *
     * @param function 消费者
     * @return
     */
    public <T> CompletableFuture<T> open(final Function<CompletableFuture<T>, CompletableFuture<T>> function) {
        writeLock.lock();
        try {
            if (started.compareAndSet(false, true)) {
                openFuture = function == null ? new CompletableFuture<>() : function.apply(new CompletableFuture<>());
            }
            return openFuture;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 启动
     *
     * @param runnable
     */
    public void open(final Runnable runnable) {
        open(runnable, null);
    }

    /**
     * 启动
     *
     * @param runnable 启动成功的调用
     * @param ready    已经处于启动状态的调用
     */
    public void open(final Runnable runnable, final Runnable ready) {
        writeLock.lock();
        try {
            if (started.compareAndSet(false, true)) {
                if (runnable != null) {
                    runnable.run();
                }
            } else if (ready != null) {
                ready.run();
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 启动
     *
     * @param callable
     * @return
     * @throws Exception
     */
    public <T> T open(final Callable<T> callable) throws Exception {
        return open(callable, null);
    }

    /**
     * 启动
     *
     * @param callable 启动成功的调用
     * @param def      默认值，用于已经处于启动状态的返回值
     * @return
     * @throws Exception
     */
    public <T> T open(final Callable<T> callable, final T def) throws Exception {
        writeLock.lock();
        try {
            if(started.compareAndSet(false,true)){
                return callable == null ? def : callable.call();
            } else {
                logger.warn("maybe execute more than once ");
                return def;
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 启动
     *
     * @param callable 启动成功的调用
     * @return
     */
    public <T> T openQuiet(final Callable<T> callable) {
        return openQuiet(callable, null);
    }

    /**
     * 启动
     *
     * @param callable 启动成功的调用
     * @param def      默认值，用于已经处于启动状态的返回值
     * @return
     */
    public <T> T openQuiet(final Callable<T> callable, final T def) {
        try {
            return open(callable, def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * 关闭
     *
     * @param function 消费者
     * @return
     */
    public <T> CompletableFuture<T> close(final Function<CompletableFuture<T>, CompletableFuture<T>> function) {
        writeLock.lock();
        try {
            if (started.compareAndSet(true, false)) {
                closeFuture = function == null ? new CompletableFuture<>() : function.apply(new CompletableFuture<>());
            }
            return closeFuture;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 关闭
     *
     * @param runnable 关闭成功的调用
     */
    public void close(final Runnable runnable) {
        close(runnable, null);
    }

    /**
     * 关闭
     *
     * @param runnable 关闭成功的调用
     * @param ready    已经关闭的调用
     */
    public void close(final Runnable runnable, final Runnable ready) {
        writeLock.lock();
        try {
            if (started.compareAndSet(true, false)) {
                if (runnable != null) {
                    runnable.run();
                }
            } else if (ready != null) {
                ready.run();
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 关闭
     *
     * @param callable 关闭成功的调用
     * @return
     * @throws Exception
     */
    public <T> T close(final Callable<T> callable) throws Exception {
        return close(callable, null);
    }

    /**
     * 关闭
     *
     * @param callable 关闭成功的调用
     * @param def      默认值，用于已经处于关闭状态的返回值
     * @return
     * @throws Exception
     */
    public <T> T close(final Callable<T> callable, final T def) throws Exception {
        writeLock.lock();
        try {
            return started.compareAndSet(true, false) ? (callable == null ? def : callable.call()) : def;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 关闭
     *
     * @param callable 关闭成功的调用
     * @return
     */
    public <T> T closeQuiet(final Callable<T> callable) {
        return closeQuiet(callable, null);
    }

    /**
     * 关闭
     *
     * @param callable 关闭成功的调用
     * @param def      默认值，用于已经处于关闭状态的返回值
     * @return
     */
    public <T> T closeQuiet(final Callable<T> callable, final T def) {
        try {
            return close(callable, def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * 读执行器
     *
     * @return
     */
    public Execution reader() {
        return reader;
    }

    /**
     * 写执行器
     *
     * @return
     */
    public Execution writer() {
        return writer;
    }

    /**
     * 判断状态
     *
     * @return
     */
    public boolean isOpened() {
        return started.get();
    }


    /**
     * 执行器
     */
    public interface Execution {

        /**
         * 拿到锁，并确保在启动状态运行
         *
         * @param runnable
         */
        boolean run(Runnable runnable);

        /**
         * 拿到锁，在任何状态运行
         *
         * @param runnable
         * @return
         */
        void runAnyway(Runnable runnable);

        /**
         * 尝试拿到锁，并在启动状态执行
         *
         * @param runnable
         */
        boolean tryRun(Runnable runnable);

        /**
         * 尝试拿到锁，并在任何状态执行
         *
         * @param runnable
         */
        boolean tryRunAnyway(Runnable runnable);

        /**
         * 尝试拿到锁，并在启动状态执行
         *
         * @param runnable 执行块
         * @param timeout  拿锁的超时时间
         * @param timeUnit 时间单位
         */
        boolean tryRun(Runnable runnable, long timeout, TimeUnit timeUnit);

        /**
         * 尝试拿到锁，并在任何状态执行
         *
         * @param runnable 执行块
         * @param timeout  拿锁的超时时间
         * @param timeUnit 时间单位
         */
        boolean tryRunAnyway(Runnable runnable, long timeout, TimeUnit timeUnit);

        /**
         * 拿到锁，并确保在启动状态运行
         *
         * @param callable
         * @throws Exception
         */
        <T> T call(Callable<T> callable) throws Exception;

        /**
         * 拿到锁，并在任何状态运行
         *
         * @param callable
         * @throws Exception
         */
        <T> T callAnyway(Callable<T> callable) throws Exception;

        /**
         * 拿到锁，并确保在启动状态安静运行
         *
         * @param callable
         */
        <T> T quiet(Callable<T> callable);

        /**
         * 拿到锁，并在任何状态安静运行
         *
         * @param callable
         */
        <T> T quietAnyway(Callable<T> callable);

        /**
         * 拿到锁，并确保在启动状态运行
         *
         * @param supplier
         * @param <T>
         * @return
         */
        <T> T get(final Supplier<T> supplier);

        /**
         * 拿到锁，并在任何状态运行
         *
         * @param supplier
         * @param <T>
         * @return
         */
        <T> T getAnyway(final Supplier<T> supplier);

    }


    /**
     * 锁执行器
     */
    protected static class LockExecution implements Execution {
        protected Lock lock;
        protected AtomicBoolean state;

        public LockExecution(Lock lock, AtomicBoolean state) {
            this.lock = lock;
            this.state = state;
        }

        @Override
        public boolean run(final Runnable runnable) {
            if (runnable == null || !state.get()) {
                return false;
            }
            lock.lock();
            try {
                if (state.get()) {
                    runnable.run();
                    return true;
                }
            } finally {
                lock.unlock();
            }
            return false;
        }

        @Override
        public void runAnyway(Runnable runnable) {
            if (runnable == null) {
                return;
            }
            lock.lock();
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean tryRun(final Runnable runnable) {
            if (runnable == null || !state.get()) {
                return false;
            }
            if (lock.tryLock()) {
                try {
                    if (state.get()) {
                        runnable.run();
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            }
            return false;
        }

        @Override
        public boolean tryRunAnyway(Runnable runnable) {
            if (runnable == null) {
                return false;
            }
            if (lock.tryLock()) {
                try {
                    runnable.run();
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            return false;
        }

        @Override
        public boolean tryRun(final Runnable runnable, final long timeout, final TimeUnit timeUnit) {
            if (runnable == null || !state.get()) {
                return false;
            }
            boolean locked = false;
            try {
                if (lock.tryLock(timeout, timeUnit)) {
                    locked = true;
                    if (state.get()) {
                        runnable.run();
                        return true;
                    }
                }
            } catch (InterruptedException e) {
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }
            return false;
        }

        @Override
        public boolean tryRunAnyway(Runnable runnable, long timeout, TimeUnit timeUnit) {
            if (runnable == null) {
                return false;
            }
            boolean locked = false;
            try {
                if (lock.tryLock(timeout, timeUnit)) {
                    locked = true;
                    runnable.run();
                    return true;
                }
            } catch (InterruptedException e) {
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }
            return false;
        }

        @Override
        public <T> T call(final Callable<T> callable) throws Exception {
            if (callable == null || !state.get()) {
                return null;
            }
            lock.lock();
            try {
                return state.get() ? callable.call() : null;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public <T> T callAnyway(Callable<T> callable) throws Exception {
            if (callable == null) {
                return null;
            }
            lock.lock();
            try {
                return callable.call();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public <T> T quiet(final Callable<T> callable) {
            try {
                return call(callable);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public <T> T quietAnyway(Callable<T> callable) {
            try {
                return callAnyway(callable);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public <T> T get(final Supplier<T> supplier) {
            if (supplier == null || !state.get()) {
                return null;
            }
            lock.lock();
            try {
                return state.get() ? supplier.get() : null;
            } catch (Exception e) {
                return null;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public <T> T getAnyway(Supplier<T> supplier) {
            if (supplier == null) {
                return null;
            }
            lock.lock();
            try {
                return supplier.get();
            } catch (Exception e) {
                return null;
            } finally {
                lock.unlock();
            }
        }
    }
}
