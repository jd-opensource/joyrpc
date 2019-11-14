package io.joyrpc.cluster.discovery.registry.broadcast;

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

import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryExpiredListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
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
import io.joyrpc.context.Environment;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.util.Daemon;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.joyrpc.Plugin.ENVIRONMENT;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.event.UpdateEvent.UpdateType.FULL;
import static io.joyrpc.event.UpdateEvent.UpdateType.UPDATE;

/**
 * hazelcast注册中心实现
 */
public class BroadCastRegistry extends AbstractRegistry {

    private static final Logger logger = LoggerFactory.getLogger(BroadCastRegistry.class);

    /**
     * 备份个数
     */
    public static final URLOption<Integer> BACKUP_COUNT = new URLOption<>("backupCount", 1);

    /**
     * 异步备份个数
     */
    public static final URLOption<Integer> ASYNC_BACKUP_COUNT = new URLOption<>("asyncBackupCount", 1);
    /**
     * hazelcast集群分组名称
     */
    public static final URLOption<String> BROADCAST_GROUP_NAME = new URLOption<>("broadCastGroupName", "dev");
    /**
     * 节点广播端口
     */
    public static final URLOption<Integer> NETWORK_PORT = new URLOption<>("networkPort", 6701);
    /**
     * 节点广播端口递增个数
     */
    public static final URLOption<Integer> NETWORK_PORT_COUNT = new URLOption<>("networkPortCount", 100);
    /**
     * multicast分组
     */
    public static final URLOption<String> MULTICAST_GROUP = new URLOption<>("multicastGroup", "224.2.2.3");
    /**
     * multicast组播端口
     */
    public static final URLOption<Integer> MULTICAST_PORT = new URLOption<>("multicastPort", 64327);
    /**
     * 节点失效时间参数
     */
    public static final URLOption<Long> NODE_EXPIRED_TIME = new URLOption<>("nodeExpiredTime", 30000L);
    /**
     * hazelcast实例配置
     */
    protected Config cfg;
    /**
     * hazelcast实例
     */
    protected HazelcastInstance instance;
    /**
     * 根路径
     */
    protected String root;
    /**
     * 节点时效时间, 至少 NODE_EXPIRED_TIME.getValue() ms
     */
    protected long nodeExpiredTime;
    /**
     * 存储provider的Map的路径函数
     */
    protected Function<URL, String> providersRootKeyFunc;
    /**
     * 存在provider或者consumer的Map的路径函数
     */
    protected Function<URL, String> serviceRootKeyFunc;
    /**
     * provider或consumer在存储map中的key值的函数
     */
    protected Function<URL, String> serviceNodeKeyFunc;
    /**
     * 存储接口配置的Map的路径函数
     */
    protected Function<URL, String> configRootKeyFunc;
    /**
     * 集群订阅管理
     */
    protected SubscriberManager clusterSubscriberManager;
    /**
     * 配置订阅管理
     */
    protected SubscriberManager configSubscriberManager;
    /**
     * consuner与provider与注册中心心跳task
     */
    protected Daemon daemon;

    public BroadCastRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        this.cfg = new Config();
        int cpus = ENVIRONMENT.get().getInteger(Environment.CPU_CORES) * 2;
        Properties properties = cfg.getProperties();
        properties.setProperty("hazelcast.operation.thread.count", String.valueOf(cpus));
        properties.setProperty("hazelcast.operation.generic.thread.count", String.valueOf(cpus));
        properties.setProperty("hazelcast.memcache.enabled", "false");
        properties.setProperty("hazelcast.rest.enabled", "false");
        //从url里面获取hazelcast参数
        properties.putAll(url.startsWith("hazelcast."));
        //不创建关闭钩子
        properties.setProperty("hazelcast.shutdownhook.enabled", "false");

        //同步复制，可以读取从
        cfg.getMapConfig("default").setBackupCount(url.getPositiveInt(BACKUP_COUNT)).
                setAsyncBackupCount(url.getNaturalInt(ASYNC_BACKUP_COUNT)).setReadBackupData(false);
        cfg.getGroupConfig().setName(url.getString(BROADCAST_GROUP_NAME));
        cfg.getNetworkConfig().setPort(url.getPositiveInt(NETWORK_PORT)).setPortCount(url.getInteger(NETWORK_PORT_COUNT));
        cfg.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true)
                .setMulticastGroup(url.getString(MULTICAST_GROUP)).setMulticastPort(url.getPositiveInt(MULTICAST_PORT));
        this.nodeExpiredTime = Math.max(url.getLong(NODE_EXPIRED_TIME), NODE_EXPIRED_TIME.getValue());
        this.root = url.getString("namespace", GlobalContext.getString(PROTOCOL_KEY));
        if (root.charAt(0) == '/') {
            root = root.substring(1);
        }
        if (root.charAt(root.length() - 1) == '/') {
            root = root.substring(0, root.length() - 1);
        }
        this.providersRootKeyFunc = u -> root + "/service/" + u.getPath() + "/" + u.getString(ALIAS_OPTION) + "/" + SIDE_PROVIDER;
        this.serviceRootKeyFunc = u -> root + "/service/" + u.getPath() + "/" + u.getString(ALIAS_OPTION) + "/" + u.getString(ROLE_OPTION);
        this.serviceNodeKeyFunc = u -> u.getProtocol() + "://" + u.getHost() + ":" + u.getPort();
        this.configRootKeyFunc = u -> root + "/config/" + u.getPath() + "/" + u.getString(ROLE_OPTION) + "/" + GlobalContext.getString(KEY_APPNAME);
        this.clusterSubscriberManager = new SubscriberManager(providersRootKeyFunc);
        this.configSubscriberManager = new SubscriberManager(configRootKeyFunc);
    }

    @Override
    protected CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            instance = Hazelcast.newHazelcastInstance(cfg);
            daemon = Daemon.builder().name("BroadCastRegistry-" + registryId + "-heartbeat-task")
                    .interval(Math.max(15000, nodeExpiredTime / 3))
                    .runnable(() -> registers.forEach((key, meta) -> lease(meta)))
                    .error(e -> logger.error("Error occurs while leasing, caused by " + e.getMessage(), e))
                    .build();
            daemon.start();
            future.complete(null);
        } catch (Exception e) {
            logger.error(String.format("Error occurs while connect, caused by %s", e.getMessage()), e);
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 续约
     *
     * @param meta
     */
    protected void lease(final RegisterMeta meta) {
        //registerTime不为0，说明已经注册，执行续期
        if (meta.getRegisterTime() != 0) {
            URL url = meta.getUrl();
            String serviceRootKey = serviceRootKeyFunc.apply(url);
            String nodeKey = serviceNodeKeyFunc.apply(url);
            IMap<String, URL> map = instance.getMap(serviceRootKey);
            //计算新的ttl，并续期
            long newTtl = SystemClock.now() - meta.getRegisterTime() + nodeExpiredTime;
            boolean isNotExpired = map.setTtl(nodeKey, newTtl, TimeUnit.MILLISECONDS);
            //节点已经过期，重新添加回节点，并修改meta的registerTime
            if (!isNotExpired) {
                map.put(nodeKey, url, nodeExpiredTime, TimeUnit.MILLISECONDS);
                meta.setRegisterTime(SystemClock.now());
            }
        }
    }

    @Override
    protected CompletableFuture<Void> disconnect() {
        if (daemon != null) {
            daemon.stop();
            daemon = null;
        }
        if (instance != null) {
            instance.shutdown();
        }
        connected.set(false);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doRegister(URLKey url) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            IMap<String, URL> iMap = instance.getMap(serviceRootKeyFunc.apply(url.getUrl()));
            iMap.putAsync(serviceNodeKeyFunc.apply(url.getUrl()), url.getUrl(), nodeExpiredTime, TimeUnit.MILLISECONDS)
                    .andThen(new AsyncCallback<>(future,
                            e -> logger.error(String.format("Error occurs while do register of %s, caused by %s",
                                    url.getKey(), e.getMessage()), e)));
        } catch (Exception e) {
            logger.error(String.format("Error occurs while do register of %s, caused by %s", url.getKey(), e.getMessage()), e);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    protected CompletableFuture<Void> doDeregister(URLKey url) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            IMap<String, URL> iMap = instance.getMap(serviceRootKeyFunc.apply(url.getUrl()));
            iMap.removeAsync(serviceNodeKeyFunc.apply(url.getUrl())).andThen(
                    new AsyncCallback<>(future,
                            e -> logger.error(String.format("Error occurs while do deregister of %s, caused by %s",
                                    url.getKey(), e.getMessage()), e)));

        } catch (HazelcastInstanceNotActiveException e) {
            future.completeExceptionally(e);
        } catch (Exception e) {
            logger.error(String.format("Error occurs while do deregister of %s, caused by %s", url.getKey(), e.getMessage()), e);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    protected CompletableFuture<Void> doSubscribe(URLKey url, ClusterHandler handler) {
        return clusterSubscriberManager.subscribe(url, new ClusterListener(url.getUrl(), handler));
    }

    @Override
    protected CompletableFuture<Void> doUnsubscribe(URLKey url, ClusterHandler handler) {
        return clusterSubscriberManager.unSubscribe(url);
    }

    @Override
    protected CompletableFuture<Void> doSubscribe(URLKey url, ConfigHandler handler) {
        return configSubscriberManager.subscribe(url, new ConfigListener(url.getUrl(), handler));
    }

    @Override
    protected CompletableFuture<Void> doUnsubscribe(URLKey url, ConfigHandler handler) {
        return configSubscriberManager.unSubscribe(url);
    }

    /**
     * 订阅管理器
     */
    protected class SubscriberManager {

        /**
         * listener id列表
         */
        protected Map<String, String> listenerIds = new ConcurrentHashMap<>();
        /**
         * 共享map路径函数
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
         * @param listener
         * @return
         */
        public CompletableFuture<Void> subscribe(URLKey urlKey, SubscribeListener listener) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            try {
                listenerIds.computeIfAbsent(urlKey.getKey(), o -> {
                    IMap map = instance.getMap(function.apply(urlKey.getUrl()));
                    listener.onFullEvent(map);
                    return map.addEntryListener(listener, true);
                });
                future.complete(null);
            } catch (HazelcastInstanceNotActiveException e) {
                logger.error(String.format("Error occurs while subscribe of %s, caused by %s", urlKey.getKey(), e.getMessage()));
                future.completeExceptionally(e);
            } catch (Exception e) {
                logger.error(String.format("Error occurs while subscribe of %s, caused by %s", urlKey.getKey(), e.getMessage()), e);
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
                String listenerId = listenerIds.remove(urlKey.getKey());
                if (listenerId != null) {
                    IMap map = instance.getMap(function.apply(urlKey.getUrl()));
                    map.removeEntryListener(listenerId);
                }
                future.complete(null);
            } catch (HazelcastInstanceNotActiveException e) {
                logger.error(String.format("Error occurs while unsubscribe of %s, caused by %s", urlKey.getKey(), e.getMessage()));
                future.completeExceptionally(e);
            } catch (Exception e) {
                logger.error(String.format("Error occurs while unsubscribe of %s, caused by %s", urlKey.getKey(), e.getMessage()), e);
                future.completeExceptionally(e);
            }
            return future;
        }

    }

    /**
     * 监听器接口
     */
    protected interface SubscribeListener extends EntryAddedListener, EntryUpdatedListener, EntryRemovedListener, EntryExpiredListener {

        /**
         * 全量更新事件
         *
         * @param map
         */
        void onFullEvent(IMap map);

    }

    /**
     * 集群事件监听
     */
    protected class ClusterListener implements SubscribeListener {

        /**
         * consumer的url
         */
        protected URL serviceUrl;
        /**
         * 集群事件handler
         */
        protected ClusterHandler handler;
        /**
         * 事件版本
         */
        protected AtomicLong version = new AtomicLong();

        public ClusterListener(URL serviceUrl, ClusterHandler handler) {
            this.serviceUrl = serviceUrl;
            this.handler = handler;
        }

        @Override
        public void entryAdded(EntryEvent entryEvent) {
            onUpdateEvent((URL) entryEvent.getValue(), ShardEventType.ADD);
        }

        @Override
        public void entryExpired(EntryEvent entryEvent) {
            onUpdateEvent((URL) entryEvent.getOldValue(), ShardEventType.DELETE);
        }

        @Override
        public void entryRemoved(EntryEvent entryEvent) {
            onUpdateEvent((URL) entryEvent.getOldValue(), ShardEventType.DELETE);
        }

        @Override
        public void entryUpdated(EntryEvent entryEvent) {
            onUpdateEvent((URL) entryEvent.getValue(), ShardEventType.UPDATE);
        }

        public void onUpdateEvent(URL providerUrl, ShardEventType eventType) {
            List<ShardEvent> shardEvents = new ArrayList<>();
            shardEvents.add(new ShardEvent(new Shard.DefaultShard(providerUrl), eventType));
            handler.handle(new ClusterEvent(BroadCastRegistry.this, null, UPDATE, version.incrementAndGet(), shardEvents));
        }


        @Override
        public void onFullEvent(IMap map) {
            List<ShardEvent> shardEvents = new ArrayList<>();
            Collection<URL> providers = map.values();
            providers.forEach(providerUrl -> {
                shardEvents.add(new ShardEvent(new Shard.DefaultShard(providerUrl), ShardEventType.ADD));
            });
            handler.handle(new ClusterEvent(BroadCastRegistry.this, null, FULL, version.incrementAndGet(), shardEvents));
        }
    }

    /**
     * 配置事件监听
     */
    protected class ConfigListener implements SubscribeListener {

        /**
         * consumer或provider的url
         */
        protected URL serviceUrl;
        /**
         * 配置事件handler
         */
        protected ConfigHandler handler;
        /**
         * 事件版本
         */
        protected AtomicLong version = new AtomicLong();

        public ConfigListener(URL serviceUrl, ConfigHandler handler) {
            this.serviceUrl = serviceUrl;
            this.handler = handler;
        }

        @Override
        public void entryAdded(EntryEvent entryEvent) {
            IMap<String, String> map = instance.getMap(configRootKeyFunc.apply(serviceUrl));
            onFullEvent(map);
        }

        @Override
        public void entryExpired(EntryEvent entryEvent) {
        }

        @Override
        public void entryRemoved(EntryEvent entryEvent) {
            IMap<String, String> map = instance.getMap(configRootKeyFunc.apply(serviceUrl));
            onFullEvent(map);
        }

        @Override
        public void entryUpdated(EntryEvent entryEvent) {
            IMap<String, String> map = instance.getMap(configRootKeyFunc.apply(serviceUrl));
            onFullEvent(map);
        }

        @Override
        public void onFullEvent(IMap map) {
            Map<String, String> datum;
            if (map != null) {
                datum = new HashMap<>(map);
            } else {
                datum = new HashMap<>();
            }
            handler.handle(new ConfigEvent(BroadCastRegistry.this, null, FULL, version.incrementAndGet(), datum));
        }
    }

    /**
     * Hazelcast的异步回调
     *
     * @param <V>
     */
    protected static class AsyncCallback<V> implements ExecutionCallback<V> {

        protected CompletableFuture<Void> future;
        protected Consumer<Throwable> error;

        /**
         * 构造函数
         *
         * @param future
         */
        public AsyncCallback(CompletableFuture<Void> future) {
            this.future = future;
        }

        /**
         * 构造函数
         *
         * @param future
         * @param error
         */
        public AsyncCallback(CompletableFuture<Void> future, Consumer<Throwable> error) {
            this.future = future;
            this.error = error;
        }

        @Override
        public void onResponse(V v) {
            future.complete(null);
        }

        @Override
        public void onFailure(Throwable e) {
            if (error != null) {
                error.accept(e);
            }
            future.completeExceptionally(e);
        }
    }

}
