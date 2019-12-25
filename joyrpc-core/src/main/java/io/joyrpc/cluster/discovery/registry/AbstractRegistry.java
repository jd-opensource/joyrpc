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
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.Event;
import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.event.UpdateEvent;
import io.joyrpc.event.UpdateEvent.UpdateType;
import io.joyrpc.extension.URL;
import io.joyrpc.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.joyrpc.Plugin.EVENT_BUS;
import static io.joyrpc.cluster.event.ClusterEvent.ShardEventType.ADD;
import static io.joyrpc.constants.Constants.ALIAS_OPTION;
import static io.joyrpc.constants.Constants.GLOBAL_SETTING;

/**
 * 注册中心基类，实现Registry接口
 *
 * @date: 23/1/2019
 */
public abstract class AbstractRegistry implements Registry, Configure {
    protected static final AtomicReferenceFieldUpdater<AbstractRegistry, Status> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractRegistry.class, Status.class, "status");
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
     * 注册
     */
    protected final Map<String, Registrion> registers = new ConcurrentHashMap<>(30);
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
    protected transient StateMachine<RegistryController> state = new StateMachine<>(this::create, null);

    /**
     * 构造函数
     *
     * @param url
     */
    public AbstractRegistry(URL url) {
        this(null, url, null);
    }

    /**
     * 构造函数
     *
     * @param name
     * @param url
     */
    public AbstractRegistry(String name, URL url) {
        this(name, url, null);
    }

    /**
     * 构造函数
     *
     * @param name
     * @param url
     * @param backup
     */
    public AbstractRegistry(final String name, final URL url, final Backup backup) {
        Objects.requireNonNull(url, "url can not be null.");
        this.name = name == null || name.isEmpty() ? url.getString("name", url.getProtocol()) : name;
        this.url = url;
        this.backup = backup;
        this.maxConnectRetryTimes = url.getInteger("maxConnectRetryTimes", -1);
        this.taskRetryInterval = url.getLong("taskRetryInterval", 500L);
        this.registryId = ID_GENERATOR.get();
    }

    /**
     * 构建控制器
     *
     * @return
     */
    protected RegistryController create() {
        return new RegistryController();
    }

    @Override
    public CompletableFuture<Void> open() {
        return state.open();
    }

    @Override
    public CompletableFuture<Void> close() {
        return state.close(false);
    }

    /**
     * 构造元数据
     *
     * @param url
     * @param keyFunc
     * @param metaFunc
     * @param map
     * @param register
     * @param <T>
     * @return
     */
    protected <T extends URLKey> T getOrCreateMeta(final URL url,
                                                   final Function<URL, String> keyFunc,
                                                   final BiFunction<URL, String, T> metaFunc,
                                                   final Map<String, T> map,
                                                   final Consumer<T> register) {
        String key = keyFunc.apply(url);
        AtomicBoolean firstTimes = new AtomicBoolean(false);
        T meta = map.computeIfAbsent(key, o -> {
            firstTimes.set(true);
            return metaFunc.apply(url, o);
        });
        //此url第一次注册，添加到任务队列
        if (switcher.isOpened() && firstTimes.get()) {
            register.accept(meta);
        }
        return meta;
    }

    @Override
    public CompletableFuture<URL> register(final URL url) {
        Objects.requireNonNull(url, "url can not be null.");
        return registers.computeIfAbsent(getRegisterKey(url), key -> {
            Registrion registrion = new Registrion(url, key);
            //存在相同Key的URL多次注册，需要增加引用计数器，在注销的时候确保没有引用了才去注销
            //TODO 计数器应该放在外面吧
            registrion.counter.incrementAndGet();
            state.whenOpen(c -> c.register(registrion));
            return registrion;
        }).registerFuture;
    }

    @Override
    public CompletableFuture<URL> deregister(final URL url, final int maxRetryTimes) {
        Objects.requireNonNull(url, "url can not be null.");
        CompletableFuture<URL> result = new CompletableFuture<>();
        registers.compute(getRegisterKey(url), (key, registrion) -> {
            if (registrion == null) {
                result.complete(url);
            } else if (registrion.counter.decrementAndGet() <= 0) {
                if (!state.whenOpen(c -> c.deregister(registrion, maxRetryTimes))) {
                    registrion.deregisterFuture.complete(url);
                }
                //TODO 是否应该放在外面，避免触发结果，还没有从Map删除
                registrion.deregisterFuture.whenComplete((v, t) -> result.complete(url));
            } else {
                result.complete(url);
            }
            return null;
        });
    }

    /**
     * 订阅
     *
     * @param subscriptions 订阅集合
     * @param subscription  订阅
     * @param consumer      消费者
     * @param <T>
     * @return 订阅成功标识
     */
    protected <T> boolean subscribe(final Set<T> subscriptions, final T subscription,
                                    final BiConsumer<RegistryController, T> consumer) {
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
     * @param <T>
     * @return 取消订阅成功标识
     */
    protected <T> boolean unsubscribe(final Set<T> subscriptions, final T subscription,
                                      final BiConsumer<RegistryController, T> consumer) {
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
     * 创建集群元数据
     *
     * @param url
     * @param key
     * @return
     */
    protected ClusterMeta createClusterMeta(final URL url, final String key) {
        return new ClusterMeta(url, key, this::dirty, getPublisher(key));
    }

    /**
     * 创建配置元数据
     *
     * @param url
     * @param key
     * @return
     */
    protected ConfigMeta createConfigMeta(final URL url, final String key) {
        return new ConfigMeta(url, key, this::dirty, getPublisher(key));
    }

    /**
     * 新任务
     *
     * @param task
     */
    protected void addNewTask(final Task task) {
        tasks.offerFirst(task);
        if (waiter != null) {
            waiter.wakeup();
        }
    }

    /**
     * 添加订阅任务
     *
     * @param subscribes
     * @param meta
     * @param function
     * @param <T>
     */
    protected <T extends SubscribeMeta> void addSubscribeTask(final Map<String, T> subscribes, final T meta,
                                                              final Function<T, CompletableFuture<Void>> function) {
        addSubscribeTask(subscribes, meta, function, 0);
    }

    /**
     * 添加订阅任务
     *
     * @param subscribes
     * @param meta
     * @param function
     * @param retryTime
     * @param <T>
     */
    protected <T extends SubscribeMeta> void addSubscribeTask(final Map<String, T> subscribes, final T meta,
                                                              final Function<T, CompletableFuture<Void>> function,
                                                              final long retryTime) {
        addNewTask(new Task(meta.getUrl(), meta.getFuture(), () -> {
            //判断订阅是否存在
            if (switcher.isOpened() && subscribes.get(meta.getKey()) == meta) {
                function.apply(meta).whenComplete((v, e) -> {
                    if (e != null) {
                        //异常重试
                        if (switcher.isOpened() && subscribes.get(meta.getKey()) == meta) {
                            addSubscribeTask(subscribes, meta, function, SystemClock.now() + taskRetryInterval);
                        }
                    }
                });
            }
            return true;
        }, retryTime));
    }

    /**
     * 添加注销任务
     *
     * @param meta          注册元数据
     * @param maxRetryTimes 最大重试次数
     */
    protected void addDeregisterTask(final RegisterMeta meta, final int maxRetryTimes) {
        addDeregisterTask(meta, 0, 0, maxRetryTimes);
    }

    /**
     * 添加注销任务
     *
     * @param meta
     * @param retryTime     下次重试时间
     * @param retries       当前重试次数
     * @param maxRetryTimes 最大重试次数
     */
    protected void addDeregisterTask(final RegisterMeta meta, final long retryTime, final int retries, final int maxRetryTimes) {
        addNewTask(new Task(meta.getUrl(), meta.getUnregister(), () -> {
            if (!registers.containsKey(meta.getKey())) {
                doDeregister(meta).whenComplete((k, e) -> {
                    //注销的时候要判断异常
                    if (e != null && retry(e) && switcher.isOpened() && !registers.containsKey(meta.getKey())) {
                        int count = retries + 1;
                        if (count > maxRetryTimes) {
                            meta.getUnregister().completeExceptionally(e);
                            return;
                        }
                        addDeregisterTask(meta, SystemClock.now() + taskRetryInterval, count, maxRetryTimes);
                    }
                });
            }
            return true;
        }, retryTime));
    }

    /**
     * 添加注册任务
     *
     * @param meta
     */
    protected void addRegisterTask(final RegisterMeta meta) {
        addRegisterTask(meta, 0);
    }

    /**
     * 添加注册任务
     *
     * @param meta
     * @param retryTime
     */
    protected void addRegisterTask(final RegisterMeta meta, final long retryTime) {
        addNewTask(new Task(meta.getUrl(), meta.getRegister(), () -> {
            //确保没有被关闭，还存在
            if (switcher.isOpened() && meta == registers.get(meta.getKey())) {
                doRegister(meta).whenComplete((v, e) -> {
                    if (e != null) {
                        if (switcher.isOpened() && registers.get(meta.getKey()) == meta) {
                            addRegisterTask(meta, SystemClock.now() + taskRetryInterval);
                        }
                    } else {
                        meta.setRegisterTime(SystemClock.now());
                    }
                });
            }
            return true;
        }, retryTime));
    }


    /**
     * 用户主动调用取消订阅，异步执行
     *
     * @param subscribes
     * @param key,
     * @param handler
     * @param function
     * @param <T>
     * @param <M>
     * @return
     */
    protected <T extends UpdateEvent, M extends SubscribeMeta<T>> boolean unsubscribe(final Map<String, M> subscribes,
                                                                                      final String key,
                                                                                      final EventHandler<T> handler,
                                                                                      final Function<M, CompletableFuture<Void>> function) {
        return switcher.reader().quietAnyway(() -> {
            //防止关闭
            M meta = subscribes.get(key);
            if (meta != null) {
                return meta.removeHandler(handler, o -> {
                    //没有监听器了，则进行注销
                    Close.close(subscribes.remove(o));
                    CompletableFuture<URL> future = meta.getFuture();
                    //判断是否订阅过
                    if (future.isDone() && !future.isCancelled() && !future.isCompletedExceptionally()) {
                        meta.newFuture();
                        addUnsubscribeTask(meta, function, 0L);
                    }
                });
            }
            return false;
        });
    }

    /**
     * 添加取消订阅任务
     *
     * @param meta
     * @param function
     * @param retryTime
     * @param <T>
     * @param <M>
     */
    protected <T extends UpdateEvent, M extends SubscribeMeta<T>> void addUnsubscribeTask(
            final M meta, final Function<M, CompletableFuture<Void>> function, long retryTime) {
        addNewTask(new Task(meta.getUrl(), meta.getFuture(), () -> {
            function.apply(meta).whenComplete((v, e) -> {
                //取消订阅的时候，需要判断异常
                if (e != null && retry(e) && switcher.isOpened()) {
                    addUnsubscribeTask(meta, function, SystemClock.now() + taskRetryInterval);
                }
            });
            return true;
        }, retryTime));
    }

    /**
     * 异常是否要重试
     *
     * @param throwable
     * @return
     */
    protected boolean retry(Throwable throwable) {
        return true;
    }

    /**
     * 注销
     *
     * @param futures
     */
    protected void register(final List<CompletableFuture<URL>> futures) {
        if (registers.isEmpty()) {
            return;
        }
        RegisterMeta meta;
        for (Map.Entry<String, RegisterMeta> entry : registers.entrySet()) {
            meta = entry.getValue();
            futures.add(meta.newFuture());
            addRegisterTask(meta);
        }
    }

    /**
     * 恢复订阅
     *
     * @param futures
     * @param subscribes
     * @param function
     * @param <M>
     */
    protected <M extends SubscribeMeta> void subscribe(final List<CompletableFuture<URL>> futures,
                                                       final Map<String, M> subscribes,
                                                       final Function<M, CompletableFuture<Void>> function) {
        M meta;
        for (Map.Entry<String, M> entry : subscribes.entrySet()) {
            meta = entry.getValue();
            futures.add(meta.newFuture());
            //添加订阅任务
            addSubscribeTask(subscribes, meta, function);
        }
    }

    /**
     * 获取注册键
     *
     * @param url
     * @return
     */
    protected String getRegisterKey(final URL url) {
        //注册:协议+接口+别名+SIDE,生产者和消费者都需要注册
        return url.toString(false, true, ALIAS_OPTION.getName(), Constants.ROLE_OPTION.getName());
    }

    /**
     * 获取集群键，加上类型参数避免和配置键一样
     *
     * @param url
     * @return
     */
    protected String getClusterKey(final URL url) {
        //接口集群订阅:协议+接口+别名+集群类型
        URL u = url.add(TYPE, "cluster");
        return u.toString(false, true, ALIAS_OPTION.getName(), TYPE);
    }

    /**
     * 获取配置键，加上类型参数避免和集群键一样
     *
     * @param url
     * @return
     */
    protected String getConfigKey(final URL url) {
        //分组信息可能在参数里面，可以在子类里面覆盖
        if (StringUtils.isEmpty(url.getPath())) {
            //全局配置订阅
            return GLOBAL_SETTING;
        } else {
            //接口配置订阅:协议+接口+别名+SIDE+配置类型
            URL u = url.add(TYPE, "config");
            return u.toString(false, true, ALIAS_OPTION.getName(), Constants.ROLE_OPTION.getName(), TYPE);
        }
    }

    /**
     * 获取通知器
     *
     * @param name
     * @return
     */
    protected <T extends Event> Publisher<T> getPublisher(final String name) {
        return EVENT_BUS.get().getPublisher(Registry.class.getSimpleName(), name);
    }

    /**
     * 打开，恢复注册和订阅
     *
     * @return
     */
    protected void doOpen(CompletableFuture<Void> future) {
        reconnect(future, 0, maxConnectRetryTimes);
    }

    /**
     * 任务调度
     *
     * @return
     */
    protected long dispatch() {
        if (!connected.get() && switcher.isOpened()) {
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
            long waitTime = executeTask();
            if (waitTime > 0) {
                if (backup != null && dirty.compareAndSet(true, false)) {
                    //备份数据
                    backup();
                }
            }
            return waitTime;
        }
    }

    /**
     * 执行任务队列中的任务
     *
     * @return
     */
    protected long executeTask() {
        long waitTime;
        //取到第一个任务
        Task task = tasks.peekFirst();
        if (task != null) {
            //判断是否超时
            waitTime = task.retryTime - SystemClock.now();
        } else {
            //没有任务则等待10秒
            waitTime = 10000L;
        }
        //有任务执行
        if (waitTime <= 0) {
            //有其它线程并发插入头部，pollFirst可能拿到其它对象
            task = tasks.pollFirst();
            boolean result;
            try {
                //执行任务
                result = task.call();
            } catch (Exception e) {
                //执行出错，则重试
                logger.error("Error occurs while executing registry task,caused by " + e.getMessage(), e);
                result = false;
            }
            if (!result && switcher.isOpened()) {
                //关闭状态只运行一次，运行状态一致重试
                task.setRetryTime(SystemClock.now() + taskRetryInterval);
                tasks.addLast(task);
            } else {
                task.complete();
            }
        }
        return waitTime;
    }

    /**
     * 建连，如果失败进行重试
     *
     * @param result        结果
     * @param retryTimes    当前重连次数
     * @param maxRetryTimes 最大重连次数
     */
    protected void reconnect(final CompletableFuture<Void> result, final long retryTimes, final int maxRetryTimes) {
        //建连接
        connect().handle((v, t) -> {
            if (!switcher.isOpened()) {
                //断开连接
                disconnect().whenComplete((c, r) -> result.completeExceptionally(new IllegalStateException("registry is already closed.")));
                return null;
            } else if (t != null) {
                //出了异常，尝试重试
                long count = retryTimes + 1;
                if (maxRetryTimes < 0 || maxRetryTimes > 0 && count <= maxRetryTimes) {
                    //失败重试
                    logger.error(String.format("Error occurs while connecting to %s, retry in %d(ms)", url.toString(false, false), 1000L));
                    reconnectTask = new ReconnectTask(result, count, maxRetryTimes, SystemClock.now() + 1000L);
                } else {
                    //连接失败
                    result.completeExceptionally(t);
                }
            } else {
                logger.info(String.format("Success connecting to %s.", url.toString(false, false)));
                //连接成功
                connected.set(true);
                waiter.wakeup();
                //恢复注册
                recover();
                result.complete(null);
            }
            return null;
        });
    }

    /**
     * 连接
     *
     * @return
     */
    protected CompletableFuture<Void> connect() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 用于断开重连恢复注册和订阅
     *
     * @return
     */
    protected CompletableFuture<Void> recover() {
        List<CompletableFuture<URL>> futures = new LinkedList<>();
        register(futures);
        subscribe(futures, clusters, m -> doSubscribe(m, m));
        subscribe(futures, configs, m -> doSubscribe(m, m));
        return futures.isEmpty() ? CompletableFuture.completedFuture(null) :
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    /**
     * 关闭
     *
     * @return
     */
    protected void doClose(CompletableFuture<Void> future) {
        unregister().handle((v, t) -> {
            if (!switcher.isOpened()) {
                //异步调用，判断这个时候处于关闭状态
                disconnect().handle((c, r) -> {
                    if (r != null) {
                        future.completeExceptionally(r);
                    } else {
                        future.complete(c);
                    }
                    return null;
                });
            }
            return null;
        });
    }

    /**
     * 取消注册
     *
     * @return
     */
    protected CompletableFuture<Void> unregister() {
        List<CompletableFuture<URL>> futures = new LinkedList<>();
        unregister(futures);
        unsubscribe(futures, clusters, (u, m) -> doUnsubscribe(u, m));
        unsubscribe(futures, configs, (u, m) -> doUnsubscribe(u, m));
        return futures.isEmpty() ? CompletableFuture.completedFuture(null) :
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    /**
     * 关闭连接
     *
     * @return
     */
    protected CompletableFuture<Void> disconnect() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 注销
     *
     * @param futures
     */
    protected void unregister(final List<CompletableFuture<URL>> futures) {
        if (registers.isEmpty()) {
            return;
        }
        RegisterMeta meta;
        CompletableFuture<URL> future;
        for (Map.Entry<String, RegisterMeta> entry : registers.entrySet()) {
            meta = entry.getValue();
            future = meta.getRegister();
            if (future.isDone() && !future.isCancelled() && !future.isCompletedExceptionally()) {
                //注册成功的进行注销操作
                futures.add(deregister(meta.getUrl()));
            }
        }
    }

    /**
     * 取消订阅
     *
     * @param futures
     */
    protected <T extends SubscribeMeta> void unsubscribe(final List<CompletableFuture<URL>> futures,
                                                         final Map<String, T> subscribes,
                                                         final BiConsumer<URLKey, T> consumer) {
        if (subscribes.isEmpty()) {
            return;
        }
        for (Map.Entry<String, T> entry : subscribes.entrySet()) {
            //添加任务
            T meta = entry.getValue();
            CompletableFuture<URL> future = meta.getFuture();
            //订阅过
            if (future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled()) {
                future = meta.newFuture();
                addNewTask(new Task(meta.getUrl(), future, () -> {
                    //meta实现了URLKey
                    consumer.accept(meta, entry.getValue());
                    return true;
                }));
                futures.add(future);
            }
        }
    }

    /**
     * 注册
     *
     * @param url
     */
    protected CompletableFuture<Void> doRegister(final URLKey url) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 注销
     *
     * @param url
     */
    protected CompletableFuture<Void> doDeregister(final URLKey url) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 集群订阅
     *
     * @param url
     * @param handler
     */
    protected CompletableFuture<Void> doSubscribe(final URLKey url, final ClusterHandler handler) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 注销集群订阅
     *
     * @param url
     * @param handler
     */
    protected CompletableFuture<Void> doUnsubscribe(final URLKey url, final ClusterHandler handler) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 配置订阅操作
     *
     * @param url     consumer/provider url
     * @param handler 配置变更事件handler
     */
    protected CompletableFuture<Void> doSubscribe(final URLKey url, final ConfigHandler handler) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 取消配置订阅操作
     *
     * @param url     consumer/provider url
     * @param handler 配置变更事件handler
     */
    protected CompletableFuture<Void> doUnsubscribe(final URLKey url, final ConfigHandler handler) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 数据更新标识，唤醒等待线程进行备份
     */
    protected void dirty() {
        if (backup != null) {
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
        if (backup != null) {
            try {
                BackupDatum datum = new BackupDatum();
                //备份集群数据
                Map<String, List<BackupShard>> backupClusters = new HashMap<>(this.clusters.size());
                this.clusters.forEach((k, v) -> {
                    if (v.persistable()) {
                        List<BackupShard> backupShards = new LinkedList<>();
                        v.datum.forEach((name, shard) -> backupShards.add(new BackupShard(shard)));
                        backupClusters.put(k, backupShards);
                    }
                });
                datum.setClusters(backupClusters);
                //备份配置数据
                Map<String, Map<String, String>> configs = new HashMap<>(this.configs.size());
                this.configs.forEach((k, v) -> {
                    if (v.persistable()) {
                        configs.put(k, v.datum);
                    }
                });
                datum.setConfigs(configs);
                //备份到backup
                backup.backup(name, datum);
            } catch (IOException e) {
                logger.error(String.format("Error occurs while backuping %s registry datum.", name), e);
            }
        }
    }

    /**
     * 恢复数据
     */
    protected void restore() {
        if (backup != null) {
            try {
                datum = backup.restore(name);
            } catch (IOException e) {
                logger.error(String.format("Error occurs while restoring %s registry datum.", name), e);
            }
        }
    }

    /**
     * 注册
     */
    protected static class Registrion extends URLKey {

        /**
         * 注册Future
         */
        protected final CompletableFuture<URL> registerFuture = new CompletableFuture<>();
        /**
         * 注销Future
         */
        protected final CompletableFuture<URL> deregisterFuture = new CompletableFuture<>();
        /**
         * 计数器
         */
        protected final AtomicLong counter = new AtomicLong(0);

        /**
         * 构造函数
         *
         * @param url
         * @param key
         */
        public Registrion(URL url, String key) {
            super(url, key);
        }

        public CompletableFuture<URL> getRegisterFuture() {
            return registerFuture;
        }

        public CompletableFuture<URL> getDeregisterFuture() {
            return deregisterFuture;
        }
    }

    /**
     * 订阅
     *
     * @param <T>
     */
    protected static class Subscription<T extends Event> extends URLKey {
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

        public ConfigSubscription(URL url, String key, ConfigHandler handler) {
            super(url, key, handler);
        }
    }

    /**
     * 控制器
     */
    protected static class RegistryController implements StateMachine.Controller {
        /**
         * 注册的Future
         */
        protected final Map<String, RegisterMeta> registers = new ConcurrentHashMap<>(20);
        /**
         * 集群订阅的Future
         */
        protected final Map<String, ClusterMeta> clusters = new ConcurrentHashMap<>(20);
        /**
         * 配置订阅Future
         */
        protected final Map<String, ConfigMeta> configs = new ConcurrentHashMap<>(20);
        /**
         * 任务队列
         */
        protected final Deque<Task> tasks = new LinkedBlockingDeque<>();
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

        @Override
        public CompletableFuture<Void> open() {
            waiter = new Waiter.MutexWaiter();
            //任务执行线程
            daemon = Daemon.builder().name("registry-dispatcher").delay(0).fault(1000L)
                    .prepare(this::restore).callable(this::dispatch).waiter(waiter).build();
            daemon.start();
            doOpen(f);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> close(boolean gracefully) {
            doClose(f);
            return f.handle((u, t) -> {
                if (daemon != null) {
                    daemon.stop();
                }
                return null;
            });
            return CompletableFuture.completedFuture(null);
        }

        public void register(final Registrion registrion) {
        }

        public void deregister(final Registrion registrion, final int maxRetryTimes) {
        }

        public void subscribe(final ClusterSubscription subscription) {
        }

        public void unsubscribe(final ClusterSubscription subscription) {
        }

        public void subscribe(final ConfigSubscription subscription) {
        }

        public void unsubscribe(final ConfigSubscription subscription) {
        }
    }

    /**
     * 重试任务
     */
    protected static class Task implements Callable<Boolean> {
        //URL
        protected URL url;
        //Future
        protected CompletableFuture<URL> future;
        //执行
        protected Callable<Boolean> callable;
        //下次重试时间
        protected long retryTime;

        /**
         * 构造函数
         *
         * @param url
         * @param future
         * @param callable
         */
        public Task(URL url, CompletableFuture<URL> future, Callable<Boolean> callable) {
            this(url, future, callable, 0);
        }

        /**
         * 构造函数
         *
         * @param url
         * @param future
         * @param callable
         * @param retryTime
         */
        public Task(URL url, CompletableFuture<URL> future, Callable<Boolean> callable, long retryTime) {
            this.url = url;
            this.future = future;
            this.callable = callable;
            this.retryTime = retryTime;
        }

        public void setRetryTime(long retryTime) {
            this.retryTime = retryTime;
        }

        @Override
        public Boolean call() throws Exception {
            if (callable.call()) {
                future.complete(url);
                return true;
            }
            return false;
        }

        /**
         * 完成
         */
        public void complete() {
            future.complete(url);
        }
    }

    /**
     * 重连任务
     */
    protected class ReconnectTask implements Runnable {
        /**
         * 结果
         */
        protected CompletableFuture<Void> result;
        /**
         * 重试次数
         */
        protected long retryTimes;
        /**
         * 最大重连次数
         */
        protected int maxRetryTimes;
        /**
         * 下次重试时间
         */
        protected long retryTime;

        public ReconnectTask(final CompletableFuture<Void> result, final long retryTimes,
                             final int maxRetryTimes, final long retryTime) {
            this.result = result;
            this.retryTimes = retryTimes;
            this.maxRetryTimes = maxRetryTimes;
            this.retryTime = retryTime;
        }

        @Override
        public void run() {
            reconnect(result, retryTimes, maxRetryTimes);
        }

        /**
         * 是否过期
         *
         * @return
         */
        public boolean isExpire() {
            return retryTime <= SystemClock.now();
        }
    }

    /**
     * 订阅信息
     *
     * @param <T>
     */
    protected static abstract class SubscribeMeta<T extends UpdateEvent> extends URLKey implements EventHandler<T>, Closeable {
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
         * Future
         */
        protected CompletableFuture<URL> future = new CompletableFuture<>();
        /**
         * 最后一次事件处理的时间
         */
        protected long lastEventTime = SystemClock.now();
        /**
         * 回调ID（用于采用回调来订阅的场景）
         */
        protected String callbackId;
        /**
         * 当数据更新后的处理器
         */
        protected Runnable dirty;

        /**
         * 构造函数
         *
         * @param url
         * @param key
         * @param dirty
         * @param publisher
         */
        public SubscribeMeta(final URL url, final String key, final Runnable dirty, final Publisher<T> publisher) {
            super(url, key);
            this.dirty = dirty;
            this.publisher = publisher;
            this.publisher.start();
        }

        public CompletableFuture<URL> getFuture() {
            return future;
        }

        public Publisher<T> getPublisher() {
            return publisher;
        }

        public long getVersion() {
            return version;
        }

        public String getCallbackId() {
            return callbackId;
        }

        public void setCallbackId(String callbackId) {
            this.callbackId = callbackId;
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
         * 新建Future
         *
         * @return
         */
        public CompletableFuture<URL> newFuture() {
            future = new CompletableFuture<>();
            return future;
        }

        /**
         * 添加监听器
         *
         * @param handler
         * @return
         */
        public synchronized boolean addHandler(final EventHandler<T> handler) {
            return publisher.addHandler(handler);
        }

        /**
         * 删除监听器
         *
         * @param handler
         * @param cleaner
         * @return
         */
        public synchronized boolean removeHandler(final EventHandler<T> handler, final Consumer<String> cleaner) {
            if (publisher.removeHandler(handler)) {
                //防止添加
                if (publisher.size() == 0) {
                    cleaner.accept(key);
                }
                return true;
            }
            return false;
        }

        /**
         * 在同步块中执行
         *
         * @param runnable
         */
        public synchronized void run(final Runnable runnable) {
            if (runnable != null) {
                runnable.run();
            }
        }

        @Override
        public void close() {
            publisher.close();
        }

        protected void dirty() {
            if (dirty != null) {
                dirty.run();
            }
        }

    }

    /**
     * 集群订阅信息
     */
    protected static class ClusterMeta extends AbstractRegistry.SubscribeMeta<ClusterEvent> implements ClusterHandler {
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
         * @param url
         * @param key
         * @param dirty
         * @param publisher
         */
        public ClusterMeta(final URL url,
                           final String key,
                           final Runnable dirty,
                           final Publisher<ClusterEvent> publisher) {
            super(url, key, dirty, publisher);
        }

        /**
         * 可持久化的
         *
         * @return
         */
        public boolean persistable() {
            return full && datum != null && !datum.isEmpty();
        }

        /**
         * 添加监听器
         *
         * @param handler
         * @return
         */
        @Override
        public synchronized boolean addHandler(final EventHandler<ClusterEvent> handler) {
            if (publisher.addHandler(handler)) {
                //有全量数据
                if (full) {
                    publisher.offer(new ClusterEvent(this, handler, UpdateType.FULL, version, full()));
                }
                return true;
            }
            return false;
        }

        /**
         * 构造全量事件
         *
         * @return
         */
        protected List<ClusterEvent.ShardEvent> full() {
            List<ClusterEvent.ShardEvent> result = new ArrayList<>(datum.size());
            datum.forEach((k, v) -> result.add(new ClusterEvent.ShardEvent(v, ADD)));
            return result;
        }

        /**
         * 更新集群数据
         *
         * @param cluster
         * @param events
         * @param protectNullDatum
         * @return
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
         * @param events
         * @param shards
         * @return
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
        public synchronized void handle(final ClusterEvent event) {
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
     * 配置订阅信息，确保先通知完整数据，再通知增量数据
     */
    protected static class ConfigMeta extends SubscribeMeta<ConfigEvent> implements ConfigHandler {
        /**
         * 全量配置信息
         */
        protected Map<String, String> datum;

        /**
         * 构造函数
         *
         * @param url
         * @param key
         * @param dirty
         * @param publisher
         */
        public ConfigMeta(final URL url,
                          final String key,
                          final Runnable dirty,
                          final Publisher<ConfigEvent> publisher) {
            super(url, key, dirty, publisher);
        }

        /**
         * 是否准备好
         *
         * @return
         */
        protected boolean ready() {
            return true;
        }

        /**
         * 可持久化的
         *
         * @return
         */
        public boolean persistable() {
            return full && datum != null;
        }

        /**
         * 添加监听器
         *
         * @param handler
         * @return
         */
        @Override
        public synchronized boolean addHandler(final EventHandler<ConfigEvent> handler) {
            if (publisher.addHandler(handler)) {
                //有全量数据，并且前置条件OK
                if (full && ready()) {
                    publisher.offer(new ConfigEvent(this, handler, version, datum));
                }
                return true;
            }
            return false;
        }

        @Override
        public synchronized void handle(final ConfigEvent event) {
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

    /**
     * 注册信息
     */
    protected static class RegisterMeta extends URLKey {
        /**
         * 注册Future
         */
        protected CompletableFuture<URL> register = new CompletableFuture<>();
        /**
         * 注销Future
         */
        protected CompletableFuture<URL> unregister;
        /**
         * 计数器
         */
        protected AtomicLong counter = new AtomicLong(0);
        /**
         * 注册时间
         */
        protected long registerTime;

        public RegisterMeta(URL url, String key) {
            super(url, key);
        }

        public CompletableFuture<URL> getRegister() {
            return register;
        }

        public CompletableFuture<URL> getUnregister() {
            return unregister;
        }

        /**
         * 新建Future
         *
         * @return
         */
        public CompletableFuture<URL> newFuture() {
            register = new CompletableFuture<>();
            unregister = null;
            return register;
        }

        /**
         * 注销
         *
         * @param consumer
         * @return
         */
        public synchronized CompletableFuture<URL> unregister(final Consumer<RegisterMeta> consumer) {
            if (unregister == null) {
                unregister = new CompletableFuture<>();
                if (consumer != null) {
                    consumer.accept(this);
                }
            }
            return unregister;
        }

        public long getRegisterTime() {
            return registerTime;
        }

        public void setRegisterTime(long registerTime) {
            this.registerTime = registerTime;
        }
    }


}
