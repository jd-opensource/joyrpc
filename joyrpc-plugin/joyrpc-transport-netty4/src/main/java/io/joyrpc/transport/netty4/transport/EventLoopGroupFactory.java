package io.joyrpc.transport.netty4.transport;

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

import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.thread.NamedThreadFactory;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.concurrent.SucceededFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static io.joyrpc.constants.Constants.*;

/**
 * eventloop 工厂类
 *
 * @date: 2019/1/29
 */
public class EventLoopGroupFactory {

    private static final Logger logger = LoggerFactory.getLogger(EventLoopGroupFactory.class);

    public static final String EVENT_LOOP_GROUP_CLIENT = "EventLoopGroup-Client";
    public static final String EVENT_LOOP_GROUP_WORKER = "EventLoopGroup-Worker";
    public static final String EVENT_LOOP_GROUP_BOSS = "EventLoopGroup-Boss";
    public static final String NETTY_EVENTLOOP_SHARE = "netty.eventloop.share";

    protected static Map<String, ShareEventLoopGroup> groups = new ConcurrentHashMap<>();


    /**
     * 通过url获取server端boss线程EventLoop
     *
     * @param url url实例
     * @return EventLoopGroup
     */
    public static EventLoopGroup getParentEventLoopGroup(URL url) {
        String key = getKey(url, EVENT_LOOP_GROUP_BOSS, false);
        ShareEventLoopGroup result = groups.computeIfAbsent(key,
                o -> create(key, url, EVENT_LOOP_GROUP_BOSS, BOSS_THREAD_OPTION));
        result.addRef();
        return result;
    }

    /**
     * 通过url获取server端worker线程EventLoop
     *
     * @param url url实例
     * @return EventLoopGroup
     */
    public static EventLoopGroup getChildEventLoopGroup(final URL url) {
        String key = getKey(url, EVENT_LOOP_GROUP_WORKER, false);
        ShareEventLoopGroup result = groups.computeIfAbsent(key,
                o -> create(key, url, EVENT_LOOP_GROUP_WORKER, IO_THREAD_OPTION));
        result.addRef();
        return result;
    }

    /**
     * 获取Key
     *
     * @param url
     * @param type
     * @param share
     * @return
     */
    protected static String getKey(final URL url, final String type, final boolean share) {
        boolean s = url.getBoolean(NETTY_EVENTLOOP_SHARE, share);
        return s ? type : type + "." + url.getAddress();
    }

    /**
     * 创建EventLoop
     *
     * @param name
     * @param url
     * @param threadName
     * @param ioThread
     * @return
     */
    protected static ShareEventLoopGroup create(final String name, final URL url, final String threadName, URLOption<Integer> ioThread) {
        int threads = url.getPositiveInt(ioThread);
        if (isUseEpoll(url)) {
            logger.info(String.format("Success creating eventLoopGroup. name:%s, threads:%d, epoll:true. ", ioThread.getName(), threads));
            return new ShareEventLoopGroup(name,
                    new EpollEventLoopGroup(threads, new NamedThreadFactory(threadName, true)), groups);
        } else {
            logger.info(String.format("Success creating eventLoopGroup. name:%s, threads:%d, epoll:false. ", ioThread.getName(), threads));
            return new ShareEventLoopGroup(name,
                    new NioEventLoopGroup(threads, new NamedThreadFactory(threadName, true)), groups);
        }
    }

    /**
     * 通过url获取client端worker线程EventLoop
     *
     * @param url url实例
     * @return EventLoopGroup
     */
    public static EventLoopGroup getClientEventLoopGroup(final URL url) {
        String key = getKey(url, EVENT_LOOP_GROUP_CLIENT, true);
        ShareEventLoopGroup result = groups.computeIfAbsent(key,
                o -> create(key, url, EVENT_LOOP_GROUP_CLIENT, IO_THREAD_OPTION));
        result.addRef();
        return result;
    }

    /**
     * 共享线程池
     */
    protected static class ShareEventLoopGroup implements EventLoopGroup {
        /**
         * 名称
         */
        protected String name;
        /**
         * 线程池
         */
        protected EventLoopGroup group;
        /**
         * 线程池容器
         */
        protected Map<String, ? extends EventLoopGroup> groups;
        /**
         * 计数器
         */
        protected AtomicLong counter = new AtomicLong(0);

        /**
         * 构造函数
         *
         * @param name
         * @param group
         * @param groups
         */
        public ShareEventLoopGroup(String name, EventLoopGroup group, Map<String, ? extends EventLoopGroup> groups) {
            this.name = name;
            this.group = group;
            this.groups = groups;
        }

        /**
         * 增加引用计数
         *
         * @return
         */
        public long addRef() {
            return counter.incrementAndGet();
        }

        /**
         * 释放引用计数
         *
         * @return
         */
        public long release() {
            long result = counter.decrementAndGet();
            if (result == 0) {
                //从容器里面删除
                groups.remove(name);
            }
            return result;
        }

        @Override
        public EventLoop next() {
            return group.next();
        }

        @Override
        public ChannelFuture register(final Channel channel) {
            return group.register(channel);
        }

        @Override
        public ChannelFuture register(final ChannelPromise promise) {
            return group.register(promise);
        }

        @Override
        public ChannelFuture register(final Channel channel, final ChannelPromise promise) {
            return group.register(channel, promise);
        }

        @Override
        public boolean isShuttingDown() {
            return group.isShuttingDown();
        }

        @Override
        public Future<?> shutdownGracefully() {
            if (release() == 0) {
                return group.shutdownGracefully();
            } else {
                return new SucceededFuture(null, Boolean.TRUE);
            }
        }

        @Override
        public Future<?> shutdownGracefully(final long quietPeriod, final long timeout, final TimeUnit unit) {
            if (release() == 0) {
                return group.shutdownGracefully(quietPeriod, timeout, unit);
            } else {
                return new SucceededFuture(null, Boolean.TRUE);
            }
        }

        @Override
        public Future<?> terminationFuture() {
            return group.terminationFuture();
        }

        @Override
        public void shutdown() {
            if (release() == 0) {
                group.shutdown();
            }
        }

        @Override
        public List<Runnable> shutdownNow() {
            if (release() == 0) {
                return group.shutdownNow();
            } else {
                return new ArrayList<>(0);
            }
        }

        @Override
        public Iterator<EventExecutor> iterator() {
            return group.iterator();
        }

        @Override
        public Future<?> submit(final Runnable task) {
            return group.submit(task);
        }

        @Override
        public <T> Future<T> submit(final Runnable task, final T result) {
            return group.submit(task, result);
        }

        @Override
        public <T> Future<T> submit(final Callable<T> task) {
            return group.submit(task);
        }

        @Override
        public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
            return group.schedule(command, delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
            return group.schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
            return group.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
            return group.scheduleAtFixedRate(command, initialDelay, delay, unit);
        }

        @Override
        public boolean isShutdown() {
            return group.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return group.isTerminated();
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
            return group.awaitTermination(timeout, unit);
        }

        @Override
        public <T> List<java.util.concurrent.Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return group.invokeAll(tasks);
        }

        @Override
        public <T> List<java.util.concurrent.Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
            return group.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return group.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return group.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(final Runnable command) {
            group.execute(command);
        }
    }

}
