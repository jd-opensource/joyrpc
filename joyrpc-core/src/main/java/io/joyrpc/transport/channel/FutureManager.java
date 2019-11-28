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

import io.joyrpc.transport.session.Session;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.Timer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.joyrpc.context.GlobalContext.timer;

/**
 * @date: 2019/1/14
 */
public class FutureManager<I, M> {

    /**
     * Future管理，有些连接并发很少不需要初始化
     */
    protected Map<I, EnhanceCompletableFuture<I, M>> futures = new ConcurrentHashMap<>();
    /**
     * ID生成器
     */
    protected Supplier<I> idGenerator;
    /**
     * 计数器
     */
    protected AtomicInteger counter = new AtomicInteger();

    protected Consumer<I> consumer;

    /**
     * 构造函数
     *
     * @param idGenerator
     */
    public FutureManager(final Supplier<I> idGenerator) {
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
        return create(messageId, timeoutMillis, null);
    }

    /**
     * 创建一个future
     *
     * @param messageId
     * @param timeoutMillis
     * @param session
     * @return
     */
    public EnhanceCompletableFuture<I, M> create(final I messageId, final long timeoutMillis, final Session session) {
        return futures.computeIfAbsent(messageId, o -> {
            //增加计数器
            counter.incrementAndGet();
            return new EnhanceCompletableFuture<>(o, session, timer().add(
                    new FutureTimeoutTask<>(messageId, SystemClock.now() + timeoutMillis, consumer)));
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
     * 清空这个manager下所有的Future
     *
     * @return
     */
    public Map<I, EnhanceCompletableFuture<I, M>> close() {
        Map<I, EnhanceCompletableFuture<I, M>> futures = this.futures;
        this.futures = new ConcurrentHashMap<>();
        this.counter = new AtomicInteger();
        return futures;
    }

    public I generateId() {
        return idGenerator.get();
    }

    public int size() {
        return futures.size();
    }

    public boolean isEmpty() {
        return futures.isEmpty();
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
