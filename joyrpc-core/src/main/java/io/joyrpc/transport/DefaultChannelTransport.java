package io.joyrpc.transport;

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
import io.joyrpc.exception.ChannelSendException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.FutureManager;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.util.Futures;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认连接传输通道
 */
public class DefaultChannelTransport implements ChannelTransport {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelTransport.class);
    /**
     * URL
     */
    protected final URL url;
    /**
     * 通道ID
     */
    protected final int transportId;
    /**
     * 连接通道
     */
    protected Channel channel;

    /**
     * 上次访问时间
     */
    protected volatile long lastRequestTime = 0;
    /**
     * 会话
     */
    protected Session session;
    /**
     * 计数器
     */
    protected AtomicInteger requests = new AtomicInteger();

    protected DefaultChannelTransport(URL url) {
        this.url = Objects.requireNonNull(url);
        this.transportId = ID_GENERATOR.get();
    }

    public DefaultChannelTransport(Channel channel, URL url) {
        this.url = Objects.requireNonNull(url);
        this.channel = Objects.requireNonNull(channel);
        this.transportId = channel.isServer() ? 0 : ID_GENERATOR.get();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public CompletableFuture<Void> oneway(final Message message) {
        requests.incrementAndGet();
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (message != null) {
            message.setMsgId(channel.getFutureManager().generateId());
            message.setSessionId(transportId);
            message.setSession(session);
            try {
                channel.send(message).whenComplete((v, error) -> {
                    requests.decrementAndGet();
                    if (error == null) {
                        result.complete(null);
                    } else {
                        result.completeExceptionally(error);
                    }
                });
            } catch (Throwable e) {
                requests.decrementAndGet();
                //捕获异常
                result.completeExceptionally(e);
            }
        } else {
            requests.decrementAndGet();
            result.completeExceptionally(new NullPointerException("message can not be null."));
        }
        return result;
    }

    @Override
    public CompletableFuture<Message> async(final Message message, final int timeoutMillis) {
        requests.incrementAndGet();
        CompletableFuture<Message> future;
        if (message == null) {
            requests.decrementAndGet();
            future = Futures.completeExceptionally(new NullPointerException("message can not be null."));
        } else if (!channel.isActive()) {
            requests.decrementAndGet();
            future = Futures.completeExceptionally(new ChannelSendException(
                    String.format("Failed sending message, caused by channel is not active. at %s",
                            Channel.toString(channel))));
        } else {
            int timeout = timeoutMillis <= 0 ? Constants.DEFAULT_TIMEOUT : timeoutMillis;
            FutureManager<Long, Message> futureManager = channel.getFutureManager();
            //设置id
            message.setMsgId(futureManager.generateId());
            message.setSessionId(transportId);
            message.setSession(session);
            //创建 future
            future = futureManager.create(message.getMsgId(), timeout, session, requests);
            try {
                channel.send(message).whenComplete((v, error) -> {
                    if (error != null) {
                        Throwable throwable = new ChannelSendException(error);
                        futureManager.completeExceptionally(message.getMsgId(), throwable);
                        logger.error("Failed sending message. caused by " + throwable.getMessage(), throwable);
                    } else {
                        lastRequestTime = SystemClock.now();
                    }
                });
            } catch (Throwable e) {
                //捕获系统异常
                futureManager.completeExceptionally(message.getMsgId(), e);
            }
        }
        return future;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    @Override
    public long getLastRequestTime() {
        return lastRequestTime;
    }

    @Override
    public int getTransportId() {
        return transportId;
    }

    @Override
    public Session session() {
        return session;
    }

    @Override
    public Session session(Session session) {
        Session oldSession = this.session;
        this.session = session;
        if (channel != null) {
            channel.addSession(this.transportId, session);
        }
        return oldSession;
    }
}
