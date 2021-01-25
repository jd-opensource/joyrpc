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
import io.joyrpc.event.AsyncResult;
import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.AuthenticationException;
import io.joyrpc.exception.ChannelClosedException;
import io.joyrpc.exception.ProtocolException;
import io.joyrpc.exception.ReconnectException;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
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
     * 客户端协议
     */
    protected ClientProtocol clientProtocol;

    protected StateMachine<Void, NodeController> stateMachine;

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
        this.stateMachine = new StateMachine<>(() -> new NodeController(this),
                null, new ShardStateTransition(shard.getState()), new StateFuture<>(
                () -> precondition == null ? CompletableFuture.completedFuture(null) : precondition, null),
                null);
    }

    /**
     * 打开
     *
     * @return CompletableFuture
     */
    protected CompletableFuture<Void> open() {
        return stateMachine.open();
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
        return ((ShardStateTransition) stateMachine.getState()).state;
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

    /**
     * 设置状态
     *
     * @param source 原状态
     * @param target 目标状态
     * @return 成功标识
     */
    protected boolean setState(final ShardState source, final ShardState target) {
        return STATE_UPDATER.compareAndSet(this, source, target);
    }

    /**
     * 心跳事件返回服务端过载
     */
    protected void weak() {
        state.weak(this::setState);
    }

    /**
     * 恢复健康
     */
    protected void healthy() {
        state.connected(this::setState);
    }

    /**
     * 计算会话心跳时间
     *
     * @param sessionTimeout
     * @return
     */
    protected long estimateSessionbeat(final long sessionTimeout) {
        //sessionTimeout不能太短，否则会出异常
        //15秒到30秒
        return Math.min(Math.max(sessionTimeout / 4, 15000L), 30000L);
    }

    /**
     * 构造认证消息
     *
     * @param session  会话
     * @param message  消息
     * @param compress 是否压缩
     * @return 协商消息
     */
    protected Message authenticate(final Session session, final Message message, final boolean compress) {
        if (message != null && session != null) {
            Header header = message.getHeader();
            header.setSerialization(session.getSerializationType());
            if (compress) {
                header.setCompression(session.getCompressionType());
            }
            header.setChecksum(session.getChecksumType());
        }
        return message;
    }

    /**
     * 创建协商协议
     *
     * @param client
     * @return
     */
    protected Message negotiate(final Client client) {
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
     * 关闭连接，并发送事件
     *
     * @param client    client
     * @param autoClose 是否关闭当前客户端
     */
    protected boolean disconnect(final Client client, final boolean autoClose) {
        return disconnect(client, result -> {
            if (!result.isSuccess()) {
                sendEvent(NodeEvent.EventType.DISCONNECT, client);
            }
        }, autoClose);
    }

    /**
     * 关闭连接，并发送事件
     *
     * @param client    客户端
     * @param consumer  消费者
     * @param autoClose 是否关闭
     */
    protected boolean disconnect(final Client client, final Consumer<AsyncResult<Node>> consumer, final boolean autoClose) {
        if (client != this.client) {
            //客户端还没有赋值，不需要抛出异常
            if (autoClose) {
                client.close(o -> consumer.accept(new AsyncResult<>(this)));
            } else {
                consumer.accept(new AsyncResult<>(this));
            }
            return true;
        } else if (state.disconnect(this::setState)) {
            //不能把节点的客户端设置为空，否则会报空异常
            if (autoClose) {
                //抛出异常，触发cluster的自动重连
                client.close(o -> consumer.accept(new AsyncResult<>(this, new ReconnectException())));
            } else {
                //不自动关闭，5秒后触发关闭事件
                consumer.accept(new AsyncResult<>(this));
            }
            return true;
        }
        return false;
    }

    /**
     * 下线事件
     *
     * @param client 监听器的客户端
     * @param event  事件
     */
    protected void onOffline(final Client client, final OfflineEvent event) {
        //传入调用时候的client防止并发。
        //获取事件中的client，若事件中client为空，取事件中的channel，判断当前node中channl是否与事件中channel相同
        Client c = event.getClient();
        Channel eChannel = event.getChannel();
        if (c == null && eChannel != null) {
            Channel cChannel = client.getChannel();
            if (eChannel.getLocalAddress() == cChannel.getLocalAddress()
                    && eChannel.getRemoteAddress() == cChannel.getRemoteAddress()) {
                c = client;
            }
        }
        //优雅下线
        if (disconnect(c, false)) {
            //下线的时候没有请求，则直接关闭连接，并广播断连事件
            if (client.getRequests() == 0) {
                doCloseAndPublish(c);
            } else {
                //优雅关闭，定时器检测没有请求后或超过2秒关闭
                timer().add(new OfflineTask(this, c));
            }
        }
    }

    /**
     * 心跳事件
     *
     * @param event
     */
    protected void onHeartbeat(final Client client, final HeartbeatEvent event) {
        if (client != this.client) {
            //还没有赋值，则忽略掉
        } else if (!event.isSuccess()) {
            //心跳失败
            if (disconnectWhenHeartbeatFails > 0 && successiveHeartbeatFails.incrementAndGet() == disconnectWhenHeartbeatFails) {
                disconnect(client, true);
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
                            healthy();
                            break;
                        case EXHAUSTED:
                            weak();
                            break;
                        case DEAD:
                            disconnect(client, true);
                            break;
                    }
                }
                //心跳存在业务逻辑，需要通知出去
                if (payload instanceof HeartbeatAware) {
                    sendEvent(NodeEvent.EventType.HEARTBEAT, payload);
                }
            }
        }
    }

    /**
     * 发送握手信息
     *
     * @param client   客户端
     * @param supplier 消息提供者
     * @param function 应答消息判断
     * @param next     执行下一步
     * @param result   最终调用
     */
    protected void handshake(final Client client, final Supplier<Message> supplier,
                             final Function<Message, Throwable> function,
                             final Consumer<Message> next,
                             final Consumer<AsyncResult<Response>> result) {
        if (state != ShardState.CONNECTING) {
            //握手阶段，状态应该是连接中
            //Cluster目前是异步处理打开
            result.accept(new AsyncResult<>(new IllegalStateException("node state is illegal.")));
            //client.runAsync(() -> result.accept(new AsyncResult<>(new IllegalStateException("node state is illegal."))));
        } else {
            try {
                Message message = supplier.get();
                if (message == null || !message.isRequest()) {
                    //需要异步处理，某些协议直接返回应答，会一致同步触发到对注册中心进行调用。
                    client.runAsync(() -> next.accept(message));
                } else {
                    client.async(message, (msg, err) -> {
                        Throwable throwable = err == null ? function.apply(msg) : err;
                        if (throwable != null) {
                            //网络异常，需要重试
                            result.accept(new AsyncResult<>((Response) null, throwable));
                        } else {
                            try {
                                next.accept(msg);
                            } catch (Throwable e) {
                                result.accept(new AsyncResult<>((Response) null, e));
                            }
                        }
                    }, 3000);
                }
            } catch (Throwable e) {
                //Cluster目前是异步处理打开
                result.accept(new AsyncResult<>(e));
                //client.runAsync(() -> result.accept(new AsyncResult<>((Response) null, e)));
            }
        }
    }

    /**
     * 协商
     *
     * @param client   客户端
     * @param consumer 消费者
     */
    protected CompletableFuture<Response> negotiation(final Client client) {
        //TODO
        return null;
    }

    /**
     * 协商
     *
     * @param client   客户端
     * @param consumer 消费者
     */
    protected void negotiation(final Client client, final Consumer<AsyncResult<Response>> consumer) {
        handshake(client,
                () -> negotiate(client),
                o -> {
                    Object result = o.getPayLoad();
                    String error = null;
                    if (result instanceof NegotiationResponse) {
                        NegotiationResponse response = (NegotiationResponse) result;
                        if (response.isSuccess()) {
                            return null;
                        } else {
                            error = String.format("Failed negotiating with node(%s) of shard(%s)",
                                    client.getUrl().getAddress(), shard.getName());
                        }
                    } else if (result instanceof Throwable) {
                        error = String.format("Failed negotiating with node(%s) of shard(%s),caused by %s",
                                client.getUrl().getAddress(), shard.getName(), ((Throwable) result).getMessage());
                    }
                    return new ProtocolException(error == null ? "protocol is not support." : error);
                },
                o -> {
                    //协商成功
                    NegotiationResponse response = (NegotiationResponse) o.getPayLoad();
                    logger.info(String.format("Success negotiating with node(%s) of shard(%s),serialization=%s,compression=%s,checksum=%s.",
                            client.getUrl().getAddress(), shard.getName(),
                            response.getSerialization(), response.getCompression(), response.getChecksum()));
                    Session session = client.getProtocol().session(clusterUrl, client);
                    session.setSessionId(client.getTransportId());
                    session.setTimeout(clusterUrl.getLong(SESSION_TIMEOUT_OPTION));
                    session.setSerialization(SERIALIZATION.get(response.getSerialization()));
                    session.setCompression(COMPRESSION.get(response.getCompression()));
                    session.setChecksum(CHECKSUM.get(response.getChecksum()));
                    session.setSerializations(response.getSerializations());
                    session.setCompressions(response.getCompressions());
                    session.setChecksums(response.getChecksums());
                    session.putAll(response.getAttributes());
                    client.session(session);
                    //认证
                    authenticate(client, consumer);
                }, consumer);
    }

    /**
     * 身份认证
     *
     * @param client   客户端
     * @param consumer 消费者
     */
    protected void authenticate(final Client client, final Consumer<AsyncResult<Response>> consumer) {
        handshake(client,
                () -> authenticate(client.session(),
                        authentication == null ? client.getProtocol().authenticate(clusterUrl, client) :
                                authentication.apply(clusterUrl), false),
                o -> {
                    SuccessResponse response = (SuccessResponse) o.getPayLoad();
                    return response.isSuccess() ? null : new AuthenticationException(response.getMessage());
                },
                o -> onAuthorized(client, o == null ? null : (Response) o.getPayLoad(), consumer),
                consumer);
    }

    /**
     * 认证成功
     *
     * @param client   客户端
     * @param response 认证应答
     * @param consumer 消费者
     */
    protected void onAuthorized(final Client client,
                                final Response response,
                                final Consumer<AsyncResult<Response>> consumer) {
        logger.info(String.format("Success authenticating with node(%s) of shard(%s)", client.getUrl().getAddress(), shard.getName()));
        consumer.accept(new AsyncResult<>(response));
    }


    protected static class NodeController implements StateController<Void> {
        /**
         * 节点
         */
        protected final Node node;
        /**
         * 客户端
         */
        protected Client client;
        /**
         * 重试信息
         */
        protected Retry retry = new Retry();
        /**
         * 当前节点启动的时间戳
         */
        protected long startTime;
        /**
         * 权重：经过预热计算后
         */
        protected int weight;
        /**
         * 心跳连续失败次数
         */
        protected AtomicLong successiveHeartbeatFails = new AtomicLong();
        /**
         * 认证结果
         */
        protected Response authorizationResponse;

        public NodeController(final Node node) {
            this.node = node;
            this.startTime = node.startTime;
            this.weight = node.weight;
        }

        @Override
        public CompletableFuture<Void> open() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            ClientProtocol clientProtocol = node.clientProtocol;
            URL url = node.url;
            //若开关未打开，打开节点，若开关打开，重连节点
            successiveHeartbeatFails.set(0);
            //Cluster中确保调用该方法只有CONNECTING状态
            //拿到客户端协议
            if (clientProtocol == null) {
                future.completeExceptionally(new ProtocolException(String.format("protocol plugin %s is not found.",
                        url.getString(VERSION, url.getProtocol()))));
            } else {
                //提供函数，减少一层包装
                final Client cl = node.factory.createClient(url, t -> node.publisher == null ?
                        new NodeClient(url, t, v -> new MyEventHandler<>(node, v)) :
                        new MetricClient(url, t, v -> new MyEventHandler<>(node, v),
                                node, node.clusterUrl, node.clusterName, node.publisher));
                if (cl == null) {
                    future.completeExceptionally(new ProtocolException(
                            String.format("transport factory plugin %s is not found.",
                                    url.getString(TRANSPORT_FACTORY_OPTION))));
                } else {
                    try {
                        cl.setProtocol(clientProtocol);
                        //心跳间隔>0才需要绑定心跳策略
                        if (node.clusterUrl.getInteger(HEARTBEAT_INTERVAL_OPTION) > 0) {
                            cl.setHeartbeatStrategy(new MyHeartbeatStrategy(client, node.clusterUrl));
                        }
                        cl.setCodec(clientProtocol.getCodec());
                        cl.setChannelHandlerChain(clientProtocol.buildChain());
                        cl.open().whenComplete((ch, error) -> {
                            if (error != null) {
                                future.completeExceptionally(error);
                            } else {
                                //发起协商，如果协商失败，则关闭连接
                                negotiation(cl).whenComplete((response, e) -> {
                                    if (e != null) {
                                        cl.close().thenRun(() -> future.completeExceptionally(e));
                                    } else if (!cl.getChannel().isActive()) {
                                        //再次判断连接状态，如果断开了，担心clientHandler收不到事件
                                        cl.close().thenRun(() -> future.completeExceptionally(new ChannelClosedException("channel is closed.")));
                                    } else {
                                        //认证成功
                                        authorizationResponse = response;
                                        //连续重连次数设置为0
                                        retry.times = 0;
                                        //若startTime为0，在session中获取远程启动时间
                                        startTime = startTime == 0 ? cl.session().getRemoteStartTime() : startTime;
                                        //每次连接后，获取目标节点的启动的时间戳，并初始化计算一次权重
                                        warmup();
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
                    } catch (Throwable e) {
                        //连接失败
                        cl.close().thenRun(() -> future.completeExceptionally(e));
                    }
                }
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
            return client.close().thenRun(() -> {

            });
        }

        /**
         * 计算预热权重
         *
         * @return 权重
         */
        protected boolean warmup() {
            if (weight != node.originWeight && node.originWeight > 0) {
                if (startTime > 0) {
                    int duration = (int) (SystemClock.now() - startTime);
                    if (duration > 0 && duration < node.warmupDuration) {
                        int w = node.warmupWeight + Math.round(((float) duration / node.warmupDuration) * node.originWeight);
                        weight = w < 1 ? 1 : Math.min(w, node.originWeight);
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * 下线事件
         *
         * @param event 事件
         */
        protected void onOffline(final OfflineEvent event) {
            //传入调用时候的client防止并发。
            //获取事件中的client，若事件中client为空，取事件中的channel，判断当前node中channl是否与事件中channel相同
            Client c = event.getClient();
            Channel eChannel = event.getChannel();
            if (c == null && eChannel != null) {
                Channel cChannel = client.getChannel();
                if (eChannel.getLocalAddress() == cChannel.getLocalAddress()
                        && eChannel.getRemoteAddress() == cChannel.getRemoteAddress()) {
                    c = client;
                }
            }
            //优雅下线
            if (disconnect(c, false)) {
                //下线的时候没有请求，则直接关闭连接，并广播断连事件
                if (client.getRequests() == 0) {
                    doCloseAndPublish(c);
                } else {
                    //优雅关闭，定时器检测没有请求后或超过2秒关闭
                    timer().add(new OfflineTask(this, c));
                }
            }
        }

        /**
         * 关闭连接，并发送事件
         *
         * @param autoClose 是否关闭
         * @return CompletableFuture
         */
        protected CompletableFuture<Void> disconnect(final boolean autoClose) {
            CompletableFuture future = new CompletableFuture();
            StateMachine<Void, NodeController> stateMachine = node.stateMachine;
            ShardStateTransition transition = (ShardStateTransition) stateMachine.getState();
            if (stateMachine.getController() == this && transition.tryDisconnect() == StateTransition.SUCCESS) {
                if (autoClose) {
                    //抛出异常，这样触发cluster的自动重连
                    client.close().thenRun(() -> node.sendEvent(NodeEvent.EventType.DISCONNECT, client));
                } else {
                    //不自动关闭，5秒后触发关闭事件
                    future.complete(null);
                }
            }
            return future;
        }

        /**
         * 节点事件处理器
         */
        public interface NodeHandler extends EventHandler<NodeEvent> {

        }

        protected static class NodeClient extends DecoratorClient<ClientTransport> {
            /**
             * 处理器
             */
            protected EventHandler<? extends TransportEvent> handler;

            /**
             * 构造函数
             *
             * @param url
             * @param transport
             * @param handlerFunction
             */
            public NodeClient(final URL url, final ClientTransport transport,
                              final Function<Client, EventHandler<? extends TransportEvent>> handlerFunction) {
                super(url, transport);
                this.handler = handlerFunction.apply(this);
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
         * 包装指标
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
             * @param url
             * @param transport
             * @param handlerFunction
             * @param node
             * @param clusterUrl
             * @param clusterName
             * @param publisher
             */
            public MetricClient(final URL url, final ClientTransport transport,
                                final Function<Client, EventHandler<? extends TransportEvent>> handlerFunction,
                                final Node node, final URL clusterUrl, final String clusterName, final Publisher<MetricEvent> publisher) {
                super(url, transport, handlerFunction);
                this.node = node;
                this.clusterUrl = clusterUrl;
                this.clusterName = clusterName;
                this.publisher = publisher;
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
             * @param request
             * @param response
             * @param startTime
             * @param endTime
             * @param throwable
             */
            protected void publish(final Message request, final Message response,
                                   final long startTime, final long endTime, Throwable throwable) {
                publisher.offer(new MetricEvent(node, null, clusterUrl, clusterName, url,
                        request, response, throwable, getRequests(),
                        startTime, endTime));
            }
        }

        /**
         * 事件监听器
         */
        protected static class MyEventHandler<T extends TransportEvent> implements EventHandler<T> {

            /**
             * 节点
             */
            protected final Node node;
            /**
             * 客户端
             */
            protected final Client client;

            /**
             * 构造函数
             *
             * @param node   节点
             * @param client 当前绑定的客户端
             */
            public MyEventHandler(final Node node, final Client client) {
                this.node = node;
                this.client = client;
            }

            @Override
            public void handle(final T event) {
                if (event instanceof InactiveEvent) {
                    //Channel断开了，需要关闭当前节点
                    node.disconnect(client, true);
                } else if (event instanceof HeartbeatEvent) {
                    //Channel心跳事件
                    node.onHeartbeat(client, (HeartbeatEvent) event);
                } else if (event instanceof OfflineEvent) {
                    //服务节点下线通知，整个Channel及其上的client都需要关闭
                    node.onOffline(client, (OfflineEvent) event);
                } else if (event instanceof SessionLostEvent) {
                    //会话丢失，该Client需要关闭。
                    Client c = ((SessionLostEvent) event).getClient();
                    if (c == client) {
                        node.disconnect(c, true);
                    }
                }
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
             * @param client
             * @param clusterUrl
             */
            public MyHeartbeatStrategy(final Client client, final URL clusterUrl) {
                this.client = client;
                this.clusterUrl = clusterUrl;
                this.interval = clusterUrl.getPositive(HEARTBEAT_INTERVAL_OPTION.getName(), HEARTBEAT_INTERVAL_OPTION.get());
                this.timeout = clusterUrl.getPositive(HEARTBEAT_TIMEOUT_OPTION.getName(), HEARTBEAT_TIMEOUT_OPTION.get());
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
             * @return
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
                if (controller.warmup()) {
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
         * 异步服务端优雅下线任务
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
                StateMachine<Void, NodeController> stateMachine = node.stateMachine;
                ShardStateTransition transition = (ShardStateTransition) stateMachine.getState();
                Client client = controller.client;
                if (transition.state == ShardState.DISCONNECT && stateMachine.getController() == controller) {
                    //最大2秒后关闭客户端
                    if (client.getRequests() == 0 || SystemClock.now() - startTime > 2000L) {
                        client.close().thenRun(() -> {
                            if (transition.state == ShardState.DISCONNECT && stateMachine.getController() == controller) {
                                node.sendEvent(NodeEvent.EventType.DISCONNECT, client);
                            }
                        });
                    } else {
                        //重新添加
                        timer().add(this);
                    }
                }

            }
        }

    }

    /**
     * 节点事件处理器
     */
    public interface NodeHandler extends EventHandler<NodeEvent> {

    }
}
