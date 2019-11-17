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
     * @param name          名称
     * @param tickTime      每一跳时间
     * @param ticks         时间轮有几条
     * @param workerThreads 工作线程数
     */
    public Timer(final String name, final long tickTime, final int ticks, final int workerThreads) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name can not be empty");
        }
        this.workerThreads = workerThreads;
        this.queue = new DelayQueue<>();
        this.timeWheel = new TimeWheel(tickTime, ticks, SystemClock.now(), queue);
        this.workerPool = Executors.newFixedThreadPool(workerThreads, new NamedThreadFactory(name + "-worker", true));
        this.bossPool = Executors.newFixedThreadPool(1, new NamedThreadFactory(name + "-boss", true));
        this.bossPool.submit(() -> {
            while (!Shutdown.isShutdown()) {
                try {
                    Slot slot = queue.poll(timeWheel.getDuration(), TimeUnit.MILLISECONDS);
                    if (slot != null) {
                        //推进时间
                        timeWheel.advance(slot.getExpiration());
                        //执行任务
                        slot.execute(this::add);
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
    public void add(final String name, final int time, final Runnable runnable) {
        if (runnable == null) {
            return;
        }
        add(new Task(name, time, runnable));
    }

    /**
     * 添加任务
     *
     * @param task 任务
     */
    protected void add(final Task task) {
        if (task == null) {
            return;
        }
        //添加失败任务直接执行
        if (!timeWheel.add(task)) {
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
         * 时间槽有几跳
         */
        protected int ticks;
        /**
         * 时间轮的周期
         */
        protected long duration;
        /**
         * 当前跳的起始时间
         */
        protected long now;
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
        protected volatile TimeWheel nextWheel;

        /**
         * 时间轮
         *
         * @param tickTime
         * @param ticks
         * @param now
         * @param queue
         */
        public TimeWheel(long tickTime, final int ticks, final long now, final DelayQueue<Slot> queue) {
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
        protected TimeWheel getNextWheel() {
            if (nextWheel == null) {
                synchronized (this) {
                    if (nextWheel == null) {
                        nextWheel = new TimeWheel(duration, ticks, now, queue);
                    }
                }
            }
            return nextWheel;
        }

        /**
         * 添加任务到时间轮
         */
        public boolean add(final Task task) {
            //TODO 会不会和boss线程并发执行冲突
            long time = task.getTime();
            if (time < now + tickTime) {
                //过期任务直接执行
                return false;
            } else if (time < now + duration) {
                //该任务在一个时间轮里面，则加入到对应的时间槽
                long count = time / tickTime + time % tickTime == 0 ? 0 : 1;
                Slot slot = slots[(int) (count % ticks)];
                slot.add(task);
                //重新设置时间槽的过期时间，第一次则防止到队列里面
                if (slot.setExpiration(count * tickTime)) {
                    //添加到延迟队列中
                    queue.offer(slot);
                }
                return true;
            } else {
                //放到下一层的时间轮
                return getNextWheel().add(task);
            }
        }

        /**
         * 推进时间
         */
        public void advance(final long timestamp) {
            if (timestamp >= now + tickTime) {
                now = timestamp - (timestamp % tickTime);
                if (nextWheel != null) {
                    //推进下层时间轮时间
                    nextWheel.advance(timestamp);
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
            return name;
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
         * 设置过期时间
         */
        public boolean setExpiration(final long expire) {
            if (expire != expiration) {
                expiration = expire;
                return true;
            }
            return false;
        }

        /**
         * 获取过期时间
         */
        public long getExpiration() {
            return expiration;
        }

        /**
         * 新增任务
         */
        public synchronized void add(final Task task) {
            if (task != null && task.slot == null) {
                task.slot = this;
                Task tail = root.pre;
                task.next = root;
                task.pre = tail;
                tail.next = task;
                root.pre = task;
            }
        }

        /**
         * 移除任务
         */
        protected void remove(final Task task) {
            if (task != null && task.slot == this) {
                task.next.pre = task.pre;
                task.pre.next = task.next;
                task.slot = null;
                task.next = null;
                task.pre = null;
            }
        }

        /**
         * 执行任务
         *
         * @param consumer 消费者
         */
        public synchronized void execute(final Consumer<Task> consumer) {
            Task task = root.next;
            while (task != root) {
                remove(task);
                consumer.accept(task);
                task = root.next;
            }
            expiration = -1L;
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            return Math.max(0, unit.convert(expiration - SystemClock.now(), TimeUnit.MILLISECONDS));
        }

        @Override
        public int compareTo(final Delayed o) {
            return o instanceof Slot ? Long.compare(expiration, ((Slot) o).expiration) : 0;
        }
    }
}
