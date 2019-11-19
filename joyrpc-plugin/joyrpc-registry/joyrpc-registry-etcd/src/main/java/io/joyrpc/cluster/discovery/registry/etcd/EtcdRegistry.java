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
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.util.Daemon;
import io.joyrpc.util.StringUtils;
import io.joyrpc.util.Waiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.etcd.jetcd.watch.WatchEvent.EventType.PUT;
import static io.joyrpc.Plugin.CONFIG_EVENT_HANDLER;
import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.event.UpdateEvent.UpdateType;
import static io.joyrpc.event.UpdateEvent.UpdateType.FULL;
import static io.joyrpc.event.UpdateEvent.UpdateType.UPDATE;
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
     * 客户端
     */
    protected volatile Client client;

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
     * 集群节点事件watch
     */
    protected WatcherManager cluster;
    /**
     * 配置事件watch
     */
    protected WatcherManager config;
    /**
     * 注册provider的过期时间
     */
    protected long timeToLive;
    /**
     * 注册provider的统一续约id
     */
    protected volatile long leaseId;
    /**
     * 注册provider的统一续约task
     */
    protected Daemon daemon;
    /**
     * 连续续约失败次数
     */
    protected AtomicInteger leaseErr = new AtomicInteger();

    /**
     * 构造函数
     *
     * @param name
     * @param url
     * @param backup
     */
    public EtcdRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        this.address = url.getString(Constants.ADDRESS_OPTION);
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
        cluster = new WatcherManager(clusterFunction);
        config = new WatcherManager(configFunction);
    }

    /**
     * 续约
     */
    protected void lease() {
        client.getLeaseClient().keepAliveOnce(leaseId).whenComplete((res, err) -> {
            if (err != null) {
                //连续续约三次失败
                if (leaseErr.incrementAndGet() >= 3) {
                    logger.error(String.format("Error occurs while lease than 3 times, caused by %s. reconnect....", err.getMessage()));
                    //先关闭连接，再重连
                    disconnect().whenComplete((v, t) -> reconnect(new CompletableFuture<>(), 0, maxConnectRetryTimes));
                } else {
                    logger.error(String.format("Error occurs while lease, caused by %s.", err.getMessage()));
                }
            } else {
                leaseErr.set(0);
            }
        });
    }

    @Override
    protected CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client = Client.builder().lazyInitialization(false).endpoints(address).authority(authority).build();
        //生成统一续约id，并启动续约task
        CompletableFuture<LeaseGrantResponse> grant = client.getLeaseClient().grant(timeToLive / 1000);
        grant.whenComplete((res, err) -> {
            if (err != null) {
                future.completeExceptionally(err);
            } else {
                leaseId = res.getID();
                leaseErr.set(0);
                daemon = Daemon.builder().name("EtcdRegistry-" + registryId + "-lease-task")
                        .interval(Math.max(timeToLive / 5, 10000))
                        .condition(switcher::isOpened)
                        .runnable(this::lease)
                        .waiter(new Waiter.MutexWaiter())
                        .build();
                daemon.start();
                future.complete(null);
            }
        });
        return future;
    }

    @Override
    protected CompletableFuture<Void> disconnect() {
        if (daemon != null) {
            daemon.stop();
            daemon = null;
        }
        if (client != null) {
            client.close();
        }
        cluster.close();
        config.close();
        leaseId = 0;
        connected.set(false);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doRegister(final URLKey url) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String path = serviceFunction.apply(url.getUrl());
        if (leaseId <= 0) {
            //没有租约
            future.completeExceptionally(new IllegalStateException(
                    String.format("Error occurs while register provider of %s, caused by no leaseId. retry....", path)));
        } else {
            //有租约
            ByteSequence key = ByteSequence.from(path, UTF_8);
            ByteSequence value = ByteSequence.from(url.getUrl().toString(), UTF_8);
            Client cl = client;
            try {
                PutOption putOption = PutOption.newBuilder().withLeaseId(leaseId).build();
                cl.getKVClient().put(key, value, putOption).whenComplete(
                        new MyConsumer<>(future, cl, throwable ->
                                String.format("Error occurs while register provider of %s, caused by %s. retry....",
                                        path, throwable.getMessage())));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }
        return future;
    }

    @Override
    protected CompletableFuture<Void> doDeregister(final URLKey url) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String path = serviceFunction.apply(url.getUrl());
        Client cl = client;
        cl.getKVClient().delete(ByteSequence.from(path, UTF_8)).whenComplete(
                new MyConsumer<>(future, cl, throwable ->
                        String.format("Error occurs while deregister provider of %s, caused by %s. retry....",
                                path, throwable.getMessage())));

        return future;
    }

    @Override
    protected CompletableFuture<Void> doSubscribe(final URLKey url, final ClusterHandler handler) {
        return cluster.subscribe(client, url, new ClusterListener(url, handler));
    }

    @Override
    protected CompletableFuture<Void> doUnsubscribe(final URLKey url, final ClusterHandler handler) {
        return cluster.unsubscribe(url);
    }

    @Override
    protected CompletableFuture<Void> doSubscribe(final URLKey url, final ConfigHandler handler) {
        return config.subscribe(client, url, new ConfigListener(url, handler));
    }

    @Override
    protected CompletableFuture<Void> doUnsubscribe(final URLKey url, final ConfigHandler handler) {
        return config.unsubscribe(url);
    }

    /**
     * 订阅监听器
     */
    protected interface SubscribeListener extends Watch.Listener {

        @Override
        default void onNext(WatchResponse response) {
            List<WatchEvent> events = response.getEvents();
            if (events != null && !events.isEmpty()) {
                onUpdate(events, response.getHeader().getRevision(), UPDATE);
            }
        }

        /**
         * 更新操作
         *
         * @param events
         * @param version
         * @param updateType
         */
        void onUpdate(List<WatchEvent> events, long version, UpdateType updateType);

        @Override
        default void onError(Throwable throwable) {
            logger.error(throwable.getMessage(), throwable);
        }

        @Override
        default void onCompleted() {

        }
    }

    /**
     * 配置监听器
     */
    protected class ConfigListener implements SubscribeListener {

        /**
         * 键
         */
        protected URLKey url;
        /**
         * 集群处理器
         */
        protected ConfigHandler handler;

        /**
         * 构造函数
         */
        public ConfigListener(URLKey url, ConfigHandler handler) {
            this.url = url;
            this.handler = handler;
        }

        @Override
        public void onUpdate(List<WatchEvent> events, long version, UpdateType updateType) {
            if (events != null && !events.isEmpty()) {
                events.forEach(e -> {
                    switch (e.getEventType()) {
                        case PUT:
                            publish(version, e.getKeyValue().getValue().toString(UTF_8));
                            break;
                        case DELETE:
                            publish(version, new HashMap<>());
                            break;
                    }
                });
            } else {
                publish(version, new HashMap<>());
            }
        }

        /**
         * 发布数据
         *
         * @param revision
         * @param text
         */
        public void publish(final long revision, final String text) {
            Map<String, String> datum = JSON.get().parseObject(text, Map.class);
            publish(revision, datum);
        }

        /**
         * 发布数据
         *
         * @param revision
         * @param datum
         */
        public void publish(final long revision, final Map<String, String> datum) {
            String className = url.getUrl().getPath();
            Map<String, String> oldAttrs = GlobalContext.getInterfaceConfig(className);
            //全局配置动态配置变更
            CONFIG_EVENT_HANDLER.extensions().forEach(v -> v.handle(className, oldAttrs, datum));
            //修改全局配置
            GlobalContext.put(className, datum);
            //TODO 是否需要实例配置
            handler.handle(new ConfigEvent(EtcdRegistry.this, null, FULL, revision, new HashMap<>()));
        }

    }

    /**
     * 集群监听器
     */
    protected class ClusterListener implements SubscribeListener {
        /**
         * 集群处理器
         */
        protected ClusterHandler handler;
        /**
         * 键
         */
        protected URLKey url;

        /**
         * 构造函数
         *
         * @param handler
         */
        public ClusterListener(URLKey url, ClusterHandler handler) {
            this.url = url;
            this.handler = handler;
        }

        @Override
        public void onUpdate(List<WatchEvent> events, long version, UpdateType updateType) {
            List<ShardEvent> shardEvents = new ArrayList<>();
            events.forEach(e -> {
                switch (e.getEventType()) {
                    case PUT:
                        shardEvents.add(new ShardEvent(
                                new DefaultShard(URL.valueOf(e.getKeyValue().getValue().toString(UTF_8))),
                                ShardEventType.ADD));
                        break;
                    case DELETE:
                        shardEvents.add(new ShardEvent(
                                new DefaultShard(convertByDelKey(e.getKeyValue().getKey().toString(UTF_8))),
                                ShardEventType.DELETE));
                        break;
                }
            });
            if (!shardEvents.isEmpty()) {
                handler.handle(new ClusterEvent(EtcdRegistry.this, null, updateType, version, shardEvents));
            }
        }

        /**
         * 通过删除的key，转换成URL(集群删除节点，只根据shardName删除，所有能够获得ip:port即可)
         *
         * @param delKey
         * @return
         */
        private URL convertByDelKey(String delKey) {
            if (StringUtils.isNotEmpty(delKey)) {
                String[] strs = delKey.substring(delKey.lastIndexOf("/") + 1).split("_");
                return new URL(strs[0], strs[1], Integer.parseInt(strs[2]));
            }
            return null;
        }

    }

    /**
     * 监听器
     */
    protected static class MyWatcher {

        /**
         * 监听器
         */
        protected Watch.Listener listener;
        /**
         * 监听器
         */
        protected Watch.Watcher watcher;

        /**
         * 构造函数
         *
         * @param listener
         * @param watcher
         */
        public MyWatcher(Watch.Listener listener, Watch.Watcher watcher) {
            this.listener = listener;
            this.watcher = watcher;
        }

        public Watch.Listener getListener() {
            return listener;
        }

        public Watch.Watcher getWatcher() {
            return watcher;
        }

        /**
         * 关闭
         */
        public void close() {
            watcher.close();
        }
    }

    /**
     * 观察者管理器
     */
    protected static class WatcherManager {
        /**
         * 管理的观察者
         */
        protected Map<String, MyWatcher> watchers = new ConcurrentHashMap<>();
        /**
         * 路径函数
         */
        protected Function<URL, String> function;

        /**
         * 构造函数
         *
         * @param function
         */
        public WatcherManager(Function<URL, String> function) {
            this.function = function;
        }

        /**
         * 订阅
         *
         * @param client
         * @param url
         * @param listener
         * @return
         */
        public CompletableFuture<Void> subscribe(final Client client, final URLKey url, final SubscribeListener listener) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            //先查询
            String path = function.apply(url.getUrl());
            ByteSequence key = ByteSequence.from(path, UTF_8);
            //先查询，无异常后添加watcher，若结果不为空，通知FULL事件
            GetOption getOption = GetOption.newBuilder().withPrefix(key).build();
            client.getKVClient().get(key, getOption).whenComplete((res, err) -> {
                if (err != null) {
                    logger.error(String.format("Error occurs while subscribe of %s, caused by %s. retry....", path, err.getMessage()), err);
                    future.completeExceptionally(err);
                } else {
                    List<WatchEvent> events = new ArrayList<>();
                    res.getKvs().forEach(kv -> events.add(new WatchEvent(kv, null, PUT)));
                    listener.onUpdate(events, res.getHeader().getRevision(), FULL);
                    //添加watch
                    try {
                        WatchOption watchOption = WatchOption.newBuilder().withPrefix(key).build();
                        Watch.Watcher watcher = client.getWatchClient().watch(key, watchOption, listener);
                        MyWatcher myWatcher = new MyWatcher(listener, watcher);
                        MyWatcher oldWatcher = watchers.put(url.getKey(), myWatcher);
                        if (oldWatcher != null) {
                            oldWatcher.close();
                        }
                        future.complete(null);
                    } catch (Exception e) {
                        logger.error(String.format("Error occurs while subscribe of %s, caused by %s. retry....", path, e.getMessage()), e);
                        future.completeExceptionally(e);
                    }
                }
            });
            return future;
        }

        /**
         * 取消订阅
         *
         * @param url
         * @return
         */
        public CompletableFuture<Void> unsubscribe(final URLKey url) {
            MyWatcher watcher = watchers.remove(url.getKey());
            if (watcher != null) {
                watcher.close();
            }
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 关闭
         */
        public void close() {
            watchers.forEach((key, watcher) -> watcher.close());
            watchers.clear();
        }
    }

    /**
     * 异步事件消费者
     *
     * @param <T>
     */
    protected class MyConsumer<T> implements BiConsumer<T, Throwable> {
        /**
         * Future
         */
        protected CompletableFuture<Void> future;
        /**
         * 客户端
         */
        protected Client target;
        /**
         * 异常信息提供者
         */
        protected Function<Throwable, String> error;
        /**
         * 成功消费者
         */
        protected Consumer<T> consumer;

        /**
         * 构造函数
         *
         * @param future
         * @param target
         * @param error
         */
        public MyConsumer(CompletableFuture<Void> future, Client target, Function<Throwable, String> error) {
            this(future, target, error, null);
        }

        /**
         * 构造函数
         *
         * @param future
         * @param target
         * @param error
         * @param consumer
         */
        public MyConsumer(CompletableFuture<Void> future, Client target, Function<Throwable, String> error, Consumer<T> consumer) {
            this.future = future;
            this.target = target;
            this.error = error;
            this.consumer = consumer;
        }

        @Override
        public void accept(final T t, final Throwable throwable) {
            if (!switcher.isOpened() || target != client) {
                //已经关闭，或者创建了新的客户端
                future.complete(null);
            } else if (throwable != null) {
                //TODO 判断租期失效异常，触发重连逻辑
                if (error != null) {
                    logger.error(error.apply(throwable), throwable);
                }
                future.completeExceptionally(throwable);
            } else {
                if (consumer != null) {
                    consumer.accept(t);
                }
                future.complete(null);
            }
        }
    }

}
