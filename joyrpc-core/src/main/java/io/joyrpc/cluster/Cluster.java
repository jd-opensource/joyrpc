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
import io.joyrpc.util.Timer;
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
import static io.joyrpc.context.GlobalContext.timer;

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
    //集群监听器
    protected ClusterHandler clusterHandler;
    //开关
    protected Switcher switcher = new Switcher();
    //是否启用SSL
    protected boolean sslEnable;
    /**
     * 是否通知了等到初始化的消费者
     */
    protected AtomicBoolean mail = new AtomicBoolean();
    /**
     * 连接就绪通知器
     */
    protected Officer officer;
    /**
     * 打开的次数
     */
    protected AtomicLong opens = new AtomicLong(0);
    /**
     * 选举的次数
     */
    protected AtomicLong candicates = new AtomicLong(0);

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
            Optional.ofNullable(metricPublisher).ifPresent(p -> p.start());
            //定期触发快照
            Optional.ofNullable(dashboard).ifPresent(o -> timer().add(new DashboardTask(this, opens.incrementAndGet())));
            registar.subscribe(url, clusterHandler);
            officer = new Officer(this, f);
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
            Close.close(clusterPublisher)
                    .close(metricPublisher)
                    .close(() -> registar.unsubscribe(url, clusterHandler))
                    .close(() -> clearNodes(future));
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
        backups = new LinkedList<>();
        connects.clear();
        readys = new ArrayList<>(0);

        List<Node> copy = new LinkedList<>(nodes.values());
        nodes.clear();
        final AtomicInteger counter = new AtomicInteger(copy.size());
        final Consumer<AsyncResult<Node>> consumer = future == null ? null : result -> {
            if (counter.decrementAndGet() == 0) {
                future.complete(new AsyncResult<>(Cluster.this));
            }
        };
        copy.forEach(o -> o.close(consumer));
        copy.clear();
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
        //增加选举次数，正在重连的任务会自动放弃
        candicates.incrementAndGet();
        //冷备节点，已经在算法里面做了最优打散
        backups = new LinkedList<>();
        List<Node> candidates = new LinkedList<>();
        List<Node> notSupportProtocols = new LinkedList<>();
        recommend(candidates, notSupportProtocols);
        //用最新的参数进行更新
        Candidature.Result result = candidature.candidate(url, Candidate.builder().cluster(this).
                region(registar).nodes(candidates).size(minSize).
                build());
        Optional.ofNullable(officer).ifPresent(o -> o.setTriggerWhen(result.getSize()));
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
            //异步回调，需要在onConnect里面判断状态
            node.open(n -> switcher.writer().run(() -> onConnect(n)));
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
            timer().add(new ReconnectTask(this, first));
        }
    }

    /**
     * 节点打开事件，异步回调，需要重新判断状态
     *
     * @param result
     */
    protected void onConnect(final AsyncResult<Node> result) {
        Node owner = result.getResult();
        if (!isOpened()) {
            owner.close();
            logger.warn(String.format("Close the unused node instance %s. because the cluster is closed. ", owner.getName()));
        } else if (opens.get() != owner.getClusterVersion()) {
            owner.close();
            logger.warn(String.format("Close the unused node instance %s. because the cluster is reopened. ", owner.getName()));
        } else if (nodes.get(owner.getName()) != owner) {
            //不存在了，或者变更成了其它实例
            owner.close();
            logger.info(String.format("Close the unused node instance %s. because it is removed or updated. ", owner.getName()));
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
        Node old = connects.put(node.getName(), node);
        readys = new ArrayList<>(connects.values());
        if (null != old && old != node) {
            old.close();
        }
        Optional.ofNullable(officer).ifPresent(o -> o.connect());
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

        if (logger.isInfoEnabled()) {
            logger.info(String.format("add shard %s (region=%s,dataCenter=%s,protocol=%s,version=%s,weight=%d) when cluster event for %s",
                    shard.getName(),
                    shard.getRegion(),
                    shard.getDataCenter(),
                    shard.getProtocol(),
                    shard.getUrl().getString("version", ""),
                    shard.getWeight(),
                    name));
        }
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
        return new Node(name, url, opens.get(), shard, factory, authorization, this::onNodeEvent,
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
        switch (type) {
            case DISCONNECT:
                //异步执行，防止死锁
                timer().add(new DisconnectTask(this, node));
                break;
        }
        clusterPublisher.offer(event);
    }

    /**
     * 管理连接就绪通知
     */
    protected static class Officer {
        /**
         * 集群
         */
        protected Cluster cluster;
        /**
         * 打开Future
         */
        protected CompletableFuture<AsyncResult<Cluster>> future;
        /**
         * 初始化建连的超时时间
         */
        protected long initTimeout;
        /**
         * 通知触发的连接数量
         */
        protected int triggerWhen;
        /**
         * 是否通知了等到初始化的消费者
         */
        protected AtomicBoolean mail;
        /**
         * 第一次选择
         */
        protected AtomicBoolean first = new AtomicBoolean(false);
        /**
         * 打开的连接的数量
         */
        protected AtomicLong counter = new AtomicLong();

        /**
         * 构造函数
         *
         * @param cluster
         * @param future
         */
        public Officer(Cluster cluster, CompletableFuture<AsyncResult<Cluster>> future) {
            this.cluster = cluster;
            this.future = future;
            this.triggerWhen = cluster.initSize;
            this.initTimeout = cluster.initTimeout;
            this.mail = cluster.mail;
            if (triggerWhen <= 0) {
                //不需要等到初始化连接
                timer().add("ReadyTask " + cluster.name, SystemClock.now(), () -> future.complete(new AsyncResult(this)));
            } else if (initTimeout > 0 && !mail.get()) {
                //超时检测
                timer().add("ReadyTask " + cluster.name, SystemClock.now() + initTimeout, this::timeout);
            }
        }

        /**
         * 选举完毕，更新初始化就绪需要的连接数
         *
         * @param size
         */
        public void setTriggerWhen(int size) {
            if (size > 0 && triggerWhen > 0 && first.compareAndSet(false, true)) {
                //第一次选择结果，对参数进行修正，避免initSize设置的不合理
                triggerWhen = Math.max(1, Math.min(size * 2 / 3, triggerWhen));
            }
        }

        /**
         * 启动超时通知
         */
        protected void timeout() {
            if (future != null && mail.compareAndSet(false, true)) {
                //run确保在打开状态
                cluster.switcher.writer().run(() -> future.complete(
                        new AsyncResult<>(cluster, new InitializationException("initialization timeout."))));
            }
        }

        /**
         * 触发连接就绪通知
         */
        public void connect() {
            if (triggerWhen > 0 && future != null && counter.incrementAndGet() == triggerWhen
                    && mail.compareAndSet(false, true)) {
                //初始化连接成功
                future.complete(new AsyncResult<>(cluster));
            }
        }
    }

    /**
     * 面板快照任务
     */
    protected static class DashboardTask implements Timer.TimeTask {
        /**
         * 集群
         */
        protected Cluster cluster;
        /**
         * 面板
         */
        protected final Dashboard dashboard;
        /**
         * 打开的对象
         */
        protected final long version;
        /**
         * 上次快照时间
         */
        protected long lastSnapshotTime;
        /**
         * 下次快照时间
         */
        protected long time;
        /**
         * 名称
         */
        protected final String name;


        /**
         * 构造函数
         *
         * @param cluster
         * @param version
         */
        public DashboardTask(final Cluster cluster, final long version) {
            this.cluster = cluster;
            this.dashboard = cluster.dashboard;
            this.version = version;
            //把集群指标过期分布到1秒钟以内，避免同时进行快照
            this.lastSnapshotTime = SystemClock.now() + ThreadLocalRandom.current().nextInt(1000);
            this.time = lastSnapshotTime + dashboard.getMetric().getWindowTime();
            this.dashboard.setLastSnapshotTime(lastSnapshotTime);
            this.name = this.getClass().getSimpleName() + " " + cluster.name;
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
            if (cluster.opens.get() == version && cluster.isOpened()) {
                dashboard.snapshot();
                time = SystemClock.now() + dashboard.getMetric().getWindowTime();
                timer().add(this);
            }
        }
    }

    /**
     * 节点任务
     */
    protected static abstract class NodeTask implements Timer.TimeTask {
        /**
         * 集群
         */
        protected final Cluster cluster;
        /**
         * 节点
         */
        protected final Node node;

        protected final String name;

        /**
         * 构造函数
         *
         * @param cluster
         * @param node
         */
        public NodeTask(Cluster cluster, Node node) {
            this.cluster = cluster;
            this.node = node;
            this.name = this.getClass().getSimpleName() + " " + node.getName();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void run() {
            if (node.getClusterVersion() == cluster.opens.get() && cluster.isOpened()) {
                //防止并发执行
                cluster.switcher.writer().tryRun(() -> {
                    if (node.getClusterVersion() == cluster.opens.get()) {
                        doRun();
                    }
                });
            }
        }

        /**
         * 执行
         */
        protected abstract void doRun();

    }

    /**
     * 断开连接任务
     */
    protected static class DisconnectTask extends NodeTask {

        /**
         * 构造函数
         *
         * @param cluster
         * @param node
         */
        public DisconnectTask(Cluster cluster, Node node) {
            super(cluster, node);
        }

        @Override
        public long getTime() {
            return SystemClock.now();
        }

        @Override
        protected void doRun() {
            cluster.onDisconnect(node);
        }
    }

    /**
     * 重连任务
     */
    protected static class ReconnectTask extends NodeTask {
        /**
         * 构造函数
         *
         * @param cluster
         * @param node
         */
        public ReconnectTask(Cluster cluster, Node node) {
            super(cluster, node);
        }

        @Override
        public long getTime() {
            return node.getRetry().getRetryTime();
        }

        @Override
        protected void doRun() {
            if (cluster.exists(node)) {
                node.retry.incrementTimes();
                cluster.connect(node);
            }
        }

    }

}
