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
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.ChannelClosedException;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.TransportException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.DefaultHeartbeatTrigger;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.heartbeat.HeartbeatTrigger;
import io.joyrpc.transport.transport.ClientTransport;
import io.joyrpc.util.Status;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import static io.joyrpc.util.Status.*;
import static io.joyrpc.util.Timer.timer;

/**
 * @date: 2019/3/7
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
    public void getChannel(final ClientTransport transport,
                           final Consumer<AsyncResult<Channel>> consumer,
                           final Connector connector) {
        if (connector == null) {
            if (consumer != null) {
                consumer.accept(new AsyncResult<>(new ConnectionException("opener can not be null.")));
            }
            return;
        }
        //创建缓存通道
        channels.computeIfAbsent(transport.getChannelName(),
                o -> new PoolChannel(transport, connector, beforeClose)).connect(consumer);
    }

    @Override
    public abstract String getChannelKey(ClientTransport transport);

    /**
     * 池化的通道
     */
    protected static class PoolChannel extends DecoratorChannel {
        protected static final AtomicReferenceFieldUpdater<PoolChannel, Status> STATE_UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(PoolChannel.class, Status.class, "status");
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
         * 消费者
         */
        protected Queue<Consumer<AsyncResult<Channel>>> consumers = new ConcurrentLinkedQueue<>();
        /**
         * 状态
         */
        protected volatile Status status = CLOSED;
        /**
         * 计数器
         */
        protected AtomicLong counter = new AtomicLong(0);
        /**
         * 心跳失败次数
         */
        protected AtomicInteger heartbeatFails = new AtomicInteger(0);
        /**
         * 关闭回调
         */
        protected Consumer<PoolChannel> beforeClose;
        /**
         * 连接消费者
         */
        protected Consumer<AsyncResult<Channel>> afterConnect;


        /**
         * 构造函数
         *
         * @param transport
         * @param connector
         * @param beforeClose
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

            this.afterConnect = event -> {
                if (event.isSuccess()) {
                    addRef();
                }
            };
        }

        /**
         * 建立连接
         *
         * @param consumer
         */
        protected void connect(final Consumer<AsyncResult<Channel>> consumer) {
            //连接成功处理器
            final Consumer<AsyncResult<Channel>> c = consumer == null ? afterConnect : afterConnect.andThen(consumer);
            //修改状态
            if (STATE_UPDATER.compareAndSet(this, CLOSED, OPENING)) {
                consumers.offer(c);
                connector.connect(r -> {
                            if (r.isSuccess()) {
                                channel = r.getResult();
                                channel.setAttribute(CHANNEL_KEY, name);
                                channel.setAttribute(EVENT_PUBLISHER, publisher);
                                channel.setAttribute(HEARTBEAT_FAILED_COUNT, heartbeatFails);
                                channel.getFutureManager().open();
                                STATE_UPDATER.set(this, OPENED);
                                //后面添加心跳，防止心跳检查状态退出
                                trigger = strategy == null || strategy.getHeartbeat() == null ? null :
                                        new DefaultHeartbeatTrigger(this, url, strategy, publisher);
                                if (trigger != null) {
                                    switch (strategy.getHeartbeatMode()) {
                                        case IDLE:
                                            //利用Channel的Idle事件进行心跳检测
                                            channel.setAttribute(Channel.IDLE_HEARTBEAT_TRIGGER, trigger);
                                            break;
                                        case TIMING:
                                            //定时心跳
                                            timer().add(new HeartbeatTask(this));
                                    }
                                }

                                publisher.start();
                                publish(new AsyncResult<>(PoolChannel.this));
                            } else {
                                STATE_UPDATER.set(this, CLOSED);
                                publish(new AsyncResult<>(r.getThrowable()));
                            }
                        }
                );
            } else {
                switch (status) {
                    case OPENED:
                        c.accept(new AsyncResult(PoolChannel.this));
                        break;
                    case OPENING:
                        consumers.add(c);
                        //二次判断，防止并发
                        switch (status) {
                            case OPENING:
                                break;
                            case OPENED:
                                publish(new AsyncResult<>(PoolChannel.this));
                                break;
                            default:
                                publish(new AsyncResult<>(new ConnectionException()));
                                break;
                        }
                        break;
                    default:
                        c.accept(new AsyncResult<>(new ConnectionException()));

                }
            }
        }

        /**
         * 发布消息
         *
         * @param result
         */
        protected void publish(final AsyncResult result) {
            Consumer<AsyncResult<Channel>> consumer;
            while ((consumer = consumers.poll()) != null) {
                consumer.accept(result);
            }
        }

        /**
         * 怎讲引用计数
         *
         * @return
         */
        protected long addRef() {
            return counter.incrementAndGet();
        }

        @Override
        public void send(final Object object, final Consumer<SendResult> consumer) {
            switch (status) {
                case OPENED:
                    super.send(object, consumer);
                    break;
                default:
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
            return super.isActive() && status == OPENED;
        }

        @Override
        public boolean close() {
            CountDownLatch latch = new CountDownLatch(1);
            final Throwable[] err = new Throwable[1];
            final boolean[] res = new boolean[]{false};
            try {
                close(r -> {
                    if (r.getThrowable() != null) {
                        err[0] = r.getThrowable();
                    } else if (!r.isSuccess()) {
                        res[0] = false;
                    }
                });
                latch.await();
            } catch (InterruptedException e) {
            }
            if (err[0] != null) {
                throw new TransportException(err[0]);
            }
            return res[0];
        }

        @Override
        public void close(final Consumer<AsyncResult<Channel>> consumer) {
            if (counter.decrementAndGet() == 0) {
                beforeClose.accept(this);
                if (STATE_UPDATER.compareAndSet(this, OPENED, CLOSING)) {
                    if (channel.getFutureManager().isEmpty() || !channel.isActive()) {
                        //没有请求，或者channel已经不可以，立即关闭
                        doClose(consumer);
                    } else {
                        //异步关闭
                        timer().add(new CloseChannelTask(this, consumer));
                    }
                } else {
                    switch (status) {
                        case OPENING:
                        case OPENED:
                            timer().add(new CloseChannelTask(this, consumer));
                            break;
                        default:
                            Optional.ofNullable(consumer).ifPresent(o -> o.accept(new AsyncResult<>(this)));
                    }
                }
            } else {
                Optional.ofNullable(consumer).ifPresent(o -> o.accept(new AsyncResult<>(this)));
            }
        }

        /**
         * 关闭
         *
         * @param consumer
         */
        protected void doClose(final Consumer<AsyncResult<Channel>> consumer) {
            channel.close(r -> {
                STATE_UPDATER.set(this, CLOSED);
                publisher.close();
                consumer.accept(r);
            });
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
        protected Consumer<AsyncResult<Channel>> consumer;

        /**
         * 构造函数
         *
         * @param channel
         * @param consumer
         */
        public CloseChannelTask(PoolChannel channel, Consumer<AsyncResult<Channel>> consumer) {
            this.channel = channel;
            this.consumer = consumer;
        }

        @Override
        public String getName() {
            return "CloseChannelTask-" + channel.name;
        }

        @Override
        public long getTime() {
            return SystemClock.now() + 400L;
        }

        @Override
        public void run() {
            if (channel.getFutureManager().isEmpty() || !channel.isActive()) {
                //没有请求了,或channel已经不可用
                channel.doClose(consumer);
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
        protected PoolChannel channel;
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
         * @param channel
         */
        public HeartbeatTask(final PoolChannel channel) {
            this.channel = channel;
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
            if (channel.status == OPENED) {//只有在该状态下执行，手工关闭状态不要触发
                try {
                    trigger.run();
                } catch (Exception e) {
                    logger.error(String.format("Error occurs while trigger heartbeat to %s, caused by: %s",
                            Channel.toString(channel.getRemoteAddress()), e.getMessage()), e);
                }
                time = SystemClock.now() + interval;
                timer().add(this);
            }
            logger.debug(String.format("Heartbeat task was run， channel %s status is %s.",
                    Channel.toString(channel.getRemoteAddress()), channel.status.name()));
        }
    }

}
