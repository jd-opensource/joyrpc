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
import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.channel.ServerChannel;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.ProtocolAdapter;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.util.*;
import io.joyrpc.util.StateMachine.IntStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static io.joyrpc.Plugin.EVENT_BUS;
import static io.joyrpc.constants.Constants.EVENT_PUBLISHER_SERVER_NAME;
import static io.joyrpc.constants.Constants.EVENT_PUBLISHER_TRANSPORT_CONF;
import static io.joyrpc.util.Timer.timer;

/**
 * 抽象的服务通道
 */
public abstract class AbstractServerTransport implements ServerTransport {
    private static final Logger logger = LoggerFactory.getLogger(AbstractServerTransport.class);
    public static final Function<String, Throwable> THROWABLE_FUNCTION = error -> new ConnectionException(error);
    /**
     * 计数器
     */
    protected static AtomicLong COUNTER = new AtomicLong(0);
    /**
     * 编码
     */
    protected Codec codec;
    /**
     * 协议适配器
     */
    protected ProtocolAdapter adapter;
    /**
     * 处理链
     */
    protected ChannelHandlerChain chain;
    /**
     * 服务URL参数
     */
    protected URL url;
    /**
     * 监听的IP
     */
    protected String host;
    /**
     * 服务通道
     */
    protected ServerChannel serverChannel;
    /**
     * 上下文
     */
    protected Map<Channel, ChannelTransport> transports = new ConcurrentHashMap<>();
    /**
     * 业务线程池
     */
    protected ThreadPoolExecutor bizThreadPool;
    /**
     * 事件发布器
     */
    protected Publisher<TransportEvent> publisher;
    /**
     * 打开
     */
    protected Function<ServerTransport, CompletableFuture<Void>> beforeOpen;
    /**
     * 关闭
     */
    protected Function<ServerTransport, CompletableFuture<Void>> afterClose;
    /**
     * ID
     */
    protected int transportId = ID_GENERATOR.get();
    /**
     * 状态机
     */
    protected IntStateMachine<Channel, StateController<Channel>> stateMachine = new IntStateMachine<>(
            () -> new TransportController(this), THROWABLE_FUNCTION,
            new StateFuture<>(
                    () -> beforeOpen == null ? null : beforeOpen.apply(AbstractServerTransport.this),
                    () -> afterClose == null ? null : afterClose.apply(AbstractServerTransport.this)));

    public AbstractServerTransport(URL url) {
        this(url, null, null);
    }

    public AbstractServerTransport(final URL url,
                                   final Function<ServerTransport, CompletableFuture<Void>> beforeOpen,
                                   final Function<ServerTransport, CompletableFuture<Void>> afterClose) {
        this.url = url;
        this.host = url.getString(Constants.BIND_IP_KEY, url.getHost());
        this.publisher = EVENT_BUS.get().getPublisher(EVENT_PUBLISHER_SERVER_NAME,
                String.valueOf(COUNTER.incrementAndGet()), EVENT_PUBLISHER_TRANSPORT_CONF);
        this.beforeOpen = beforeOpen;
        this.afterClose = afterClose;
    }

    @Override
    public CompletableFuture<Channel> open() {
        return stateMachine.open();
    }

    @Override
    public CompletableFuture<Channel> close() {
        return stateMachine.close(false);
    }

    /**
     * 启动服务
     *
     * @param host 地址
     * @param port 端口
     */
    protected abstract CompletableFuture<Channel> bind(String host, int port);

    @Override
    public State getState() {
        return stateMachine.getState();
    }

    /**
     * 返回所有的Channel
     *
     * @return
     */
    public List<Channel> getChannels() {
        return new ArrayList<>(transports.keySet());
    }

    @Override
    public List<ChannelTransport> getChannelTransports() {
        return new ArrayList<>(transports.values());
    }

    @Override
    public ServerChannel getServerChannel() {
        return serverChannel;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return serverChannel.getLocalAddress();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setChannelHandlerChain(final ChannelHandlerChain chain) {
        this.chain = chain;
    }

    @Override
    public void setCodec(final Codec codec) {
        this.codec = codec;
    }

    @Override
    public void setAdapter(final ProtocolAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void setBizThreadPool(final ThreadPoolExecutor bizThreadPool) {
        this.bizThreadPool = bizThreadPool;
    }

    @Override
    public ThreadPoolExecutor getBizThreadPool() {
        return this.bizThreadPool;
    }

    @Override
    public void addEventHandler(final EventHandler handler) {
        publisher.addHandler(handler);
    }

    @Override
    public void removeEventHandler(final EventHandler handler) {
        publisher.removeHandler(handler);
    }

    @Override
    public int getTransportId() {
        return transportId;
    }

    /**
     * 绑定Channel和Transport
     *
     * @param channel
     * @param transport
     */
    protected void addChannel(final Channel channel, final ChannelTransport transport) {
        if (channel != null && transport != null) {
            transports.put(channel, transport);
            try {
                timer().add(new EvictSessionTask(channel));
            } catch (Exception e) {
                logger.error(String.format("Error occurs while add evict session task for channel %s,  caused by: %s",
                        Channel.toString(channel.getRemoteAddress()), e.getMessage()), e);
                throw e;
            }
        }
    }

    /**
     * 删除Channel
     *
     * @param channel
     */
    protected void removeChannel(final Channel channel) {
        if (channel != null) {
            transports.remove(channel);
        }
    }

    /**
     * 清理过期的任务
     */
    protected static class EvictSessionTask implements Timer.TimeTask {
        /**
         * 通道
         */
        protected Channel channel;
        /**
         * 任务名称
         */
        protected String name;
        /**
         * 清理时间
         */
        protected long time;
        /**
         * 清理时间间隔
         */
        protected int interval = 10000;

        /**
         * 构造函数
         *
         * @param channel
         */
        public EvictSessionTask(Channel channel) {
            this.channel = channel;
            this.name = this.getClass().getSimpleName() + "-" + Channel.toString(channel.getRemoteAddress());
            this.time = SystemClock.now() + interval;
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
            if (channel.isActive()) {
                try {
                    channel.evictSession();
                } catch (Exception e) {
                    logger.error(
                            String.format("Error occurs while run evict session task for channel %s,  caused by: %s",
                                    Channel.toString(channel.getRemoteAddress()), e.getMessage()), e);
                }
                time = SystemClock.now() + interval;
                timer().add(this);
            }
        }
    }

    /**
     * 控制器
     */
    protected static class TransportController implements StateController<Channel>, EventHandler<StateEvent> {
        /**
         * 通道
         */
        protected AbstractServerTransport transport;

        public TransportController(AbstractServerTransport transport) {
            this.transport = transport;
        }

        @Override
        public void handle(final StateEvent event) {
            switch (event.getType()) {
                case StateEvent.SUCCESS_OPEN:
                    logger.info(String.format("Success binding server to %s:%d", transport.host, transport.url.getPort()));
                    break;
                case StateEvent.FAIL_OPEN:
                    logger.error(String.format("Failed binding server to %s:%d", transport.host, transport.url.getPort()));
                    break;
                case StateEvent.SUCCESS_CLOSE:
                    logger.info(String.format("Success destroying server at %s:%d", transport.host, transport.url.getPort()));
                    break;
            }
        }

        @Override
        public CompletableFuture<Channel> open() {
            return transport.bind(transport.host, transport.url.getPort()).whenComplete((ch, e) -> {
                if (e == null) {
                    transport.serverChannel = (ServerChannel) ch;
                    transport.publisher.start();
                }
            });
        }

        @Override
        public CompletableFuture<Channel> close(boolean gracefully) {
            ServerChannel ch = transport.serverChannel;
            CompletableFuture<Channel> future = (ch == null ? CompletableFuture.completedFuture(null) : ch.close());
            return future.whenComplete((c, error) -> {
                transport.publisher.close();
                //channel不设置为null，防止正在处理的请求报空指针错误
                //serverChannel = null;
            });
        }
    }
}
