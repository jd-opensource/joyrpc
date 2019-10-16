package io.joyrpc.util;

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
     * 执行块
     */
    protected Runnable runnable;
    /**
     * 执行块
     */
    protected Consumer<Throwable> error;
    /**
     * 时间间隔
     */
    protected long interval;
    /**
     * 延迟执行时间
     */
    protected long delay;
    /**
     * 判断是否要继续
     */
    protected Supplier<Boolean> condition;
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
     * @param name     名称
     * @param runnable 执行块
     * @param interval 时间间隔
     */
    public Daemon(final String name, final Runnable runnable, final long interval) {
        this(name, runnable, null, interval, interval, null);
    }

    /**
     * 构造函数
     *
     * @param name     名称
     * @param runnable 执行块
     * @param error    异常消费者
     * @param interval 时间间隔
     */
    public Daemon(final String name, final Runnable runnable, final Consumer<Throwable> error, final long interval) {
        this(name, runnable, error, interval, interval, null);
    }

    /**
     * 构造函数
     *
     * @param name      名称
     * @param runnable  执行块
     * @param interval  时间间隔
     * @param condition 判断是否要继续
     */
    public Daemon(final String name, final Runnable runnable, final long interval, final Supplier<Boolean> condition) {
        this(name, runnable, null, interval, interval, condition);
    }

    /**
     * 构造函数
     *
     * @param name      名称
     * @param runnable  执行块
     * @param error     异常消费者
     * @param interval  时间间隔
     * @param condition 判断是否要继续
     */
    public Daemon(final String name, final Runnable runnable, final Consumer<Throwable> error, final long interval,
                  final Supplier<Boolean> condition) {
        this(name, runnable, error, interval, interval, condition);
    }

    /**
     * 构造函数
     *
     * @param name      名称
     * @param runnable  执行块
     * @param error     异常消费者
     * @param interval  时间间隔
     * @param delay     延迟执行的时间
     * @param condition 判断是否要继续
     */
    public Daemon(final String name, final Runnable runnable, final Consumer<Throwable> error, final long interval,
                  final long delay, final Supplier<Boolean> condition) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name can not be empty");
        } else if (runnable == null) {
            throw new IllegalArgumentException("runnable can not be empty");
        }
        this.name = name;
        this.runnable = runnable;
        this.error = error;
        this.interval = interval;
        this.delay = delay;
        this.condition = condition;
    }


    public void setDelay(long delay) {
        this.delay = delay;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * 是否启动标识
     *
     * @return
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
                boolean first = true;
                long time;
                while (continuous()) {
                    try {
                        //第一次运行
                        if (first) {
                            first = false;
                            time = delay;
                        } else {
                            time = interval;
                        }
                        if (time > 0) {
                            //等待一段时间
                            await(time);
                            //再次判断是否继续
                            if (continuous()) {
                                runnable.run();
                            }
                        } else {
                            runnable.run();
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Throwable e) {
                        if (error != null) {
                            error.accept(e);
                        }
                    }
                }
            }, name);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * 等待时间
     *
     * @param time
     * @throws InterruptedException
     */
    protected void await(long time) throws InterruptedException {
        Thread.sleep(time);
    }

    /**
     * 是否要继续
     *
     * @return
     */
    protected boolean continuous() {
        return started.get() && (condition == null || condition.get());
    }

    /**
     * 停止
     */
    public void stop() {
        if (started.compareAndSet(true, false)) {
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
    }

}
