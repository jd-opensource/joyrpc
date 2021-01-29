package io.joyrpc.cluster;

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

import io.joyrpc.cluster.event.MetricEvent;
import io.joyrpc.cluster.event.NodeEvent;
import io.joyrpc.cluster.event.OfflineEvent;
import io.joyrpc.cluster.event.SessionLostEvent;
import io.joyrpc.codec.checksum.Checksum;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.constants.Constants;
import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.AuthenticationException;
import io.joyrpc.exception.ChannelClosedException;
import io.joyrpc.exception.ProtocolException;
import io.joyrpc.extension.URL;
import io.joyrpc.metric.Dashboard;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.protocol.Protocol.ProtocolVersion;
import io.joyrpc.protocol.message.HeartbeatAware;
import io.joyrpc.protocol.message.Response;
import io.joyrpc.protocol.message.SuccessResponse;
import io.joyrpc.protocol.message.heartbeat.HeartbeatResponse;
import io.joyrpc.protocol.message.negotiation.NegotiationResponse;
import io.joyrpc.transport.Client;
import io.joyrpc.transport.DecoratorClient;
import io.joyrpc.transport.EndpointFactory;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.event.HeartbeatEvent;
import io.joyrpc.transport.event.InactiveEvent;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.message.Header;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.transport.ClientTransport;
import io.joyrpc.util.Shutdown;
import io.joyrpc.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.Futures.whenComplete;
import static io.joyrpc.util.Timer.timer;

/**
 * 节点
 */
public class Node implements Shard {
    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    protected static final String VERSION = "version";
    protected static final String DISCONNECT_WHEN_HEARTBEAT_FAILS = "disconnectWhenHeartbeatFails";
    public static final String START_TIMESTAMP = "startTime";

    /**
     * 集群URL
     */
    protected URL clusterUrl;
    /**
     * 集群名称
     */
    protected String clusterName;
    /**
     * 分片
     */
    protected Shard shard;
    /**
     * 心跳连续失败就断连的次数
     */
    protected int disconnectWhenHeartbeatFails;
    /**
     * 节点事件监听器
     */
    protected NodeHandler nodeHandler;
    /**
     * 统计指标事件发布器
     */
    protected Publisher<MetricEvent> publisher;
    /**
     * 客户端工厂类
     */
    protected EndpointFactory factory;
    /**
     * 分片的URL
     */
    protected URL url;
    /**
     * 身份认证提供者
     */
    protected Function<URL, Message> authentication;
    /**
     * 仪表盘
     */
    protected Dashboard dashboard;
    /**
     * 原始权重
     */
    protected int originWeight;
    /**
     * 预热启动权重
     */
    protected int warmupWeight;
    /**
     * 当前节点启动的时间戳
     */
    protected long startTime;
    /**
     * 权重：经过预热计算后
     */
    protected int weight;

    /**
     * 会话心跳间隔
     */
    protected long sessionbeatInterval;
    /**
     * 超时时间
     */
    protected long sessionTimeout;

    /**
     * 预热加载时间
     */
    protected int warmupDuration;
    /**
     * 指标事件处理器
     */
    protected EventHandler<MetricEvent> handler;
    /**
     * 前置条件
     */
    protected CompletableFuture<Void> precondition;
    /**
     * 是否启用ssl
     */
    protected boolean sslEnable;
    /**
     * 别名
     */
    protected String alias;
    /**
     * 服务网格标识
     */
    protected boolean mesh;
    /**
     * 重试信息
     */
    protected Retry retry = new Retry();
    /**
     * 客户端协议
     */
    protected ClientProtocol clientProtocol;

    protected StateMachine<Void, ShardStateTransition, NodeController> stateMachine;

    /**
     * 构造函数
     *
     * @param clusterName 集群名称
     * @param clusterUrl  集群URL
     * @param shard       分片
     */
    public Node(final String clusterName, final URL clusterUrl, final Shard shard) {
        this(clusterName, clusterUrl, shard, ENDPOINT_FACTORY.get(), null, null, null, null);
    }

    /**
     * 构造函数
     *
     * @param clusterName    集群名称
     * @param clusterUrl     集群URL
     * @param shard          分片
     * @param factory        连接工程
     * @param authentication 授权
     * @param nodeHandler    节点事件处理器
     * @param dashboard      当前节点指标面板
     * @param publisher      额外的指标事件监听器
     */
    public Node(final String clusterName, final URL clusterUrl,
                final Shard shard,
                final EndpointFactory factory,
                final Function<URL, Message> authentication,
                final NodeHandler nodeHandler,
                final Dashboard dashboard,
                final Publisher<MetricEvent> publisher) {
        Objects.requireNonNull(clusterUrl, "clusterUrl can not be null.");
        Objects.requireNonNull(shard, "shard can not be null.");
        Objects.requireNonNull(factory, "factory can not be null.");
        //会话超时时间最低60s
        this.sessionTimeout = clusterUrl.getPositiveLong(SESSION_TIMEOUT_OPTION);
        if (sessionTimeout < 60000) {
            sessionTimeout = 60000;
            this.clusterUrl = clusterUrl.add(SESSION_TIMEOUT_OPTION.getName(), 60000);
        } else {
            this.clusterUrl = clusterUrl;
        }
        this.clusterName = clusterName;
        this.shard = shard;
        this.factory = factory;
        this.authentication = authentication;
        this.nodeHandler = nodeHandler;
        //仪表盘
        this.dashboard = dashboard;
        this.publisher = publisher;
        if (publisher != null && dashboard != null) {
            //节点的Dashboard应该只能收到本节点的指标事件
            this.handler = dashboard.wrap(o -> o.getSource() == this);
            this.publisher.addHandler(handler);
        }
        this.disconnectWhenHeartbeatFails = clusterUrl.getInteger(DISCONNECT_WHEN_HEARTBEAT_FAILS, 3);
        this.sessionbeatInterval = estimateSessionbeat(sessionTimeout);
        //原始的URL
        this.url = shard.getUrl();
        //判断节点是否启用ssl
        this.sslEnable = url.getBoolean(SSL_ENABLE);
        //合并集群参数，去掉集群URL带的本地启动时间
        this.url = url.addIfAbsent(clusterUrl.remove(START_TIMESTAMP));
        //启动时间、和预热权重有关系
        this.startTime = url.getLong(START_TIMESTAMP, 0L);
        this.originWeight = shard.getWeight();
        this.warmupDuration = clusterUrl.getInteger(Constants.WARMUP_DURATION_OPTION);
        this.warmupWeight = clusterUrl.getPositiveInt(Constants.WARMUP_ORIGIN_WEIGHT_OPTION);
        this.weight = warmupDuration > 0 ? warmupWeight : originWeight;
        this.alias = url.getString(Constants.ALIAS_OPTION);
        this.mesh = url.getBoolean(SERVICE_MESH_OPTION);
        this.clientProtocol = CLIENT_PROTOCOL_SELECTOR.select(new ProtocolVersion(url.getProtocol(), url.getString(VERSION_KEY)));
        this.stateMachine = new StateMachine<>("node " + shard.getName(),
                () -> new NodeController(this), null, new ShardStateTransition(shard.getState()),
                new StateFuture<>(() -> precondition, null), null);
    }

    /**
     * 打开
     *
     * @return CompletableFuture
     */
    protected CompletableFuture<Void> open() {
        if (clientProtocol == null) {
            return Futures.completeExceptionally(ProtocolException.noneOf("protocol", url.getString(VERSION, url.getProtocol())));
        }
        return stateMachine.open().whenComplete((v, e) -> {
            if (e == null) {
                //连续重连次数设置为0
                retry.times = 0;
            }
        });
    }

    /**
     * 关闭，不会触发断开连接事件
     */
    protected CompletableFuture<Void> close() {
        return stateMachine.close(false);
    }

    protected Retry getRetry() {
        return retry;
    }

    @Override
    public ShardState getState() {
        return stateMachine.getState().state;
    }

    public Client getClient() {
        return stateMachine.getController().client;
    }

    @Override
    public String getName() {
        return shard.getName();
    }

    @Override
    public String getProtocol() {
        return shard.getProtocol();
    }

    public ClientProtocol getClientProtocol() {
        return clientProtocol;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public boolean isSslEnable() {
        return sslEnable;
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    /**
     * 获取重试时间
     *
     * @return 重试时间
     */
    public long getRetryTime() {
        return retry.retryTime;
    }

    protected void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String getDataCenter() {
        return shard.getDataCenter();
    }

    @Override
    public String getRegion() {
        return shard.getRegion();
    }

    public void setPrecondition(CompletableFuture<Void> precondition) {
        this.precondition = precondition;
    }

    public String getAlias() {
        return alias;
    }

    public boolean isMesh() {
        return mesh;
    }

    protected ShardStateTransition getTransition() {
        return stateMachine.getState();
    }

    /**
     * 计算会话心跳时间
     *
     * @param sessionTimeout 会话超时时间
     * @return 会话心跳时间
     */
    protected long estimateSessionbeat(final long sessionTimeout) {
        //sessionTimeout不能太短，否则会出异常
        //15秒到30秒
        return Math.min(Math.max(sessionTimeout / 4, 15000L), 30000L);
    }

    /**
     * 广播事件
     *
     * @param type 类型
     */
    protected void sendEvent(final NodeEvent.EventType type) {
        if (nodeHandler != null) {
            nodeHandler.handle(new NodeEvent(type, this, null));
        }
    }

    /**
     * 广播事件
     *
     * @param type    事件类型
     * @param payload 载体
     */
    protected void sendEvent(final NodeEvent.EventType type, final Object payload) {
        if (nodeHandler != null) {
            nodeHandler.handle(new NodeEvent(type, this, payload));
        }
    }

    /**
     * 创建客户端
     *
     * @return 客户端
     */
    protected Client newClient(final EventHandler<TransportEvent> handler) {
        Client client = factory.createClient(url, t -> publisher == null ?
                new NodeClient(url, t, handler) :
                new MetricClient(url, t, handler, this));
        if (client != null) {
            client.setProtocol(clientProtocol);
            //心跳间隔>0才需要绑定心跳策略
            if (clusterUrl.getInteger(HEARTBEAT_INTERVAL_OPTION) > 0) {
                client.setHeartbeatStrategy(new MyHeartbeatStrategy(client, clusterUrl));
            }
            client.setCodec(clientProtocol.getCodec());
            client.setChannelHandlerChain(clientProtocol.buildChain());
        }
        return client;
    }

    /**
     * 计算预热权重
     *
     * @return 权重
     */
    protected boolean warmup() {
        if (weight != originWeight && originWeight > 0) {
            if (startTime > 0) {
                int duration = (int) (SystemClock.now() - startTime);
                if (duration > 0 && duration < warmupDuration) {
                    int w = warmupWeight + Math.round(((float) duration / warmupDuration) * originWeight);
                    weight = w < 1 ? 1 : Math.min(w, originWeight);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 创建认证消息
     * @param client 客户端
     * @return 认证消息
     */
    protected Message createAuthenticationMessage(final Client client) {
        Session session = client.session();
        Message message =authentication == null ? client.getProtocol().authenticate(clusterUrl, client) : authentication.apply(clusterUrl);
        if (message != null && session != null) {
            Header header = message.getHeader();
            header.setSerialization(session.getSerializationType());
            header.setChecksum(session.getChecksumType());
        }
        return message;
    }

    /**
     * 创建协商消息
     * @param client 客户端
     * @return 协商消息
     */
    protected Message createNegotiateMessage(final Client client) {
        Message message = client.getProtocol().negotiate(clusterUrl, client);
        if (message != null) {
            //设置协商协议的序列化方式
            Header header = message.getHeader();
            if (header.getSerialization() == 0) {
                header.setSerialization((byte) Serialization.JAVA_ID);
            }
        }
        return message;
    }

    /**
     * 节点控制器
     */
    protected static class NodeController implements StateController<Void> {
        /**
         * 节点
         */
        protected final Node node;
        /**
         * 心跳连续失败次数
         */
        protected final AtomicLong successiveHeartbeatFails = new AtomicLong();

        protected final EventHandler<TransportEvent> handler = this::onEvent;

        /**
         * 客户端
         */
        protected Client client;
        /**
         * 认证结果
         */
        protected Response authorizationResponse;

        public NodeController(final Node node) {
            this.node = node;
        }

        @Override
        public CompletableFuture<Void> open() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            successiveHeartbeatFails.set(0);
            //Cluster中确保调用该方法只有CONNECTING状态
            final Client cl = node.newClient(handler);
            if (cl == null) {
                future.completeExceptionally(ProtocolException.noneOf("transport factory", node.url.getString(TRANSPORT_FACTORY_OPTION)));
            } else {
                cl.open().whenComplete((ch, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        //发起协商，如果协商失败，则关闭连接
                        negotiation(cl).whenComplete((response, e) -> {
                            if (e != null) {
                                whenComplete(cl.close(), () -> future.completeExceptionally(e));
                            } else if (!cl.getChannel().isActive()) {
                                //再次判断连接状态，如果断开了，担心clientHandler收不到事件
                                whenComplete(cl.close(), () -> future.completeExceptionally(new ChannelClosedException("channel is closed.")));
                            } else {
                                //认证成功
                                authorizationResponse = response;
                                //若startTime为0，在session中获取远程启动时间
                                node.startTime = node.startTime == 0 ? cl.session().getRemoteStartTime() : node.startTime;
                                //每次连接后，获取目标节点的启动的时间戳，并初始化计算一次权重
                                node.warmup();
                                client = cl;
                                //心跳定时任务
                                timer().add(new SessionbeatTask(node, this));
                                //预热定时任务
                                timer().add(new WarmupTask(node, this));
                                //面板刷新
                                Optional.ofNullable(node.dashboard).ifPresent(d -> timer().add(new DashboardTask(node, this)));
                                node.sendEvent(NodeEvent.EventType.CONNECT);
                                future.complete(null);
                            }
                        });
                    }
                });
            }
            return future;
        }

        @Override
        public void fireClose() {
            if (node.precondition != null) {
                node.precondition.complete(null);
            }
        }

        @Override
        public CompletableFuture<Void> close(final boolean gracefully) {
            //移除Dashboard的监听器
            Optional.ofNullable(node.publisher).ifPresent(o -> o.removeHandler(node.handler));
            return client.close().handle((v, e) -> null);
        }

        /**
         * 事件处理
         *
         * @param event 事件
         */
        protected void onEvent(final TransportEvent event) {
            if (event instanceof InactiveEvent) {
                //Channel断开了，需要关闭当前节点
                disconnect(true);
            } else if (event instanceof HeartbeatEvent) {
                //Channel心跳事件
                onHeartbeat((HeartbeatEvent) event);
            } else if (event instanceof OfflineEvent) {
                OfflineEvent oe = (OfflineEvent) event;
                Channel eChannel = oe.getChannel();
                Channel cChannel = client.getChannel();
                if (oe.getClient() == client || (eChannel != null
                        && (eChannel.getLocalAddress() == cChannel.getLocalAddress()
                        && eChannel.getRemoteAddress() == cChannel.getRemoteAddress()))) {
                    //服务节点下线通知，整个Channel及其上的client都需要关闭
                    onOffline((OfflineEvent) event);
                }
            } else if (event instanceof SessionLostEvent) {
                SessionLostEvent sessionLostEvent = (SessionLostEvent) event;
                if (sessionLostEvent.getClient() == client) {
                    //会话丢失，该Client需要关闭。
                    disconnect(true);
                }
            }
        }

        /**
         * 发送握手信息
         *
         * @param client           客户端
         * @param requestSupplier  消息提供者
         * @param responseConsumer 应答消息处理器
         * @return 应答
         */
        protected CompletableFuture<Response> handshake(final Client client,
                                                        final Supplier<Message> requestSupplier,
                                                        final BiConsumer<Message, CompletableFuture<Response>> responseConsumer) {
            CompletableFuture<Response> future = new CompletableFuture<>();
            if (!node.stateMachine.test(state -> state.isOpening(), this)) {
                //握手阶段，状态应该是连接中
                future.completeExceptionally(node.stateMachine.createIllegalStateException());
            } else {
                try {
                    Message message = requestSupplier.get();
                    if (message == null || !message.isRequest()) {
                        //需要异步处理，某些协议直接返回应答，会一致同步触发到对注册中心进行调用。
                        client.runAsync(() -> responseConsumer.accept(message, future));
                    } else {
                        client.async(message, 3000).whenComplete((response, err) -> {
                            try {
                                responseConsumer.accept(response, future);
                            } catch (Throwable e) {
                                future.completeExceptionally(e);
                            }
                        });
                    }
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }
            return future;
        }

        /**
         * 协商
         *
         * @param client 客户端
         */
        protected CompletableFuture<Response> negotiation(final Client client) {
            return handshake(client,
                    () -> node.createNegotiateMessage(client),
                    (message, future) -> {
                        Object result = message == null ? null : message.getPayLoad();
                        if (result instanceof NegotiationResponse) {
                            NegotiationResponse response = (NegotiationResponse) result;
                            if (!response.isSuccess()) {
                                future.completeExceptionally(new ProtocolException(String.format("Failed negotiating with node(%s) of shard(%s)",
                                        client.getUrl().getAddress(), node.getName())));
                            } else {
                                //协商成功
                                logger.info(String.format("Success negotiating with node(%s) of shard(%s),serialization=%s,compression=%s,checksum=%s.",
                                        client.getUrl().getAddress(), node.getName(),
                                        response.getSerialization(), response.getCompression(), response.getChecksum()));
                                Session session = client.getProtocol().session(node.clusterUrl, client);
                                session.setSessionId(client.getTransportId());
                                session.setTimeout(node.clusterUrl.getLong(SESSION_TIMEOUT_OPTION));
                                session.setSerialization(SERIALIZATION.get(response.getSerialization()));
                                session.setCompression(COMPRESSION.get(response.getCompression()));
                                session.setChecksum(CHECKSUM.get(response.getChecksum()));
                                session.setSerializations(response.getSerializations());
                                session.setCompressions(response.getCompressions());
                                session.setChecksums(response.getChecksums());
                                session.putAll(response.getAttributes());
                                client.session(session);
                                //认证
                                authenticate(client).whenComplete((r, e) -> {
                                    if (e != null) {
                                        future.completeExceptionally(e);
                                    } else {
                                        future.complete(r);
                                    }
                                });
                            }
                        } else if (result instanceof Throwable) {
                            future.completeExceptionally(new ProtocolException(String.format("Failed negotiating with node(%s) of shard(%s),caused by %s",
                                    client.getUrl().getAddress(), node.getName(), ((Throwable) result).getMessage())));
                        } else {
                            future.completeExceptionally(new ProtocolException("protocol is not support."));
                        }
                    });
        }

        /**
         * 身份认证
         *
         * @param client 客户端
         */
        protected CompletableFuture<Response> authenticate(final Client client) {
            return handshake(client, () -> node.createAuthenticationMessage(client),
                    (message, future) -> {
                        SuccessResponse response = message == null ? null : (SuccessResponse) message.getPayLoad();
                        if (response == null || response.isSuccess()) {
                            logger.info(String.format("Success authenticating with node(%s) of shard(%s)", client.getUrl().getAddress(), node.getName()));
                            future.complete(response);
                        } else {
                            future.completeExceptionally(new AuthenticationException(response.getMessage()));
                        }
                    });
        }



        /**
         * 心跳事件
         *
         * @param event 心跳事件
         */
        protected void onHeartbeat(final HeartbeatEvent event) {
            if (!event.isSuccess()) {
                //心跳失败
                if (node.disconnectWhenHeartbeatFails > 0 && successiveHeartbeatFails.incrementAndGet() == node.disconnectWhenHeartbeatFails) {
                    disconnect(true);
                }
            } else {
                successiveHeartbeatFails.set(0);
                Message response = event.getResponse();
                if (response != null) {
                    Object payload = response.getPayLoad();
                    if (payload instanceof HeartbeatResponse) {
                        switch (((HeartbeatResponse) payload).getHealthState()) {
                            case HEALTHY:
                                //从虚弱状态恢复
                                onHealthy();
                                break;
                            case EXHAUSTED:
                                onWeak();
                                break;
                            case DEAD:
                                disconnect(true);
                                break;
                        }
                    }
                    //心跳存在业务逻辑，需要通知出去
                    if (payload instanceof HeartbeatAware) {
                        node.sendEvent(NodeEvent.EventType.HEARTBEAT, payload);
                    }
                }
            }
        }


        /**
         * 下线事件
         *
         * @param event 事件
         */
        protected void onOffline(final OfflineEvent event) {
            //优雅下线
            disconnect(false).whenComplete((v, e) -> {
                if (client.getRequests() == 0) {
                    //下线的时候没有请求，则直接关闭连接，并广播断连事件
                    closeAndPublish();
                } else {
                    //优雅关闭，定时器检测没有请求后或超过2秒关闭
                    timer().add(new OfflineTask(node, this));
                }
            });
        }

        /**
         * 关闭连接，并广播事件，触发重连逻辑
         */
        protected void closeAndPublish() {
            client.close().whenComplete((v, e) -> {
                if (node.stateMachine.test(state -> state.isDisconnect(), this)) {
                    node.sendEvent(NodeEvent.EventType.DISCONNECT, client);
                }
            });
        }

        /**
         * 心跳事件返回服务端过载
         */
        protected void onWeak() {
            node.stateMachine.getState().tryWeak();
        }

        /**
         * 恢复健康
         */
        protected void onHealthy() {
            node.stateMachine.getState().tryOpened();
        }

        /**
         * 关闭连接，并发送事件
         *
         * @param autoClose 是否关闭
         * @return CompletableFuture
         */
        protected CompletableFuture<Void> disconnect(final boolean autoClose) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            if (node.stateMachine.getController() == this) {
                if (node.stateMachine.getState().tryDisconnect() == StateTransition.SUCCESS) {
                    if (autoClose) {
                        //抛出异常，这样触发cluster的自动重连
                        client.close().whenComplete((v, e) -> node.sendEvent(NodeEvent.EventType.DISCONNECT, client));
                    } else {
                        //不自动关闭，5秒后触发关闭事件
                        future.complete(null);
                    }
                } else {
                    future.completeExceptionally(node.stateMachine.createIllegalStateException());
                }
            } else {
                future.completeExceptionally(node.stateMachine.createIllegalStateException());
            }
            return future;
        }

    }

    /**
     * 节点事件处理器
     */
    public interface NodeHandler extends EventHandler<NodeEvent> {

    }

    /**
     * 节点客户端
     */
    protected static class NodeClient extends DecoratorClient<ClientTransport> {
        /**
         * 处理器
         */
        protected EventHandler<? extends TransportEvent> handler;

        /**
         * 构造函数
         *
         * @param url       url
         * @param transport 通道
         * @param handler   处理器
         */
        public NodeClient(final URL url, final ClientTransport transport,
                          final EventHandler<? extends TransportEvent> handler) {
            super(url, transport);
            this.addEventHandler(handler);
        }

        @Override
        public CompletableFuture<Channel> close() {
            //优雅下线，需要注销监听器，否则连接断开又触发Inactive事件
            removeEventHandler(handler);
            return super.close();
        }

    }

    /**
     * 具有指标计算能力的客户端
     */
    protected static class MetricClient extends NodeClient {

        /**
         * 节点
         */
        protected final Node node;
        /**
         * 集群URL
         */
        protected final URL clusterUrl;
        /**
         * 集群名称
         */
        protected final String clusterName;
        /**
         * 统计指标事件发布器
         */
        protected final Publisher<MetricEvent> publisher;

        /**
         * 构造函数
         *
         * @param url       url
         * @param transport 通道
         * @param handler   处理器
         * @param node      节点
         */
        public MetricClient(final URL url, final ClientTransport transport,
                            final EventHandler<? extends TransportEvent> handler,
                            final Node node) {
            super(url, transport, handler);
            this.node = node;
            this.clusterUrl = node.clusterUrl;
            this.clusterName = node.clusterName;
            this.publisher = node.publisher;
        }

        @Override
        public CompletableFuture<Message> async(final Message message, final int timeoutMillis) {
            //判空,验证是否需要统计
            final long startTime = SystemClock.now();
            try {
                return transport.async(message, timeoutMillis).whenComplete((r, t) ->
                        publish(message, r, startTime, SystemClock.now(), t));
            } catch (Exception e) {
                publish(message, null, startTime, SystemClock.now(), e);
                throw e;
            }
        }

        /**
         * 根据请求,返回值,异常,开始时间,结束时间,发送统计事件
         *
         * @param request   请求
         * @param response  应答
         * @param startTime 起始时间
         * @param endTime   终止时间
         * @param throwable 异常
         */
        protected void publish(final Message request, final Message response,
                               final long startTime, final long endTime, Throwable throwable) {
            publisher.offer(new MetricEvent(node, null, clusterUrl, clusterName, url,
                    request, response, throwable, getRequests(),
                    startTime, endTime));
        }
    }

    /**
     * 重连信息
     */
    protected static class Retry {
        /**
         * 连续重试失败次数
         */
        protected int times;
        /**
         * 下一次重试时间
         */
        protected long retryTime;

        public Retry() {
        }

        public Retry(long retryTime) {
            this.retryTime = retryTime;
        }

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }

        public long getRetryTime() {
            return retryTime;
        }

        public void setRetryTime(long retryTime) {
            this.retryTime = retryTime;
        }

        /**
         * 增加重连次数
         */
        public void incrementTimes() {
            times++;
        }

        /**
         * 是否过期
         *
         * @return
         */
        public boolean expire() {
            return SystemClock.now() >= retryTime;
        }
    }

    /**
     * 心跳策略
     */
    protected static class MyHeartbeatStrategy implements HeartbeatStrategy {
        /**
         * 客户端
         */
        protected Client client;
        /**
         * URL参数
         */
        protected URL clusterUrl;
        /**
         * 心跳间隔
         */
        protected int interval;
        /**
         * 超时时间
         */
        protected int timeout;
        /**
         * 心跳策略
         */
        protected HeartbeatMode mode;
        /**
         * 心跳消息提供者
         */
        protected Supplier<Message> heartbeatSupplier;

        /**
         * 构造函数
         *
         * @param client     客户端
         * @param clusterUrl 集群URL
         */
        public MyHeartbeatStrategy(final Client client, final URL clusterUrl) {
            this.client = client;
            this.clusterUrl = clusterUrl;
            this.interval = clusterUrl.getPositiveInt(HEARTBEAT_INTERVAL_OPTION);
            this.timeout = clusterUrl.getPositiveInt(HEARTBEAT_TIMEOUT_OPTION);
            try {
                mode = HeartbeatMode.valueOf(clusterUrl.getString(HEARTBEAT_MODE_OPTION));
            } catch (IllegalArgumentException e) {
                mode = HeartbeatMode.TIMING;
            }
            this.heartbeatSupplier = () -> createHeartbeatMessage();
        }

        /**
         * 创建心跳消息
         *
         * @return 心跳消息
         */
        protected Message createHeartbeatMessage() {
            Session session = client.session();
            //会话存在才发生消息
            if (session != null) {
                Message message = client.getProtocol().heartbeat(clusterUrl, client);
                if (message != null) {
                    message.setSessionId(session.getSessionId());
                    if (message.getHeader().getSerialization() <= 0) {
                        message.getHeader().setSerialization(session.getSerializationType());
                    }
                    return message;
                }
            }
            return null;
        }

        @Override
        public Supplier<Message> getHeartbeat() {
            return heartbeatSupplier;
        }

        @Override
        public int getInterval() {
            return interval;
        }

        @Override
        public int getTimeout() {
            return timeout;
        }

        @Override
        public HeartbeatMode getHeartbeatMode() {
            return mode;
        }
    }

    /**
     * 节点任务
     */
    protected static abstract class NodeTask implements Timer.TimeTask {
        /**
         * 节点
         */
        protected final Node node;
        /**
         * 客户端
         */
        protected final NodeController controller;
        /**
         * 名称
         */
        protected final String name;

        /**
         * 构造函数
         *
         * @param node       节点
         * @param controller 控制器
         */
        public NodeTask(Node node, NodeController controller) {
            this.node = node;
            this.controller = controller;
            this.name = this.getClass().getSimpleName() + "-" + node.getName();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getTime() {
            return SystemClock.now();
        }

        @Override
        public void run() {
            if (!Shutdown.isShutdown() && node.stateMachine.isOpen(controller)) {
                doRun();
            }
        }

        /**
         * 执行
         */
        protected void doRun() {

        }
    }

    /**
     * 预热
     */
    protected static class WarmupTask extends NodeTask {

        /**
         * 构造函数
         *
         * @param node       节点
         * @param controller 客户端
         */
        public WarmupTask(Node node, NodeController controller) {
            super(node, controller);
        }

        @Override
        public long getTime() {
            return SystemClock.now();
        }

        @Override
        protected void doRun() {
            if (node.warmup()) {
                timer().add(this);
            }
        }
    }

    /**
     * 面板快照任务
     */
    protected static class DashboardTask extends NodeTask {

        /**
         * 面板
         */
        protected final Dashboard dashboard;
        /**
         * 窗口时间
         */
        protected final long windowTime;
        /**
         * 下次快照时间
         */
        protected long time;

        /**
         * 构造函数
         *
         * @param node       节点
         * @param controller 控制器
         */
        public DashboardTask(final Node node, final NodeController controller) {
            super(node, controller);
            this.dashboard = node.dashboard;
            this.windowTime = dashboard.getMetric().getWindowTime();
            //把集群指标过期分布到1秒钟以内，避免同时进行快照
            long lastSnapshotTime = SystemClock.now() + ThreadLocalRandom.current().nextInt(1000);
            this.time = lastSnapshotTime + windowTime;
            dashboard.setLastSnapshotTime(lastSnapshotTime);
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        protected void doRun() {
            dashboard.snapshot();
            time = SystemClock.now() + windowTime;
            timer().add(this);
        }
    }

    /**
     * 会话心跳
     */
    protected static class SessionbeatTask extends NodeTask {

        /**
         * 上次心跳时间
         */
        protected long lastTime;
        /**
         * 下次心跳时间
         */
        protected long time;

        /**
         * 构造函数
         *
         * @param node       节点
         * @param controller 客户端
         */
        public SessionbeatTask(final Node node, final NodeController controller) {
            super(node, controller);
            //随机打散心跳时间
            this.lastTime = SystemClock.now() + ThreadLocalRandom.current().nextInt((int) node.sessionbeatInterval);
            this.time = lastTime + node.sessionbeatInterval;
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        protected void doRun() {
            Client client = controller.client;
            ClientProtocol protocol = client.getProtocol();
            Session session = client.session();
            Message message = protocol.sessionbeat(node.clusterUrl, client);
            if (message != null) {
                Header header = message.getHeader();
                header.setSerialization(session.getSerialization().getTypeId());
                header.setCompression(Compression.NONE);
                header.setChecksum(Checksum.NONE);
                //TODO 会话心跳最好不要增加请求数
                client.oneway(message);
                //定时送心跳
                time = SystemClock.now() + node.sessionbeatInterval;
                timer().add(this);
            }
        }
    }

    /**
     * 异步服务端优雅下线任务，等待所有请求结束或超时
     */
    protected static class OfflineTask extends NodeTask {
        /**
         * 起始时间
         */
        protected long startTime;

        /**
         * 构造函数
         *
         * @param node       节点
         * @param controller 控制器
         */
        public OfflineTask(final Node node, final NodeController controller) {
            super(node, controller);
            startTime = SystemClock.now();
        }

        @Override
        public long getTime() {
            return SystemClock.now() + 200L;
        }

        @Override
        public void run() {
            if (node.stateMachine.test(state -> state.isDisconnect(), controller)) {
                //最大2秒后关闭客户端
                if (controller.client.getRequests() == 0 || SystemClock.now() - startTime > 2000L) {
                    controller.closeAndPublish();
                } else {
                    //重新添加
                    timer().add(this);
                }
            }

        }
    }

}
