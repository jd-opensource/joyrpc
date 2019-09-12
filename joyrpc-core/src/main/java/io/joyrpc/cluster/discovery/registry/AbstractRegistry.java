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
import io.joyrpc.constants.Version;
import io.joyrpc.context.Environment;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.Event;
import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.event.UpdateEvent;
import io.joyrpc.extension.URL;
import io.joyrpc.util.Close;
import io.joyrpc.util.StringUtils;
import io.joyrpc.util.Switcher;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.joyrpc.Plugin.EVENT_BUS;
import static io.joyrpc.cluster.event.ClusterEvent.ShardEventType.ADD;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.Environment.*;

/**
 * 注册中心基类，实现Registry接口
 *
 * @date: 23/1/2019
 */
public abstract class AbstractRegistry implements Registry, Configure {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);

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
     * 备份恢复的数据
     */
    protected BackupDatum datum;
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
     * 开关
     */
    protected Switcher switcher = new Switcher();
    /**
     * 任务派发
     */
    protected Dispatcher dispatcher;
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
     * 注册中心对象id
     */
    protected int registryId;

    /**
     * 任务重试时间间隔
     */
    protected long taskRetryInterval;

    /**
     * 由refer与export的url到注册中心存储url的转换function
     */
    public static final Function<URL, URL> REGISTER_URL_FUNCTION = url -> {
        Map<String, String> params = new HashMap<>();
        params.put(ALIAS_OPTION.getName(), url.getString(ALIAS_OPTION));
        params.put(BUILD_VERSION_KEY, String.valueOf(Version.BUILD_VERSION));
        params.put(VERSION_KEY, GlobalContext.getString(PROTOCOL_VERSION_KEY));
        params.put(KEY_APPAPTH, GlobalContext.getString(APPLICATION_PATH));
        params.put(KEY_APPID, GlobalContext.getString(APPLICATION_ID));
        params.put(KEY_APPNAME, GlobalContext.getString(APPLICATION_NAME));
        params.put(KEY_APPINSID, GlobalContext.getString(APPLICATION_INSTANCE));
        params.put(REGION, GlobalContext.getString(REGION));
        params.put(DATA_CENTER, GlobalContext.getString(DATA_CENTER));
        params.put(Constants.JAVA_VERSION_KEY, GlobalContext.getString(Environment.JAVA_VERSION));
        params.put(ROLE_OPTION.getName(), url.getString(ROLE_OPTION));
        params.put(SERIALIZATION_OPTION.getName(), url.getString(SERIALIZATION_OPTION));
        params.put(TIMEOUT_OPTION.getName(), url.getString(TIMEOUT_OPTION.getName()));
        params.put(WEIGHT_OPTION.getName(), url.getString(WEIGHT_OPTION.getName()));
        params.put(DYNAMIC_OPTION.getName(), url.getString(DYNAMIC_OPTION.getName()));
        if (url.getBoolean(SSL_ENABLE)) {
            //ssl标识
            params.put(SSL_ENABLE.getName(), "true");
        }
        if (url.getBoolean(GENERIC_OPTION)) {
            //泛化调用标识
            params.put(GENERIC_OPTION.getName(), "true");
        }
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath(), params);
    };

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
     * 由refer与export的url到注册中心存储url的转换
     *
     * @return
     */
    protected Function<URL, URL> serviceUrlFunction() {
        return REGISTER_URL_FUNCTION;
    }

    @Override
    public CompletableFuture<Void> open() {
        //多次调用也能拿到正确的Future
        return switcher.open(f -> {
            dirty.set(false);
            connected.set(false);
            //任务执行线程
            dispatcher = new Dispatcher();
            dispatcher.start();
            doOpen(f);
            return f.whenComplete((v, t) -> {
                if (t != null) {
                    //出现异常，自动关闭
                    close();
                }
            });
        });
    }

    @Override
    public CompletableFuture<Void> close() {
        //多次调用也能拿到正确的Future
        //TODO 调用以后不能再次open了
        return switcher.close(f -> {
            doClose(f);
            return f.handle((u, t) -> {
                if (dispatcher != null) {
                    dispatcher.close();
                }
                return null;
            });
        });
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
        //防止在关闭中
        return switcher.reader().quietAnyway(() -> {
            RegisterMeta meta = getOrCreateMeta(url, this::getRegisterKey, (u, k) -> new RegisterMeta(u, k),
                    registers, this::addRegisterTask);
            //存在相同Key的URL多次注册，需要增加引用计数器，在注销的时候确保没有引用了才去注销
            meta.counter.incrementAndGet();
            return meta.getRegister();
        });
    }

    @Override
    public CompletableFuture<URL> deregister(final URL url, final int maxRetryTimes) {
        Objects.requireNonNull(url, "url can not be null.");
        return switcher.reader().quietAnyway(() -> {
            String key = getRegisterKey(url);
            RegisterMeta meta = registers.get(key);
            if (meta == null) {
                return CompletableFuture.completedFuture(url);
            } else if (meta.counter.decrementAndGet() == 0) {
                //没有引用了，删除并反注册
                registers.remove(key);
                return meta.unregister(o -> addDeregisterTask(o, maxRetryTimes));
            } else {
                return CompletableFuture.completedFuture(url);
            }
        });
    }

    @Override
    public boolean subscribe(final URL url, final ClusterHandler handler) {
        Objects.requireNonNull(url, "url can not be null.");
        Objects.requireNonNull(handler, "handler can not be null.");
        return switcher.reader().quietAnyway(() -> getOrCreateMeta(url, this::getClusterKey, this::createClusterMeta,
                clusters, (meta) -> addSubscribeTask(clusters, meta, m -> doSubscribe(m, m))).addHandler(handler));
    }

    @Override
    public boolean unsubscribe(final URL url, final ClusterHandler handler) {
        Objects.requireNonNull(url, "url can not be null.");
        Objects.requireNonNull(handler, "handler can not be null.");
        return unsubscribe(clusters, getClusterKey(url), handler, m -> doUnsubscribe(m, m));
    }

    @Override
    public boolean subscribe(final URL url, final ConfigHandler handler) {
        Objects.requireNonNull(url, "url can not be null.");
        Objects.requireNonNull(handler, "handler can not be null.");
        return switcher.reader().quietAnyway(() -> getOrCreateMeta(url, this::getConfigKey, this::createConfigMeta,
                configs, (meta) -> addSubscribeTask(configs, meta, m -> doSubscribe(m, m))).addHandler(handler));
    }

    @Override
    public boolean unsubscribe(final URL url, final ConfigHandler handler) {
        Objects.requireNonNull(url, "url can not be null.");
        Objects.requireNonNull(handler, "handler can not be null.");
        return unsubscribe(configs, getConfigKey(url), handler, m -> doUnsubscribe(m, m));
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getDataCenter() {
        return dataCenter;
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
        return new ClusterMeta(url, key);
    }

    /**
     * 创建配置元数据
     *
     * @param url
     * @param key
     * @return
     */
    protected ConfigMeta createConfigMeta(final URL url, final String key) {
        return new ConfigMeta(url, key);
    }

    /**
     * 新任务
     *
     * @param task
     */
    protected void addNewTask(final Task task) {
        tasks.offerFirst(task);
        if (dispatcher != null) {
            dispatcher.wakeup();
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
     * 获取集群键
     *
     * @param url
     * @return
     */
    protected String getClusterKey(final URL url) {
        //接口集群订阅:协议+接口+别名
        return url.toString(false, true, ALIAS_OPTION.getName());
    }

    /**
     * 获取配置键
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
            //接口配置订阅:协议+接口+别名+SIDE
            return url.toString(false, true, ALIAS_OPTION.getName(), Constants.ROLE_OPTION.getName());
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
                dispatcher.wakeup();
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
            if (dispatcher != null) {
                dispatcher.wakeup();
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
                    if (v.full && !v.datum.isEmpty()) {
                        List<BackupShard> backupShards = new LinkedList<>();
                        v.datum.forEach((name, shard) -> backupShards.add(new BackupShard(shard)));
                        backupClusters.put(k, backupShards);
                    }
                });
                datum.setClusters(backupClusters);
                //备份配置数据
                Map<String, Map<String, String>> configs = new HashMap<>(this.configs.size());
                this.configs.forEach((k, v) -> {
                    if (v.full && v.datum != null) {
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
     * 任务派发器
     */
    protected class Dispatcher extends Thread {
        /**
         * 启动标识
         */
        protected AtomicBoolean started = new AtomicBoolean(true);
        /**
         * 锁
         */
        protected Object mutex;

        public Dispatcher() {
            setDaemon(true);
            setName("registry-dispatcher");
            this.mutex = new Object();
        }

        /**
         * 停止
         */
        public void close() {
            started.set(false);
            Thread.interrupted();
        }

        /**
         * 唤醒
         */
        public void wakeup() {
            synchronized (mutex) {
                mutex.notifyAll();
            }
        }

        /**
         * 等到指定时间
         *
         * @param waitTime 时间
         */
        protected void await(final long waitTime) {
            synchronized (mutex) {
                try {
                    mutex.wait(waitTime);
                } catch (InterruptedException e) {
                }
            }
        }

        @Override
        public void run() {

            long waitTime;
            //恢复数据
            restore();
            while (started.get()) {
                if (!connected.get() && switcher.isOpened()) {
                    //当前还没有连接上，则判断是否有重连任务
                    reconnect();
                    //等到连接通知
                    await(1000L);
                } else {
                    waitTime = execute();
                    if (waitTime > 0) {
                        if (backup != null && dirty.compareAndSet(true, false)) {
                            //备份数据
                            backup();
                        }
                        await(waitTime);
                    }
                }
            }

        }

        /**
         * 执行任务
         *
         * @return
         */
        protected long execute() {
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
                    logger.error(e.getMessage(), e);
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
         * 进行重连
         */
        protected void reconnect() {
            ReconnectTask task = reconnectTask;
            if (task != null && task.isExpire()) {
                reconnectTask = null;
                //重连
                task.run();
            }
        }


    }

    /**
     * 订阅信息
     *
     * @param <T>
     */
    protected abstract class SubscribeMeta<T extends UpdateEvent> extends URLKey implements EventHandler<T>, Closeable {
        /**
         * 当前数据版本
         */
        protected long version;
        /**
         * 全量数据
         */
        protected boolean full;
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
         * 构造函数
         *
         * @param url
         * @param key
         * @param publisher
         */
        public SubscribeMeta(URL url, String key, Publisher<T> publisher) {
            super(url, key);
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

    }

    /**
     * 集群订阅信息
     */
    protected class ClusterMeta extends AbstractRegistry.SubscribeMeta<ClusterEvent> implements ClusterHandler {
        /**
         * 分片信息
         */
        protected Map<String, Shard> datum;

        /**
         * 构造函数
         *
         * @param url
         * @param key
         */
        public ClusterMeta(final URL url, final String key) {
            this(url, key, AbstractRegistry.this.getPublisher("registry.cluster." + key));
        }

        /**
         * 构造函数
         *
         * @param url
         * @param key
         * @param publisher
         */
        public ClusterMeta(final URL url, final String key, final Publisher<ClusterEvent> publisher) {
            super(url, key, publisher);
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
                    publisher.offer(new ClusterEvent(this, handler, UpdateEvent.UpdateType.FULL, version, full()));
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
         * @return
         */
        protected void update(final Map<String, Shard> cluster, final List<ClusterEvent.ShardEvent> events, boolean protectNullDatum) {
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

        @Override
        public synchronized void handle(final ClusterEvent event) {
            lastEventTime = SystemClock.now();
            boolean old = full;
            event.getType().update(url, (full, protectNullDatum) -> {
                //过期数据，不处理
                if (version > 0 && version >= event.getVersion()) {
                    return;
                }
                //是否已经有全量数据
                this.full = full ? true : this.full;
                //设置版本
                version = event.getVersion();
                //创建集群原数据,如果为全量数据，原数据为空map
                Map<String, Shard> cluster = !full && datum != null ? new HashMap<>(datum) : new HashMap<>();
                //更新，设置最新集群数据
                update(cluster, event.getDatum(), protectNullDatum);
                //TODO 初始化 version不一定为0，这里需要优化下
                //最新集群数据为空，且空保护，且版本号大于0（非初始化），不更新
                if (cluster.isEmpty() && protectNullDatum && version > 0) {
                    logger.warn("the datum of cluster event can not be null, version is " + version);
                } else {
                    datum = cluster;
                    //如果存在全量数据，通知事件
                    if (this.full) {
                        if (event.getType() == UpdateEvent.UpdateType.CLEAR) {
                            publisher.offer(new ClusterEvent(this, null, UpdateEvent.UpdateType.CLEAR, version, event.getDatum()));
                        } else if (!old) {
                            //如果以前不是全量数据，收到了全量数据事件，则广播合并完的全量数据
                            publisher.offer(new ClusterEvent(this, null, UpdateEvent.UpdateType.FULL, version, full()));
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
    protected class ConfigMeta extends SubscribeMeta<ConfigEvent> implements ConfigHandler {
        /**
         * 全量配置信息
         */
        protected Map<String, String> datum;

        /**
         * 构造函数
         *
         * @param url
         * @param key
         */
        public ConfigMeta(final URL url, final String key) {
            this(url, key, AbstractRegistry.this.getPublisher("registry.config." + key));
        }

        /**
         * 构造函数
         *
         * @param url
         * @param key
         * @param publisher
         */
        public ConfigMeta(final URL url, final String key, final Publisher<ConfigEvent> publisher) {
            super(url, key, publisher);
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
         * 添加监听器
         *
         * @param handler
         * @return
         */
        @Override
        public synchronized boolean addHandler(final EventHandler<ConfigEvent> handler) {
            if (publisher.addHandler(handler)) {
                //有全量数据
                if (full) {
                    publisher.offer(new ConfigEvent(this, handler, UpdateEvent.UpdateType.FULL, version, datum));
                }
                return true;
            }
            return false;
        }

        @Override
        public synchronized void handle(final ConfigEvent event) {
            lastEventTime = SystemClock.now();
            boolean old = full;
            if (datum == null) {
                datum = event.getDatum();
                version = event.getVersion();
                full = event.getType() == UpdateEvent.UpdateType.FULL;
            } else if (version >= event.getVersion()) {
                //版本低，如果是全量数据，考虑进行合并
                if (event.getType() == UpdateEvent.UpdateType.FULL && !full) {
                    Map<String, String> map = new HashMap<>(event.getDatum());
                    map.putAll(datum);
                    datum = map;
                    full = true;
                } else {
                    //丢掉过期数据
                    return;
                }
            } else if (event.getType() == UpdateEvent.UpdateType.FULL) {
                datum = event.getDatum();
                full = true;
                version = event.getVersion();
            } else {
                //复制一份，防止在并发读取
                Map<String, String> map = new HashMap<>(datum);
                map.putAll(event.getDatum());
                datum = map;
                version = event.getVersion();
            }
            //有完整的数据
            if (full) {
                //是否准备好，适用于多个配置来源合并通知
                if (ready()) {
                    //判断是全量数据初始化还是增量更新
                    publisher.offer(new ConfigEvent(this, null,
                            !old ? UpdateEvent.UpdateType.FULL : UpdateEvent.UpdateType.UPDATE, version, !old ? datum : event.getDatum()));
                }
                //保存数据
                dirty();
            }
        }

    }

    /**
     * 注册信息
     */
    protected class RegisterMeta extends URLKey {
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

    }


}
