package io.joyrpc.cluster.discovery.registry;

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
import io.joyrpc.cluster.discovery.backup.BackupDatum;
import io.joyrpc.cluster.discovery.backup.BackupShard;
import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.cluster.discovery.naming.ClusterHandler;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.Event;
import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.event.UpdateEvent;
import io.joyrpc.event.UpdateEvent.UpdateType;
import io.joyrpc.extension.URL;
import io.joyrpc.util.*;
import io.joyrpc.util.StateMachine.StateFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.joyrpc.Plugin.EVENT_BUS;
import static io.joyrpc.cluster.event.ClusterEvent.ShardEventType.ADD;
import static io.joyrpc.constants.Constants.*;

/**
 * 注册中心基类，实现Registry接口
 *
 * @date: 23/1/2019
 */
public abstract class AbstractRegistry implements Registry, Configure {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);
    public static final String TYPE = "type";

    /**
     * 注册中心URL
     */
    protected URL url;
    /**
     * 注册中心名称，符合标识符规范
     */
    protected String name;
    /**
     * 数据备份
     */
    protected Backup backup;
    /**
     * 数据中心
     */
    protected String dataCenter;
    /**
     * 地域
     */
    protected String region;
    /**
     * 最大连接重试次数，<0无限重试，=0不重试，>0则表示最大重试次数
     */
    protected int maxConnectRetryTimes;
    /**
     * 注册中心对象id
     */
    protected int registryId;
    /**
     * 任务重试时间间隔
     */
    protected long taskRetryInterval;
    /**
     * 备份时间间隔
     */
    protected long backupInterval;
    /**
     * 注册
     */
    protected final Map<String, Registion> registers = new ConcurrentHashMap<>(30);
    /**
     * 集群订阅
     */
    protected final Set<ClusterSubscription> clusters = new CopyOnWriteArraySet<>();
    /**
     * 配置订阅
     */
    protected final Set<ConfigSubscription> configs = new CopyOnWriteArraySet<>();
    /**
     * 控制器
     */
    protected transient StateMachine<RegistryController<? extends AbstractRegistry>> state = new StateMachine<>(this::create, null);

    /**
     * 构造函数
     *
     * @param url URL
     */
    public AbstractRegistry(URL url) {
        this(null, url, null);
    }

    /**
     * 构造函数
     *
     * @param name 名称
     * @param url  URL
     */
    public AbstractRegistry(String name, URL url) {
        this(name, url, null);
    }

    /**
     * 构造函数
     *
     * @param name   名称
     * @param url    URL
     * @param backup 备份
     */
    public AbstractRegistry(final String name, final URL url, final Backup backup) {
        Objects.requireNonNull(url, "url can not be null.");
        this.name = name == null || name.isEmpty() ? url.getString("name", url.getProtocol()) : name;
        this.url = url;
        this.backup = backup;
        this.maxConnectRetryTimes = url.getInteger(REGISTRY_MAX_CONNECT_RETRY_TIMES_OPTION);
        this.taskRetryInterval = url.getPositiveLong(REGISTRY_TASK_RETRY_INTERVAL_OPTION);
        this.backupInterval = url.getPositiveLong(REGISTRY_BACKUP_INTERVAL_OPTION);
        this.registryId = ID_GENERATOR.get();
    }

    /**
     * 构建控制器
     *
     * @return 新建的控制器
     */
    protected RegistryController<? extends AbstractRegistry> create() {
        return new RegistryController<>(this);
    }

    @Override
    public CompletableFuture<Void> open() {
        return state.open(this::doOpen);
    }

    /**
     * 打开
     */
    protected void doOpen() {
        logger.info("Start connecting to registry " + name);
    }

    @Override
    public CompletableFuture<Void> close() {
        return state.close(false, this::doClose);
    }

    /**
     * 关闭
     */
    protected void doClose() {
        registers.forEach((key, value) -> value.close());
    }

    @Override
    public CompletableFuture<URL> register(final URL url) {
        Objects.requireNonNull(url, "url can not be null.");
        //首次创建
        AtomicBoolean first = new AtomicBoolean(false);
        //获取注册对象
        Registion registion = registers.computeIfAbsent(getRegisterKey(url), key -> {
            first.set(true);
            return createRegistion(url, key);
        });
        //存在相同Key的URL多次注册，需要增加引用计数器，在注销的时候确保没有引用了才去注销
        registion.addRef();
        //判断是否第一次创建
        if (first.get()) {
            //判断当前状态是否打开，如果打开则进行注册
            state.whenOpen(c -> c.register(registion));
        }
        return registion.getFuture().getOpenFuture();
    }

    /**
     * 构建注册对象
     *
     * @param url url
     * @param key 键
     * @return 注册对象
     */
    protected Registion createRegistion(final URL url, final String key) {
        return new Registion(url, key);
    }

    @Override
    public CompletableFuture<URL> deregister(final URL url, final int maxRetryTimes) {
        Objects.requireNonNull(url, "url can not be null.");
        final CompletableFuture<URL>[] results = new CompletableFuture[1];
        registers.compute(getRegisterKey(url), (key, reg) -> {
            if (reg == null || reg.decRef() > 0) {
                results[0] = CompletableFuture.completedFuture(url);
            } else {
                results[0] = reg.getFuture().getCloseFuture();
                if (!state.whenOpen(c -> c.deregister(reg, maxRetryTimes))) {
                    results[0].complete(url);
                }
            }
            return null;
        });
        return results[0];
    }

    /**
     * 订阅
     *
     * @param subscriptions 订阅集合
     * @param subscription  订阅
     * @param consumer      消费者
     * @param <T>           泛型
     * @return 订阅成功标识
     */
    protected <T extends Subscription<?>> boolean subscribe(final Set<T> subscriptions, final T subscription,
                                                            final BiConsumer<RegistryController<?>, T> consumer) {
        if (subscriptions.add(subscription)) {
            state.whenOpen(c -> consumer.accept(c, subscription));
            return true;
        } else {
            return false;
        }
    }

    /**
     * 取消订阅
     *
     * @param subscriptions 订阅集合
     * @param subscription  订阅
     * @param consumer      消费者
     * @param <T>           泛型
     * @return 取消订阅成功标识
     */
    protected <T extends Subscription<?>> boolean unsubscribe(final Set<T> subscriptions, final T subscription,
                                                              final BiConsumer<RegistryController<?>, T> consumer) {
        if (subscriptions.remove(subscription)) {
            state.whenOpen(c -> consumer.accept(c, subscription));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean subscribe(final URL url, final ClusterHandler handler) {
        Objects.requireNonNull(url, "url can not be null.");
        Objects.requireNonNull(handler, "handler can not be null.");
        return subscribe(clusters, new ClusterSubscription(url, getClusterKey(url), handler), RegistryController::subscribe);
    }

    @Override
    public boolean unsubscribe(final URL url, final ClusterHandler handler) {
        Objects.requireNonNull(url, "url can not be null.");
        Objects.requireNonNull(handler, "handler can not be null.");
        return unsubscribe(clusters, new ClusterSubscription(url, getClusterKey(url), handler), RegistryController::unsubscribe);
    }

    @Override
    public boolean subscribe(final URL url, final ConfigHandler handler) {
        Objects.requireNonNull(url, "url can not be null.");
        Objects.requireNonNull(handler, "handler can not be null.");
        return subscribe(configs, new ConfigSubscription(url, getConfigKey(url), handler), RegistryController::subscribe);
    }

    @Override
    public boolean unsubscribe(final URL url, final ConfigHandler handler) {
        Objects.requireNonNull(url, "url can not be null.");
        Objects.requireNonNull(handler, "handler can not be null.");
        return unsubscribe(configs, new ConfigSubscription(url, getConfigKey(url), handler), RegistryController::unsubscribe);
    }

    @Override
    public String getRegion() {
        return (region == null || region.isEmpty()) ? GlobalContext.getString(REGION) : region;
    }

    @Override
    public String getDataCenter() {
        return (dataCenter == null || dataCenter.isEmpty()) ? GlobalContext.getString(DATA_CENTER) : dataCenter;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    /**
     * 获取注册键
     *
     * @param url url
     * @return 键
     */
    protected String getRegisterKey(final URL url) {
        //注册:协议+接口+别名+SIDE,生产者和消费者都需要注册
        return url.toString(false, true, ALIAS_OPTION.getName(), ROLE_OPTION.getName());
    }

    /**
     * 获取集群键，加上类型参数避免和配置键一样
     *
     * @param url url
     * @return 键
     */
    protected String getClusterKey(final URL url) {
        //接口集群订阅:协议+接口+别名+集群类型
        URL u = url.add(TYPE, "cluster");
        return u.toString(false, true, ALIAS_OPTION.getName(), TYPE);
    }

    /**
     * 获取配置键，加上类型参数避免和集群键一样
     *
     * @param url url
     * @return 键
     */
    protected String getConfigKey(final URL url) {
        //分组信息可能在参数里面，可以在子类里面覆盖
        if (StringUtils.isEmpty(url.getPath())) {
            //全局配置订阅
            return GLOBAL_SETTING;
        } else {
            //接口配置订阅:协议+接口+别名+SIDE+配置类型
            URL u = url.add(TYPE, "config");
            return u.toString(false, true, ALIAS_OPTION.getName(), ROLE_OPTION.getName(), TYPE);
        }
    }

    /**
     * 状态
     */
    protected static abstract class StateKey extends URLKey {
        /**
         * 路径
         */
        protected String path;
        /**
         * 状态Future
         */
        protected volatile StateFuture<URL> future = new StateFuture<>();

        /**
         * 构造函数
         *
         * @param url url
         * @param key 键
         */
        public StateKey(final URL url, final String key) {
            super(url, key);
        }

        /**
         * 构造函数
         *
         * @param url  url
         * @param key  键
         * @param path 路径
         */
        public StateKey(URL url, String key, String path) {
            super(url, key);
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public StateFuture<URL> getFuture() {
            return future;
        }

        public String name() {
            return "";
        }
    }

    /**
     * 注册
     */
    protected static class Registion extends StateKey {
        /**
         * 计数器
         */
        protected final AtomicInteger counter = new AtomicInteger(0);

        /**
         * 构造函数
         *
         * @param url URL
         * @param key 键
         */
        public Registion(final URL url, final String key) {
            super(url, key);
        }

        /**
         * 构造函数
         *
         * @param url  url
         * @param key  键
         * @param path 路径
         */
        public Registion(final URL url, final String key, final String path) {
            super(url, key, path);
        }

        /**
         * 增加引用计数器
         *
         * @return 引用计数
         */
        public int addRef() {
            return counter.incrementAndGet();
        }

        /**
         * 减少引用计数器
         *
         * @return 引用计数
         */
        public int decRef() {
            return counter.decrementAndGet();
        }

        @Override
        public String name() {
            return key;
        }

        /**
         * 关闭
         */
        public void close() {
            StateFuture<URL> f = future;
            future = new StateFuture<>();
            f.close();
        }
    }

    /**
     * 订阅
     *
     * @param <T>
     */
    protected static class Subscription<T extends Event> extends URLKey implements EventHandler<T> {
        protected final EventHandler<T> handler;

        /**
         * 构造函数
         *
         * @param url     url
         * @param key     键
         * @param handler 参数
         */
        public Subscription(final URL url, final String key, final EventHandler<T> handler) {
            super(url, key);
            this.handler = handler;
        }

        public EventHandler<T> getHandler() {
            return handler;
        }

        @Override
        public void handle(final T event) {
            handler.handle(event);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            Subscription<?> that = (Subscription<?>) o;

            return handler.equals(that.handler);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + handler.hashCode();
            return result;
        }
    }

    /**
     * 集群订阅
     */
    protected static class ClusterSubscription extends Subscription<ClusterEvent> {

        public ClusterSubscription(URL url, String key, ClusterHandler handler) {
            super(url, key, handler);
        }
    }

    /**
     * 配置订阅
     */
    protected static class ConfigSubscription extends Subscription<ConfigEvent> {

        /**
         * 构造函数
         *
         * @param url     url
         * @param key     键
         * @param handler 处理器
         */
        public ConfigSubscription(URL url, String key, ConfigHandler handler) {
            super(url, key, handler);
        }
    }

    /**
     * 控制器
     */
    protected static class RegistryController<R extends AbstractRegistry> implements StateMachine.Controller {
        /**
         * 注册
         */
        protected R registry;
        /**
         * 注册的Future
         */
        protected Map<String, Registion> registers = new ConcurrentHashMap<>(20);
        /**
         * 集群订阅的Future
         */
        protected final Map<String, ClusterBooking> clusters = new ConcurrentHashMap<>(20);
        /**
         * 配置订阅Future
         */
        protected final Map<String, ConfigBooking> configs = new ConcurrentHashMap<>(20);
        /**
         * 任务队列
         */
        protected final Deque<Task> tasks = new ConcurrentLinkedDeque<>();
        /**
         * 任务派发
         */
        protected Daemon daemon;
        /**
         * 等待
         */
        protected Waiter waiter;
        /**
         * 数据是否做了修改
         */
        protected AtomicBoolean dirty = new AtomicBoolean();
        /**
         * 重连任务
         */
        protected ReconnectTask reconnectTask;
        /**
         * 连接状态
         */
        protected AtomicBoolean connected = new AtomicBoolean(false);
        /**
         * 备份恢复的数据
         */
        protected BackupDatum datum;
        /**
         * 上次备份时间
         */
        protected long lastBackup;

        /**
         * 构造函数
         *
         * @param registry 注册中心对象
         */
        public RegistryController(final R registry) {
            this.registry = registry;
            registry.registers.forEach((k, v) -> register(v));
            registry.clusters.forEach(this::subscribe);
            registry.configs.forEach(this::subscribe);
        }

        @Override
        public CompletableFuture<Void> open() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            waiter = new Waiter.MutexWaiter();
            //任务执行线程
            daemon = Daemon.builder().name("registry-dispatcher").delay(0).fault(1000L)
                    .prepare(this::restore).callable(this::dispatch).waiter(waiter).build();
            daemon.start();
            doOpen(future);
            return future;
        }

        @Override
        public CompletableFuture<Void> close(final boolean gracefully) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            doClose(future);
            return future.handle((u, t) -> {
                if (daemon != null) {
                    daemon.stop();
                }
                return null;
            });
        }

        /**
         * 注册
         *
         * @param registion 注册
         */
        public void register(final Registion registion) {
            if (registers.putIfAbsent(registion.getKey(), registion) == null) {
                addBookingTask(registers, registion, this::doRegister);
            }
        }

        /**
         * 注销
         *
         * @param registion  注册
         * @param maxRetries 最大重试次数
         */
        public void deregister(final Registion registion, final int maxRetries) {
            Registion remove = registers.remove(registion.getKey());
            if (remove != null) {
                addNewTask(new Task("registering " + registion.name(), remove.getUrl(),
                        remove.getFuture().getCloseFuture(), () -> doDeregister(remove),
                        0, 0, maxRetries,
                        t -> (t == null || retry(t)) && !registers.containsKey(remove.getKey())));
            }
        }

        /**
         * 订阅集群
         *
         * @param subscription 订阅
         */
        public void subscribe(final ClusterSubscription subscription) {
            //在锁里面
            subscribe(clusters, subscription, this::createClusterBooking, this::doSubscribe);
        }

        /**
         * 取消订阅集群
         *
         * @param subscription 订阅
         */
        public void unsubscribe(final ClusterSubscription subscription) {
            unsubscribe(clusters, subscription);
        }

        /**
         * 订阅配置
         *
         * @param subscription 订阅
         */
        public void subscribe(final ConfigSubscription subscription) {
            subscribe(configs, subscription, this::createConfigBooking, this::doSubscribe);
        }

        /**
         * 取消订阅配置
         *
         * @param subscription 订阅
         */
        public void unsubscribe(final ConfigSubscription subscription) {
            unsubscribe(configs, subscription);
        }

        /**
         * 订阅
         *
         * @param subscriptions 订阅集合
         * @param subscription  订阅
         * @param creationFunc  构造函数
         * @param doFunc        执行函数
         */
        protected <M extends UpdateEvent<?>, T extends Booking<M>> void subscribe(final Map<String, T> subscriptions,
                                                                                  final Subscription<M> subscription,
                                                                                  final Function<URLKey, T> creationFunc,
                                                                                  final Function<T, CompletableFuture<Void>> doFunc) {
            Maps.computeIfAbsent(subscriptions, subscription.getKey(), key -> creationFunc.apply(subscription), (v, added) -> {
                v.addHandler(subscription.getHandler());
                if (added) {
                    //需要在后面执行，否则addBookingTask太快，还没有被添加到subscriptions就执行了，会被直接丢弃
                    addBookingTask(subscriptions, v, doFunc);
                }
            });
        }

        /**
         * 取消订阅
         *
         * @param subscriptions 订阅集合
         * @param subscription  订阅
         */
        protected <M extends UpdateEvent<?>, T extends Booking<M>> void unsubscribe(final Map<String, T> subscriptions,
                                                                                    final Subscription<M> subscription) {
            subscriptions.computeIfPresent(subscription.getKey(), (k, v) -> {
                //在锁里面，防止在订阅
                final boolean[] flags = new boolean[1];
                v.removeHandler(subscription.getHandler(), key -> flags[0] = true);
                return flags[0] ? null : v;
            });
        }

        /**
         * 是否打开
         *
         * @return 打开标识
         */
        protected boolean isOpen() {
            return registry.state.isOpen(this);
        }

        /**
         * 恢复数据
         */
        protected void restore() {
            if (registry.backup != null) {
                try {
                    datum = registry.backup.restore(registry.name);
                } catch (IOException e) {
                    logger.error(String.format("Error occurs while restoring %s registry datum.", registry.name), e);
                }
            }
        }

        /**
         * 打开，恢复注册和订阅
         */
        protected void doOpen(final CompletableFuture<Void> future) {
            reconnect(future, 0, registry.maxConnectRetryTimes);
        }

        /**
         * 建连，如果失败进行重试
         *
         * @param future     结果
         * @param retryTimes 当前重连次数
         * @param maxRetries 最大重连次数
         */
        protected void reconnect(final CompletableFuture<Void> future, final long retryTimes, final int maxRetries) {
            //建连接
            doConnect().whenComplete((v, t) -> {
                if (!isOpen()) {
                    //断开连接
                    CompletableFuture<Void> f = t == null ? doDisconnect() : CompletableFuture.completedFuture(null);
                    f.whenComplete((c, r) -> future.completeExceptionally(new IllegalStateException("registry is already closed.")));
                } else if (t != null) {
                    //出了异常，尝试重试
                    long count = retryTimes + 1;
                    if (maxRetries < 0 || maxRetries > 0 && count <= maxRetries) {
                        //失败重试，<0无限重试，=0不重试，>0则表示最大重试次数
                        logger.error(String.format("Error occurs while connecting to %s, retry in %d(ms)", registry.name, 1000L));
                        reconnectTask = new ReconnectTask(() -> reconnect(future, count, maxRetries), SystemClock.now() + 1000L);
                    } else {
                        //连接失败
                        future.completeExceptionally(t);
                    }
                } else {
                    logger.info(String.format("Success connecting to %s.", registry.name));
                    //连接成功
                    connected.set(true);
                    waiter.wakeup();
                    //恢复注册
                    recover();
                    future.complete(null);
                }
            });
        }

        /**
         * 获取通知器
         *
         * @param name 名称
         * @return 事件发布器
         */
        protected <T extends Event> Publisher<T> getPublisher(final String name) {
            return EVENT_BUS.get().getPublisher(Registry.class.getSimpleName(), name);
        }

        /**
         * 创建集群元数据
         *
         * @param key key
         * @return 集群元数据
         */
        protected ClusterBooking createClusterBooking(final URLKey key) {
            return new ClusterBooking(key, this::dirty, getPublisher(key.getKey()));
        }

        /**
         * 创建配置元数据
         *
         * @param key key
         * @return 配置元数据
         */
        protected ConfigBooking createConfigBooking(final URLKey key) {
            return new ConfigBooking(key, this::dirty, getPublisher(key.getKey()));
        }

        /**
         * 新任务
         *
         * @param task 任务
         */
        protected void addNewTask(final Task task) {
            //TODO 重试的任务是否应该放在后面？
            if (logger.isDebugEnabled()) {
                logger.debug("add task " + task.getName());
            }
            tasks.offerFirst(task);
            if (waiter != null) {
                waiter.wakeup();
            }
        }

        /**
         * 添加任务
         *
         * @param subscriptions 集合
         * @param subscription  订阅
         * @param function      执行函数
         * @param <T>           泛型
         * @return 异步Future
         */
        protected <T extends StateKey> CompletableFuture<URL> addBookingTask(final Map<String, T> subscriptions,
                                                                             final T subscription,
                                                                             final Function<T, CompletableFuture<Void>> function) {
            CompletableFuture<URL> future = subscription.getFuture().getOpenFuture();
            addNewTask(new Task("subscribing " + subscription.name(), subscription.getUrl(),
                    future, () -> function.apply(subscription),
                    0, 0, -1, r -> subscriptions.containsKey(subscription.getKey())));
            return future;
        }

        /**
         * 异常是否要重试
         *
         * @param throwable 异常
         * @return 重试标识
         */
        protected boolean retry(final Throwable throwable) {
            return true;
        }

        /**
         * 任务调度
         *
         * @return 等到时间
         */
        protected long dispatch() {
            if (!connected.get() && isOpen()) {
                //当前还没有连接上，则判断是否有重连任务
                ReconnectTask task = reconnectTask;
                if (task != null && task.isExpire()) {
                    reconnectTask = null;
                    //重连
                    task.run();
                }
                //等到连接通知
                return 1000L;
            } else {
                long waitTime = execute();
                if (waitTime > 0
                        && registry.backup != null
                        && (SystemClock.now() - lastBackup) > registry.backupInterval
                        && dirty.compareAndSet(true, false)) {
                    //备份数据，通过时间间隔避免网关等大集群频繁备份操作
                    lastBackup = SystemClock.now();
                    backup();
                }
                return waitTime;
            }
        }

        /**
         * 执行任务队列中的任务
         *
         * @return 等到时间
         */
        protected long execute() {
            long waitTime;
            //取到第一个任务
            Task task = tasks.peekFirst();
            if (task != null) {
                //判断是否超时
                waitTime = task.getRetryTime() - SystemClock.now();
            } else {
                //没有任务则等待10秒
                waitTime = 10000L;
            }
            //有任务执行
            if (waitTime <= 0) {
                //有其它线程并发插入头部，pollFirst可能拿到其它对象
                task = tasks.pollFirst();
                //再次判断
                if (task != null) {
                    //判断是否超时
                    waitTime = task.getRetryTime() - SystemClock.now();
                    if (waitTime <= 0) {
                        execute(task);
                    } else {
                        //重新入队
                        tasks.addFirst(task);
                    }
                } else {
                    //没有任务则等待10秒
                    waitTime = 10000L;
                }
            } else if (task != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Wait %d(ms) to execute %s", waitTime, task.getName()));
                }
            }
            return waitTime;
        }

        /**
         * 执行任务
         *
         * @param task 任务
         */
        protected void execute(final Task task) {
            if (!isOpen()) {
                task.completeExceptionally(new IllegalStateException("registry is closed."));
            } else if (!task.test(null)) {
                task.completeExceptionally(new IllegalStateException("url is removed."));
            } else {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Start calling task %s, remain tasks %d", task.getName(), tasks.size()));
                    }
                    //执行任务
                    task.call().whenComplete((v, t) -> {
                        if (!isOpen()) {
                            task.completeExceptionally(new IllegalStateException("registry is closed."));
                        } else if (!task.test(t)) {
                            task.completeExceptionally(t == null ? new IllegalStateException("url is removed.") : t);
                        } else if (t != null) {
                            if (task.getMaxRetries() < 0 || task.getRetry() < task.getMaxRetries()) {
                                //重试
                                task.setRetryTime(SystemClock.now() + registry.taskRetryInterval);
                                task.setRetry(task.getRetry() + 1);
                                tasks.addLast(task);
                            } else {
                                task.completeExceptionally(t);
                            }
                        } else {
                            task.complete();
                        }
                    });
                } catch (Throwable e) {
                    //执行出错，则重试
                    logger.error("Error occurs while executing registry task,caused by " + e.getMessage(), e);
                    if (!isOpen()) {
                        task.completeExceptionally(new IllegalStateException("registry is closed."));
                    } else if (!task.test(e)) {
                        task.completeExceptionally(e);
                    } else if (task.getMaxRetries() < 0 || task.getRetry() < task.getMaxRetries()) {
                        //重试
                        task.setRetryTime(SystemClock.now() + registry.taskRetryInterval);
                        task.setRetry(task.getRetry() + 1);
                        tasks.addLast(task);
                    } else {
                        task.completeExceptionally(e);
                    }
                }
            }
        }


        /**
         * 用于断开重连恢复注册和订阅
         *
         * @return 恢复的Future
         */
        protected CompletableFuture<Void> recover() {
            List<CompletableFuture<URL>> futures = new LinkedList<>();
            registers.forEach((k, v) -> futures.add(addBookingTask(registers, v, this::doRegister)));
            clusters.forEach((k, v) -> futures.add(addBookingTask(clusters, v, this::doSubscribe)));
            configs.forEach((k, v) -> futures.add(addBookingTask(configs, v, this::doSubscribe)));
            return futures.isEmpty() ? CompletableFuture.completedFuture(null) :
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }

        /**
         * 关闭
         */
        protected void doClose(final CompletableFuture<Void> future) {
            deregister().handle((v, t) -> {
                //异步调用，判断这个时候处于关闭状态
                doDisconnect().handle((c, r) -> {
                    if (r != null) {
                        future.completeExceptionally(r);
                    } else {
                        future.complete(c);
                    }
                    return null;
                });
                return null;
            });
        }

        /**
         * 在关闭的过程中取消注册
         *
         * @return 异步Future
         */
        protected CompletableFuture<Void> deregister() {
            List<CompletableFuture<URL>> futures = new LinkedList<>();
            unsubscribe(futures, registers, this::doDeregister);
            unsubscribe(futures, clusters, this::doUnsubscribe);
            unsubscribe(futures, configs, this::doUnsubscribe);
            return futures.isEmpty() ? CompletableFuture.completedFuture(null) :
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }

        /**
         * 取消订阅
         *
         * @param futures       Future集合
         * @param subscriptions 订阅
         * @param function      消费者
         */
        protected <T extends StateKey> void unsubscribe(final List<CompletableFuture<URL>> futures,
                                                        final Map<String, T> subscriptions,
                                                        final Function<T, CompletableFuture<Void>> function) {
            subscriptions.forEach((k, v) -> {
                CompletableFuture<URL> openFuture = v.getFuture().getOpenFuture();
                CompletableFuture<URL> closeFuture = v.getFuture().getCloseFuture();
                //判断是否订阅成功过
                if ((openFuture.isDone() || !openFuture.completeExceptionally(new IllegalStateException()))
                        && !openFuture.isCompletedExceptionally()
                        && !openFuture.isCancelled()) {
                    addNewTask(new Task("unsubsribing " + v.name(), v.getUrl(), closeFuture,
                            () -> function.apply(v), 0, 0, 0, null));
                } else {
                    closeFuture.complete(v.getUrl());
                }
                futures.add(closeFuture);
            });
        }

        /**
         * 连接
         *
         * @return 异步Future
         */
        protected CompletableFuture<Void> doConnect() {
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 关闭连接
         *
         * @return 异步Future
         */
        protected CompletableFuture<Void> doDisconnect() {
            connected.set(false);
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 注册
         *
         * @param registion 注册
         * @return 异步Future
         */
        protected CompletableFuture<Void> doRegister(final Registion registion) {
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 注销
         *
         * @param registion 注册
         * @return 异步Future
         */
        protected CompletableFuture<Void> doDeregister(final Registion registion) {
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 集群订阅
         *
         * @param booking booking
         * @return 异步Future
         */
        protected CompletableFuture<Void> doSubscribe(final ClusterBooking booking) {
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 注销集群订阅
         *
         * @param booking booking
         * @return 异步Future
         */
        protected CompletableFuture<Void> doUnsubscribe(final ClusterBooking booking) {
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 配置订阅操作
         *
         * @param booking booking
         * @return 异步Future
         */
        protected CompletableFuture<Void> doSubscribe(final ConfigBooking booking) {
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 取消配置订阅操作
         *
         * @param booking booking
         * @return 异步Future
         */
        protected CompletableFuture<Void> doUnsubscribe(final ConfigBooking booking) {
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 数据更新标识，唤醒等待线程进行备份
         */
        protected void dirty() {
            if (registry.backup != null) {
                dirty.set(true);
                if (waiter != null) {
                    waiter.wakeup();
                }
            }
        }

        /**
         * 备份数据
         */
        protected void backup() {
            if (registry.backup != null) {
                try {
                    BackupDatum datum = new BackupDatum();
                    //备份集群数据
                    Map<String, List<BackupShard>> backupClusters = new HashMap<>(clusters.size());
                    clusters.forEach((k, v) -> {
                        if (v.persistable()) {
                            List<BackupShard> backupShards = new LinkedList<>();
                            v.datum.forEach((name, shard) -> backupShards.add(new BackupShard(shard)));
                            backupClusters.put(k, backupShards);
                        }
                    });
                    datum.setClusters(backupClusters);
                    //备份配置数据
                    Map<String, Map<String, String>> backupConfigs = new HashMap<>(configs.size());
                    configs.forEach((k, v) -> {
                        if (v.persistable()) {
                            backupConfigs.put(k, v.datum);
                        }
                    });
                    datum.setConfigs(backupConfigs);
                    //备份到backup
                    registry.backup.backup(registry.name, datum);
                } catch (IOException e) {
                    logger.error(String.format("Error occurs while backuping %s registry datum.", registry.name), e);
                }
            }
        }
    }

    /**
     * 重试任务
     */
    protected static class Task implements Callable<CompletableFuture<Void>> {
        /**
         * 名称
         */
        protected String name;
        /**
         * URL
         */
        protected URL url;
        /**
         * Future
         */
        protected CompletableFuture<URL> future;
        /**
         * 执行
         */
        protected Callable<CompletableFuture<Void>> callable;
        /**
         * 下次重试时间
         */
        protected long retryTime;
        /**
         * 重试次数
         */
        protected int retry;
        /**
         * 最大重试次数
         */
        protected int maxRetries;
        /**
         * 判断是否要重试
         */
        protected Predicate<Throwable> predicate;

        /**
         * 构造函数
         *
         * @param name       名称
         * @param url        url
         * @param future     future
         * @param callable   执行代码
         * @param retryTime  重试时间
         * @param retry      重试次数
         * @param maxRetries 最大重试次数<br/>
         *                   <li>>0 最大重试次数</li>
         *                   <li>=0 不重试</li>
         *                   <li><0 永久重试</li>
         * @param predicate  异常断言
         */
        public Task(String name, URL url, CompletableFuture<URL> future, Callable<CompletableFuture<Void>> callable, long retryTime,
                    int retry, int maxRetries, Predicate<Throwable> predicate) {
            this.name = name;
            this.url = url;
            this.future = future;
            this.callable = callable;
            this.retryTime = retryTime;
            this.retry = retry;
            this.maxRetries = maxRetries;
            this.predicate = predicate;
        }

        public String getName() {
            return name;
        }

        public long getRetryTime() {
            return retryTime;
        }

        public void setRetryTime(long retryTime) {
            this.retryTime = retryTime;
        }

        public int getRetry() {
            return retry;
        }

        public void setRetry(int retry) {
            this.retry = retry;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public boolean test(Throwable throwable) {
            return predicate == null || predicate.test(throwable);
        }

        @Override
        public CompletableFuture<Void> call() throws Exception {
            return callable.call();
        }

        /**
         * 完成
         */
        public void complete() {
            future.complete(url);
        }

        public void completeExceptionally(Throwable e) {
            future.completeExceptionally(e);
        }
    }

    /**
     * 重连任务
     */
    protected static class ReconnectTask implements Runnable {
        /**
         * 结果
         */
        protected Runnable runnable;
        /**
         * 下次重试时间
         */
        protected long retryTime;

        public ReconnectTask(final Runnable runnable, final long retryTime) {
            this.runnable = runnable;
            this.retryTime = retryTime;
        }

        @Override
        public void run() {
            runnable.run();
        }

        /**
         * 是否过期
         *
         * @return 过期标识
         */
        public boolean isExpire() {
            return retryTime <= SystemClock.now();
        }
    }

    /**
     * 内部订阅
     *
     * @param <T>
     */
    protected static abstract class Booking<T extends UpdateEvent<?>> extends StateKey implements EventHandler<T>, Closeable {
        /**
         * 当前数据版本，-1表示还没有初始化
         */
        protected long version = -1;
        /**
         * 全量数据
         */
        protected volatile boolean full;
        /**
         * 通知器
         */
        protected Publisher<T> publisher;
        /**
         * 最后一次事件处理的时间
         */
        protected long lastEventTime = SystemClock.now();
        /**
         * 当数据更新后的处理器
         */
        protected Runnable dirty;

        /**
         * 构造函数
         *
         * @param key       key
         * @param dirty     脏数据处理器
         * @param publisher 事件发布者
         */
        public Booking(final URLKey key, final Runnable dirty, final Publisher<T> publisher) {
            this(key, dirty, publisher, null);
        }

        /**
         * 构造函数
         *
         * @param key       键
         * @param dirty     脏函数
         * @param publisher 事件发布器
         * @param path      路径
         */
        public Booking(final URLKey key, Runnable dirty, Publisher<T> publisher, String path) {
            super(key.getUrl(), key.getKey(), path);
            this.dirty = dirty;
            this.publisher = publisher;
            this.publisher.start();
        }

        public Publisher<T> getPublisher() {
            return publisher;
        }

        public long getVersion() {
            return version;
        }

        public void setVersion(long version) {
            this.version = version;
        }

        public long getLastEventTime() {
            return lastEventTime;
        }

        public void setLastEventTime(long lastEventTime) {
            this.lastEventTime = lastEventTime;
        }

        public boolean isFull() {
            return full;
        }

        /**
         * 可持久化的
         *
         * @return 可持久化标识
         */
        public boolean persistable() {
            return false;
        }

        /**
         * 是否准备好
         *
         * @return 准备好标识
         */
        protected boolean ready() {
            return true;
        }

        /**
         * 添加监听器
         *
         * @param handler 处理器
         * @return 成功标识
         */
        public boolean addHandler(final EventHandler<T> handler) {
            if (publisher.addHandler(handler)) {
                //有全量数据
                if (full && ready()) {
                    publisher.offer(createFullEvent(handler));
                }
                return true;
            }
            return false;
        }

        /**
         * 删除监听器
         *
         * @param handler 处理器
         * @param cleaner 清理
         * @return 成功标识
         */
        public boolean removeHandler(final EventHandler<T> handler, final Consumer<String> cleaner) {
            if (publisher.removeHandler(handler)) {
                if (publisher.size() == 0) {
                    cleaner.accept(key);
                }
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            publisher.close();
        }

        /**
         * 创建全量事件
         *
         * @param handler 事件
         * @return 全量事件
         */
        protected abstract T createFullEvent(EventHandler<T> handler);

        /**
         * 脏数据处理
         */
        protected void dirty() {
            if (dirty != null) {
                dirty.run();
            }
        }

    }

    /**
     * 内部集群订阅
     */
    protected static class ClusterBooking extends Booking<ClusterEvent> implements ClusterHandler {
        /**
         * 分片信息
         */
        protected volatile Map<String, Shard> datum;
        /**
         * 没有全量数据的时候，合并的增量信息
         */
        protected Map<String, ClusterEvent.ShardEvent> events;

        /**
         * 构造函数
         *
         * @param key       key
         * @param dirty     脏数据处理器
         * @param publisher 事件发布者
         */
        public ClusterBooking(final URLKey key, final Runnable dirty, final Publisher<ClusterEvent> publisher) {
            super(key, dirty, publisher);
        }

        /**
         * 构造函数
         *
         * @param key       key
         * @param dirty     脏数据处理器
         * @param publisher 事件发布者
         * @param path      路径
         */
        public ClusterBooking(final URLKey key, final Runnable dirty, final Publisher<ClusterEvent> publisher, final String path) {
            super(key, dirty, publisher, path);
        }

        @Override
        public boolean persistable() {
            return full && datum != null && !datum.isEmpty();
        }

        @Override
        public String name() {
            return "cluster " + key;
        }

        @Override
        protected ClusterEvent createFullEvent(final EventHandler<ClusterEvent> handler) {
            return new ClusterEvent(this, handler, UpdateType.FULL, version, full());
        }

        /**
         * 构造全量事件
         *
         * @return 全量数据
         */
        protected List<ClusterEvent.ShardEvent> full() {
            List<ClusterEvent.ShardEvent> result = new ArrayList<>(datum.size());
            datum.forEach((k, v) -> result.add(new ClusterEvent.ShardEvent(v, ADD)));
            return result;
        }

        /**
         * 更新集群数据
         *
         * @param cluster          集群
         * @param events           事件
         * @param protectNullDatum 是否保护空数据
         */
        protected void update(final Map<String, Shard> cluster, final Collection<ClusterEvent.ShardEvent> events,
                              final boolean protectNullDatum) {
            if (events != null) {
                Shard shard;
                for (ClusterEvent.ShardEvent e : events) {
                    shard = e.getShard();
                    switch (e.getType()) {
                        case UPDATE:
                        case ADD:
                            cluster.put(shard.getName(), shard);
                            break;
                        case DELETE:
                            if (cluster.size() > 1 || !protectNullDatum) {
                                cluster.remove(shard.getName(), shard);
                            }
                            break;
                    }
                }
            }
        }

        /**
         * 合并增量事件
         *
         * @param events 事件
         * @param shards 分片
         * @return 合并后的事件
         */
        protected Map<String, ClusterEvent.ShardEvent> update(final Map<String, ClusterEvent.ShardEvent> events,
                                                              final List<ClusterEvent.ShardEvent> shards) {
            Map<String, ClusterEvent.ShardEvent> result = events;
            if (result == null) {
                result = new HashMap<>();
            }
            if (shards != null) {
                for (ClusterEvent.ShardEvent event : shards) {
                    result.put(event.getShard().getName(), event);
                }
            }
            return result;
        }

        @Override
        public void handle(final ClusterEvent event) {
            lastEventTime = SystemClock.now();
            event.getType().update(url, (fullDatum, protectNullDatum) -> {
                if (!full && !fullDatum) {
                    //没有全量数据
                    if (event.getVersion() > version) {
                        //合并最新的增量数据
                        events = update(events, event.getDatum());
                        version = event.getVersion();
                    }
                    return;
                } else if (full && version >= event.getVersion()) {
                    //有全量数据了，丢弃过期数据
                    return;
                }
                //如果是增量数据，则复制一份原来的数据
                Map<String, Shard> cluster = !fullDatum && datum != null ? new HashMap<>(datum) : new HashMap<>();
                //更新，设置最新集群数据
                update(cluster, event.getDatum(), protectNullDatum);
                if (full && cluster.isEmpty() && protectNullDatum) {
                    //有全量数据了，最新集群数据为空，且空保护，不更新
                    logger.warn("the datum of cluster event can not be null, version is " + event.getVersion());
                    //设置版本
                    version = Math.max(version, event.getVersion());
                } else {
                    if (fullDatum && !full && events != null) {
                        //当前数据是全量数据，以前有增量数据
                        if (version > event.getVersion()) {
                            //全量数据版本更老，则合并
                            update(cluster, events.values(), protectNullDatum);
                        }
                        events = null;
                    }
                    boolean old = full;
                    datum = cluster;
                    version = Math.max(version, event.getVersion());
                    if (fullDatum && !full) {
                        //设置全量数据，确保前面datum已经设置，防止并发线程读取
                        full = true;
                    }
                    //如果存在全量数据，通知事件
                    if (full) {
                        if (event.getType() == UpdateType.CLEAR) {
                            publisher.offer(new ClusterEvent(this, null, UpdateType.CLEAR, version, event.getDatum()));
                        } else if (!old) {
                            //如果以前不是全量数据，收到了全量数据事件，则广播合并完的全量数据
                            publisher.offer(new ClusterEvent(this, null, UpdateType.FULL, version, full()));
                        } else {
                            //以前是全量数据，则直接广播本次更新数据
                            publisher.offer(new ClusterEvent(this, null, event.getType(), version, event.getDatum()));
                        }
                        //保存数据
                        dirty();
                    }
                }
            });
        }
    }

    /**
     * 内部配置订阅，确保先通知完整数据，再通知增量数据
     */
    protected static class ConfigBooking extends Booking<ConfigEvent> implements ConfigHandler {
        /**
         * 全量配置信息
         */
        protected Map<String, String> datum;

        /**
         * 构造函数
         *
         * @param key       key
         * @param dirty     脏数据处理器
         * @param publisher 事件发布者
         */
        public ConfigBooking(final URLKey key, final Runnable dirty, final Publisher<ConfigEvent> publisher) {
            super(key, dirty, publisher);
        }

        /**
         * 构造函数
         *
         * @param key       key
         * @param dirty     脏数据处理器
         * @param publisher 事件发布者
         * @param path      路径
         */
        public ConfigBooking(URLKey key, Runnable dirty, Publisher<ConfigEvent> publisher, String path) {
            super(key, dirty, publisher, path);
        }

        @Override
        public boolean persistable() {
            return full && datum != null;
        }

        @Override
        public String name() {
            return "config " + key;
        }

        @Override
        protected ConfigEvent createFullEvent(final EventHandler<ConfigEvent> handler) {
            return new ConfigEvent(this, handler, version, datum);
        }

        @Override
        public void handle(final ConfigEvent event) {
            lastEventTime = SystemClock.now();
            //都是全量数据更新
            if (datum == null || event.getVersion() > version) {
                datum = event.getDatum() == null ? new HashMap<>() : event.getDatum();
                version = event.getVersion();
                full = true;
                //是否准备好，适用于多个配置来源合并通知
                if (ready()) {
                    //判断是全量数据初始化还是增量更新
                    publisher.offer(new ConfigEvent(this, null, version, datum));
                }
                //保存数据
                dirty();
            }
        }

    }

}
