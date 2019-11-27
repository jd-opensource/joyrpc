package io.joyrpc.util;

import io.joyrpc.thread.NamedThreadFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 时间轮调度器
 */
public class Timer {
    /**
     * 延迟队列
     */
    protected DelayQueue<Slot> queue;
    /**
     * 底层时间轮
     */
    protected TimeWheel timeWheel;
    /**
     * 工作线程数
     */
    protected int workerThreads;
    /**
     * 过期任务执行线程
     */
    protected ExecutorService workerPool;
    /**
     * 轮询延迟队列获取过期任务线程
     */
    protected ExecutorService bossPool;

    /**
     * 构造函数
     *
     * @param tickTime      每一跳时间
     * @param ticks         时间轮有几条
     * @param workerThreads 工作线程数
     */
    public Timer(final long tickTime, final int ticks, final int workerThreads) {
        this(null, tickTime, ticks, workerThreads);
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
        if (tickTime <= 0) {
            throw new IllegalArgumentException("tickTime must be greater than 0");
        } else if (ticks <= 0) {
            throw new IllegalArgumentException("ticks must be greater than 0");
        } else if (workerThreads <= 0) {
            throw new IllegalArgumentException("workerThreads must be greater than 0");
        }
        this.workerThreads = workerThreads;
        this.queue = new DelayQueue<>();
        this.timeWheel = new TimeWheel(tickTime, ticks, SystemClock.now(), queue);
        String prefix = name == null || name.isEmpty() ? "timer" : name;
        this.workerPool = Executors.newFixedThreadPool(workerThreads, new NamedThreadFactory(prefix + "-worker", true));
        this.bossPool = Executors.newFixedThreadPool(1, new NamedThreadFactory(prefix + "-boss", true));
        this.bossPool.submit(() -> {
            while (!Shutdown.isShutdown()) {
                try {
                    //停止的时候要及时中断，把timeWheel.getDuration()改成了1000ms
                    Slot slot = queue.poll(1000L, TimeUnit.MILLISECONDS);
                    if (slot != null && !Shutdown.isShutdown()) {
                        //推进时间
                        timeWheel.advance(slot.getExpiration());
                        //执行任务
                        slot.flush(this::add);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    /**
     * 添加任务
     *
     * @param name     名称
     * @param time     任务执行时间
     * @param runnable 执行任务
     */
    public void add(final String name, final long time, final Runnable runnable) {
        if (runnable == null) {
            return;
        }
        add(new Task(name, time, runnable));
    }

    /**
     * 添加任务
     *
     * @param task
     */
    public void add(final TimeTask task) {
        if (task != null) {
            add(task.getName(), task.getTime(), task);
        }
    }

    /**
     * 添加任务，至少需要一跳
     *
     * @param name     名称
     * @param time     任务执行时间
     * @param runnable 执行任务
     */
    public void addLeastOneTick(final String name, final long time, final Runnable runnable) {
        if (runnable == null) {
            return;
        }
        long nextTickTime = timeWheel.getNextTickTime();
        add(new Task(name, time < nextTickTime ? nextTickTime : time, runnable));
    }

    /**
     * 添加任务，至少需要一条
     *
     * @param task 任务
     */
    public void addLeastOneTick(final TimeTask task) {
        if (task != null) {
            addLeastOneTick(task.getName(), task.getTime(), task);
        }
    }

    /**
     * 添加任务
     *
     * @param task 任务
     */
    protected void add(final Task task) {
        //添加失败任务直接执行
        if (task != null && !timeWheel.add(task)) {
            workerPool.submit(task);
        }
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
        protected volatile long now;
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
        protected volatile TimeWheel next;

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
                synchronized (this) {
                    if (next == null) {
                        next = new TimeWheel(duration, ticks, now, queue);
                    }
                }
            }
            return next;
        }

        /**
         * 获取下一跳的时间
         *
         * @return
         */
        public long getNextTickTime() {
            return now + tickTime;
        }

        /**
         * 添加任务到时间轮
         */
        public boolean add(final Task task) {
            long current = now;
            long time = task.getTime() - current;
            if (time < tickTime) {
                //过期任务直接执行
                return false;
            } else if (time < duration) {
                //该任务在一个时间轮里面，则加入到对应的时间槽
                long count = time / tickTime;
                Slot slot = slots[(int) (count % ticks)];
                //添加到槽里面，防止和boss线程并发执行冲突
                switch (slot.add(task, slot.getExpiration(), current + count * tickTime)) {
                    case Slot.SUCCESS_FIRST:
                        queue.offer(slot);
                        return true;
                    case Slot.SUCCESS:
                        return true;
                    default:
                        return false;
                }
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
    protected static class Task implements Runnable {
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
         * 构造函数
         *
         * @param name
         * @param time
         * @param runnable
         */
        public Task(final String name, final long time, final Runnable runnable) {
            this.time = time;
            this.name = name;
            this.runnable = runnable;
            this.slot = null;
            this.next = null;
            this.pre = null;
        }

        public long getTime() {
            return time;
        }

        @Override
        public String toString() {
            return name == null || name.isEmpty() ? super.toString() : name;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }

    /**
     * 时间槽
     */
    protected static class Slot implements Delayed {

        public static final int FAIL = 0;
        public static final int SUCCESS_FIRST = 1;
        public static final int SUCCESS = 2;

        /**
         * 过期时间
         */
        protected volatile long expiration = -1L;
        /**
         * 根节点
         */
        protected Task root = new Task("root", -1L, null);

        /**
         * 构造函数
         */
        public Slot() {
            root.pre = root;
            root.next = root;
        }

        /**
         * 获取过期时间
         */
        public long getExpiration() {
            return expiration;
        }

        /**
         * 新增任务
         *
         * @param task    任务
         * @param version 当前槽的过期时间，防止和boss线程并发执行冲突
         * @param expire  新的过期时间
         * @return
         */
        public synchronized int add(final Task task, final long version, final long expire) {
            if (expiration == version) {
                task.slot = this;
                Task tail = root.pre;
                task.next = root;
                task.pre = tail;
                tail.next = task;
                root.pre = task;
                if (expiration != expire) {
                    expiration = expire;
                    return SUCCESS_FIRST;
                }
                return SUCCESS;
            }
            return FAIL;
        }

        /**
         * 当前槽已经过期，执行任务
         *
         * @param consumer 消费者
         */
        public synchronized void flush(final Consumer<Task> consumer) {
            Task task = root.next;
            while (task != root) {
                task.next.pre = task.pre;
                task.pre.next = task.next;
                task.slot = null;
                task.next = null;
                task.pre = null;
                consumer.accept(task);
                task = root.next;
            }
            expiration = -1L;
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
}
