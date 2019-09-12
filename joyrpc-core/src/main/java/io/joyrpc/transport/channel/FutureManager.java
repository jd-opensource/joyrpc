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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @date: 2019/1/14
 */
public class FutureManager<I, M> {

    protected FutureTimeoutManager timeoutManager;
    /**
     * Future管理，有些连接并发很少不需要初始化
     */
    protected Map<I, EnhanceCompletableFuture<I, M>> futures = new ConcurrentHashMap<>();

    protected Supplier<I> idGenerator;
    /**
     * 清理工作
     */
    protected Runnable checker;
    /**
     * 计数器
     */
    protected AtomicInteger counter = new AtomicInteger();

    /**
     * 构造函数
     *
     * @param idGenerator
     */
    public FutureManager(final Supplier<I> idGenerator) {
        this(idGenerator, null);
    }

    /**
     * 构造函数
     *
     * @param idGenerator
     * @param timeoutManager
     */
    public FutureManager(final Supplier<I> idGenerator, final FutureTimeoutManager timeoutManager) {
        this.idGenerator = idGenerator;
        this.checker = this::check;
        this.timeoutManager = timeoutManager == null ? FutureTimeoutManager.INSTANCE : timeoutManager;
    }

    /**
     * 超时检查
     */
    protected void check() {
        if (counter.get() == 0) {
            return;
        }
        //定时遍历
        Iterator<Map.Entry<I, EnhanceCompletableFuture<I, M>>> iterator = futures.entrySet().iterator();
        Map.Entry<I, EnhanceCompletableFuture<I, M>> entry;
        EnhanceCompletableFuture<I, M> future;
        while (iterator.hasNext()) {
            entry = iterator.next();
            future = entry.getValue();
            //已经超时，处理
            if (future.isExpire() && !future.isDone()) {
                if (futures.remove(entry.getKey()) != null) {
                    counter.decrementAndGet();
                    //超时，并且没有complicated，complicated一个超时异常
                    future.completeExceptionally(new TimeoutException("future is timeout."));
                }
            }
        }
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
            return new EnhanceCompletableFuture<>(o, session, SystemClock.now() + timeoutMillis);
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
     * 根据msgId移除掉一个Future
     *
     * @param messageId
     * @return
     */
    public EnhanceCompletableFuture<I, M> remove(final I messageId) {
        EnhanceCompletableFuture<I, M> result = futures.remove(messageId);
        if (result != null) {
            //减少计数器
            counter.decrementAndGet();
        }
        return result;
    }

    /**
     * 开启FutureManager（注册timeout事件）
     */
    public void open() {
        this.timeoutManager.register(checker);
    }

    /**
     * 清空这个manager下所有的Future
     *
     * @return
     */
    public Map<I, EnhanceCompletableFuture<I, M>> close() {
        Map<I, EnhanceCompletableFuture<I, M>> futures = this.futures;
        timeoutManager.deregister(checker);
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

}
