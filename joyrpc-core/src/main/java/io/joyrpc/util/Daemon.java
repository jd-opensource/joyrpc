package io.joyrpc.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 守护线程
 */
public class Daemon {

    /**
     * 名称
     */
    protected String name;
    /**
     * 准备
     */
    protected Runnable prepare;
    /**
     * 执行块
     */
    protected Callable<Waiting> callable;
    /**
     * 执行块
     */
    protected Consumer<Throwable> error;
    /**
     * 延迟执行时间
     */
    protected long delay;
    /**
     * 容错时间
     */
    protected long fault;
    /**
     * 判断是否要继续
     */
    protected Supplier<Boolean> condition;
    /**
     * 时间等待
     */
    protected Waiter waiter;
    /**
     * 线程
     */
    protected Thread thread;
    /**
     * 启动标识
     */
    protected AtomicBoolean started = new AtomicBoolean();

    /**
     * 构造函数
     *
     * @param name      名称
     * @param runnable  执行块
     * @param interval  时间间隔
     * @param delay     延迟执行的时间
     * @param error     异常消费者
     * @param fault     异常等待时间
     * @param condition 判断是否要继续
     * @param waiter    等待对象
     */
    public Daemon(final String name, final Runnable prepare, final Runnable runnable, final long interval, final long delay,
                  final Consumer<Throwable> error, final long fault,
                  final Supplier<Boolean> condition, final Waiter waiter) {
        this(name, prepare,
                () -> {
                    runnable.run();
                    return new Waiting(interval);
                },
                delay, error, fault, condition, waiter);
    }

    /**
     * 构造函数
     *
     * @param name      名称
     * @param prepare   准备
     * @param callable  执行块，返回下一次等到时间
     * @param delay     延迟执行的时间
     * @param error     异常消费者
     * @param fault     异常等待时间
     * @param condition 判断是否要继续
     * @param waiter    等待对象
     */
    public Daemon(final String name, final Runnable prepare, final Callable<Waiting> callable, final long delay,
                  final Consumer<Throwable> error, final long fault,
                  final Supplier<Boolean> condition,
                  final Waiter waiter) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name can not be empty");
        } else if (callable == null) {
            throw new IllegalArgumentException("callable can not be null");
        }
        this.name = name;
        this.prepare = prepare;
        this.callable = callable;
        this.error = error;
        this.delay = delay;
        this.fault = fault;
        this.condition = condition;
        this.waiter = waiter == null ? new Waiter.SleepWaiter() : waiter;
    }

    /**
     * 是否启动标识
     *
     * @return 启动标识
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * 启动
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            thread = new Thread(() -> {
                //准备
                if (prepare != null) {
                    prepare.run();
                }
                Waiting waiting = new Waiting(delay);
                while (continuous()) {
                    try {
                        //判断是否要等等
                        if (waiting.getTime() > 0) {
                            //等待一段时间
                            waiter.await(waiting.time, TimeUnit.MILLISECONDS, waiting.getCondition());
                            //再次判断是否继续
                            if (continuous()) {
                                waiting = callable.call();
                            }
                        } else {
                            waiting = callable.call();
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Throwable e) {
                        if (error != null) {
                            error.accept(e);
                        }
                        waiting = new Waiting(fault);
                    }
                }
            }, name);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * 是否要继续
     *
     * @return 继续标识
     */
    protected boolean continuous() {
        return started.get() && (condition == null || condition.get());
    }

    /**
     * 停止
     */
    public void stop() {
        if (started.compareAndSet(true, false)) {
            //唤醒等待线程
            waiter.wakeup();
            if (thread != null) {
                //线程终止
                thread.interrupt();
                thread = null;
            }
        }
    }

    /**
     * 构建构造器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器
     */
    public static final class Builder {
        /**
         * 名称
         */
        protected String name;
        /**
         * 准备
         */
        protected Runnable prepare;
        /**
         * 执行块
         */
        protected Runnable runnable;
        /**
         * 执行块
         */
        protected Callable<Waiting> callable;
        /**
         * 执行块
         */
        protected Consumer<Throwable> error;
        /**
         * 延迟执行时间
         */
        protected Long delay;
        /**
         * 执行间隔
         */
        protected Long interval;
        /**
         * 容错时间
         */
        protected Long fault;
        /**
         * 判断是否要继续
         */
        protected Supplier<Boolean> condition;
        /**
         * 时间等待
         */
        protected Waiter waiter;

        public Builder() {
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder prepare(Runnable val) {
            prepare = val;
            return this;
        }

        public Builder runnable(Runnable val) {
            runnable = val;
            return this;
        }

        public Builder callable(Callable<Waiting> val) {
            callable = val;
            return this;
        }

        public Builder error(Consumer<Throwable> val) {
            error = val;
            return this;
        }

        public Builder delay(long val) {
            delay = val;
            return this;
        }

        public Builder interval(long val) {
            interval = val;
            if (fault == null) {
                fault = val;
            }
            if (delay == null) {
                delay = fault;
            }
            return this;
        }

        public Builder fault(long val) {
            fault = val;
            return this;
        }

        public Builder condition(Supplier<Boolean> val) {
            condition = val;
            return this;
        }

        public Builder waiter(Waiter val) {
            waiter = val;
            return this;
        }

        public Daemon build() {
            return callable != null ? new Daemon(name, prepare, callable, delay == null ? 0 : delay, error, fault == null ? 0 : fault, condition, waiter) :
                    new Daemon(name, prepare, runnable, interval == null ? 0 : interval, delay == null ? 0 : delay, error, fault == null ? 0 : fault, condition, waiter);
        }

    }

    /**
     * 等待对象
     */
    public static final class Waiting {
        /**
         * 等待时间
         */
        protected long time;
        /**
         * 判断条件
         */
        protected Supplier<Boolean> condition;

        public Waiting(long time) {
            this.time = time;
        }

        public Waiting(long time, Supplier<Boolean> condition) {
            this.time = time;
            this.condition = condition;
        }

        public long getTime() {
            return time;
        }

        public Supplier<Boolean> getCondition() {
            return condition;
        }
    }
}
