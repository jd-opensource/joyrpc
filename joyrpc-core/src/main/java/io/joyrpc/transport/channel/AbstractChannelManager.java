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
import io.joyrpc.exception.ChannelClosedException;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.exception.LafException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.DefaultHeartbeatTrigger;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.heartbeat.HeartbeatTrigger;
import io.joyrpc.transport.transport.ClientTransport;
import io.joyrpc.util.Futures;
import io.joyrpc.util.StateController;
import io.joyrpc.util.StateMachine.IntStateMachine;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.joyrpc.util.Timer.timer;

/**
 * 抽象的通道管理器
 */
public abstract class AbstractChannelManager implements ChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(AbstractChannelManager.class);

    /**
     * channel
     */
    protected Map<String, PoolChannel> channels = new ConcurrentHashMap<>();
    /**
     * 关闭的消费者
     */
    protected Consumer<PoolChannel> beforeClose;

    /**
     * 构造函数
     *
     * @param url
     */
    protected AbstractChannelManager(URL url) {
        this.beforeClose = o -> channels.remove(o.name);
    }

    @Override
    public CompletableFuture<Channel> getChannel(final ClientTransport transport, final Connector connector) {
        if (connector == null) {
            return Futures.completeExceptionally(new ConnectionException("opener can not be null."));
        } else {
            //创建缓存通道
            PoolChannel poolChannel = channels.computeIfAbsent(transport.getChannelName(),
                    o -> new PoolChannel(transport, connector, beforeClose));
            return poolChannel.connect();
        }
    }

    @Override
    public abstract String getChannelKey(ClientTransport transport);

    /**
     * 池化的通道
     */
    protected static class PoolChannel extends DecoratorChannel implements Connector {
        protected static final Function<String, Throwable> THROWABLE_FUNCTION = error -> new ConnectionException(error);
        /**
         * 消息发布
         */
        protected Publisher<TransportEvent> publisher;
        /**
         * URL
         */
        protected URL url;
        /**
         * 名称
         */
        protected String name;
        /**
         * 心跳策略
         */
        protected HeartbeatStrategy strategy;
        /**
         * 心跳
         */
        protected HeartbeatTrigger trigger;
        /**
         * 打开
         */
        protected Connector connector;
        /**
         * 计数器
         */
        protected AtomicLong counter = new AtomicLong(0);
        /**
         * 关闭回调
         */
        protected Consumer<PoolChannel> beforeClose;
        /**
         * 状态机
         */
        protected IntStateMachine<Channel, StateController<Channel>> stateMachine = new IntStateMachine<>(() -> new PoolChannelController(this), THROWABLE_FUNCTION);

        /**
         * 构造函数
         *
         * @param transport   通道
         * @param connector   连接器
         * @param beforeClose 关闭前事件
         */
        protected PoolChannel(final ClientTransport transport,
                              final Connector connector,
                              final Consumer<PoolChannel> beforeClose) {
            super(null);
            this.publisher = transport.getPublisher();
            this.name = transport.getChannelName();
            this.connector = connector;
            this.beforeClose = beforeClose;
            this.strategy = transport.getHeartbeatStrategy();
        }

        @Override
        public CompletableFuture<Channel> connect() {
            CompletableFuture<Channel> future = new CompletableFuture<>();
            stateMachine.open().whenComplete((ch, error) -> {
                if (error == null) {
                    counter.incrementAndGet();
                    future.complete(this);
                } else {
                    future.completeExceptionally(error);
                }
            });
            return future;
        }

        /**
         * 建连
         *
         * @return CompletableFuture
         */
        protected CompletableFuture<Channel> doConnect(PoolChannelController controller) {
            CompletableFuture<Channel> future = new CompletableFuture<>();
            connector.connect().whenComplete((ch, error) -> {
                if (error == null) {
                    channel = ch;
                    ch.setAttribute(CHANNEL_KEY, name);
                    ch.setAttribute(EVENT_PUBLISHER, publisher);
                    ch.getFutureManager().open();
                    publisher.start();
                    trigger = strategy == null || strategy.getHeartbeat() == null ? null :
                            new DefaultHeartbeatTrigger(this, url, strategy, publisher);
                    if (trigger != null) {
                        switch (strategy.getHeartbeatMode()) {
                            case IDLE:
                                //利用Channel的Idle事件进行心跳检测
                                ch.setAttribute(Channel.IDLE_HEARTBEAT_TRIGGER, trigger);
                                break;
                            case TIMING:
                                //定时心跳
                                timer().add(new HeartbeatTask(this, controller));
                        }
                    }
                    future.complete(ch);
                } else {
                    future.completeExceptionally(error);
                }
            });
            return future;
        }

        @Override
        public void send(final Object object, final Consumer<SendResult> consumer) {
            if (stateMachine.isOpened()) {
                super.send(object, consumer);
            } else {
                LafException throwable = new ChannelClosedException(
                        String.format("Send request exception, causing channel is not opened. at  %s : %s",
                                Channel.toString(this), object.toString()));
                if (consumer != null) {
                    consumer.accept(new SendResult(throwable, this));
                } else {
                    throw throwable;
                }
            }
        }

        @Override
        public boolean isActive() {
            return super.isActive() && stateMachine.isOpened();
        }

        /**
         * 判断是否还有正在处理的请求
         *
         * @return 是否还有正在处理的请求标识
         */
        protected boolean isEmpty() {
            return channel.getFutureManager().isEmpty() || !channel.isActive();
        }

        @Override
        public CompletableFuture<Channel> close() {
            if (counter.decrementAndGet() == 0) {
                //从池中移除掉
                return stateMachine.close(false, () -> beforeClose.accept(this));
            } else {
                return CompletableFuture.completedFuture(channel);
            }
        }

        /**
         * 关闭
         *
         * @param future
         */
        protected void doClose(final CompletableFuture<Channel> future) {
            channel.close().whenComplete((ch, error) -> {
                publisher.close();
                future.complete(ch);
            });
        }
    }

    /**
     * 控制器
     */
    protected static class PoolChannelController implements StateController<Channel> {

        /**
         * 连接通道
         */
        protected final PoolChannel channel;

        public PoolChannelController(PoolChannel channel) {
            this.channel = channel;
        }

        @Override
        public CompletableFuture<Channel> open() {
            return channel.doConnect(this);
        }

        @Override
        public CompletableFuture<Channel> close(boolean gracefully) {
            CompletableFuture future = new CompletableFuture();
            if (channel.isEmpty()) {
                //没有请求，或者channel已经不可以，立即关闭
                channel.doClose(future);
            } else {
                //异步关闭
                timer().add(new CloseChannelTask(channel, future));
            }
            return future;
        }
    }

    /**
     * 异步关闭Channel任务
     */
    protected static class CloseChannelTask implements Timer.TimeTask {

        /**
         * Channel
         */
        protected PoolChannel channel;
        /**
         * 消费者
         */
        protected CompletableFuture<Channel> future;

        /**
         * 构造函数
         *
         * @param channel
         * @param future
         */
        public CloseChannelTask(final PoolChannel channel, final CompletableFuture<Channel> future) {
            this.channel = channel;
            this.future = future;
        }

        @Override
        public String getName() {
            return this.getClass().getSimpleName() + "-" + channel.name;
        }

        @Override
        public long getTime() {
            return SystemClock.now() + 400L;
        }

        @Override
        public void run() {
            if (channel.isEmpty()) {
                //没有请求了
                channel.doClose(future);
            } else {
                //等等
                timer().add(this);
            }
        }
    }

    /**
     * 心跳任务
     */
    protected static class HeartbeatTask implements Timer.TimeTask {

        /**
         * Channel
         */
        protected final PoolChannel channel;
        /**
         * 控制器
         */
        protected final StateController<Channel> controller;
        /**
         * 策略
         */
        protected final HeartbeatStrategy strategy;
        /**
         * 心跳触发器
         */
        protected final HeartbeatTrigger trigger;
        /**
         * 名称
         */
        protected final String name;
        /**
         * 心跳时间间隔
         */
        protected final int interval;
        /**
         * 心跳时间
         */
        protected long time;

        /**
         * 构造函数
         *
         * @param channel    连接通道
         * @param controller 控制器
         */
        public HeartbeatTask(final PoolChannel channel, final StateController<Channel> controller) {
            this.channel = channel;
            this.controller = controller;
            this.trigger = channel.trigger;
            this.strategy = trigger.strategy();
            this.interval = strategy.getInterval() <= 0 ? HeartbeatStrategy.DEFAULT_INTERVAL : strategy.getInterval();
            this.time = SystemClock.now() + ThreadLocalRandom.current().nextInt(interval);
            this.name = this.getClass().getSimpleName() + "-" + channel.name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public void run() {
            //添加任务是在opening状态
            if (channel.stateMachine.isOpen(controller)) {
                if (channel.stateMachine.isOpened(controller)) {
                    //打开状态才执行心跳
                    try {
                        trigger.run();
                    } catch (Exception e) {
                        logger.error(String.format("Error occurs while trigger heartbeat to %s, caused by: %s",
                                Channel.toString(channel.getRemoteAddress()), e.getMessage()), e);
                    }
                }
                time = SystemClock.now() + interval;
                timer().add(this);
            }
        }
    }

}
