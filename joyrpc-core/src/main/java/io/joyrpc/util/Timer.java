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

import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.thread.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.ENVIRONMENT;
import static io.joyrpc.constants.Constants.TIMER_THREADS;

/**
 * 时间轮调度器
 */
public class Timer {

    private final static Logger logger = LoggerFactory.getLogger(Timer.class);

    /**
     * 默认定时器
     */
    protected static volatile Timer timer;

    /**
     * 延迟队列
     */
    protected DelayQueue<Slot> queue;
    /**
     * 底层时间轮
     */
    protected TimeWheel timeWheel;
    /**
     * 过期任务执行线程
     */
    protected ExecutorService workerPool;
    /**
     * 轮询延迟队列获取过期任务线程
     */
    protected ExecutorService bossPool;
    /**
     * 放弃的任务
     */
    protected Queue<Task> cancels = new ConcurrentLinkedQueue<>();
    /**
     * 待分配的任务，防止并发
     */
    protected Queue<Task> flying = new ConcurrentLinkedQueue<>();
    /**
     * 待处理的任务计数
     */
    protected AtomicLong tasks = new AtomicLong(0);
    /**
     * 最大待处理任务
     */
    protected long maxTasks;
    /**
     * 任务执行完毕的消费者
     */
    protected Consumer<Task> afterRun;
    /**
     * 放弃的消费者
     */
    protected Consumer<Task> afterCancel;
    /**
     * 任务执行之前的消费者
     */
    protected Consumer<Task> beforeRun;

    /**
     * 构造函数
     *
     * @param tickTime      每一跳时间
     * @param ticks         时间轮有几条
     * @param workerThreads 工作线程数
     */
    public Timer(final long tickTime, final int ticks, final int workerThreads) {
        this(null, tickTime, ticks, workerThreads, 0);
    }

    /**
     * 构造函数
     *
     * @param name          名称
     * @param tickTime      每一跳时间
     * @param ticks         时间轮有几条
     * @param workerThreads 工作线程数
     */
    public Timer(final String name, final long tickTime, final int ticks, final int workerThreads) {
        this(name, tickTime, ticks, workerThreads, 0);
    }

    /**
     * 构造函数
     *
     * @param name          名称
     * @param tickTime      每一跳时间
     * @param ticks         时间轮有几条
     * @param workerThreads 工作线程数
     * @param maxTasks      最大待处理任务
     */
    public Timer(final String name, final long tickTime, final int ticks, final int workerThreads, final long maxTasks) {
        if (tickTime <= 0) {
            throw new IllegalArgumentException("tickTime must be greater than 0");
        } else if (ticks <= 0) {
            throw new IllegalArgumentException("ticks must be greater than 0");
        } else if (workerThreads <= 0) {
            throw new IllegalArgumentException("workerThreads must be greater than 0");
        }
        this.maxTasks = maxTasks;
        this.afterRun = o -> tasks.decrementAndGet();
        this.afterCancel = this::cancel;
        this.beforeRun = this::supply;
        this.queue = new DelayQueue<>();
        this.timeWheel = new TimeWheel(tickTime, ticks, SystemClock.now(), queue);
        String prefix = name == null || name.isEmpty() ? "timer" : name;
        this.workerPool = Executors.newFixedThreadPool(workerThreads, new NamedThreadFactory(prefix + "-worker", true));
        this.bossPool = Executors.newFixedThreadPool(1, new NamedThreadFactory(prefix + "-boss", true));
        this.bossPool.submit(() -> {
            while (!Shutdown.isShutdown()) {
                try {
                    //拉取一跳时间
                    Slot slot = queue.poll(timeWheel.tickTime, TimeUnit.MILLISECONDS);
                    if (!Shutdown.isShutdown()) {
                        //处理放弃的任务
                        cancel();
                        //添加新增的任务，如果当前任务已经过期则立刻执行，否则放入后续的槽中
                        supply();
                        if (slot != null) {
                            //推进一跳
                            timeWheel.advance(slot.expiration);
                            //执行任务
                            slot.flush(beforeRun);
                        } else {
                            //推进一跳
                            timeWheel.advance(timeWheel.now + timeWheel.tickTime);
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                    break;
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw e;
                }
            }
        });
    }

    /**
     * 获取默认的Timer
     *
     * @return
     */
    public static Timer timer() {
        if (timer == null) {
            synchronized (Timer.class) {
                if (timer == null) {
                    Parametric parametric = new MapParametric(GlobalContext.getContext());
                    timer = new Timer("default", 200, 300,
                            parametric.getPositive(TIMER_THREADS, Math.min(ENVIRONMENT.get().cpuCores() * 2 + 2, 10)));
                }
            }
        }
        return timer;
    }

    /**
     * 放弃任务
     */
    protected void cancel() {
        Task task;
        //移除放弃的任务
        while ((task = cancels.poll()) != null) {
            //放弃任务，进入队列之前已经修改了计数器，这里不需要再处理。
            task.remove();
        }
    }

    /**
     * 添加任务
     */
    protected void supply() {
        Task task;
        //添加任务，1跳最多10万次
        for (int i = 0; i < 100000; i++) {
            task = flying.poll();
            if (task == null) {
                break;
            }
            if (!task.isCancelled()) {
                supply(task);
            }
        }
    }

    /**
     * 添加任务
     *
     * @param task 任务
     * @return
     */
    protected void supply(final Task task) {
        //添加失败任务直接执行
        if (!timeWheel.add(task)) {
            workerPool.submit(task);
        }
    }

    /**
     * 添加任务，至少需要一跳
     *
     * @param name     名称
     * @param time     任务执行时间
     * @param runnable 执行任务
     * @return
     */
    public Timeout add(final String name, final long time, final Runnable runnable) {
        return runnable == null ? null : add(new Task(name, timeWheel.getLeastOneTick(time), runnable, afterRun, afterCancel));
    }

    /**
     * 添加任务，至少需要一条
     *
     * @param task 任务
     * @return
     */
    public Timeout add(final TimeTask task) {
        return task == null ? null : add(new Task(task.getName(), timeWheel.getLeastOneTick(task.getTime()), task, afterRun, afterCancel));
    }

    /**
     * 添加任务
     *
     * @param task 任务
     * @return
     */
    protected Timeout add(final Task task) {
        if (maxTasks > 0 && tasks.incrementAndGet() > maxTasks) {
            tasks.decrementAndGet();
            throw new RejectedExecutionException("the maximum of pending tasks is " + maxTasks);
        }
        flying.add(task);
        return task;
    }

    /**
     * 放弃任务
     *
     * @param task
     */
    protected void cancel(final Task task) {
        tasks.decrementAndGet();
        cancels.add(task);
    }

    /**
     * 时间轮
     */
    protected static class TimeWheel {
        /**
         * 一跳的时间
         */
        protected long tickTime;
        /**
         * 有几跳
         */
        protected int ticks;
        /**
         * 周期
         */
        protected long duration;
        /**
         * 当前时间，是tickTime的整数倍
         */
        protected long now;
        /**
         * 当前槽的位置
         */
        protected int index;
        /**
         * 延迟队列
         */
        protected DelayQueue<Slot> queue;
        /**
         * 时间槽
         */
        protected Slot[] slots;
        /**
         * 下一层时间轮
         */
        protected TimeWheel next;

        /**
         * 时间轮
         *
         * @param tickTime
         * @param ticks
         * @param now
         * @param queue
         */
        public TimeWheel(final long tickTime, final int ticks, final long now, final DelayQueue<Slot> queue) {
            this.tickTime = tickTime;
            this.ticks = ticks;
            this.duration = ticks * tickTime;
            this.slots = new Slot[ticks];
            //当前时间为一跳的整数倍
            this.now = now - (now % tickTime);
            this.queue = queue;
            for (int i = 0; i < ticks; i++) {
                slots[i] = new Slot();
            }
        }

        public long getDuration() {
            return duration;
        }

        /**
         * 创建或者获取下一层时间轮
         */
        protected TimeWheel getNext() {
            if (next == null) {
                next = new TimeWheel(duration, ticks, now, queue);
            }
            return next;
        }

        /**
         * 获取至少一跳的时间
         *
         * @return
         */
        public long getLeastOneTick(final long time) {
            long result = SystemClock.now() + tickTime;
            return Math.max(time, result);
        }

        /**
         * 添加任务到时间轮
         */
        public boolean add(final Task task) {
            long time = task.getTime() - now;
            if (time < tickTime) {
                //过期任务直接执行
                return false;
            } else if (time < duration) {
                //该任务在一个时间轮里面，则加入到对应的时间槽
                int count = (int) (time / tickTime);
                Slot slot = slots[(count + index) % ticks];
                //添加到槽里面
                if (slot.add(task, now + count * tickTime) == Slot.HEAD) {
                    queue.offer(slot);
                }
                return true;
            } else {
                //放到下一层的时间轮
                return getNext().add(task);
            }
        }

        /**
         * 推进时间
         */
        public void advance(final long timestamp) {
            if (timestamp >= now + tickTime) {
                now = timestamp - (timestamp % tickTime);
                index++;
                if (index >= ticks) {
                    index = 0;
                }
                if (next != null) {
                    //推进下层时间轮时间
                    next.advance(timestamp);
                }
            }
        }
    }

    /**
     * 任务
     */
    protected static class Task implements Runnable, Timeout {
        protected static final int INIT = 0;
        protected static final int CANCELLED = 1;
        protected static final int EXPIRED = 2;
        protected static final AtomicIntegerFieldUpdater<Task> STATE_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(Task.class, "state");

        /**
         * 名称
         */
        protected String name;
        /**
         * 执行的时间
         */
        protected long time;
        /**
         * 任务
         */
        protected Runnable runnable;
        /**
         * 执行完毕的消费者
         */
        protected Consumer<Task> afterRun;
        /**
         * 放弃的消费者
         */
        protected Consumer<Task> afterCancel;
        /**
         * 时间槽
         */
        protected Slot slot;
        /**
         * 下一个节点
         */
        protected Task next;
        /**
         * 上一个节点
         */
        protected Task pre;
        /**
         * 状态
         */
        protected volatile int state = INIT;


        /**
         * 构造函数
         *
         * @param name
         * @param time
         * @param runnable
         * @param afterRun
         * @param afterCancel
         */
        public Task(final String name, final long time, final Runnable runnable,
                    final Consumer<Task> afterRun,
                    final Consumer<Task> afterCancel) {
            this.time = time;
            this.name = name;
            this.runnable = runnable;
            this.afterRun = afterRun;
            this.afterCancel = afterCancel;
            this.slot = null;
            this.next = null;
            this.pre = null;
        }

        protected long getTime() {
            return time;
        }

        @Override
        public String toString() {
            return name == null || name.isEmpty() ? super.toString() : name;
        }

        @Override
        public void run() {
            if (STATE_UPDATER.compareAndSet(this, INIT, EXPIRED)) {
                runnable.run();
                if (afterRun != null) {
                    afterRun.accept(this);
                }
            }
        }

        @Override
        public boolean isExpired() {
            return state == EXPIRED;
        }

        @Override
        public boolean isCancelled() {
            return state == CANCELLED;
        }

        @Override
        public boolean cancel() {
            if (STATE_UPDATER.compareAndSet(this, INIT, CANCELLED)) {
                if (afterCancel != null) {
                    afterCancel.accept(this);
                }
                return true;
            }
            return false;
        }

        /**
         * 移除
         */
        void remove() {
            if (slot != null) {
                slot.remove(this);
            }
        }
    }

    /**
     * 时间槽
     */
    protected static class Slot implements Delayed {

        public static final int HEAD = 1;
        public static final int TAIL = 2;

        /**
         * 过期时间
         */
        protected long expiration = -1L;
        /**
         * 根节点
         */
        protected Task root = new Task("root", -1L, null, null, null);

        /**
         * 构造函数
         */
        public Slot() {
            root.pre = root;
            root.next = root;
        }

        /**
         * 新增任务
         *
         * @param task   任务
         * @param expire 新的过期时间
         * @return
         */
        protected int add(final Task task, final long expire) {
            task.slot = this;
            Task tail = root.pre;
            task.next = root;
            task.pre = tail;
            tail.next = task;
            root.pre = task;
            if (expiration == -1L) {
                expiration = expire;
                return HEAD;
            }
            return TAIL;
        }

        /**
         * 移除任务
         *
         * @param task
         */
        protected void remove(final Task task) {
            task.next.pre = task.pre;
            task.pre.next = task.next;
            task.slot = null;
            task.next = null;
            task.pre = null;
        }

        /**
         * 当前槽已经过期，执行任务
         *
         * @param consumer 消费者
         */
        protected void flush(final Consumer<Task> consumer) {
            List<Task> ts = new LinkedList<>();
            Task task = root.next;
            while (task != root) {
                remove(task);
                ts.add(task);
                task = root.next;
            }
            expiration = -1L;
            ts.forEach(consumer);
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            long delayMs = expiration - SystemClock.now();
            return Math.max(0, unit.convert(delayMs, TimeUnit.MILLISECONDS));
        }

        @Override
        public int compareTo(final Delayed o) {
            return o instanceof Slot ? Long.compare(expiration, ((Slot) o).expiration) : 0;
        }
    }

    /**
     * 任务
     */
    public interface TimeTask extends Runnable {
        /**
         * 任务名称
         *
         * @return
         */
        String getName();

        /**
         * 执行时间
         *
         * @return
         */
        long getTime();
    }

    /**
     * 超时对象
     */
    public interface Timeout {

        /**
         * 是否过期了
         *
         * @return
         */
        boolean isExpired();

        /**
         * 是否放弃了
         *
         * @return
         */
        boolean isCancelled();

        /**
         * 放弃
         *
         * @return
         */
        boolean cancel();
    }

    /**
     * 代理任务
     */
    public static class DelegateTask implements TimeTask {
        /**
         * 名称
         */
        protected String name;
        /**
         * 时间
         */
        protected long time;
        /**
         * 执行代码
         */
        protected Runnable runnable;

        /**
         * 构造函数
         *
         * @param name     名称
         * @param time     时间
         * @param runnable 执行代码
         */
        public DelegateTask(final String name, final long time, final Runnable runnable) {
            this.name = name;
            this.time = time;
            this.runnable = runnable;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public void run() {
            if (runnable != null) {
                runnable.run();
            }
        }
    }
}
