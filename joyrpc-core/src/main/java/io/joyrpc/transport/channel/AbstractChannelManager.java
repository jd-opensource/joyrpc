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
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.exception.TransportException;
import io.joyrpc.extension.URL;
import io.joyrpc.thread.NamedThreadFactory;
import io.joyrpc.transport.Endpoint;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.DefaultHeartbeatTrigger;
import io.joyrpc.transport.heartbeat.HeartbeatManager;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.transport.ClientTransport;
import io.joyrpc.util.Shutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.HEARTBEAT_MANAGER_FACTORY;

/**
 * @date: 2019/3/7
 */
public abstract class AbstractChannelManager implements ChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(AbstractChannelManager.class);

    protected static final Queue<ChannelCloser> CLOSERS = new ConcurrentLinkedQueue<>();
    /**
     * 线程池
     */
    protected static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("AbstractChannelManager", true));
    /**
     * channel
     */
    protected Map<String, PoolChannel> channels = new ConcurrentHashMap<>();
    /**
     * 心跳管理器
     */
    protected HeartbeatManager heartbeatManager;

    static {
        scheduler.scheduleWithFixedDelay(() -> {
                    List<ChannelCloser> retry = new LinkedList<>();
                    while (!CLOSERS.isEmpty()) {
                        ChannelCloser closer = CLOSERS.poll();
                        if (!closer.close()) {
                            retry.add(closer);
                        }
                    }
                    //没有关闭成功的，添加回队列
                    if (!retry.isEmpty()) {
                        CLOSERS.addAll(retry);
                    }
                },
                200, 200, TimeUnit.MILLISECONDS);
        Shutdown.addHook(scheduler::shutdown);
    }

    /**
     * 构造函数
     *
     * @param url
     */
    protected AbstractChannelManager(URL url) {
        this.heartbeatManager = HEARTBEAT_MANAGER_FACTORY.get().get(url);
    }

    @Override
    public void getChannel(final ClientTransport transport, final Consumer<AsyncResult<Channel>> consumer, final ChannelOpener opener) {
        if (opener == null) {
            if (consumer != null) {
                consumer.accept(new AsyncResult<>(new ConnectionException("opener can not be null.")));
            }
            return;
        }
        //计数器
        PoolChannel channel = channels.computeIfAbsent(transport.getChannelName(), o -> new PoolChannel(transport.getPublisher(), opener));
        //Channel open成功，ChannelManager 处理consumer
        Consumer<AsyncResult<Channel>> succeed = event -> {
            if (event.isSuccess()) {
                //transport计数累加
                channel.addRef();
                //channel设置key
                channel.getAttribute(Channel.CHANNEL_KEY, o -> transport.getChannelName());
                //添加心跳
                HeartbeatStrategy heartbeatStrategy = transport.getHeartbeatStrategy();
                if (heartbeatStrategy != null && heartbeatStrategy.getHeartbeat() != null) {
                    heartbeatManager.add(channel, heartbeatStrategy,
                            () -> new DefaultHeartbeatTrigger(channel, transport.getUrl(), heartbeatStrategy, transport.getPublisher()));
                }
            }
        };
        //执行open channel的操作
        channel.open(consumer == null ? succeed : succeed.andThen(consumer));
    }

    @Override
    public abstract String getChannelKey(ClientTransport transport);

    /**
     * 池化的通道
     */
    protected class PoolChannel extends DecoratorChannel {
        /**
         * 打开
         */
        protected ChannelOpener opener;
        /**
         * 消费者
         */
        protected Queue<Consumer<AsyncResult<Channel>>> consumers = new ConcurrentLinkedQueue<>();
        /**
         * 状态
         */
        protected AtomicReference<Endpoint.Status> status = new AtomicReference<>(Endpoint.Status.CLOSED);
        /**
         * 计数器
         */
        protected AtomicLong counter = new AtomicLong(0);
        /**
         * 消息发布
         */
        protected Publisher<TransportEvent> publisher;

        /**
         * 构造函数
         *
         * @param publisher
         * @param opener
         */
        protected PoolChannel(Publisher<TransportEvent> publisher, ChannelOpener opener) {
            super(null);
            this.publisher = publisher;
            this.opener = opener;
        }

        /**
         * 建立连接
         *
         * @param consumer
         */
        protected void open(final Consumer<AsyncResult<Channel>> consumer) {
            if (status.compareAndSet(Endpoint.Status.CLOSED, Endpoint.Status.OPENING)) {
                consumers.offer(consumer);
                opener.openChannel(r -> {
                            if (r.isSuccess()) {
                                channel = r.getResult();
                                channel.setAttribute(EVENT_PUBLISHER, publisher);
                                //开启futrueManager
                                channel.getFutureManager().open();
                                status.set(Endpoint.Status.OPENED);
                                publisher.start();
                                publish();
                            } else {
                                status.set(Endpoint.Status.CLOSED);
                                publish(r.getThrowable());
                            }
                        }
                );
            } else {
                switch (status.get()) {
                    case OPENED:
                        consumer.accept(new AsyncResult(PoolChannel.this));
                        break;
                    case OPENING:
                        consumers.add(consumer);
                        //二次判断，防止并发
                        switch (status.get()) {
                            case OPENING:
                                break;
                            case OPENED:
                                publish();
                                break;
                            default:
                                publish(new ConnectionException());
                                break;
                        }
                        break;
                    default:
                        consumer.accept(new AsyncResult<>(new ConnectionException()));

                }
            }
        }

        /**
         * 发布消息
         */
        protected void publish() {
            while (!consumers.isEmpty()) {
                Consumer<AsyncResult<Channel>> consumer = consumers.poll();
                if (consumer != null) {
                    consumer.accept(new AsyncResult<>(PoolChannel.this));
                }
            }
        }

        /**
         * 发布异常消息
         *
         * @param throwable
         */
        protected void publish(final Throwable throwable) {
            while (!consumers.isEmpty()) {
                Consumer<AsyncResult<Channel>> consumer = consumers.poll();
                if (consumer != null) {
                    consumer.accept(new AsyncResult<>(throwable));
                }
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
        public boolean isActive() {
            boolean isActive = super.isActive();
            boolean isOpened = status.get() == Endpoint.Status.OPENED;
            if (isActive && isOpened) {
                return true;
            }
            if (isActive != isOpened) {
                logger.warn(String.format("Channel status is error, channel active status is %b, but the open status is %b.", isActive, isOpened));
            }
            return false;
        }

        @Override
        public boolean close() {
            CountDownLatch latch = new CountDownLatch(1);
            final Throwable[] err = new Throwable[1];
            final boolean[] res = new boolean[]{true};
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
            if (status.compareAndSet(Endpoint.Status.OPENED, Endpoint.Status.CLOSING)) {
                if (channel.getFutureManager().isEmpty() || !channel.isActive()) {
                    //没有请求，或者channel已经不可以，立即关闭
                    doClose(consumer);
                } else {
                    //异步关闭
                    CLOSERS.add(new ChannelCloser(this, consumer));
                }
            }
        }

        /**
         * 关闭
         *
         * @param consumer
         */
        protected void doClose(final Consumer<AsyncResult<Channel>> consumer) {
            channel.close(r -> {
                status.set(Endpoint.Status.CLOSED);
                publisher.close();
                consumer.accept(r);
            });
        }

        @Override
        public boolean disconnect() {
            if (counter.decrementAndGet() == 0) {
                heartbeatManager.remove(this);
                channels.remove(channel.getAttribute(CHANNEL_KEY));
                return close();
            }
            return true;
        }

        @Override
        public void disconnect(final Consumer<AsyncResult<Channel>> consumer) {
            if (counter.decrementAndGet() == 0) {
                channel.close(r -> {
                    heartbeatManager.remove(this);
                    channels.remove(channel.getAttribute(CHANNEL_KEY));
                    publisher.close();
                    if (r.isSuccess()) {
                        consumer.accept(new AsyncResult<>(this));
                    } else {
                        consumer.accept(new AsyncResult<>(this, r.getThrowable()));
                    }
                });
            } else {
                consumer.accept(new AsyncResult<>(this));
            }
        }

    }

    /**
     * Channel关闭器
     */
    protected class ChannelCloser {
        /**
         * Channel
         */
        protected PoolChannel poolChannel;

        /**
         * 消费者
         */
        protected Consumer<AsyncResult<Channel>> consumer;

        /**
         * 构造函数
         *
         * @param poolChannel
         * @param consumer
         */
        public ChannelCloser(PoolChannel poolChannel, Consumer<AsyncResult<Channel>> consumer) {
            this.poolChannel = poolChannel;
            this.consumer = consumer;
        }

        /**
         * 关闭
         */
        public boolean close() {
            if (poolChannel.status.get() == Endpoint.Status.OPENED) {
                //重新打开了，不需要关闭，直接remove
                return true;
            } else if (poolChannel.getFutureManager().isEmpty() || !poolChannel.channel.isActive()) {
                //没有请求了,或channl已经不可用
                poolChannel.doClose(consumer);
                return true;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return Objects.equals(poolChannel, ((ChannelCloser) o).poolChannel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(poolChannel);
        }
    }

}
