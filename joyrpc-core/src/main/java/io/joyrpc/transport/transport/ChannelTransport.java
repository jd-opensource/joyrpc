package io.joyrpc.transport.transport;

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


import io.joyrpc.constants.Constants;
import io.joyrpc.exception.RpcException;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;

import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * ChannelTransport
 */
public interface ChannelTransport extends Transport {

    /**
     * 获取channel
     *
     * @return
     */
    Channel getChannel();

    /**
     * 发送一个消息（不关心响应）
     *
     * @param message
     */
    CompletableFuture<Void> oneway(Message message);

    /**
     * 同步发送一个请求
     *
     * @param message
     * @param timeoutMillis
     * @return
     */
    default Message sync(final Message message, final int timeoutMillis) throws RpcException, TimeoutException {
        try {
            int timeout = timeoutMillis <= 0 ? Constants.DEFAULT_TIMEOUT : timeoutMillis;
            return message == null ? null : async(message, timeout).get((long) timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | RpcException e) {
            throw e;
        } catch (ExecutionException e) {
            throw new RpcException(e.getMessage(), e.getCause());
        } catch (Exception e) {
            throw new RpcException(e);
        }
    }

    /**
     * 异步发送一个请求
     *
     * @param message
     * @param timeoutMillis
     * @return
     */
    CompletableFuture<Message> async(Message message, int timeoutMillis);

    /**
     * 异步发送一个请求
     *
     * @param message
     * @param action
     * @param timeoutMillis
     * @return
     */
    default void async(final Message message, final BiConsumer<Message, Throwable> action, final int timeoutMillis) {
        CompletableFuture<Message> future = async(message, timeoutMillis);
        if (action != null) {
            future.whenComplete(action);
        }
    }

    /**
     * 获取远程地址
     *
     * @return
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 获取最后发送请求成功的时间
     *
     * @return
     */
    long getLastRequestTime();

    /**
     * 返回会话
     *
     * @return
     */
    Session session();

    /**
     * 绑定会话
     *
     * @param session
     * @return
     */
    Session session(Session session);

}
