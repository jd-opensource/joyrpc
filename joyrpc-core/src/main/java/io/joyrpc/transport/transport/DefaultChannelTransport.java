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
import io.joyrpc.exception.ChannelSendException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.FutureManager;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * 默认ChannelTransport实现
 */
public class DefaultChannelTransport implements ChannelTransport {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelTransport.class);
    /**
     * URL
     */
    protected URL url;
    /**
     * 物理Channel
     */
    protected Channel channel;
    /**
     * 通道ID
     */
    protected int transportId;
    /**
     * 上次访问时间
     */
    protected volatile long lastRequestTime = 0;
    /**
     * 会话
     */
    protected Session session;

    protected DefaultChannelTransport(URL url) {
        this.url = url;
        this.transportId = ID_GENERATOR.get();
    }

    public DefaultChannelTransport(Channel channel, URL url) {
        this.url = url;
        this.channel = channel;
        if (channel.isServer()) {
            this.transportId = 0;
        } else {
            this.transportId = ID_GENERATOR.get();
        }
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public CompletableFuture<Void> oneway(final Message message) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (message != null) {
            message.setMsgId(channel.getFutureManager().generateId());
            message.setSessionId(transportId);
            message.setSession(session);
            channel.send(message, r -> {
                if (r.isSuccess()) {
                    result.complete(null);
                } else {
                    result.completeExceptionally(r.getThrowable());
                }
            });
        } else {
            result.completeExceptionally(new NullPointerException("message can not be null."));
        }
        return result;
    }

    @Override
    public CompletableFuture<Message> async(final Message message, final int timeoutMillis) {
        CompletableFuture<Message> future;
        if (message == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new NullPointerException("message can not be null."));
        } else if (!channel.isActive()) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new ChannelSendException(String.format("Send message exception, because channel is not available, causing channel is not available. at %s : %s",
                    Channel.toString(channel), message.toString())));
        } else {
            int timeout = timeoutMillis <= 0 ? Constants.SEND_TIMEOUT_OPTION.get() : timeoutMillis;
            FutureManager<Integer, Message> futureManager = channel.getFutureManager();
            //设置id
            message.setMsgId(futureManager.generateId());
            message.setSessionId(transportId);
            message.setSession(session);
            //创建 future
            future = futureManager.create(message.getMsgId(), timeout, session);

            channel.send(message, r -> {
                if (!r.isSuccess()) {
                    Throwable throwable = r.getThrowable() == null
                            ? new ChannelSendException("unknown exception.")
                            : new ChannelSendException(r.getThrowable());
                    CompletableFuture<Message> cf = futureManager.remove(message.getMsgId());
                    if (cf != null) {
                        cf.completeExceptionally(throwable);
                        logger.error("failed sending message. The error is " + throwable.getMessage(), throwable);
                    }
                } else {
                    lastRequestTime = SystemClock.now();
                }
            });
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
    public URL getUrl() {
        return url;
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
