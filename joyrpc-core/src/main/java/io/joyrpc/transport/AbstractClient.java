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

import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.channel.ChannelManager;
import io.joyrpc.transport.channel.ChannelManager.Connector;
import io.joyrpc.transport.channel.ChannelManagerFactory;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.util.State;
import io.joyrpc.util.StateController;
import io.joyrpc.util.StateMachine.IntStateMachine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static io.joyrpc.Plugin.CHANNEL_MANAGER_FACTORY;
import static io.joyrpc.Plugin.EVENT_BUS;
import static io.joyrpc.constants.Constants.*;

/**
 * 抽象的客户端通道
 */
public abstract class AbstractClient extends DefaultChannelTransport implements TransportClient {

    protected static final Function<String, Throwable> THROWABLE_FUNCTION = error -> new ConnectionException(error);
    /**
     * 编解码
     */
    protected Codec codec;
    /**
     * 处理链
     */
    protected ChannelChain chain;
    /**
     * Channel管理器
     */
    protected ChannelManager channelManager;
    /**
     * 心跳策略
     */
    protected HeartbeatStrategy heartbeatStrategy;
    /**
     * 业务线程池
     */
    protected ExecutorService workerPool;
    /**
     * 名称
     */
    protected String name;
    /**
     * 事件发布器
     */
    protected Publisher<TransportEvent> publisher;
    /**
     * 客户端协议
     */
    protected ClientProtocol protocol;
    /**
     * 打开的结果
     */
    protected IntStateMachine<Channel, StateController<Channel>> stateMachine = new IntStateMachine<>(
            () -> new StateController<Channel>() {
                @Override
                public CompletableFuture<Channel> open() {
                    return channelManager.connect(AbstractClient.this, getConnector()).whenComplete((ch, error) -> channel = ch);
                }

                @Override
                public CompletableFuture<Channel> close(boolean gracefully) {
                    int id = transportId;
                    Channel ch = channel;
                    if (ch == null) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return ch.close().whenComplete((c, error) -> {
                            //channel不设置为null，防止正在处理的请求报空指针错误
                            //channel = null;
                            ch.removeSession(id);
                        });
                    }
                }
            }, THROWABLE_FUNCTION);

    /**
     * 构造函数
     *
     * @param url
     */
    public AbstractClient(URL url) {
        super(url);
        ChannelManagerFactory factory = CHANNEL_MANAGER_FACTORY.getOrDefault(url.getString(CHANNEL_MANAGER_FACTORY_OPTION));
        this.channelManager = factory.getChannelManager(url);
        this.name = channelManager.getChannelKey(this);
        this.publisher = EVENT_BUS.get().getPublisher(EVENT_PUBLISHER_CLIENT_NAME, name, EVENT_PUBLISHER_TRANSPORT_CONF);
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
     * 获取连接器
     *
     * @return 连接器
     */
    protected abstract Connector getConnector();

    @Override
    public int getRequests() {
        return requests.get();
    }

    @Override
    public void setHeartbeatStrategy(final HeartbeatStrategy heartbeatStrategy) {
        this.heartbeatStrategy = heartbeatStrategy;
    }

    @Override
    public HeartbeatStrategy getHeartbeatStrategy() {
        return heartbeatStrategy;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Publisher<TransportEvent> getPublisher() {
        return publisher;
    }

    @Override
    public State getState() {
        return stateMachine.getState();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setChain(final ChannelChain chain) {
        this.chain = chain;
    }

    @Override
    public void setCodec(final Codec codec) {
        this.codec = codec;
    }

    @Override
    public ExecutorService getWorkerPool() {
        return this.workerPool;
    }

    @Override
    public void addEventHandler(final EventHandler<? extends TransportEvent> handler) {
        if (handler != null) {
            publisher.addHandler((EventHandler<TransportEvent>) handler);
        }
    }

    @Override
    public void removeEventHandler(final EventHandler<? extends TransportEvent> handler) {
        if (handler != null) {
            publisher.removeHandler((EventHandler<TransportEvent>) handler);
        }
    }

    @Override
    public ClientProtocol getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(final ClientProtocol protocol) {
        this.protocol = protocol;
        if (channel != null) {
            channel.setAttribute(Channel.PROTOCOL, protocol);
        }
    }

}
