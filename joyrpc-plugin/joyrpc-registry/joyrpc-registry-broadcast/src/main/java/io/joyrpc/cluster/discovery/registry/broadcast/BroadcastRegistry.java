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
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.discovery.registry.URLKey;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEventType;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.Publisher;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.util.Futures;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static io.joyrpc.Plugin.ENVIRONMENT;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.event.UpdateEvent.UpdateType.FULL;
import static io.joyrpc.event.UpdateEvent.UpdateType.UPDATE;
import static io.joyrpc.util.Timer.timer;

/**
 * hazelcast注册中心实现
 */
public class BroadcastRegistry extends AbstractRegistry {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastRegistry.class);

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
     * 构造函数
     *
     * @param name   名称
     * @param url    url
     * @param backup 备份
     */
    public BroadcastRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        this.cfg = new Config();
        int cpus = ENVIRONMENT.get().cpuCores() * 2;
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
    }

    @Override
    protected Registion createRegistion(final URL url, final String key) {
        return new BroadcastRegistion(url, key, serviceRootKeyFunc.apply(url), serviceNodeKeyFunc.apply(url));
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new BroadcastController(this);
    }

    /**
     * 广播控制器
     */
    protected static class BroadcastController extends RegistryController<BroadcastRegistry> {
        /**
         * hazelcast实例
         */
        protected volatile HazelcastInstance instance;
        /**
         * 续约间隔
         */
        protected long leaseInterval;
        /**
         * 续约任务名称
         */
        protected String leaseTaskName;

        /**
         * 构造函数
         *
         * @param registry 注册中心
         */
        public BroadcastController(final BroadcastRegistry registry) {
            super(registry);
            this.leaseInterval = Math.max(15000, registry.nodeExpiredTime / 3);
            this.leaseTaskName = "Lease-" + registry.registryId;
        }

        @Override
        protected ClusterBooking createClusterBooking(final URLKey key) {
            return new BroadcastClusterBooking(key, this::dirty, getPublisher(key.getKey()), registry.providersRootKeyFunc.apply(key.getUrl()));
        }

        @Override
        protected ConfigBooking createConfigBooking(final URLKey key) {
            return new BroadcastConfigBooking(key, this::dirty, getPublisher(key.getKey()), registry.configRootKeyFunc.apply(key.getUrl()));
        }

        @Override
        protected CompletableFuture<Void> doConnect() {
            return Futures.call(future -> {
                instance = Hazelcast.newHazelcastInstance(registry.cfg);
                future.complete(null);
            });
        }

        /**
         * 续约
         *
         * @param registion 注册
         */
        protected void lease(final BroadcastRegistion registion) {
            if (isOpen() && registers.containsKey(registion.getKey())) {
                try {
                    URL url = registion.getUrl();
                    IMap<String, URL> map = instance.getMap(registion.getPath());
                    //计算新的ttl，并续期
                    long newTtl = SystemClock.now() - registion.getRegisterTime() + registry.nodeExpiredTime;
                    boolean isNotExpired = map.setTtl(registion.getNode(), newTtl, TimeUnit.MILLISECONDS);
                    //节点已经过期，重新添加回节点，并修改meta的registerTime
                    if (!isNotExpired) {
                        map.putAsync(registion.getNode(), url, registry.nodeExpiredTime, TimeUnit.MILLISECONDS).andThen(new ExecutionCallback<URL>() {
                            @Override
                            public void onResponse(final URL response) {
                                registion.setRegisterTime(SystemClock.now());
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                if (!(t instanceof HazelcastInstanceNotActiveException)) {
                                    logger.error("Error occurs while leasing registion " + registion.getKey(), t);
                                }
                            }
                        });
                    }
                } catch (HazelcastInstanceNotActiveException e) {
                    logger.error("Error occurs while leasing registion " + registion.getKey() + ", caused by " + e.getMessage());
                } catch (Exception e) {
                    logger.error("Error occurs while leasing registion " + registion.getKey(), e);
                } finally {
                    if (isOpen() && registers.containsKey(registion.getKey())) {
                        timer().add(new Timer.DelegateTask(leaseTaskName, SystemClock.now() + registion.getLeaseInterval(), () -> lease(registion)));
                    }
                }
            }
        }

        @Override
        protected CompletableFuture<Void> doDisconnect() {
            if (instance != null) {
                instance.shutdown();
            }
            return super.doDisconnect();
        }

        @Override
        protected CompletableFuture<Void> doRegister(final Registion registion) {
            return Futures.call(future -> {
                BroadcastRegistion broadcastRegistion = (BroadcastRegistion) registion;
                IMap<String, URL> iMap = instance.getMap(broadcastRegistion.getPath());
                iMap.putAsync(broadcastRegistion.getNode(), registion.getUrl(), registry.nodeExpiredTime, TimeUnit.MILLISECONDS).andThen(new ExecutionCallback<URL>() {
                    @Override
                    public void onResponse(final URL response) {
                        if (!isOpen()) {
                            future.completeExceptionally(new IllegalStateException("controller is closed."));
                        } else {
                            long interval = ThreadLocalRandom.current().nextLong(registry.nodeExpiredTime / 3, registry.nodeExpiredTime * 2 / 5);
                            interval = Math.max(15000, interval);
                            broadcastRegistion.setRegisterTime(SystemClock.now());
                            broadcastRegistion.setLeaseInterval(interval);
                            if (isOpen()) {
                                timer().add(new Timer.DelegateTask(leaseTaskName, SystemClock.now() + interval, () -> lease(broadcastRegistion)));
                            }
                            future.complete(null);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable e) {
                        if (isOpen()) {
                            logger.error(String.format("Error occurs while do register of %s, caused by %s", registion.getKey(), e.getMessage()));
                        }
                        future.completeExceptionally(e);
                    }
                });
            });
        }

        @Override
        protected CompletableFuture<Void> doDeregister(final Registion registion) {
            return Futures.call(future -> {
                String name = registry.serviceRootKeyFunc.apply(registion.getUrl());
                String key = registry.serviceNodeKeyFunc.apply(registion.getUrl());
                IMap<String, URL> iMap = instance.getMap(name);
                iMap.removeAsync(key).andThen(new ExecutionCallback<URL>() {
                    @Override
                    public void onResponse(final URL response) {
                        future.complete(null);
                    }

                    @Override
                    public void onFailure(final Throwable e) {
                        future.completeExceptionally(e);
                    }
                });
            });
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(final ClusterBooking booking) {
            return Futures.call(new Futures.Executor<Void>() {
                @Override
                public void execute(final CompletableFuture<Void> future) {
                    BroadcastClusterBooking broadcastBooking = (BroadcastClusterBooking) booking;
                    IMap<String, URL> map = instance.getMap(broadcastBooking.getPath());
                    List<ShardEvent> events = new LinkedList<>();
                    map.values().forEach(url -> events.add(new ShardEvent(new Shard.DefaultShard(url), ShardEventType.ADD)));
                    broadcastBooking.setListenerId(map.addEntryListener(broadcastBooking, true));
                    future.complete(null);
                    booking.handle(new ClusterEvent(booking, null, FULL, broadcastBooking.getStat().incrementAndGet(), events));
                }

                @Override
                public void onException(final Exception e) {
                    if (e instanceof HazelcastInstanceNotActiveException) {
                        logger.error(String.format("Error occurs while subscribe of %s, caused by %s", booking.getKey(), e.getMessage()));
                    } else {
                        logger.error(String.format("Error occurs while subscribe of %s, caused by %s", booking.getKey(), e.getMessage()), e);
                    }
                }
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ClusterBooking booking) {
            return Futures.call(future -> {
                BroadcastClusterBooking broadcastBooking = (BroadcastClusterBooking) booking;
                String listenerId = broadcastBooking.getListenerId();
                if (listenerId != null) {
                    IMap<String, URL> map = instance.getMap(broadcastBooking.getPath());
                    map.removeEntryListener(listenerId);
                }
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(ConfigBooking booking) {
            return Futures.call(new Futures.Executor<Void>() {
                @Override
                public void execute(final CompletableFuture<Void> future) {
                    BroadcastConfigBooking broadcastBooking = (BroadcastConfigBooking) booking;
                    IMap<String, String> imap = instance.getMap(broadcastBooking.getPath());
                    Map<String, String> map = new HashMap<>(imap);
                    broadcastBooking.setListenerId(imap.addEntryListener(broadcastBooking, true));
                    future.complete(null);
                    booking.handle(new ConfigEvent(booking, null, broadcastBooking.getStat().incrementAndGet(), map));
                }

                @Override
                public void onException(final Exception e) {
                    if (e instanceof HazelcastInstanceNotActiveException) {
                        logger.error(String.format("Error occurs while subscribe of %s, caused by %s", booking.getKey(), e.getMessage()));
                    } else {
                        logger.error(String.format("Error occurs while subscribe of %s, caused by %s", booking.getKey(), e.getMessage()), e);
                    }
                }
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ConfigBooking booking) {
            return Futures.call(future -> {
                BroadcastConfigBooking broadcastBooking = (BroadcastConfigBooking) booking;
                String listenerId = broadcastBooking.getListenerId();
                if (listenerId != null) {
                    IMap<String, URL> map = instance.getMap(broadcastBooking.getPath());
                    map.removeEntryListener(listenerId);
                }
                future.complete(null);
            });
        }
    }

    /**
     * 广播注册
     */
    protected static class BroadcastRegistion extends Registion {

        /**
         * 注册时间
         */
        protected long registerTime;
        /**
         * 续约时间间隔
         */
        protected long leaseInterval;
        /**
         * 节点
         */
        protected String node;

        public BroadcastRegistion(final URL url, final String key, final String path, final String node) {
            super(url, key, path);
            this.node = node;
        }

        @Override
        public void close() {
            registerTime = 0;
            super.close();
        }

        public long getRegisterTime() {
            return registerTime;
        }

        public void setRegisterTime(long registerTime) {
            this.registerTime = registerTime;
        }

        public long getLeaseInterval() {
            return leaseInterval;
        }

        public void setLeaseInterval(long leaseInterval) {
            this.leaseInterval = leaseInterval;
        }

        public String getNode() {
            return node;
        }
    }

    /**
     * 广播集群订阅
     */
    protected static class BroadcastClusterBooking extends ClusterBooking implements EntryAddedListener<String, URL>,
            EntryUpdatedListener<String, URL>, EntryRemovedListener<String, URL>, EntryExpiredListener<String, URL> {
        /**
         * 计数器
         */
        protected AtomicLong stat = new AtomicLong(0);
        /**
         * 监听器ID
         */
        protected String listenerId;

        public BroadcastClusterBooking(final URLKey key, final Runnable dirty, final Publisher<ClusterEvent> publisher, final String path) {
            super(key, dirty, publisher, path);
            this.path = path;
        }

        @Override
        public void entryAdded(final EntryEvent<String, URL> event) {
            handle(new ClusterEvent(this, null, UPDATE, stat.incrementAndGet(),
                    Collections.singletonList(new ShardEvent(new Shard.DefaultShard(event.getValue()), ShardEventType.ADD))));
        }

        @Override
        public void entryExpired(final EntryEvent<String, URL> event) {
            handle(new ClusterEvent(this, null, UPDATE, stat.incrementAndGet(),
                    Collections.singletonList(new ShardEvent(new Shard.DefaultShard(event.getOldValue()), ShardEventType.DELETE))));
        }

        @Override
        public void entryRemoved(final EntryEvent<String, URL> event) {
            handle(new ClusterEvent(this, null, UPDATE, stat.incrementAndGet(),
                    Collections.singletonList(new ShardEvent(new Shard.DefaultShard(event.getOldValue()), ShardEventType.DELETE))));
        }

        @Override
        public void entryUpdated(final EntryEvent<String, URL> event) {
            handle(new ClusterEvent(this, null, UPDATE, stat.incrementAndGet(),
                    Collections.singletonList(new ShardEvent(new Shard.DefaultShard(event.getValue()), ShardEventType.UPDATE))));
        }

        public String getListenerId() {
            return listenerId;
        }

        public void setListenerId(String listenerId) {
            this.listenerId = listenerId;
        }

        public AtomicLong getStat() {
            return stat;
        }
    }

    /**
     * 广播配置订阅
     */
    protected static class BroadcastConfigBooking extends ConfigBooking implements EntryAddedListener<String, String>,
            EntryUpdatedListener<String, String>, EntryRemovedListener<String, String>, EntryExpiredListener<String, String> {

        /**
         * 计数器
         */
        protected AtomicLong stat = new AtomicLong(0);
        /**
         * 监听器ID
         */
        protected String listenerId;

        /**
         * 构造函数
         *
         * @param key       key
         * @param dirty     脏函数
         * @param publisher 发布器
         * @param path      配置路径
         */
        public BroadcastConfigBooking(final URLKey key,
                                      final Runnable dirty,
                                      final Publisher<ConfigEvent> publisher,
                                      final String path) {
            super(key, dirty, publisher, path);
        }

        @Override
        public void entryAdded(final EntryEvent<String, String> event) {
            Map<String, String> data = datum == null ? new HashMap<>() : new HashMap<>(datum);
            data.put(event.getKey(), event.getValue());
            handle(new ConfigEvent(this, null, stat.incrementAndGet(), data));
        }

        @Override
        public void entryExpired(final EntryEvent<String, String> event) {
            Map<String, String> data = datum == null ? new HashMap<>() : new HashMap<>(datum);
            data.remove(event.getKey());
            handle(new ConfigEvent(this, null, stat.incrementAndGet(), data));
        }

        @Override
        public void entryRemoved(final EntryEvent<String, String> event) {
            Map<String, String> data = datum == null ? new HashMap<>() : new HashMap<>(datum);
            data.remove(event.getKey());
            handle(new ConfigEvent(this, null, stat.incrementAndGet(), data));
        }

        @Override
        public void entryUpdated(final EntryEvent<String, String> event) {
            Map<String, String> data = datum == null ? new HashMap<>() : new HashMap<>(datum);
            data.put(event.getKey(), event.getValue());
            handle(new ConfigEvent(this, null, stat.incrementAndGet(), data));
        }

        public String getListenerId() {
            return listenerId;
        }

        public void setListenerId(String listenerId) {
            this.listenerId = listenerId;
        }

        public AtomicLong getStat() {
            return stat;
        }

    }

}
