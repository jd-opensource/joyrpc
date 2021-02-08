package io.joyrpc.transport.heartbeat;

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
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.FutureManager;
import io.joyrpc.transport.event.HeartbeatEvent;
import io.joyrpc.transport.event.InactiveEvent;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.message.Message;
import io.joyrpc.util.Shutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 默认心跳触发器
 */
public class DefaultHeartbeatTrigger implements HeartbeatTrigger {

    private static final Logger logger = LoggerFactory.getLogger(DefaultHeartbeatTrigger.class);
    /**
     * 通道
     */
    protected final Channel channel;
    /**
     * URL
     */
    protected final URL url;
    /**
     * 心跳策略
     */
    protected final HeartbeatStrategy strategy;
    /**
     * 事件发布器
     */
    protected final Publisher<TransportEvent> publisher;
    /**
     * 心跳应答
     */
    protected final BiConsumer<Message, Throwable> afterRun;

    /**
     * 构造函数
     *
     * @param channel   连接通道
     * @param url       url
     * @param strategy  心跳策略
     * @param publisher 事件发布器
     */
    public DefaultHeartbeatTrigger(Channel channel, URL url, HeartbeatStrategy strategy, Publisher<TransportEvent> publisher) {
        this.channel = channel;
        this.url = url;
        this.strategy = strategy;
        this.publisher = publisher;
        this.afterRun = (msg, err) -> publisher.offer(err != null ? new HeartbeatEvent(channel, url, err) : new HeartbeatEvent(msg, channel, url));
    }

    @Override
    public HeartbeatStrategy strategy() {
        return strategy;
    }

    @Override
    public void run() {
        Message heartbeatMessage;
        Supplier<Message> supplier = strategy.getHeartbeat();
        //关机状态不发送心跳了
        if (!Shutdown.isShutdown() && supplier != null && (heartbeatMessage = supplier.get()) != null) {
            if (channel.isActive()) {
                FutureManager<Long, Message> futureManager = channel.getFutureManager();
                //设置id
                heartbeatMessage.setMsgId(futureManager.generateId());
                //创建future
                futureManager.create(heartbeatMessage.getMsgId(), strategy.getTimeout(), afterRun);
                //发送消息
                channel.send(heartbeatMessage, r -> {
                    //心跳有应答消息，异步应答后会自动从futureManager删除
                    if (!r.isSuccess()) {
                        futureManager.completeExceptionally(heartbeatMessage.getMsgId(), r.getThrowable());
                        logger.error(String.format("Error occurs while sending heartbeat to %s, caused by:",
                                Channel.toString(channel.getRemoteAddress())), r.getThrowable());
                    }
                });
            } else {
                publisher.offer(new InactiveEvent(channel));
            }
        }
    }

}
