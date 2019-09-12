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


import io.joyrpc.event.AsyncResult;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.session.SessionManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @date: 2019/1/7
 */
public interface Channel {

    String BIZ_THREAD_POOL = "bizThreadPool";

    String IDLE_HEARTBEAT_TRIGGER = "idleHeartbeatTrigger";

    String CHANNEL_TRANSPORT = "CHANNEL_TRANSPORT";

    String SERVER_CHANNEL = "SERVER_CHANNEL";

    String HEARTBEAT_FAILED_COUNT = "heartbeat.failed.count";

    String CHANNEL_KEY = "CHANNEL_KEY";

    String PROTOCOL = "PROTOCOL";

    String PAYLOAD = "PAYLOAD";

    String IS_SERVER = "IS_SERVER";

    String EVENT_PUBLISHER = "EVENT_PUBLISHER";

    /**
     * 连接转字符串
     *
     * @return
     */
    static String toString(Channel channel) {
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
     * @param object
     */
    void send(Object object);

    /**
     * 发送一个object信息
     *
     * @param object
     * @param consumer
     */
    void send(Object object, Consumer<SendResult> consumer);

    /**
     * 批量发送消息
     *
     * @param objects 消息列表
     */
    void sendList(List<Object> objects);

    /**
     * 关闭channel
     *
     * @return
     */
    boolean close();

    /**
     * 异步关闭channel
     *
     * @param consumer
     */
    void close(Consumer<AsyncResult<Channel>> consumer);

    /**
     * 断开channel
     *
     * @return
     */
    boolean disconnect();

    /**
     * 异步断开channel
     *
     * @param consumer
     * @return
     */
    void disconnect(Consumer<AsyncResult<Channel>> consumer);

    /**
     * 获取本地地址
     *
     * @return
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取远程地址
     *
     * @return
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 是否可写
     *
     * @return
     */
    boolean isWritable();

    /**
     * 是否存活
     *
     * @return
     */
    boolean isActive();

    /**
     * 获取属性
     *
     * @param key
     * @param <T>
     * @return
     */
    <T> T getAttribute(String key);

    /**
     * 获取属性，如果为null返回默认值
     *
     * @param key
     * @param def
     * @param <T>
     * @return
     */
    default <T> T getAttribute(String key, T def) {
        T result = getAttribute(key);
        return result == null ? def : result;
    }

    /**
     * 获取属性，没有的时候调用Function创建
     *
     * @param key
     * @param function
     * @param <T>
     * @return
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
     * @param key
     * @param value
     */
    void setAttribute(String key, Object value);

    /**
     * 删除属性
     *
     * @param key
     * @return
     */
    Object removeAttribute(String key);

    /**
     * 获取Future管理器
     *
     * @return
     */
    FutureManager<Integer, Message> getFutureManager();

    /**
     * 申请一个ChannelBuffer
     *
     * @return ChannelBuffer
     */
    ChannelBuffer buffer();

    /**
     * 申请一个ChannelBuffer
     *
     * @param initialCapacity 初始长度
     * @return ChannelBuffer
     */
    ChannelBuffer buffer(int initialCapacity);

    /**
     * 申请一个ChannelBuffer
     *
     * @param initialCapacity 初始长度
     * @param maxCapacity     最大长度
     * @return ChannelBuffer
     */
    ChannelBuffer buffer(int initialCapacity, int maxCapacity);

    /**
     * 获取session管理器
     *
     * @return SessionManager
     */
    SessionManager getSessionManager();

    boolean isServer();

    default Session getSession(int sessionId) {
        return getSessionManager().get(sessionId);
    }

    default Session addSession(int sessionId, Session session) {
        return getSessionManager().put(sessionId, session);
    }

    default Session addIfAbsentSession(int sessionId, Session session) {
        return getSessionManager().putIfAbsent(sessionId, session);
    }

    default Session removeSession(int sessionId) {
        return getSessionManager().remove(sessionId);
    }

    /**
     * 触发异常事件
     *
     * @param caught
     */
    void fireCaught(Throwable caught);


}
