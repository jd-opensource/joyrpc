package io.joyrpc.cluster.discovery.registry.zk;

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

import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.naming.ClusterHandler;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.discovery.registry.URLKey;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEventType;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.Publisher;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.api.CreateOption;
import org.apache.curator.x.async.api.ExistsOption;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.event.UpdateEvent.UpdateType.FULL;
import static io.joyrpc.event.UpdateEvent.UpdateType.UPDATE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode.POST_INITIALIZED_EVENT;
import static org.apache.curator.x.async.api.CreateOption.createParentsIfNeeded;
import static org.apache.curator.x.async.api.CreateOption.setDataIfExists;
import static org.apache.zookeeper.CreateMode.EPHEMERAL;

/**
 * zk注册中心
 */
public class ZKRegistry extends AbstractRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ZKRegistry.class);

    /**
     * session超时时间参数
     */
    public static final URLOption<Integer> SESSION_TIMEOUT = new URLOption<>("sessionTimeout", 60000);

    /**
     * zk异步Curator对象
     */
    protected AsyncCuratorFramework asyncCurator;
    /**
     * 目标地址
     */
    protected String address;
    /**
     * session过期时间
     */
    protected int sessionTimeout;
    /**
     * 根路径
     */
    protected String root;
    /**
     * 服务的路径函数 /根路径/service/接口/别名/consumer|provider/ip:port
     */
    protected Function<URL, String> serviceFunction;
    /**
     * 集群的路径函数 /根路径/service/接口/别名/provider
     */
    protected Function<URL, String> clusterFunction;
    /**
     * 接口配置路径函数(接口级全局配置) /根路径/config/接口/consumer|provider
     */
    protected Function<URL, String> configFunction;
    /**
     * 集群节点订阅事件管理
     */
    protected SubscriberManager clusterManager;
    /**
     * 配置订阅事件管理
     */
    protected SubscriberManager configManager;

    public ZKRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        this.address = url.getString(Constants.ADDRESS_OPTION);
        this.sessionTimeout = url.getInteger(SESSION_TIMEOUT);
        this.root = url.getString("namespace", GlobalContext.getString(PROTOCOL_KEY));
        if (root.charAt(0) != '/') {
            root = "/" + root;
        }
        if (root.charAt(root.length() - 1) == '/') {
            root = root.substring(0, root.length() - 1);
        }
        this.serviceFunction = u -> root + "/service/" + u.getPath() + "/" + u.getString(ALIAS_OPTION) + "/" + u.getString(ROLE_OPTION) + "/" + u.getProtocol() + "_" + u.getHost() + "_" + u.getPort();
        this.clusterFunction = u -> root + "/service/" + u.getPath() + "/" + u.getString(ALIAS_OPTION) + "/" + SIDE_PROVIDER;
        this.configFunction = u -> root + "/config/" + u.getPath() + "/" + u.getString(ROLE_OPTION) + "/" + GlobalContext.getString(KEY_APPNAME);
        this.clusterManager = new SubscriberManager(clusterFunction);
        this.configManager = new SubscriberManager(configFunction);
    }

    @Override
    protected CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            CuratorFramework cf = CuratorFrameworkFactory.builder()
                    .connectString(address)
                    .sessionTimeoutMs(sessionTimeout)
                    .connectionTimeoutMs(sessionTimeout)
                    .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                    .build();
            cf.start();
            cf.getConnectionStateListenable().addListener((curator, state) -> {
                if (state.isConnected()) {
                    logger.warn("zk connection state is changed to " + state + ", ZKRegistry while be recover.");
                    recover();
                } else {
                    logger.warn("zk connection state is changed to " + state + ".");
                }
            });
            asyncCurator = AsyncCuratorFramework.wrap(cf);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    protected CompletableFuture<Void> disconnect() {
        if (asyncCurator != null) {
            asyncCurator.unwrap().close();
        }
        connected.set(false);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doRegister(URLKey url) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String path = serviceFunction.apply(url.getUrl());
        String value = url.getUrl().toString();
        try {
            //判断节点是否存在
            Set<ExistsOption> existsOptions = new HashSet<ExistsOption>() {{
                add(ExistsOption.createParentsIfNeeded);
            }};
            asyncCurator.checkExists().withOptions(existsOptions).forPath(path).whenComplete((stat, exist) -> {
                //若存在，删除临时节点
                if (stat != null) {
                    try {
                        asyncCurator.unwrap().delete().forPath(path);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                //添加临时节点
                Set<CreateOption> options = new HashSet<CreateOption>() {{
                    add(createParentsIfNeeded);
                    add(setDataIfExists);
                }};
                asyncCurator.create().withOptions(options, EPHEMERAL).forPath(path, value.getBytes(UTF_8)).whenComplete((n, err) -> {
                    if (err != null) {
                        logger.error(err.getMessage(), err);
                        future.completeExceptionally(err);
                    } else {
                        future.complete(null);
                    }
                });
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    protected CompletableFuture<Void> doDeregister(URLKey url) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String path = serviceFunction.apply(url.getUrl());
        try {
            asyncCurator.delete().forPath(path).whenComplete((n, err) -> {
                if (err != null) {
                    future.completeExceptionally(err);
                } else {
                    future.complete(null);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    protected CompletableFuture<Void> doSubscribe(URLKey url, ClusterHandler handler) {
        return clusterManager.subscribe(url, new ClusterSubscriberExecutor(url.getUrl(), handler));
    }

    @Override
    protected CompletableFuture<Void> doUnsubscribe(URLKey url, ClusterHandler handler) {
        return clusterManager.unSubscribe(url);
    }

    @Override
    protected CompletableFuture<Void> doSubscribe(URLKey url, ConfigHandler handler) {
        return configManager.subscribe(url, new ConfigSubscriberExecutor(url.getUrl(), handler));
    }

    @Override
    protected CompletableFuture<Void> doUnsubscribe(URLKey url, ConfigHandler handler) {
        return configManager.unSubscribe(url);
    }

    protected static class ZKController extends RegistryController<ZKRegistry>{

        /**
         * 客户端
         */
        protected CuratorFramework client;

        public ZKController(ZKRegistry registry) {
            super(registry);
        }
    }

    /**
     * 配置订阅
     */
    protected static class ZKConfigBooking extends ConfigBooking {

        /**
         * 客户端
         */
        protected CuratorFramework client;
        /**
         * 路径函数
         */
        protected Function<URL, String> function;
        /**
         * zk节点监听cache
         */
        protected NodeCache nodeCache;

        public ZKConfigBooking(final URLKey key, final Runnable dirty, final Publisher<ConfigEvent> publisher) {
            super(key, dirty, publisher);
        }

        @Override
        public void start() throws Exception {
            status = Status.STARTING;
            try {
                String path = configFunction.apply(url);
                Stat pathStat = asyncCurator.unwrap().checkExists().creatingParentsIfNeeded().forPath(path);
                if (pathStat == null) {
                    asyncCurator.unwrap().create().creatingParentsIfNeeded().forPath(path, new byte[0]);
                }
                nodeCache = new NodeCache(asyncCurator.unwrap(), path);
                nodeCache.getListenable().addListener(() -> {
                    ChildData childData = nodeCache.getCurrentData();
                    Map<String, String> datum;
                    if (childData == null) {
                        //被删掉了
                        datum = new HashMap<>();
                    } else {
                        byte[] data = childData.getData();
                        if (data != null && data.length > 0) {
                            datum = JSON.get().parseObject(new String(data, UTF_8), Map.class);
                        } else {
                            datum = new HashMap<>();
                        }
                    }
                    handler.handle(new ConfigEvent(ZKRegistry.this, null, version.incrementAndGet(), datum));
                });
                nodeCache.start();
                status = Status.STARTED;
            } catch (Exception e) {
                status = SubscriberExecutor.Status.CLOSED;
                throw e;
            }
        }
    }

    /**
     * 订阅事件管理器
     */
    protected class SubscriberManager {

        /**
         * 订阅执行Executor列表
         */
        protected Map<String, SubscriberExecutor> subscribers = new ConcurrentHashMap<>();

        /**
         * 路径函数
         */
        protected Function<URL, String> function;


        /**
         * 构造方法
         *
         * @param function
         */
        public SubscriberManager(Function<URL, String> function) {
            this.function = function;
        }

        /**
         * 订阅操作
         *
         * @param urlKey
         * @param subscriber
         * @return
         */
        public CompletableFuture<Void> subscribe(URLKey urlKey, SubscriberExecutor subscriber) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            SubscriberExecutor old = subscribers.putIfAbsent(function.apply(urlKey.getUrl()), subscriber);
            try {
                //old不存在，说明是第一次注册，或者之前反注册过，启动入参subscriber
                //old存在，且状态为CLOSED，说明之前注册过，但没有启动成功，这里启动old
                //old存在，且状态不是CLOSED，这里防止重复操作，直接返回
                if (old == null) {
                    subscriber.start();
                } else if (old.getStatus() == SubscriberExecutor.Status.CLOSED) {
                    old.start();
                }
                future.complete(null);
            } catch (Exception e) {
                if (old == null) {
                    subscriber.close();
                } else {
                    old.close();
                }
                logger.error(e.getMessage(), e);
                future.completeExceptionally(e);
            }
            return future;
        }

        /**
         * 取消订阅操作
         *
         * @param urlKey
         * @return
         */
        public CompletableFuture<Void> unSubscribe(URLKey urlKey) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            try {
                SubscriberExecutor subscriber = subscribers.remove(function.apply(urlKey.getUrl()));
                if (subscriber != null) {
                    subscriber.close();
                }
                future.complete(null);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                future.completeExceptionally(e);
            }
            return future;
        }

    }

    /**
     * 事件订阅执行Executor
     */
    protected interface SubscriberExecutor {

        /**
         * 开始订阅
         *
         * @throws Exception
         */
        void start() throws Exception;

        /**
         * 终止订阅
         */
        void close();

        /**
         * 获取SubscriberExecutor启动状态
         *
         * @return
         */
        Status getStatus();

        /**
         * SubscriberExecutor启动状态
         */
        enum Status {
            CLOSED, STARTING, STARTED, CLOSING
        }
    }

    /**
     * 集群节点订阅Executor
     */
    protected class ClusterSubscriberExecutor implements SubscriberExecutor {

        /**
         * consumer url
         */
        protected URL url;
        /**
         * 集群事件handler
         */
        protected ClusterHandler handler;

        /**
         * zk节点监听cache
         */
        protected PathChildrenCache pathChildrenCache;
        /**
         * zk集群父节点path
         */
        protected String path;
        /**
         * 事件版本
         */
        protected AtomicLong version = new AtomicLong();
        /**
         * 是否已经初始化
         */
        protected AtomicBoolean initialized = new AtomicBoolean();
        /**
         * 启动状态
         */
        protected Status status;

        /**
         * 构造方法
         *
         * @param url
         * @param handler
         */
        public ClusterSubscriberExecutor(URL url, ClusterHandler handler) {
            this.url = url;
            this.handler = handler;
        }

        @Override
        public void start() throws Exception {
            status = Status.STARTING;
            path = clusterFunction.apply(url);
            try {
                //添加监听
                pathChildrenCache = new PathChildrenCache(asyncCurator.unwrap(), path, true);
                pathChildrenCache.getListenable().addListener((curatorFramework, curatorEvent) -> {
                    switch (curatorEvent.getType()) {
                        case CHILD_ADDED:
                            onUpdateShardEvent(ShardEventType.ADD, curatorEvent.getData());
                            break;
                        case CHILD_UPDATED:
                            onUpdateShardEvent(ShardEventType.UPDATE, curatorEvent.getData());
                            break;
                        case CHILD_REMOVED:
                            onUpdateShardEvent(ShardEventType.DELETE, curatorEvent.getData());
                            break;
                        case INITIALIZED:
                            onInitShardsEvent(curatorEvent.getInitialData());
                            break;
                    }
                });
                //启动监听
                pathChildrenCache.start(POST_INITIALIZED_EVENT);
                status = Status.STARTED;
            } catch (Exception e) {
                status = Status.CLOSED;
                throw e;
            }
        }

        /**
         * 增量更新
         *
         * @param eventType
         * @param childData
         */
        protected void onUpdateShardEvent(ShardEventType eventType, ChildData childData) {
            if (!initialized.get()) {
                return;
            }
            byte[] data = childData.getData();
            if (data != null) {
                List<ShardEvent> shardEvents = new ArrayList<>();
                URL providerUrl = URL.valueOf(new String(data, UTF_8));
                shardEvents.add(new ShardEvent(new Shard.DefaultShard(providerUrl), eventType));
                handler.handle(new ClusterEvent(ZKRegistry.this, null, UPDATE, version.incrementAndGet(), shardEvents));
            }
        }

        /**
         * 全量更新
         *
         * @param children
         * @throws Exception
         */
        protected void onInitShardsEvent(List<ChildData> children) throws Exception {
            initialized.set(true);
            List<ShardEvent> shardEvents = new ArrayList<>();
            if (children != null && !children.isEmpty()) {
                children.forEach(childData -> {
                    try {
                        byte[] data = childData.getData();
                        if (data != null && data.length > 0) {
                            URL providerUrl = URL.valueOf(new String(data, UTF_8));
                            shardEvents.add(new ShardEvent(new Shard.DefaultShard(providerUrl), ShardEventType.ADD));
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });
            }
            handler.handle(new ClusterEvent(ZKRegistry.this, null, FULL, version.incrementAndGet(), shardEvents));
        }

        @Override
        public void close() {
            status = Status.CLOSED;
            //终止监听
            try {
                pathChildrenCache.close();
            } catch (Throwable e) {
                //内部实现不抛异常，忽略
            }
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    /**
     * 配置订阅Executor
     */
    protected class ConfigSubscriberExecutor implements SubscriberExecutor {

        /**
         * Consumer/Provider url
         */
        protected URL url;

        /**
         * 配置监听handler
         */
        protected ConfigHandler handler;

        /**
         * zk节点监听cache
         */
        protected NodeCache nodeCache;

        /**
         * 事件版本
         */
        protected AtomicLong version = new AtomicLong();

        /**
         * 启动状态
         */
        protected Status status;

        /**
         * 构造方法
         *
         * @param url
         * @param handler
         */
        public ConfigSubscriberExecutor(URL url, ConfigHandler handler) {
            this.url = url;
            this.handler = handler;
        }

        @Override
        public void start() throws Exception {
            status = Status.STARTING;
            try {
                String path = configFunction.apply(url);
                Stat pathStat = asyncCurator.unwrap().checkExists().creatingParentsIfNeeded().forPath(path);
                if (pathStat == null) {
                    asyncCurator.unwrap().create().creatingParentsIfNeeded().forPath(path, new byte[0]);
                }
                nodeCache = new NodeCache(asyncCurator.unwrap(), path);
                nodeCache.getListenable().addListener(() -> {
                    ChildData childData = nodeCache.getCurrentData();
                    Map<String, String> datum;
                    if (childData == null) {
                        //被删掉了
                        datum = new HashMap<>();
                    } else {
                        byte[] data = childData.getData();
                        if (data != null && data.length > 0) {
                            datum = JSON.get().parseObject(new String(data, UTF_8), Map.class);
                        } else {
                            datum = new HashMap<>();
                        }
                    }
                    handler.handle(new ConfigEvent(ZKRegistry.this, null, version.incrementAndGet(), datum));
                });
                nodeCache.start();
                status = Status.STARTED;
            } catch (Exception e) {
                status = Status.CLOSED;
                throw e;
            }
        }

        @Override
        public void close() {
            status = Status.CLOSED;
            try {
                nodeCache.close();
            } catch (Throwable e) {
                //内部实现不抛异常，忽略
            }
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }
}
