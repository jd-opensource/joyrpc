package io.joyrpc.cluster.discovery.registry.etcd;

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

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
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
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.util.StringUtils;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static io.etcd.jetcd.watch.WatchEvent.EventType.PUT;
import static io.joyrpc.Plugin.CONFIG_EVENT_HANDLER;
import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.event.UpdateEvent.UpdateType;
import static io.joyrpc.event.UpdateEvent.UpdateType.FULL;
import static io.joyrpc.event.UpdateEvent.UpdateType.UPDATE;
import static io.joyrpc.util.Timer.timer;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * ETCD注册中心实现<br/>
 * 统一命名空间为"/joyrpc"<br/>
 * 注册的路径为"/joyrpc/service/接口名/分组别名"<br/>
 * 配置的路径为"/joyrpc/config/接口名"<br/>
 */
public class EtcdRegistry extends AbstractRegistry {

    private static final Logger logger = LoggerFactory.getLogger(EtcdRegistry.class);

    /**
     * 服务节点超时时间
     */
    private static final URLOption<Long> TTL = new URLOption<>("ttl", 60000L);

    private static final URLOption<String> AUTHORITY = new URLOption<>("authority", (String) null);

    /**
     * 目标地址
     */
    protected String address;
    /**
     * 用户认证
     */
    protected String authority;
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
     * 接口配置路径函数(接口级全局配置) /根路径/config/接口/consumer|provider/应用key
     */
    protected Function<URL, String> configFunction;
    /**
     * 注册provider的过期时间
     */
    protected long timeToLive;

    /**
     * 构造函数
     *
     * @param name
     * @param url
     * @param backup
     */
    public EtcdRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        this.address = URL.valueOf(url.getString(Constants.ADDRESS_OPTION), "http", 2379, null).toString();
        this.authority = url.getString(AUTHORITY);
        this.timeToLive = Math.max(url.getLong(TTL), 30000L);
        root = url.getString("namespace", GlobalContext.getString(PROTOCOL_KEY));
        if (root.charAt(0) != '/') {
            root = "/" + root;
        }
        if (root.charAt(root.length() - 1) == '/') {
            root = root.substring(0, root.length() - 1);
        }

        serviceFunction = u -> root + "/service/" + u.getPath() + "/" + u.getString(ALIAS_OPTION) + "/" + u.getString(ROLE_OPTION) + "/" + u.getProtocol() + "_" + u.getHost() + "_" + u.getPort();
        clusterFunction = u -> root + "/service/" + u.getPath() + "/" + u.getString(ALIAS_OPTION) + "/" + SIDE_PROVIDER;
        String appName = GlobalContext.getString(KEY_APPNAME);
        configFunction = u -> root + "/config/" + u.getPath() + "/" + u.getString(ROLE_OPTION) + "/" + (StringUtils.isEmpty(appName) ? "no_app" : appName);
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new EtcdController(this);
    }

    @Override
    protected Registion createRegistion(final URL url, final String key) {
        return new Registion(url, key, serviceFunction.apply(url));
    }

    /**
     * ETCD控制器
     */
    protected static class EtcdController extends RegistryController<EtcdRegistry> {

        /**
         * 客户端
         */
        protected volatile Client client;
        /**
         * 注册provider的统一续约id
         */
        protected volatile long leaseId;
        /**
         * 续约间隔
         */
        protected long leaseInterval;
        /**
         * 续约任务名称
         */
        protected String leaseTaskName;
        /**
         * 连续续约失败次数
         */
        protected AtomicInteger leaseErr = new AtomicInteger();


        /**
         * 构造函数
         *
         * @param registry 注册中心
         */
        public EtcdController(EtcdRegistry registry) {
            super(registry);
            this.leaseInterval = Math.max(registry.timeToLive / 5, 10000);
            this.leaseTaskName = "Lease-" + registry.registryId;
        }

        @Override
        protected ClusterBooking createClusterBooking(final URLKey key) {
            return new EtcdClusterBooking(key, this::dirty, getPublisher(key.getKey()), registry.clusterFunction.apply(key.getUrl()));
        }

        @Override
        protected ConfigBooking createConfigBooking(final URLKey key) {
            return new EtcdConfigBooking(key, this::dirty, getPublisher(key.getKey()), registry.configFunction.apply(key.getUrl()));
        }

        @Override
        protected CompletableFuture<Void> doConnect() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            client = Client.builder().lazyInitialization(false).endpoints(registry.address).authority(registry.authority).build();
            //生成统一续约id，并启动续约task
            CompletableFuture<LeaseGrantResponse> grant = client.getLeaseClient().grant(registry.timeToLive / 1000);
            grant.whenComplete((res, err) -> {
                if (err != null) {
                    client.close();
                    client = null;
                    future.completeExceptionally(err);
                } else {
                    leaseId = res.getID();
                    leaseErr.set(0);
                    //续约
                    timer().add(new Timer.DelegateTask(leaseTaskName, SystemClock.now() + leaseInterval, this::lease));
                    future.complete(null);
                }
            });
            return future;
        }

        /**
         * 续约
         */
        protected void lease() {
            if (isOpen()) {
                client.getLeaseClient().keepAliveOnce(leaseId).whenComplete((res, err) -> {
                    if (isOpen()) {
                        if (err != null) {
                            //连续续约三次失败
                            if (leaseErr.incrementAndGet() >= 3) {
                                logger.error(String.format("Error occurs while lease than 3 times, caused by %s. reconnect....", err.getMessage()));
                                //先关闭连接，再重连
                                doDisconnect().whenComplete((v, t) -> {
                                    if (isOpen()) {
                                        reconnect(new CompletableFuture<>(), 0, registry.maxConnectRetryTimes);
                                    }
                                });
                            } else {
                                logger.error(String.format("Error occurs while lease, caused by %s.", err.getMessage()));
                            }
                        } else {
                            leaseErr.set(0);
                            //继续续约
                            timer().add(new Timer.DelegateTask(leaseTaskName, SystemClock.now() + leaseInterval, this::lease));
                        }
                    }
                });
            }
        }

        @Override
        protected CompletableFuture<Void> doDisconnect() {
            if (client != null) {
                client.close();
            }
            leaseId = 0;
            return super.doDisconnect();
        }

        @Override
        protected CompletableFuture<Void> doRegister(final Registion registion) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            if (leaseId <= 0) {
                //没有租约
                future.completeExceptionally(new IllegalStateException(
                        String.format("Error occurs while register provider of %s, caused by no leaseId. retry....", registion.getPath())));
            } else {
                //有租约
                PutOption putOption = PutOption.newBuilder().withLeaseId(leaseId).build();
                ByteSequence key = ByteSequence.from(registion.getPath(), UTF_8);
                ByteSequence value = ByteSequence.from(registion.getUrl().toString(), UTF_8);
                client.getKVClient().put(key, value, putOption).whenComplete((r, t) -> {
                    if (!isOpen()) {
                        //已经关闭，或者创建了新的客户端
                        future.completeExceptionally(new IllegalStateException("controller is closed."));
                    } else if (t != null) {
                        //TODO 判断租期失效异常，触发重连逻辑
                        logger.error(String.format("Error occurs while register provider of %s, caused by %s. retry....",
                                registion.getPath(), t.getMessage()), t);
                        future.completeExceptionally(t);
                    } else {
                        future.complete(null);
                    }
                });
            }
            return future;
        }

        @Override
        protected CompletableFuture<Void> doDeregister(final Registion registion) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            ByteSequence key = ByteSequence.from(registion.getPath(), UTF_8);
            client.getKVClient().delete(key).whenComplete((r, t) -> {
                if (t != null) {
                    future.completeExceptionally(t);
                } else {
                    future.complete(null);
                }
            });
            return future;
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(final ClusterBooking booking) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            EtcdClusterBooking etcdBooking = (EtcdClusterBooking) booking;
            //先查询
            ByteSequence key = ByteSequence.from(etcdBooking.getPath(), UTF_8);
            //先查询，无异常后添加watcher，若结果不为空，通知FULL事件
            GetOption getOption = GetOption.newBuilder().withPrefix(key).build();
            client.getKVClient().get(key, getOption).whenComplete((res, err) -> {
                if (!isOpen()) {
                    future.completeExceptionally(new IllegalStateException("controller is closed."));
                } else if (err != null) {
                    logger.error(String.format("Error occurs while subscribe of %s, caused by %s. retry....", etcdBooking.getPath(), err.getMessage()), err);
                    future.completeExceptionally(err);
                } else {
                    List<WatchEvent> events = new ArrayList<>();
                    res.getKvs().forEach(kv -> events.add(new WatchEvent(kv, null, PUT)));
                    etcdBooking.onUpdate(events, res.getHeader().getRevision(), FULL);
                    //添加watch
                    try {
                        WatchOption watchOption = WatchOption.newBuilder().withPrefix(key).build();
                        Watch.Watcher watcher = client.getWatchClient().watch(key, watchOption, etcdBooking);
                        etcdBooking.setWatcher(watcher);
                        future.complete(null);
                    } catch (Exception e) {
                        logger.error(String.format("Error occurs while subscribe of %s, caused by %s. retry....", etcdBooking.getPath(), e.getMessage()), e);
                        future.completeExceptionally(e);
                    }
                }
            });
            return future;
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ClusterBooking booking) {
            EtcdClusterBooking etcdBooking = (EtcdClusterBooking) booking;
            Watch.Watcher watcher = etcdBooking.getWatcher();
            if (watcher != null) {
                watcher.close();
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(final ConfigBooking booking) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            EtcdConfigBooking etcdBooking = (EtcdConfigBooking) booking;
            //先查询
            ByteSequence key = ByteSequence.from(etcdBooking.getPath(), UTF_8);
            //先查询，无异常后添加watcher，若结果不为空，通知FULL事件
            GetOption getOption = GetOption.newBuilder().withPrefix(key).build();
            client.getKVClient().get(key, getOption).whenComplete((res, err) -> {
                if (!isOpen()) {
                    future.completeExceptionally(new IllegalStateException("controller is closed."));
                } else if (err != null) {
                    logger.error(String.format("Error occurs while subscribe of %s, caused by %s. retry....", etcdBooking.getPath(), err.getMessage()), err);
                    future.completeExceptionally(err);
                } else {
                    List<WatchEvent> events = new ArrayList<>();
                    res.getKvs().forEach(kv -> events.add(new WatchEvent(kv, null, PUT)));
                    etcdBooking.onUpdate(events, res.getHeader().getRevision());
                    //添加watch
                    try {
                        WatchOption watchOption = WatchOption.newBuilder().withPrefix(key).build();
                        Watch.Watcher watcher = client.getWatchClient().watch(key, watchOption, etcdBooking);
                        etcdBooking.setWatcher(watcher);
                        future.complete(null);
                    } catch (Exception e) {
                        logger.error(String.format("Error occurs while subscribe of %s, caused by %s. retry....", etcdBooking.getPath(), e.getMessage()), e);
                        future.completeExceptionally(e);
                    }
                }
            });
            return future;
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ConfigBooking booking) {
            EtcdConfigBooking etcdBooking = (EtcdConfigBooking) booking;
            Watch.Watcher watcher = etcdBooking.getWatcher();
            if (watcher != null) {
                watcher.close();
            }
            return CompletableFuture.completedFuture(null);
        }

    }

    /**
     * 集群配置
     */
    protected static class EtcdClusterBooking extends ClusterBooking implements Watch.Listener {
        /**
         * 监听器
         */
        protected Watch.Watcher watcher;

        public EtcdClusterBooking(URLKey key, Runnable dirty, Publisher<ClusterEvent> publisher, String path) {
            super(key, dirty, publisher, path);
        }

        @Override
        public void onNext(WatchResponse response) {
            List<WatchEvent> events = response.getEvents();
            if (events != null && !events.isEmpty()) {
                onUpdate(events, response.getHeader().getRevision(), UPDATE);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error(throwable.getMessage(), throwable);
        }

        @Override
        public void onCompleted() {

        }

        /**
         * 更新
         *
         * @param events     事件
         * @param version    版本
         * @param updateType 更新类型
         */
        public void onUpdate(final List<WatchEvent> events, final long version, final UpdateType updateType) {
            List<ShardEvent> shardEvents = new ArrayList<>();
            events.forEach(e -> {
                try {
                    String value = e.getKeyValue().getValue().toString(UTF_8);
                    switch (e.getEventType()) {
                        case PUT:
                            shardEvents.add(new ShardEvent(new DefaultShard(URL.valueOf(value)), ShardEventType.ADD));
                            break;
                        case DELETE:
                            //通过删除的key，转换成URL(集群删除节点，只根据shardName删除，所有能够获得ip:port即可)
                            int pos = value.lastIndexOf('/');
                            if (pos >= 0) {
                                value = value.substring(pos + 1);
                            }
                            String[] parts = value.split("_");
                            shardEvents.add(new ShardEvent(new DefaultShard(new URL(parts[0], parts[1], Integer.parseInt(parts[2]))), ShardEventType.DELETE));
                            break;
                    }
                } catch (Exception ignored) {
                }
            });
            handle(new ClusterEvent(this, null, updateType, version, shardEvents));
        }

        public Watch.Watcher getWatcher() {
            return watcher;
        }

        public void setWatcher(Watch.Watcher watcher) {
            this.watcher = watcher;
        }
    }

    /**
     * ETCD配置订阅
     */
    protected static class EtcdConfigBooking extends ConfigBooking implements Watch.Listener {
        /**
         * 监听器
         */
        protected Watch.Watcher watcher;

        public EtcdConfigBooking(final URLKey key, final Runnable dirty, final Publisher<ConfigEvent> publisher, final String path) {
            super(key, dirty, publisher, path);
        }

        @Override
        public void onNext(final WatchResponse response) {
            List<WatchEvent> events = response.getEvents();
            if (events != null && !events.isEmpty()) {
                onUpdate(events, response.getHeader().getRevision());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error(throwable.getMessage(), throwable);
        }

        @Override
        public void onCompleted() {

        }

        /**
         * 更新
         *
         * @param events  事件
         * @param version 版本
         */
        public void onUpdate(final List<WatchEvent> events, final long version) {
            if (events != null && !events.isEmpty()) {
                events.forEach(e -> {
                    Map<String, String> datum = null;
                    switch (e.getEventType()) {
                        case PUT:
                            datum = JSON.get().parseObject(e.getKeyValue().getValue().toString(UTF_8), Map.class);
                            break;
                        case DELETE:
                            datum = new HashMap<>();
                            break;
                    }
                    if (datum != null) {
                        String className = url.getPath();
                        Map<String, String> oldAttrs = GlobalContext.getInterfaceConfig(className);
                        final Map<String, String> data = datum;
                        //全局配置动态配置变更
                        CONFIG_EVENT_HANDLER.extensions().forEach(v -> v.handle(className, oldAttrs == null ? new HashMap<>() : oldAttrs, data));
                        //修改全局配置
                        GlobalContext.put(className, datum);
                        //TODO 是否需要实例配置
                        handle(new ConfigEvent(this, null, version, datum));
                    }
                });
            } else {
                handle(new ConfigEvent(this, null, version, new HashMap<>()));
            }
        }

        public Watch.Watcher getWatcher() {
            return watcher;
        }

        public void setWatcher(Watch.Watcher watcher) {
            this.watcher = watcher;
        }
    }

}
