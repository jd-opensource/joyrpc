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


import io.joyrpc.event.Publisher;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.session.SessionManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * 连接通道
 */
public interface Channel {

    String IDLE_HEARTBEAT_TRIGGER = "idleHeartbeatTrigger";

    String CHANNEL_TRANSPORT = "CHANNEL_TRANSPORT";

    String PROTOCOL = "PROTOCOL";

    /**
     * 获取名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 关闭
     *
     * @return CompletableFuture
     */
    CompletableFuture<Channel> close();

    /**
     * 连接通道转字符串
     *
     * @param channel 通道
     * @return 字符串
     */
    static String toString(final Channel channel) {
        return toString(channel.getLocalAddress()) + " -> " + toString(channel.getRemoteAddress());

    }

    /**
     * InetSocketAddress转 host:port 字符串
     *
     * @param address InetSocketAddress转
     * @return host:port 字符串
     */
    static String toString(final InetSocketAddress address) {
        if (address == null) {
            return "";
        } else {
            InetAddress inetAddress = address.getAddress();
            return inetAddress == null ? address.getHostName() :
                    inetAddress.getHostAddress() + ":" + address.getPort();
        }
    }

    /**
     * 发送一个object信息
     *
     * @param object 对象
     */
    CompletableFuture<Void> send(Object object);

    /**
     * 发送消息
     *
     * @param object   对象
     * @param consumer 消费者
     */
    default void send(final Object object, final BiConsumer<Void, Throwable> consumer) {
        if (consumer != null) {
            send(object).whenComplete(consumer);
        } else {
            send(object);
        }
    }

    /**
     * 获取本地地址
     *
     * @return 本地地址
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取远程地址
     *
     * @return 远端地址
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 是否可写
     *
     * @return 可写标识
     */
    boolean isWritable();

    /**
     * 是否存活
     *
     * @return 存活标识
     */
    boolean isActive();

    /**
     * 获取属性
     *
     * @param key 键
     * @param <T>
     * @return 值
     */
    <T> T getAttribute(String key);

    /**
     * 获取属性，如果为null返回默认值
     *
     * @param key 键
     * @param def 默认值
     * @param <T>
     * @return 值
     */
    default <T> T getAttribute(String key, T def) {
        T result = getAttribute(key);
        return result == null ? def : result;
    }

    /**
     * 获取属性，没有的时候调用Function创建
     *
     * @param key      键
     * @param function 函数
     * @param <T>
     * @return 值
     */
    default <T> T getAttribute(final String key, final Function<String, T> function) {
        if (key == null) {
            return null;
        }
        T result = getAttribute(key);
        if (result == null) {
            result = function.apply(key);
            setAttribute(key, result);
        }
        return result;
    }

    /**
     * 设置属性
     *
     * @param key   键
     * @param value 值
     * @return 通道
     */
    Channel setAttribute(String key, Object value);

    /**
     * 设置属性
     *
     * @param key       键
     * @param value     值
     * @param predicate 断言
     * @return 通道
     */
    default Channel setAttribute(final String key, final Object value, final BiPredicate<String, Object> predicate) {
        if (predicate.test(key, value)) {
            return setAttribute(key, value);
        }
        return this;
    }

    /**
     * 删除属性
     *
     * @param key 键
     * @return 值
     */
    Object removeAttribute(String key);

    /**
     * 获取Future管理器
     *
     * @return Future管理器
     */
    FutureManager<Long, Message> getFutureManager();

    /**
     * 获取会话管理器
     *
     * @return 会话管理器
     */
    SessionManager getSessionManager();

    /**
     * 获取事件发布器
     *
     * @return 事件发布器
     */
    Publisher<TransportEvent> getPublisher();

    /**
     * 获取业务线程池
     *
     * @return 线程池
     */
    ExecutorService getWorkerPool();

    /**
     * 获取数据包大小
     *
     * @return 数据包大小
     */
    int getPayloadSize();

    /**
     * 申请一个缓冲区
     *
     * @return 缓冲区
     */
    ChannelBuffer buffer();

    /**
     * 申请一个缓冲区
     *
     * @param initialCapacity 初始长度
     * @return 缓冲区
     */
    ChannelBuffer buffer(int initialCapacity);

    /**
     * 申请一个缓冲区
     *
     * @param initialCapacity 初始长度
     * @param maxCapacity     最大长度
     * @return 缓冲区
     */
    ChannelBuffer buffer(int initialCapacity, int maxCapacity);

    /**
     * 是否是服务端
     *
     * @return
     */
    boolean isServer();

    /**
     * 获取会话
     *
     * @param sessionId 会话ID
     * @return 会话
     */
    default Session getSession(final int sessionId) {
        return getSessionManager().get(sessionId);
    }

    /**
     * 设置会话
     *
     * @param sessionId 会话ID
     * @param session   会话
     * @return 会话
     */
    default Session addSession(final int sessionId, final Session session) {
        return getSessionManager().put(sessionId, session);
    }

    /**
     * 添加会话
     *
     * @param sessionId 会话ID
     * @param session   会话
     * @return 会话
     */
    default Session addIfAbsentSession(final int sessionId, final Session session) {
        return getSessionManager().putIfAbsent(sessionId, session);
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return
     */
    default Session removeSession(final int sessionId) {
        return getSessionManager().remove(sessionId);
    }

    /**
     * 驱逐过期会话
     */
    default void evictSession() {
        getSessionManager().evict();
    }

    /**
     * 会话心跳
     *
     * @param sessionId 会话ID
     */
    default boolean beatSession(final int sessionId) {
        return getSessionManager().beat(sessionId);
    }

    /**
     * 触发异常事件
     *
     * @param caught 异常
     */
    void fireCaught(Throwable caught);

}
