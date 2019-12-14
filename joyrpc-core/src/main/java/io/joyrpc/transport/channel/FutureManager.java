package io.joyrpc.transport.channel;

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

import io.joyrpc.exception.ChannelClosedException;
import io.joyrpc.transport.session.Session;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.Timer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.joyrpc.util.Timer.timer;

/**
 * @date: 2019/1/14
 */
public class FutureManager<I, M> {
    /**
     * 通道
     */
    protected Channel channel;
    /**
     * ID生成器
     */
    protected Supplier<I> idGenerator;
    /**
     * 计数器
     */
    protected AtomicInteger counter = new AtomicInteger();
    /**
     * 消费者
     */
    protected Consumer<I> consumer;
    /**
     * Future管理，有些连接并发很少不需要初始化
     */
    protected Map<I, EnhanceCompletableFuture<I, M>> futures = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param channel
     * @param idGenerator
     */
    public FutureManager(final Channel channel, final Supplier<I> idGenerator) {
        this.channel = channel;
        this.idGenerator = idGenerator;
        this.consumer = id -> {
            EnhanceCompletableFuture<I, M> future = futures.remove(id);
            if (future != null) {
                counter.decrementAndGet();
                //超时
                future.completeExceptionally(new TimeoutException("future is timeout."));
            }
        };
    }

    /**
     * 创建一个future
     *
     * @param messageId
     * @param timeoutMillis
     * @return
     */
    public EnhanceCompletableFuture<I, M> create(final I messageId, final long timeoutMillis) {
        return create(messageId, timeoutMillis, null, null);
    }

    /**
     * 创建一个future
     *
     * @param messageId     消息ID
     * @param timeoutMillis 超时时间
     * @param session       会话
     * @param requests      当前Transport正在处理的请求数
     * @return
     */
    public EnhanceCompletableFuture<I, M> create(final I messageId, final long timeoutMillis, final Session session,
                                                 final AtomicInteger requests) {
        return futures.computeIfAbsent(messageId, o -> {
            //增加计数器
            counter.incrementAndGet();
            return new EnhanceCompletableFuture<>(o, session,
                    timer().add(new FutureTimeoutTask<>(messageId, SystemClock.now() + timeoutMillis, consumer)),
                    requests);
        });
    }

    /**
     * 根据msgId获取future
     *
     * @param messageId
     * @return
     */
    public EnhanceCompletableFuture<I, M> get(final I messageId) {
        return futures.get(messageId);
    }

    /**
     * 根据消息移除
     *
     * @param messageId
     * @return
     */
    public EnhanceCompletableFuture<I, M> remove(final I messageId) {
        EnhanceCompletableFuture<I, M> result = futures.remove(messageId);
        if (result != null) {
            //放弃过期检查任务
            result.cancel();
            //减少计数器
            counter.decrementAndGet();
        }
        return result;
    }

    /**
     * 开启FutureManager（注册timeout事件）
     */
    public void open() {
    }

    /**
     * 清空
     *
     * @return
     */
    public void close() {
        Map<I, EnhanceCompletableFuture<I, M>> futures = this.futures;
        this.futures = new ConcurrentHashMap<>();
        this.counter = new AtomicInteger();
        Exception exception = new ChannelClosedException("channel is inactive, address is " + channel.getRemoteAddress());
        futures.forEach((id, future) -> future.cancel(exception));
        futures.clear();
    }

    /**
     * 生成ID
     *
     * @return
     */
    public I generateId() {
        return idGenerator.get();
    }

    /**
     * 待应答的请求数
     *
     * @return
     */
    public int size() {
        return counter.get();
    }

    /**
     * 是否为空
     *
     * @return
     */
    public boolean isEmpty() {
        return counter.get() == 0;
    }

    /**
     * Future超时检查任务
     *
     * @param <I>
     */
    protected static class FutureTimeoutTask<I> implements Timer.TimeTask {
        public static final String FUTURE_TIMEOUT = "FutureTimeout-";
        /**
         * 消息ID
         */
        protected I messageId;
        /**
         * 时间
         */
        protected long time;
        /**
         * 执行回调的消费者
         */
        protected Consumer<I> consumer;

        /**
         * 构造函数
         *
         * @param messageId
         * @param time
         * @param consumer
         */
        public FutureTimeoutTask(I messageId, long time, Consumer<I> consumer) {
            this.messageId = messageId;
            this.time = time;
            this.consumer = consumer;
        }

        @Override
        public String getName() {
            return FUTURE_TIMEOUT + messageId.toString();
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public void run() {
            consumer.accept(messageId);
        }
    }

}
