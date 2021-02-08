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
import io.joyrpc.util.Timer.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * 增强的CompletableFuture
 */
public class EnhanceCompletableFuture<I, M> extends CompletableFuture<M> {
    /**
     * 消息ID
     */
    protected final I messageId;
    /**
     * 会话
     */
    protected final Session session;
    /**
     * 超时时间
     */
    protected final Timeout timeout;
    /**
     * 连接上的请求计数器
     */
    protected final AtomicInteger requests;
    /**
     * 扩展属性
     */
    protected Object attr;
    /**
     * 消费者
     */
    protected final BiConsumer<M, Throwable> consumer;

    /**
     * 构造函数
     *
     * @param messageId 消息ID
     * @param session   会话
     * @param timeout   超时
     * @param requests  请求计数器
     * @param consumer  消费者
     */
    public EnhanceCompletableFuture(final I messageId,
                                    final Session session,
                                    final Timeout timeout,
                                    final AtomicInteger requests,
                                    final BiConsumer<M, Throwable> consumer) {
        this.messageId = messageId;
        this.session = session;
        this.requests = requests;
        this.timeout = timeout;
        this.consumer = consumer;
    }

    public I getMessageId() {
        return messageId;
    }

    public Session getSession() {
        return session;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public Object getAttr() {
        return attr;
    }

    public void setAttr(Object attr) {
        this.attr = attr;
    }

    @Override
    public boolean complete(final M value) {
        if (super.complete(value)) {
            cancel();
            if (consumer != null) {
                consumer.accept(value, null);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean completeExceptionally(final Throwable ex) {
        if (super.completeExceptionally(ex)) {
            cancel();
            if (consumer != null) {
                consumer.accept(null, ex);
            }
            return true;
        }
        return false;
    }

    /**
     * 放弃过期检查任务，在从Future管理器移除任务会进行调用
     */
    protected void cancel() {
        if (timeout != null && !timeout.isExpired()) {
            timeout.cancel();
        }
        if (requests != null) {
            requests.decrementAndGet();
        }
    }

}
