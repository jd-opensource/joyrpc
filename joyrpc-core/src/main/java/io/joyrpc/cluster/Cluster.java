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

import io.joyrpc.cluster.candidate.Candidature;
import io.joyrpc.cluster.discovery.naming.ClusterHandler;
import io.joyrpc.cluster.discovery.naming.Registar;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.MetricEvent;
import io.joyrpc.cluster.event.NodeEvent;
import io.joyrpc.constants.Constants;
import io.joyrpc.event.*;
import io.joyrpc.exception.AuthenticationException;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.exception.ProtocolException;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.metric.Dashboard;
import io.joyrpc.metric.Dashboard.DashboardType;
import io.joyrpc.metric.DashboardFactory;
import io.joyrpc.transport.EndpointFactory;
import io.joyrpc.transport.message.Message;
import io.joyrpc.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.CANDIDATURE_OPTION;

/**
 * 集群
 */
public class Cluster {

    private static final Logger logger = LoggerFactory.getLogger(Cluster.class);
    public static final URLOption<Long> RECONNECT_INTERVAL = new URLOption<>("reconnectInterval", 2000L);
    protected static final AtomicLong idCounter = new AtomicLong();
    public static final String EVENT_PUBLISHER_METRIC = "event.metric";
    public static final PublisherConfig EVENT_PUBLISHER_METRIC_CONF = PublisherConfig.builder().timeout(1000).build();
    public static final String EVENT_PUBLISHER_CLUSTER = "event.cluster";
    public static final PublisherConfig EVENT_PUBLISHER_CLUSTER_CONF = PublisherConfig.builder().timeout(1000).build();
    public static final List<String> DEAD_MSG = Resource.lines("network.error");
    //名称
    protected String name;
    //URL
    protected URL url;
    //目录服务
    protected Registar registar;
    //选择算法
    protected Candidature candidature;
    //客户端工厂类
    protected EndpointFactory factory;
    //授权认证提供者
    protected Function<URL, Message> authorization;
    //仪表盘提供者
    protected DashboardFactory<? extends Dashboard> dashboardFactory;
    //集群指标通知器
    protected Publisher<MetricEvent> metricPublisher;
    //集群节点事件通知器
    protected Publisher<NodeEvent> clusterPublisher;
    //重连时间间隔
    protected long reconnectInterval;
    //选择的最小分片数量
    protected int minSize;
    //初始化建连的数量
    protected int initSize;
    //初始化建连的超时时间
    protected long initTimeout;
    //每次重连最大数量
    protected int maxReconnection;
    //当前集群的指标
    protected Dashboard dashboard;
    //节点
    protected Map<String, Node> nodes = new ConcurrentHashMap<>(50);
    //可用节点
    protected volatile List<Node> readys = new ArrayList<>(0);
    //连接好的节点
    protected Map<String, Node> connects = new ConcurrentHashMap<>(50);
    //冷备节点队列
    protected LinkedList<Node> backups;
    //重连节点
    protected LinkedList<Node> reconnects;
    //重连节点队列第一条的重试信息
    protected Node.Retry retry;
    //集群监听器
    protected ClusterHandler clusterHandler;
    //开关
    protected Switcher switcher = new Switcher();
    //打开的连接的数量
    protected AtomicLong counter = new AtomicLong();
    //打开Future
    protected CompletableFuture<AsyncResult<Cluster>> openFuture;
    //第一次选择
    protected AtomicBoolean first = new AtomicBoolean();
    //是否正在处理重连，防止集群管理器多次调度，进入锁等待
    protected AtomicBoolean reconnecting = new AtomicBoolean();
    //通知触发的连接数量
    protected int triggerWhen;
    //打开的时间，用于判断是否超时
    protected long openTime;
    //是否启用SSL
    protected boolean sslEnable;
    //是否通知了等到初始化的消费者
    protected AtomicBoolean mail = new AtomicBoolean();

    public Cluster(final URL url) {
        this(null, url, null, null, null, null, null, null, null);
    }

    public Cluster(final URL url, final Registar registar) {
        this(null, url, registar, null, null, null, null, null, null);
    }

    public Cluster(final String name, final URL url) {
        this(name, url, null, null, null, null, null, null, null);
    }

    public Cluster(final String name, final URL url, final Registar registar) {
        this(name, url, registar, null, null, null, null, null, null);
    }

    public Cluster(final String name, final URL url, final Registar registar, final Candidature candidature,
                   final EndpointFactory factory, final Function<URL, Message> authorization,
                   final DashboardFactory<Dashboard> dashboardFactory,
                   final Iterable<? extends MetricHandler> metricHandlers,
                   final Publisher<NodeEvent> clusterPublisher) {
        Objects.requireNonNull(url, "url can not be null.");
        this.name = name == null || name.isEmpty() ? url.toString(false, false) : name;
        //不需要添加环境变量，由外层处理合并
        this.url = url;
        this.registar = registar == null ? REGISTAR.get(url.getString("registar", url.getProtocol())) : registar;
        Objects.requireNonNull(this.registar, "registar can not be null.");
        this.candidature = candidature != null ? candidature : CANDIDATURE.get(url.getString(CANDIDATURE_OPTION), CANDIDATURE_OPTION.getValue());
        this.factory = factory != null ? factory : ENDPOINT_FACTORY.getOrDefault(url.getString("endpointFactory"));
        this.authorization = authorization;
        this.initSize = url.getInteger(Constants.INIT_SIZE_OPTION);
        this.minSize = url.getInteger(Constants.MIN_SIZE_OPTION);
        this.initTimeout = url.getLong(Constants.INIT_TIMEOUT_OPTION);
        this.reconnectInterval = url.getLong(RECONNECT_INTERVAL);
        this.maxReconnection = url.getInteger(Constants.MAX_RECONNECTION_OPTION);
        this.clusterHandler = this::onClusterEvent;
        this.sslEnable = url.getBoolean(Constants.SSL_ENABLE);
        //创建仪表盘
        this.dashboardFactory = dashboardFactory;
        this.dashboard = dashboardFactory != null ? dashboardFactory.create(url, DashboardType.Cluster) : null;
        //构建事件发布器
        this.clusterPublisher = clusterPublisher != null ? clusterPublisher : EVENT_BUS.get().getPublisher(EVENT_PUBLISHER_CLUSTER, this.name, EVENT_PUBLISHER_CLUSTER_CONF);
        //额外的指标监听器
        if (dashboard != null || metricHandlers != null) {
            Iterator<? extends MetricHandler> it = metricHandlers.iterator();
            if (it.hasNext() || dashboard != null) {
                this.metricPublisher = EVENT_BUS.get().getPublisher(EVENT_PUBLISHER_METRIC, String.valueOf(idCounter.incrementAndGet()), EVENT_PUBLISHER_METRIC_CONF);
                this.metricPublisher.addHandler(dashboard);
                this.metricPublisher.addHandler(metricHandlers);
            }
        }
    }

    /**
     * Open Cluster,subscribe url.
     *
     * @param consumer 等到连接成功的标识，确保达到初始化连接数
     */
    public void open(final Consumer<AsyncResult<Cluster>> consumer) {
        Function<CompletableFuture<AsyncResult<Cluster>>, CompletableFuture<AsyncResult<Cluster>>> function = f -> {
            //构建事件发布器，可以重入（多次Open，Close）
            clusterPublisher.start();
            if (metricPublisher != null) {
                metricPublisher.start();
            }
            if (dashboard != null) {
                //把集群指标过期分布到1秒钟以内，避免同时进行快照
                dashboard.setLastSnapshotTime(SystemClock.now() + ThreadLocalRandom.current().nextInt(1000));
            }
            openFuture = f;
            openTime = SystemClock.now();
            first.set(false);
            registar.subscribe(url, clusterHandler);
            triggerWhen = initSize;
            if (triggerWhen <= 0) {
                //不需要等到初始化连接
                f.complete(new AsyncResult(this));
            }
            return f;
        };
        switcher.open(function).whenComplete((v, t) -> {
            Throwable throwable = t == null ? v.getThrowable() : t;
            consumer.accept(throwable == null ? new AsyncResult<>(this) : new AsyncResult<>(throwable));
        });
    }

    /**
     * Open Cluster,subscribe url.
     */
    public void open() {
        open(null);
    }

    /**
     * 关闭集群
     */
    public void close() {
        close(null);
    }

    /**
     * 关闭集群
     *
     * @param consumer
     */
    public void close(final Consumer<AsyncResult<Cluster>> consumer) {
        switcher.close(future -> {
            Close.close(clusterPublisher).close(metricPublisher);
            //注销监听器
            registar.unsubscribe(url, clusterHandler);
            //清空重连队列
            clearNodes(future);
            return future;
        }).whenComplete((v, t) -> {
            if (consumer != null) {
                consumer.accept(t == null ? new AsyncResult<>(this) : new AsyncResult<>(this, t));
            }
        });
    }

    /**
     * 关闭的时候或接收到清理事件的时候清理节点
     *
     * @param future
     */
    protected void clearNodes(final CompletableFuture<Object> future) {
        reconnects = new LinkedList<>();
        backups = new LinkedList<>();
        retry = null;
        connects.clear();
        readys = new ArrayList<>(0);
        Map<String, Node> copys = new HashMap<>(nodes);
        nodes.clear();
        final AtomicInteger counter = new AtomicInteger(copys.size());
        final Consumer<AsyncResult<Node>> consumer = future == null ? null : result -> {
            if (counter.decrementAndGet() == 0) {
                future.complete(new AsyncResult<>(Cluster.this));
            }
        };
        copys.values().forEach(o -> o.close(consumer));
        copys.clear();
    }

    /**
     * 是否启动
     *
     * @return
     */
    public boolean isOpened() {
        return switcher.isOpened();
    }

    /**
     * 添加节点事件处理器
     *
     * @param handler
     * @return
     */
    public boolean addHandler(final EventHandler<NodeEvent> handler) {
        return clusterPublisher.addHandler(handler);
    }

    /**
     * 移除节点事件处理器
     *
     * @param handler
     * @return
     */
    public boolean removeHandler(final EventHandler<NodeEvent> handler) {
        return clusterPublisher.removeHandler(handler);
    }

    /**
     * 获取可用的节点
     *
     * @return the nodes
     */
    public List<Node> getNodes() {
        return readys;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public URL getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    /**
     * 获取区域对象
     *
     * @return
     */
    public Region getRegion() {
        return registar;
    }

    /**
     * 被监管
     *
     * @return
     */
    protected List<Runnable> supervise() {
        //内部防止并发重复调用
        List<Runnable> runnables = new LinkedList<>();
        if (isOpened()) {
            if (isTimeout()) {
                runnables.add(this::timeout);
            }
            if (reconnectable()) {
                runnables.add(this::reconnect);
            }
            if (dashboard != null && dashboard.isExpired()) {
                runnables.add(dashboard::snapshot);
            }
            //遍历所有就绪节点，判断是否有任务触发
            for (Node node : readys) {
                node.supervise(runnables);
            }
        }
        return runnables;
    }

    /**
     * 是否初始化超时
     *
     * @return
     */
    protected boolean isTimeout() {
        return initTimeout > 0 && openFuture != null && !mail.get() && SystemClock.now() - openTime > initTimeout;
    }

    /**
     * 是否要重连
     *
     * @return
     */
    protected boolean reconnectable() {
        return retry != null && retry.expire();
    }

    /**
     * 判断节点是否存在
     *
     * @param node
     * @return
     */
    protected boolean exists(final Node node) {
        //根据名字获取
        Node now = nodes.get(node.getName());
        return now != null && now == node;
    }

    /**
     * 重新选举节点
     */
    protected void candidate() {
        //冷备节点，已经在算法里面做了最优打散
        backups = new LinkedList<>();
        //清空重连队列
        reconnects = new LinkedList<>();
        List<Node> candidates = new LinkedList<>();
        List<Node> notSupportProtocols = new LinkedList<>();
        recommend(candidates, notSupportProtocols);
        //用最新的参数进行更新
        Candidature.Result result = candidature.candidate(url, Candidate.builder().cluster(this).
                region(registar).nodes(candidates).size(minSize).
                build());
        int size = result.getSize();
        if (size > 0 && first.compareAndSet(false, true)) {
            //第一次选择结果，对参数进行修正，避免initSize设置的不合理
            if (triggerWhen > 0) {
                triggerWhen = Math.max(1, Math.min(size * 2 / 3, triggerWhen));
            }
        }
        if (!notSupportProtocols.isEmpty()) {
            result.getDiscards().addAll(notSupportProtocols);
        }
//        logger.info(String.format("cluster url:%s, candidate result, candidates:%d, standbys:%d, backups:%d, discards:%d",
//                url.toString(false, true, "alias", "initTimeout", "region", "datacenter"),
//                result.getCandidates().size(),
//                result.getStandbys().size(),
//                result.getBackups().size(),
//                result.getDiscards().size()
//        ));
        retry = null;
        //命中节点建立连接
        candidate(result.getCandidates(), (s, n) -> connect(n), s -> s.getWeight());
        //热备节点建立连接
        //TODO 热备节点没有流量，影响自适应评分
        candidate(result.getStandbys(), (s, n) -> connect(n), s -> 0);
        candidate(result.getBackups(), (s, n) -> backup(n), s -> s.getWeight());
        //丢弃的节点
        candidate(result.getDiscards(), (s, n) -> discard(n), null);
        //重置可用节点，因为有些节点可能在这次选举中被放弃了
        readys = new ArrayList<>(connects.values());
    }

    /**
     * 判断协议支持的候选者，过滤掉SSL不匹配的节点
     *
     * @param candidates          候选者
     * @param notSupportProtocols 不支持协议的节点
     */
    protected void recommend(final List<Node> candidates, final List<Node> notSupportProtocols) {
        Node node;
        //遍历节点，过滤掉SSL不匹配的节点
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            node = entry.getValue();
            if (sslEnable == node.sslEnable) {
                candidates.add(node);
            } else {
                notSupportProtocols.add(node);
            }
        }
        if (candidates.isEmpty() && !notSupportProtocols.isEmpty()) {
            //非ssl连接ssl会直接断开连接
            logger.warn(String.format("there is not any %s provider ", sslEnable ? "ssl" : "none ssl"));
        }
    }

    /**
     * 处理选择的分片
     *
     * @param shards   分片
     * @param consumer 消费者
     * @param weighter 权重
     */
    protected void candidate(final List<Node> shards, final BiConsumer<Shard, Node> consumer,
                             final Function<Node, Integer> weighter) {
        Node node;
        if (shards != null) {
            for (Node shard : shards) {
                node = nodes.get(shard.getName());
                if (node != null) {
                    node.setWeight(weighter != null ? weighter.apply(shard) : shard.getWeight());
                    //设置权重
                    consumer.accept(shard, node);
                }
            }
        }
    }

    /**
     * 丢弃节点
     *
     * @param node
     */
    protected void discard(final Node node) {
        //关闭节点
        node.close();
        //删除连接节点
        connects.remove(node.getName(), node);
    }

    /**
     * 备份节点
     *
     * @param node
     */
    protected void backup(final Node node) {
        //关闭节点
        node.close();
        //删除连接节点
        connects.remove(node.getName(), node);
        //备份节点
        backups.add(node);
    }

    /**
     * 打开节点
     *
     * @param node
     */
    protected void connect(final Node node) {
        //初始化状态改成候选状态
        node.getState().candidate(node::setState);
        //连接中状态
        if (node.getState().connecting(node::setState)) {
            node.open(n -> switcher.writer().run(() -> onConnect(n)));
        }
    }

    /**
     * 启动超时通知
     */
    protected void timeout() {
        if (openFuture != null && mail.compareAndSet(false, true)) {
            //run确保在打开状态
            switcher.writer().run(() -> openFuture.complete(
                    new AsyncResult<>(Cluster.this,
                            new InitializationException("initialization timeout."))));
        }
    }

    /**
     * 检查重连队列，对超过了重试时间间隔的节点进行重连
     */
    protected void reconnect() {
        //防止集群管理器并发调度重连，进入锁等待
        if (reconnectable() && reconnecting.compareAndSet(false, true)) {
            switcher.writer().run(() -> {
                if (reconnectable()) {
                    //先置空重试信息，避免集群管理器再次触发
                    this.retry = null;
                    Node node;
                    Node.Retry retry = null;
                    int count = 0;
                    //把头部需要重连的节点进行重连
                    while ((node = reconnects.peekFirst()) != null) {
                        retry = node.getRetry();
                        if (!nodes.containsKey(node.getName())) {
                            //已经被删除了，直接移除掉
                            reconnects.pollFirst();
                        } else if (retry.expire()) {
                            //到了重连的事件
                            node = reconnects.pollFirst();
                            retry.incrementTimes();
                            connect(node);
                            retry = null;
                            if (maxReconnection > 0 && ++count == maxReconnection) {
                                //控制一次最多重连几个个节点
                                break;
                            }
                        } else {
                            break;
                        }

                    }
                    this.retry = retry;
                }
            });
            reconnecting.set(false);
        }

    }

    /**
     * 从备选节点选择一条进行连接
     */
    protected void supply() {
        //随机选择一个冷备节点来连接，去掉已经不存在的节点。节点变更事件里面已经直接删除了
        Node first = backups.pollFirst();
        while (first != null) {
            if (!nodes.containsKey(first.getName())) {
                first = backups.pollFirst();
            } else {
                break;
            }
        }
        if (first != null) {
            Node.Retry retry = first.getRetry();
            if (retry.expire()) {
                //需要重连，增加重连次数
                retry.incrementTimes();
                connect(first);
            } else {
                //放入到重连队列队尾
                reconnects.add(first);
                if (reconnects.size() == 1) {
                    this.retry = first.retry;
                }
            }
        }
    }

    /**
     * 节点打开事件
     *
     * @param result
     */
    protected void onConnect(final AsyncResult<Node> result) {
        Node owner = result.getResult();
        if (!isOpened()) {
            logger.warn(String.format("OnConnect event of node %s is happened, but clusert was not opened.", owner != null ? owner.getName() : "null"));
            return;
        }
        if (nodes.get(owner.getName()) != owner) {
            //不存在了，或者变更成了其它实例
            owner.close();
            logger.info(String.format("Close the removed node %s.", owner.getName()));
        } else if (result.isSuccess()) {
            onConnect(owner);
            logger.info(String.format("Success connecting node %s.", owner.getName()));
        } else {
            Throwable throwable = result.getThrowable();
            logger.warn(String.format("Failed connecting node %s. caused by %s.", owner.getName(), throwable == null ? "Unknown error." : StringUtils.toString(throwable)));
            if (throwable != null && (throwable instanceof ProtocolException || throwable instanceof AuthenticationException)) {
                //协商失败或认证失败，最少20秒重连
                onDisconnect(result.getResult(), SystemClock.now() + Math.max(reconnectInterval, 20000L) + ThreadLocalRandom.current().nextInt(1000));
            } else if (detectDead(throwable)) {
                //目标节点不存在了，最少20秒重连
                onDisconnect(result.getResult(), SystemClock.now() + Math.max(reconnectInterval, 20000L) + ThreadLocalRandom.current().nextInt(1000));
            } else {
                onDisconnect(result.getResult());
            }
        }
    }

    /**
     * 检查目标节点是否已经不存活了
     *
     * @param throwable
     * @return
     */
    protected boolean detectDead(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        Queue<Throwable> queue = new LinkedList<>();
        queue.add(throwable);
        Throwable t;
        while (!queue.isEmpty()) {
            t = queue.poll();
            t = t instanceof ConnectionException ? t.getCause() : t;
            if (t instanceof NoRouteToHostException) {
                //没有路由
                return true;
            } else if (t instanceof ConnectException) {
                //连接异常
                String msg = t.getMessage().toLowerCase();
                for (String deadMsg : DEAD_MSG) {
                    if (msg.contains(deadMsg)) {
                        return true;
                    }
                }
                return false;
            } else if (t.getCause() != null) {
                queue.add(t.getCause());
            }
        }
        return false;
    }

    /**
     * 节点连接上
     *
     * @param node
     */
    protected void onConnect(final Node node) {
        if (node.dashboard != null) {
            //把节点指标过期分布到1秒钟以内，避免同时进行快照。
            node.dashboard.setLastSnapshotTime(SystemClock.now() + ThreadLocalRandom.current().nextInt(1000));
        }
        Node old = connects.put(node.getName(), node);
        readys = new ArrayList<>(connects.values());
        if (null != old && old != node) {
            old.close();
        }
        if (triggerWhen > 0 && openFuture != null && counter.incrementAndGet() == triggerWhen
                && mail.compareAndSet(false, true)) {
            //初始化连接成功
            openFuture.complete(new AsyncResult<>(Cluster.this));
        }
    }

    /**
     * 连接断开
     *
     * @param node 节点
     */
    protected void onDisconnect(final Node node) {
        //下次重试时间
        onDisconnect(node, SystemClock.now() + reconnectInterval + ThreadLocalRandom.current().nextInt(1000));
    }

    /**
     * 连接断开
     *
     * @param node      节点
     * @param retryTime 重连时间
     */
    protected void onDisconnect(final Node node, final long retryTime) {
        //确保在拿到开关执行，这个时候不在选举
        //两个地方调用，连接不成功，或者连接成功后断开连接
        //拿到当前节点，如果不存在或者是新的节点则直接丢弃
        if (exists(node)) {
            //判断是否连接成功过
            if (connects.remove(node.getName(), node)) {
                readys = new ArrayList<>(connects.values());
            }
            //如果没有下线，则尝试重连
            node.getRetry().setRetryTime(retryTime);
            //把当前节点放回到后备节点
            backups.addLast(node);
            //补充一个新节点
            supply();
        }
    }

    /**
     * 集群事件
     *
     * @param event
     */
    protected void onClusterEvent(final ClusterEvent event) {
        if (event == null) {
            return;
        }
        List<ClusterEvent.ShardEvent> events = event.getDatum();
        if (events == null) {
            return;
        }
        switcher.writer().run(() -> {
            int add = 0;
            if (event.getType() == UpdateEvent.UpdateType.CLEAR) {
                onClear();
            } else if (event.getType() == UpdateEvent.UpdateType.UPDATE) {
                //增量更新
                for (ClusterEvent.ShardEvent e : events) {
                    switch (e.getType()) {
                        case DELETE:
                            onDeleteShard(e.getShard());
                            break;
                        case ADD:
                            add += onAddShard(e.getShard()) ? 1 : 0;
                            break;
                    }
                }
            } else if (event.getType() == UpdateEvent.UpdateType.FULL) {
                //全量更新
                //记录节点名称
                Set<String> names = new HashSet<>();
                //遍历节点，进行添加
                for (ClusterEvent.ShardEvent e : events) {
                    names.add(e.getShard().getName());
                    add += onAddShard(e.getShard()) ? 1 : 0;
                }
                //判断哪些节点被删除了
                for (Map.Entry<String, Node> node : nodes.entrySet()) {
                    if (!names.contains(node.getKey())) {
                        onDeleteShard(node.getValue());
                    }
                }
            }
            if (add > 0) {
                //新增了节点，重新选举
                candidate();
            }
        });

    }

    /**
     * 删除所有分片事件，如果注册中心权限认证失败会收到该事件
     */
    protected void onClear() {
        clearNodes(null);
    }

    /**
     * 删除分片事件
     *
     * @param shard
     */
    protected void onDeleteShard(final Shard shard) {
        String name = shard.getName();
        Node node = nodes.remove(name);
        if (node != null) {
            logger.info(String.format("delete shard %s when cluster event", shard.getName()));
            //由于注册中心的事件晚于服务端直接发送的下线命令，所以这里可以做到优雅关闭节点
            node.close(null);
            if (connects.remove(name) != null) {
                //重新设置readys节点
                readys = new ArrayList<>(connects.values());
                //从备选节点中重新创建连接
                supply();
            }
        }
    }

    /**
     * 添加分片事件
     *
     * @param shard
     */
    protected boolean onAddShard(final Shard shard) {
        URL url = shard.getUrl();
        if (url == null) {
            //节点没有地址，则删除掉
            return false;
        }
        Node previous = nodes.get(shard.getName());
        //比较是否发生变化
        if (previous != null && (previous.originWeight == shard.getWeight()
                && Objects.equals(previous.getName(), shard.getName())
                && Objects.equals(previous.getRegion(), shard.getRegion())
                && Objects.equals(previous.getDataCenter(), shard.getDataCenter())
                && Objects.equals(previous.getProtocol(), shard.getProtocol())
                && Objects.equals(previous.getUrl(), shard.getUrl()))) {
            //没有发生变化
            return false;
        }

        logger.info(String.format("add shard %s when cluster event for %s", shard.getName(), name));
        //新增节点都进行覆盖，防止分片数量发生了变化
        Node node = createNode(shard);
        previous = nodes.put(shard.getName(), node);
        if (previous != null) {
            //确保前置节点关闭，防止并发open，报连接关闭异常
            CompletableFuture<Void> waiting = new CompletableFuture();
            previous.close((result) -> {
                if (result.isSuccess()) {
                    waiting.complete(null);
                } else {
                    waiting.completeExceptionally(result.getThrowable());
                }
            });
            node.setPrecondition(waiting);
        }
        //新增节点初始化状态
        node.getState().initial(node::setState);
        return true;


    }

    /**
     * 创建节点
     *
     * @param shard 分片
     * @return
     */
    protected Node createNode(final Shard shard) {
        return new Node(name, url, shard, factory, authorization, this::onNodeEvent,
                dashboardFactory == null ? null : dashboardFactory.create(url, DashboardType.Node), metricPublisher);
    }

    /**
     * 连接事件
     *
     * @param event
     */
    protected void onNodeEvent(final NodeEvent event) {
        Node node = event.getNode();
        NodeEvent.EventType type = event.getType();
        logger.info(String.format("%s node %s.", type.getDesc(), node.getName()));
        //确保不在选举和关闭中
        switcher.writer().run(() -> {
            switch (type) {
                case DISCONNECT:
                    //连接成功并且放在connects中，后续才会触发disconnect。
                    onDisconnect(node);
                    break;
            }
        });
        clusterPublisher.offer(event);
    }

}
