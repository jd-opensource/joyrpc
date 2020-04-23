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

import io.joyrpc.cluster.Shard.DefaultShard;
import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.discovery.registry.URLKey;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEventType;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.Publisher;
import io.joyrpc.event.UpdateEvent.UpdateType;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.util.Futures;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.api.CreateOption;
import org.apache.curator.x.async.api.DeleteOption;
import org.apache.curator.x.async.api.ExistsOption;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
 * Zookeeper注册中心
 */
public class ZKRegistry extends AbstractRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ZKRegistry.class);

    /**
     * session超时时间参数
     */
    public static final URLOption<Integer> SESSION_TIMEOUT = new URLOption<>("sessionTimeout", 15000);

    /**
     * 目标地址
     */
    protected String address;
    /**
     * session过期时间
     */
    protected int sessionTimeout;
    /**
     * 连接超时时间
     */
    protected int connectionTimeout;
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
     * 构造函数
     *
     * @param name   名称
     * @param url    url
     * @param backup 备份
     */
    public ZKRegistry(final String name, final URL url, final Backup backup) {
        super(name, url, backup);
        this.address = URL.valueOf(url.getString(Constants.ADDRESS_OPTION), "zookeeper", 2181, null).getAddress();
        this.sessionTimeout = url.getInteger(SESSION_TIMEOUT);
        this.connectionTimeout = url.getInteger(CONNECT_TIMEOUT_OPTION);
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
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new ZKController(this);
    }

    @Override
    protected Registion createRegistion(final URL url, final String key) {
        return new Registion(url, key, serviceFunction.apply(url));
    }

    /**
     * ZK控制器
     */
    protected static class ZKController extends RegistryController<ZKRegistry> {

        /**
         * zk异步Curator对象
         */
        protected AsyncCuratorFramework curator;

        /**
         * 构造函数
         *
         * @param registry 注册中心
         */
        public ZKController(ZKRegistry registry) {
            super(registry);
        }

        @Override
        protected ClusterBooking createClusterBooking(final URLKey key) {
            return new ZKClusterBooking(key, this::dirty, getPublisher(key.getKey()), registry.clusterFunction.apply(key.getUrl()));
        }

        @Override
        protected ConfigBooking createConfigBooking(final URLKey key) {
            return new ZKConfigBooking(key, this::dirty, getPublisher(key.getKey()), registry.configFunction.apply(key.getUrl()));
        }

        @Override
        protected CompletableFuture<Void> doConnect() {
            return Futures.call(future -> {
                CuratorFramework client = CuratorFrameworkFactory.builder().connectString(registry.address)
                        .sessionTimeoutMs(registry.sessionTimeout)
                        .connectionTimeoutMs(registry.connectionTimeout)
                        .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                        .build();
                client.start();
                client.getConnectionStateListenable().addListener((curator, state) -> {
                    if (!isOpen()) {
                        doDisconnect().whenComplete((v, t) -> future.completeExceptionally(new IllegalStateException("controller is closed.")));
                    } else if (state.isConnected()) {
                        logger.warn("zk connection state is changed to " + state + ".");
                        if (future.isDone()) {
                            //重新注册
                            registers.forEach((k, r) -> addBookingTask(registers, r, this::doRegister));
                        } else {
                            future.complete(null);
                        }
                    } else {
                        //会自动重连
                        logger.warn("zk connection state is changed to " + state + ".");
                    }
                });
                curator = AsyncCuratorFramework.wrap(client);
            });
        }

        @Override
        protected CompletableFuture<Void> doDisconnect() {
            if (curator != null) {
                curator.unwrap().close();
            }
            return super.doDisconnect();
        }

        @Override
        protected CompletableFuture<Void> doRegister(final Registion registion) {
            Set<ExistsOption> existsOptions = new HashSet<ExistsOption>() {{
                add(ExistsOption.createParentsIfNeeded);
            }};
            Set<CreateOption> createOptions = new HashSet<CreateOption>() {{
                add(createParentsIfNeeded);
                add(setDataIfExists);
            }};
            return Futures.call(future -> {
                //判断节点是否存在
                curator.checkExists().withOptions(existsOptions).forPath(registion.getPath()).whenComplete((stat, exist) -> {
                    //若存在，删除临时节点
                    if (stat != null) {
                        try {
                            curator.unwrap().delete().forPath(registion.getPath());
                        } catch (Exception ignored) {
                        }
                    }
                    //添加临时节点
                    curator.create().withOptions(createOptions, EPHEMERAL)
                            .forPath(registion.getPath(), registion.getUrl().toString().getBytes(UTF_8)).whenComplete((n, err) -> {
                        if (err != null) {
                            future.completeExceptionally(err);
                        } else {
                            future.complete(null);
                        }
                    });
                });
            });
        }

        @Override
        protected CompletableFuture<Void> doDeregister(final Registion registion) {
            //删除节点
            Set<DeleteOption> deleteOptions = new HashSet<DeleteOption>() {{
                add(DeleteOption.quietly);
            }};
            return Futures.call(future -> curator.delete().withOptions(deleteOptions).forPath(registion.getPath()).whenComplete((n, err) -> {
                if (err != null) {
                    future.completeExceptionally(err);
                } else {
                    future.complete(null);
                }
            }));
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(final ClusterBooking booking) {
            return Futures.call(future -> {
                ZKClusterBooking zkBooking = (ZKClusterBooking) booking;
                //添加监听
                PathChildrenCache cache = new PathChildrenCache(curator.unwrap(), booking.getPath(), true);
                //启动监听
                cache.start(POST_INITIALIZED_EVENT);
                zkBooking.setChildrenCache(cache);
                future.complete(null);
                cache.getListenable().addListener((client, event) -> {
                    List<ShardEvent> events = new ArrayList<>();
                    UpdateType type = UPDATE;
                    switch (event.getType()) {
                        case INITIALIZED:
                            type = FULL;
                            List<ChildData> children = event.getInitialData();
                            if (children != null) {
                                children.forEach(childData -> addEvent(events, ShardEventType.ADD, childData));
                            }
                            break;
                        case CHILD_ADDED:
                            addEvent(events, ShardEventType.ADD, event.getData());
                            break;
                        case CHILD_UPDATED:
                            addEvent(events, ShardEventType.UPDATE, event.getData());
                            break;
                        case CHILD_REMOVED:
                            addEvent(events, ShardEventType.DELETE, event.getData());
                            break;

                    }
                    booking.handle(new ClusterEvent(registry, null, type, zkBooking.getStat().incrementAndGet(), events));
                });
            });
        }

        /**
         * 添加事件
         *
         * @param events    事件集合
         * @param type      事件类型
         * @param childData 节点数据
         */
        protected void addEvent(final List<ShardEvent> events, final ShardEventType type, final ChildData childData) {
            byte[] data = childData.getData();
            if (data != null) {
                events.add(new ShardEvent(new DefaultShard(URL.valueOf(new String(data, UTF_8))), type));
            }
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ClusterBooking booking) {
            PathChildrenCache cache = ((ZKClusterBooking) booking).getChildrenCache();
            if (cache != null) {
                try {
                    cache.close();
                } catch (IOException ignored) {
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(final ConfigBooking booking) {
            return Futures.call(future -> {
                ZKConfigBooking zkBooking = (ZKConfigBooking) booking;
                CuratorFramework client = curator.unwrap();
                Stat pathStat = client.checkExists().creatingParentsIfNeeded().forPath(booking.getPath());
                if (pathStat == null) {
                    client.create().creatingParentsIfNeeded().forPath(booking.getPath(), new byte[0]);
                }
                NodeCache cache = new NodeCache(client, booking.getPath());
                cache.start();
                zkBooking.setNodeCache(cache);
                future.complete(null);
                cache.getListenable().addListener(() -> {
                    ChildData childData = cache.getCurrentData();
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
                    booking.handle(new ConfigEvent(registry, null, zkBooking.getStat().incrementAndGet(), datum));
                });
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ConfigBooking booking) {
            NodeCache cache = ((ZKConfigBooking) booking).getNodeCache();
            if (cache != null) {
                try {
                    cache.close();
                } catch (IOException ignored) {
                }
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 配置订阅
     */
    protected static class ZKClusterBooking extends ClusterBooking {
        /**
         * zk节点监听cache
         */
        protected PathChildrenCache childrenCache;
        /**
         * 事件版本
         */
        protected AtomicLong stat = new AtomicLong();

        /**
         * 构造函数
         *
         * @param key       键
         * @param dirty     脏函数
         * @param publisher 通知器
         * @param path      路径
         */
        public ZKClusterBooking(final URLKey key, final Runnable dirty, final Publisher<ClusterEvent> publisher, final String path) {
            super(key, dirty, publisher, path);
        }

        public PathChildrenCache getChildrenCache() {
            return childrenCache;
        }

        public void setChildrenCache(PathChildrenCache childrenCache) {
            this.childrenCache = childrenCache;
        }

        public AtomicLong getStat() {
            return stat;
        }
    }

    /**
     * 配置订阅
     */
    protected static class ZKConfigBooking extends ConfigBooking {
        /**
         * zk节点监听cache
         */
        protected NodeCache nodeCache;
        /**
         * 事件版本
         */
        protected AtomicLong stat = new AtomicLong();

        /**
         * 构造函数
         *
         * @param key       键
         * @param dirty     脏函数
         * @param publisher 通知器
         * @param path      路径
         */
        public ZKConfigBooking(final URLKey key, final Runnable dirty, final Publisher<ConfigEvent> publisher, final String path) {
            super(key, dirty, publisher, path);
        }

        public NodeCache getNodeCache() {
            return nodeCache;
        }

        public void setNodeCache(NodeCache nodeCache) {
            this.nodeCache = nodeCache;
        }

        public AtomicLong getStat() {
            return stat;
        }
    }
}
