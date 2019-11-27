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
import io.joyrpc.exception.AuthorizationException;
import io.joyrpc.exception.ChannelClosedException;
import io.joyrpc.exception.ProtocolException;
import io.joyrpc.exception.ReconnectException;
import io.joyrpc.extension.URL;
import io.joyrpc.metric.Dashboard;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.protocol.message.HeartbeatAware;
import io.joyrpc.protocol.message.Response;
import io.joyrpc.protocol.message.SuccessResponse;
import io.joyrpc.protocol.message.heartbeat.HeartbeatResponse;
import io.joyrpc.protocol.message.negotiation.NegotiationResponse;
import io.joyrpc.transport.Client;
import io.joyrpc.transport.DecoratorClient;
import io.joyrpc.transport.EndpointFactory;
import io.joyrpc.transport.event.HeartbeatEvent;
import io.joyrpc.transport.event.InactiveEvent;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.message.Header;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.util.Switcher;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.GlobalContext.timer;

/**
 * 节点
 */
public class Node implements Shard {
    private static final Logger logger = LoggerFactory.getLogger(Node.class);
    protected static final String VERSION = "version";
    protected static final String DISCONNECT_WHEN_HEARTBEAT_FAILS = "disconnectWhenHeartbeatFails";
    public static final String START_TIMESTAMP = "startTime";

    //集群URL
    protected URL clusterUrl;
    //集群名称
    protected String clusterName;
    //分片
    protected Shard shard;
    //心跳连续失败就断连的次数
    protected int disconnectWhenHeartbeatFails;
    //节点事件监听器
    protected EventHandler<NodeEvent> nodeHandler;
    //统计指标事件发布器
    protected Publisher<MetricEvent> publisher;
    //客户端工厂类
    protected EndpointFactory factory;
    //分片的URL
    protected URL url;
    //授权认证提供者
    protected Function<URL, Message> authorization;
    //仪表盘
    protected Dashboard dashboard;
    //连接事件监听器
    protected EventHandler<? extends TransportEvent> clientHandler;
    //原始权重
    protected int originWeight;
    //预热启动权重
    protected int warmupWeight;
    //权重：经过预热计算后
    protected int weight;
    //状态
    protected volatile ShardState state;
    //客户端
    protected volatile Client client;
    //会话心跳间隔
    protected long sessionbeatInterval;
    //超时时间
    protected long sessionTimeout;
    //心跳
    protected AtomicBoolean sessionbeating = new AtomicBoolean();
    //开关
    protected Switcher switcher = new Switcher();
    //认证结果
    protected Response authorizationResponse;
    //重试信息
    protected Retry retry = new Retry();
    //预热加载时间
    protected int warmupDuration;
    //当前节点启动的时间戳
    protected long startTime;
    /**
     * 集群版本
     */
    protected long clusterVersion;
    /**
     * 当前节点版本
     */
    protected AtomicLong version = new AtomicLong(0);
    //心跳连续失败次数
    protected AtomicLong successiveHeartbeatFails = new AtomicLong();
    //指标事件处理器
    protected EventHandler<MetricEvent> handler;
    //前置条件
    protected CompletableFuture<Void> precondition;
    //是否启用ssl
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
     * 构造函数
     *
     * @param clusterName 集群名称
     * @param clusterUrl  集群URL
     * @param shard       分片
     */
    public Node(final String clusterName, final URL clusterUrl, final Shard shard) {
        this(clusterName, clusterUrl, 0, shard, ENDPOINT_FACTORY.get(), null, null, null, null);
    }

    /**
     * 构造函数
     *
     * @param clusterName    集群名称
     * @param clusterUrl     集群URL
     * @param clusterVersion 集群版本
     * @param shard          分片
     * @param factory        连接工程
     * @param authorization  授权
     * @param nodeHandler    节点事件处理器
     * @param dashboard      当前节点指标面板
     * @param publisher      额外的指标事件监听器
     */
    public Node(final String clusterName, final URL clusterUrl, final long clusterVersion,
                final Shard shard,
                final EndpointFactory factory,
                final Function<URL, Message> authorization,
                final EventHandler<NodeEvent> nodeHandler,
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
        this.clusterVersion = clusterVersion;
        this.shard = shard;
        this.factory = factory;
        this.authorization = authorization;
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
        this.state = shard.getState();
        this.clientHandler = event -> {
            if (event instanceof InactiveEvent) {
                //当前节点连接断开
                disconnect(client, true);
            } else if (event instanceof HeartbeatEvent) {
                onHeartbeat((HeartbeatEvent) event);
            } else if (event instanceof OfflineEvent) {
                onOffline((OfflineEvent) event);
            } else if (event instanceof SessionLostEvent) {
                //传入调用时候的client防止并发。
                disconnect(((SessionLostEvent) event).getClient(), true);
            }
        };
        this.alias = url.getString(Constants.ALIAS_OPTION);
        this.mesh = url.getBoolean(SERVICE_MESH_OPTION);
    }

    /**
     * 下线事件
     *
     * @param event
     */
    protected void onOffline(final OfflineEvent event) {
        //传入调用时候的client防止并发。
        //获取事件中的client，若事件中client为空，取事件中的channel，判断当前node中channl是否与事件中channel相同
        Client client = event.getClient();
        Client current = this.client;

        if (client == null && event.getChannel() != null && current != null
                && event.getChannel().getLocalAddress() == current.getChannel().getLocalAddress()
                && event.getChannel().getRemoteAddress() == current.getChannel().getRemoteAddress()) {
            client = current;
        }
        //优雅下线
        if (disconnect(client, false)) {
            //5秒后优雅关闭
            timer().addLeastOneTick(new OfflineTask(this, client));
        }
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
     * 打开节点，创建连接.<br/>
     *
     * @param consumer the consumer
     */
    protected void open(final Consumer<AsyncResult<Node>> consumer) {
        Objects.requireNonNull(consumer, "consumer can not be null.");
        //若开关未打开，打开节点，若开关打开，重连节点
        successiveHeartbeatFails.set(0);
        //Cluster中确保调用该方法只有CONNECTING状态
        Runnable runnable = () -> {
            //拿到客户端协议
            ClientProtocol protocol = CLIENT_PROTOCOL_SELECTOR.select(url);
            if (protocol == null) {
                state.disconnect(this::setState);
                consumer.accept(new AsyncResult<>(this, new ProtocolException(String.format("protocol plugin %s is not found.",
                        url.getString(VERSION, url.getProtocol())))));
            } else if (state != ShardState.CONNECTING) {
                consumer.accept(new AsyncResult<>(this, new IllegalStateException("node state is illegal.")));
            } else {
                final Client client = factory.createClient(url);
                if (client == null) {
                    state.disconnect(this::setState);
                    consumer.accept(new AsyncResult<>(this, new ProtocolException(
                            String.format("transport factory plugin %s is not found.",
                                    url.getString(TRANSPORT_FACTORY_OPTION)))));
                } else {
                    try {
                        client.setProtocol(protocol);
                        final long v = version.incrementAndGet();
                        open(client, result -> switcher.writer().run(() -> onOpened(client, v, result, consumer)));
                    } catch (Throwable e) {
                        //连接失败
                        client.close(null);
                        state.disconnect(this::setState);
                        consumer.accept(new AsyncResult<>(this, e));
                    }
                }
            }
        };
        if (precondition != null) {
            //等到前面节点关闭
            precondition.whenComplete((t, r) -> switcher.open(runnable, runnable));
            precondition = null;
        } else {
            switcher.open(runnable, runnable);
        }
    }

    /**
     * 打开事件
     *
     * @param client   客户端
     * @param v        打开前的版本
     * @param result   结果
     * @param consumer 消费者
     */
    protected void onOpened(final Client client, final long v, final AsyncResult<Response> result, final Consumer<AsyncResult<Node>> consumer) {
        //防止重复调用，或者状态已经变更，例如被关闭了
        if (version.get() != v || state != ShardState.CONNECTING) {
            client.close(null);
            consumer.accept(new AsyncResult<>(this, new IllegalStateException("node state is illegal.")));
        } else if (!result.isSuccess()) {
            client.close(null);
            state.disconnect(this::setState);
            consumer.accept(new AsyncResult<>(this, result.getThrowable()));
        } else {
            //认证成功
            authorizationResponse = result.getResult();
            //连续重连次数设置为0
            retry.times = 0;
            //修改状态、触发消费者
            state.connected(this::setState);
            //可统计Metric的Client
            this.client = publisher == null ? client : new MetricClient(client, this, clusterUrl, clusterName, publisher);
            //若startTime为0，在session中获取远程启动时间
            this.startTime = this.startTime == 0 ? this.client.session().getRemoteStartTime() : this.startTime;
            //每次连接后，获取目标节点的启动的时间戳，并初始化计算一次权重
            this.weight = warmup();
            //心跳定时任务
            timer().addLeastOneTick(new SessionbeatTask(this, v));
            //预热定时任务
            timer().addLeastOneTick(new WarmupTask(this, v));
            //面板刷新
            Optional.ofNullable(dashboard).ifPresent(d -> timer().addLeastOneTick(new DashboardTask(this, v)));

            //在client赋值之后绑定事件监听器
            client.addEventHandler(clientHandler);
            //再次判断连接状态，如果断开了，可能clientHandler收不到事件
            if (!client.getChannel().isActive()) {
                client.removeEventHandler(clientHandler);
                client.close(null);
                state.disconnect(this::setState);
                consumer.accept(new AsyncResult<>(this, new ChannelClosedException("channel is closed.")));
            } else {
                sendEvent(NodeEvent.EventType.CONNECT);
                consumer.accept(new AsyncResult<>(this));
            }
        }
    }

    /**
     * 由Cluster进行关闭.
     */
    protected void close() {
        close(null);
    }

    /**
     * 内部关闭连接，可能重连
     *
     * @param client
     * @param consumer
     */
    protected void close(final Client client, final Consumer<AsyncResult<Node>> consumer) {
        if (client != null) {
            client.removeEventHandler(clientHandler);
            client.close(consumer == null ? null : o -> consumer.accept(new AsyncResult<>(o, this)));
        } else if (consumer != null) {
            consumer.accept(new AsyncResult<>(this));
        }
    }

    /**
     * 关闭，不会触发断开连接事件
     *
     * @param consumer the consumer
     */
    protected void close(final Consumer<AsyncResult<Node>> consumer) {
        switcher.close(() -> {
            //移除Dashboard的监听器
            if (publisher != null && handler != null) {
                publisher.removeHandler(handler);
            }
            state.close(this::setState);
            close(client, consumer);
            client = null;
            precondition = null;
        });

    }

    protected Retry getRetry() {
        return retry;
    }

    @Override
    public ShardState getState() {
        return state;
    }

    protected void setState(ShardState state) {
        this.state = state;
    }

    public Client getClient() {
        return client;
    }

    /**
     * 心跳事件返回服务端过载
     */
    protected void weak() {
        if (state == ShardState.CONNECTED) {
            switcher.writer().run(() -> {
                if (state == ShardState.CONNECTED) {
                    state.weak(this::setState);
                }
            });
        }
    }

    /**
     * 恢复健康
     */
    protected void healthy() {
        if (state == ShardState.WEAK) {
            switcher.writer().run(() -> {
                if (state == ShardState.WEAK) {
                    state.connected(this::setState);
                }
            });
        }
    }

    @Override
    public String getName() {
        return shard.getName();
    }

    @Override
    public String getProtocol() {
        return shard.getProtocol();
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

    protected long getClusterVersion() {
        return clusterVersion;
    }

    /**
     * 消息协商
     *
     * @param session
     * @param message
     * @param compress
     * @return
     */
    protected Message negotiate(final Session session, final Message message, final boolean compress) {
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
        Message message = client.getProtocol().negotiation(clusterUrl, client);
        //设置协商协议的序列化方式
        Header header = message.getHeader();
        if (header.getSerialization() == 0) {
            header.setSerialization((byte) Serialization.JAVA_ID);
        }
        return message;
    }


    /**
     * 广播事件
     *
     * @param type
     */
    protected void sendEvent(final NodeEvent.EventType type) {
        if (nodeHandler != null) {
            nodeHandler.handle(new NodeEvent(type, this, null));
        }
    }

    /**
     * 广播事件
     *
     * @param type
     * @param payload
     */
    protected void sendEvent(final NodeEvent.EventType type, final Object payload) {
        if (nodeHandler != null) {
            nodeHandler.handle(new NodeEvent(type, this, payload));
        }
    }

    /**
     * 创建并打开连接.
     *
     * @param client   客户端
     * @param consumer 消费者
     */
    protected void open(final Client client, final Consumer<AsyncResult<Response>> consumer) {
        ClientProtocol protocol = client.getProtocol();
        //心跳间隔>0才需要绑定心跳策略
        if (clusterUrl.getInteger(HEARTBEAT_INTERVAL_OPTION) > 0) {
            client.setHeartbeatStrategy(new MyHeartbeatStrategy(client, clusterUrl));
        }
        client.setCodec(protocol.getCodec());
        client.setChannelHandlerChain(protocol.buildChain());
        client.open(event -> {
            if (event.isSuccess()) {
                //发起协商，如果协商失败，则关闭连接
                negotiation(client, consumer);
            } else {
                consumer.accept(new AsyncResult<>((Response) null, event.getThrowable()));
            }
        });
    }

    /**
     * 发送会话心跳信息
     */
    protected void sessionbeat() {
        //保留一份现场
        Client client = this.client;
        ClientProtocol protocol = client.getProtocol();
        Session session = client.session();
        if (client != null && protocol != null) {
            Message message = protocol.sessionbeat(clusterUrl, client);
            Header header = message.getHeader();
            header.setSerialization(session.getSerialization().getTypeId());
            header.setCompression(Compression.NONE);
            header.setChecksum(Checksum.NONE);
            if (message != null) {
                client.oneway(message);
            }
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
        if (client == null || client != this.client) {
            return false;
        }
        //switcher的机制确保节点被关闭后不会执行重连
        return switcher.writer().quiet(() -> {
            if (client != this.client) {
                return false;
            }
            //合法的状态，Connecting,Connected,Weak,Disconnect
            if (state.disconnect(this::setState)) {
                if (client != null) {
                    //优雅下线不会立即关闭当前client，会暂存起来等到channel关闭或超时关闭。
                    //优雅下线，需要注销监听器，否则连接断开又触发Inactive事件
                    client.removeEventHandler(clientHandler);
                    if (autoClose) {
                        client.close(null);
                    }
                }
                this.client = null;
                consumer.accept(new AsyncResult<>(this, new ReconnectException()));
                return true;
            }
            return false;
        });
    }

    /**
     * 心跳事件
     *
     * @param event
     */
    protected void onHeartbeat(final HeartbeatEvent event) {
        if (!event.isSuccess()) {
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
            client.runAsync(() -> result.accept(new AsyncResult<>((Response) null, e)));
        }
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
                o -> ((NegotiationResponse) o.getPayLoad()).isSuccess() ? null : new ProtocolException("protocol is not support."),
                o -> {
                    //协商成功
                    NegotiationResponse response = (NegotiationResponse) o.getPayLoad();
                    logger.info(String.format("success negotiating with node(%s) of shard(%s),serialization=%s,compression=%s,checksum=%s.",
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
                    authorize(client, consumer);
                }, consumer);
    }

    /**
     * 认证
     *
     * @param client   客户端
     * @param consumer 消费者
     */
    protected void authorize(final Client client, final Consumer<AsyncResult<Response>> consumer) {
        handshake(client,
                () -> negotiate(client.session(), authorization == null ? client.getProtocol().authorization(clusterUrl, client) :
                        authorization.apply(clusterUrl), false),
                o -> {
                    SuccessResponse response = (SuccessResponse) o.getPayLoad();
                    return response.isSuccess() ? null : new AuthorizationException(response.getMessage(), clusterUrl);
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
        logger.info(String.format("success authenticating with node(%s) of shard(%s)", client.getUrl().getAddress(), shard.getName()));
        consumer.accept(new AsyncResult<>(response));
    }

    /**
     * 计算预热权重
     *
     * @return
     */
    protected int warmup() {
        int result = originWeight;
        if (weight != originWeight && originWeight > 0) {
            if (startTime > 0) {
                int duration = (int) (SystemClock.now() - startTime);
                if (duration > 0 && duration < warmupDuration) {
                    int w = warmupWeight + Math.round(((float) duration / warmupDuration) * originWeight);
                    result = w < 1 ? 1 : (w > originWeight ? originWeight : w);
                }
            }
        }
        return result;
    }

    /**
     * 包装指标
     */
    protected static class MetricClient extends DecoratorClient<Client> {

        //节点
        protected final Node node;
        //集群URL
        protected final URL clusterUrl;
        //集群名称
        protected final String clusterName;
        //统计指标事件发布器
        protected final Publisher<MetricEvent> publisher;

        /**
         * 构造函数
         *
         * @param client
         * @param node
         * @param clusterUrl
         * @param publisher
         */
        public MetricClient(final Client client, final Node node,
                            final URL clusterUrl, final String clusterName,
                            final Publisher<MetricEvent> publisher) {
            super(client);
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
                return transport.async(message, timeoutMillis).whenComplete((r, t) -> {
                    publish(message, r, startTime, SystemClock.now(), t);
                });
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
                    request, response, throwable, getChannel().getFutureManager().size(),
                    startTime, endTime));
        }
    }

    /**
     * 重连信息
     */
    protected static class Retry {
        //连续重试失败次数
        protected int times;
        //下一次重试时间
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
        protected Node node;
        /**
         * 版本
         */
        protected final long version;
        /**
         * 名称
         */
        protected final String name;

        /**
         * 构造函数
         *
         * @param node
         * @param version
         */
        public NodeTask(Node node, long version) {
            this.node = node;
            this.version = version;
            this.name = this.getClass().getSimpleName() + " " + node.getName();
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
            if (node.version.get() == version) {
                doRun();
            }
        }

        /**
         * 执行
         */
        protected abstract void doRun();
    }

    /**
     * 预热
     */
    protected static class WarmupTask extends NodeTask {

        /**
         * 构造函数
         *
         * @param node
         * @param version
         */
        public WarmupTask(Node node, long version) {
            super(node, version);
        }

        @Override
        public long getTime() {
            return SystemClock.now();
        }

        @Override
        protected void doRun() {
            if (node.warmup() != node.originWeight) {
                timer().addLeastOneTick(this);
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
        protected Dashboard dashboard;
        /**
         * 上次快照时间
         */
        protected long lastSnapshotTime;
        /**
         * 下次快照时间
         */
        protected long time;

        /**
         * 构造函数
         *
         * @param node
         * @param version
         */
        public DashboardTask(final Node node, final long version) {
            super(node, version);
            this.dashboard = node.dashboard;
            //把集群指标过期分布到1秒钟以内，避免同时进行快照
            this.lastSnapshotTime = SystemClock.now() + ThreadLocalRandom.current().nextInt(1000);
            this.time = lastSnapshotTime + dashboard.getMetric().getWindowTime();
            dashboard.setLastSnapshotTime(lastSnapshotTime);
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        protected void doRun() {
            switch (node.state) {
                case CONNECTED:
                case WEAK:
                    dashboard.snapshot();
                    time = SystemClock.now() + dashboard.getMetric().getWindowTime();
                    timer().addLeastOneTick(this);
            }
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
         * @param node
         * @param version
         */
        public SessionbeatTask(final Node node, final long version) {
            super(node, version);
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
            switch (node.state) {
                case CONNECTED:
                case WEAK:
                    node.sessionbeat();
                    time = SystemClock.now() + node.sessionbeatInterval;
                    timer().addLeastOneTick(this);
            }
        }
    }

    /**
     * 服务端下线任务
     */
    protected static class OfflineTask implements Timer.TimeTask {
        /**
         * 节点
         */
        protected Node node;
        /**
         * 连接
         */
        protected Client client;

        /**
         * 构造函数
         *
         * @param node
         * @param client
         */
        public OfflineTask(Node node, Client client) {
            this.node = node;
            this.client = client;
        }

        @Override
        public String getName() {
            return this.getClass().getSimpleName() + " " + node.getName();
        }

        @Override
        public long getTime() {
            //五秒关闭
            return SystemClock.now() + 5000L;
        }

        @Override
        public void run() {
            switch (client.getStatus()) {
                case CLOSED:
                case CLOSING:
                    break;
                default:
                    node.close(client, null);
            }
        }
    }
}
