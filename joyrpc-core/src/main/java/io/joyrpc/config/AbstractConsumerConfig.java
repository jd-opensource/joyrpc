package io.joyrpc.config;

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

import io.joyrpc.GenericService;
import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.annotation.Alias;
import io.joyrpc.annotation.Service;
import io.joyrpc.cluster.candidate.Candidature;
import io.joyrpc.cluster.distribution.ExceptionPredication;
import io.joyrpc.cluster.distribution.LoadBalance;
import io.joyrpc.cluster.distribution.Route;
import io.joyrpc.cluster.distribution.Router;
import io.joyrpc.cluster.event.NodeEvent;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.config.validator.ValidatePlugin;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.context.RequestContext;
import io.joyrpc.event.EventHandler;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.channel.ChannelManagerFactory;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.Futures;
import io.joyrpc.util.Status;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static io.joyrpc.GenericService.GENERIC;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.ClassUtils.isReturnFuture;
import static io.joyrpc.util.Status.*;

/**
 * 抽象消费者配置
 */
public abstract class AbstractConsumerConfig<T> extends AbstractInterfaceConfig {
    protected static final AtomicReferenceFieldUpdater<AbstractConsumerConfig, Status> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractConsumerConfig.class, Status.class, "status");
    private final static Logger logger = LoggerFactory.getLogger(AbstractConsumerConfig.class);

    /**
     * 注册中心配置，只能配置一个
     */
    @Valid
    protected RegistryConfig registry;
    /**
     * 直连调用地址
     */
    protected String url;
    /**
     * 是否泛化调用
     */
    protected Boolean generic = false;
    /**
     * 集群处理，默认是failfast
     */
    @ValidatePlugin(extensible = Route.class, name = "ROUTE", defaultValue = DEFAULT_ROUTE)
    protected String cluster;
    /**
     * The Loadbalance. 负载均衡
     */
    @ValidatePlugin(extensible = LoadBalance.class, name = "LOADBALANCE", defaultValue = DEFAULT_LOADBALANCE)
    protected String loadbalance;
    /**
     * 粘连算法，尽量保持同一个目标地址
     */
    protected Boolean sticky;
    /**
     * 是否jvm内部调用（provider和consumer配置在同一个jvm内，则走本地jvm内部，不走远程）
     */
    protected Boolean injvm;
    /**
     * 是否强依赖（即没有服务节点就启动失败）
     */
    protected Boolean check;
    /**
     * 默认序列化
     */
    @ValidatePlugin(extensible = Serialization.class, name = "SERIALIZATION", defaultValue = DEFAULT_SERIALIZATION)
    protected String serialization;
    /**
     * 初始化连接数
     */
    protected Integer initSize;
    /**
     * 最小连接数
     */
    protected Integer minSize;
    /**
     * 候选者算法插件
     */
    @ValidatePlugin(extensible = Candidature.class, name = "CANDIDATURE", defaultValue = DEFAULT_CANDIDATURE)
    protected String candidature;
    /**
     * The Retries. 失败后重试次数
     */
    protected Integer retries;
    /**
     * 可以重试的逗号分隔的异常全路径类名
     */
    protected String failoverWhenThrowable;
    /**
     * 重试异常判断接口插件
     */
    @ValidatePlugin(extensible = ExceptionPredication.class, name = "EXCEPTION_PREDICATION")
    protected String failoverPredication;
    /**
     * channel创建模式
     * shared:共享(默认),unshared:独享
     */
    @ValidatePlugin(extensible = ChannelManagerFactory.class, name = "CHANNEL_MANAGER_FACTORY", defaultValue = DEFAULT_HANNEL_FACTORY)
    protected String channelFactory;
    /**
     * 路由规则
     */
    @ValidatePlugin(extensible = Router.class, name = "ROUTER")
    protected String router;
    /**
     * 预热权重
     */
    protected Integer warmupWeight;
    /**
     * 预热时间
     */
    protected Integer warmupDuration;
    /**
     * 泛化调用的类
     */
    protected transient Class<?> genericClass;
    /**
     * 代理实现类
     */
    protected transient volatile T stub;
    /**
     * 事件监听器
     */
    protected transient List<EventHandler<NodeEvent>> eventHandlers = new CopyOnWriteArrayList<>();
    /**
     * 打开的结果
     */
    protected transient volatile CompletableFuture<Void> openFuture;
    /**
     * 关闭Future
     */
    protected transient volatile CompletableFuture<Void> closeFuture = CompletableFuture.completedFuture(null);
    /**
     * 状态
     */
    protected transient volatile Status status = Status.CLOSED;
    /**
     * 控制器
     */
    protected transient volatile AbstractConsumerController<T, ? extends AbstractConsumerConfig> controller;

    public AbstractConsumerConfig() {
    }

    public AbstractConsumerConfig(AbstractConsumerConfig config) {
        super(config);
        this.registry = config.registry;
        this.url = config.url;
        this.generic = config.generic;
        this.cluster = config.cluster;
        this.loadbalance = config.loadbalance;
        this.sticky = config.sticky;
        this.injvm = config.injvm;
        this.check = config.check;
        this.serialization = config.serialization;
        this.initSize = config.initSize;
        this.minSize = config.minSize;
        this.candidature = config.candidature;
        this.retries = config.retries;
        this.failoverWhenThrowable = config.failoverWhenThrowable;
        this.failoverPredication = config.failoverPredication;
        this.channelFactory = config.channelFactory;
        this.router = config.router;
        this.warmupWeight = config.warmupWeight;
        this.warmupDuration = config.warmupDuration;
    }

    public AbstractConsumerConfig(AbstractConsumerConfig config, String alias) {
        this(config);
        this.alias = alias;
    }

    /**
     * 代理对象，用于Spring场景提前返回代理
     *
     * @return
     */
    public T proxy() {
        if (stub == null) {
            stub = getProxyFactory().getProxy((Class<T>) getProxyClass(), (proxy, method, args) -> {
                AbstractConsumerController<T, ? extends AbstractConsumerConfig> cc = controller;
                if (cc == null) {
                    switch (status) {
                        case CLOSING:
                            throw new RpcException("Consumer config is closing. " + name());
                        case CLOSED:
                            throw new RpcException("Consumer config is closed. " + name());
                        default:
                            throw new RpcException("Consumer config is opening. " + name());
                    }
                } else {
                    return cc.invoke(proxy, method, args);
                }
            });
        }
        return stub;
    }

    @Override
    protected boolean isClose() {
        return status.isClose() || super.isClose();
    }

    /**
     * 异步引用
     *
     * @return
     */
    public CompletableFuture<T> refer() {
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFuture<Void> future = new CompletableFuture<>();
        final T obj = refer(future);
        future.whenComplete((v, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else {
                result.complete(obj);
            }
        });
        return result;
    }

    /**
     * 引用一个远程服务，用于Spring场景，优先返回代理对象
     *
     * @param future 操作结果消费者
     * @return 接口代理类
     * @see AbstractConsumerConfig#refer()
     * @see AbstractConsumerConfig#proxy()
     */
    @Deprecated
    public T refer(final CompletableFuture<Void> future) {
        if (STATE_UPDATER.compareAndSet(this, Status.CLOSED, Status.OPENING)) {
            GlobalContext.getContext();
            logger.info(String.format("Start refering consumer %s with bean id %s", name(), id));
            final CompletableFuture<Void> f = new CompletableFuture<>();
            final AbstractConsumerController<T, ? extends AbstractConsumerConfig> cc = create();
            openFuture = f;
            controller = cc;
            cc.open().whenComplete((v, e) -> {
                if (openFuture != f || e == null && !STATE_UPDATER.compareAndSet(this, Status.OPENING, Status.OPENED)) {
                    logger.error(String.format("Error occurs while referring %s with bean id %s,caused by state is illegal. ", name(), id));
                    //已经被关闭了
                    Throwable throwable = new InitializationException("state is illegal.");
                    f.completeExceptionally(throwable);
                    Optional.ofNullable(future).ifPresent(o -> o.completeExceptionally(throwable));
                    cc.close();
                } else if (e != null) {
                    logger.error(String.format("Error occurs while referring %s with bean id %s,caused by %s. ", name(), id, e.getMessage()), e);
                    f.completeExceptionally(e);
                    Optional.ofNullable(future).ifPresent(o -> o.completeExceptionally(e));
                    unrefer(false);
                } else {
                    logger.info(String.format("Success refering consumer %s with bean id %s", name(), id));
                    f.complete(null);
                    Optional.ofNullable(future).ifPresent(o -> o.complete(null));
                    //触发配置更新
                    cc.update();
                }
            });
        } else {
            switch (status) {
                case OPENING:
                case OPENED:
                    //可重入，没有并发调用
                    Futures.chain(openFuture, future);
                    break;
                default:
                    //其它状态不应该并发执行
                    Optional.ofNullable(future).ifPresent(o -> o.completeExceptionally(new InitializationException("state is illegal.")));
            }
        }

        return stub;


    }

    /**
     * 创建消费控制器
     *
     * @return
     */
    protected abstract AbstractConsumerController<T, ? extends AbstractConsumerConfig> create();

    /**
     * 注销
     *
     * @return
     */
    public CompletableFuture<Void> unrefer() {
        return unrefer(GlobalContext.asParametric().getBoolean(Constants.GRACEFULLY_SHUTDOWN_OPTION));
    }

    /**
     * 注销
     *
     * @param gracefully
     * @return
     */
    public CompletableFuture<Void> unrefer(final boolean gracefully) {
        if (STATE_UPDATER.compareAndSet(this, OPENING, CLOSING)) {
            logger.info(String.format("Start unrefering consumer %s with bean id %s", name(), id));
            CompletableFuture<Void> future = new CompletableFuture<>();
            closeFuture = future;
            //终止等待的请求
            controller.broken();
            openFuture.whenComplete((v, e) -> {
                //openFuture完成后会自动关闭控制器
                logger.info("Success unrefering consumer " + name());
                status = CLOSED;
                controller = null;
                future.complete(null);
            });
            return closeFuture;
        } else if (STATE_UPDATER.compareAndSet(this, OPENED, CLOSING)) {
            logger.info(String.format("Start unrefering consumer  %s with bean id %s", name(), id));
            //状态从打开到关闭中，该状态只能变更为CLOSED
            CompletableFuture<Void> future = new CompletableFuture<>();
            closeFuture = future;
            controller.close(gracefully).whenComplete((o, s) -> {
                logger.info("Success unrefering consumer " + name());
                status = CLOSED;
                controller = null;
                future.complete(null);
            });
            return closeFuture;
        } else {
            switch (status) {
                case CLOSING:
                case CLOSED:
                    return closeFuture;
                default:
                    return Futures.completeExceptionally(new IllegalStateException("Status is illegal."));
            }
        }
    }

    @Override
    public String name() {
        if (name == null) {
            name = GlobalContext.getString(PROTOCOL_KEY) + "://" + interfaceClazz + "?" + Constants.ALIAS_OPTION.getName() + "=" + alias + "&" + Constants.ROLE_OPTION.getName() + "=" + Constants.SIDE_CONSUMER;
        }
        return name;
    }

    @Override
    public Class getProxyClass() {
        if (Boolean.TRUE.equals(generic)) {
            if (genericClass == null) {
                //获取泛化类名，便于兼容历史版本
                String className = GlobalContext.getString(GENERIC_CLASS);
                if (className == null || className.isEmpty()) {
                    genericClass = GenericService.class;
                } else {
                    try {
                        Class<?> clazz = ClassUtils.forName(className);
                        genericClass = clazz.isInterface() && GenericService.class.isAssignableFrom(clazz) ? clazz : GenericService.class;
                    } catch (ClassNotFoundException e) {
                        genericClass = GenericService.class;
                    }
                }
            }
            return genericClass;

        }
        return super.getInterfaceClass();
    }

    public RegistryConfig getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryConfig registry) {
        this.registry = registry;
    }

    @Override
    public boolean isSubscribe() {
        return subscribe;
    }

    @Override
    public void setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public Boolean isGeneric() {
        return generic;
    }

    public void setGeneric(Boolean generic) {
        this.generic = generic;
    }

    public Boolean getSticky() {
        return sticky;
    }

    public void setSticky(Boolean sticky) {
        this.sticky = sticky;
    }

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public Boolean isInjvm() {
        return injvm;
    }

    public void setInjvm(Boolean injvm) {
        this.injvm = injvm;
    }

    public String getRouter() {
        return router;
    }

    public void setRouter(String router) {
        this.router = router;
    }

    public Integer getInitSize() {
        return initSize;
    }

    public void setInitSize(Integer initSize) {
        this.initSize = initSize;
    }

    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    public String getCandidature() {
        return candidature;
    }

    public void setCandidature(String candidature) {
        this.candidature = candidature;
    }

    public String getFailoverWhenThrowable() {
        return failoverWhenThrowable;
    }

    public void setFailoverWhenThrowable(String failoverWhenThrowable) {
        this.failoverWhenThrowable = failoverWhenThrowable;
    }

    public String getChannelFactory() {
        return channelFactory;
    }

    public void setChannelFactory(String channelFactory) {
        this.channelFactory = channelFactory;
    }

    public T getStub() {
        return stub;
    }

    public Integer getWarmupWeight() {
        return warmupWeight;
    }

    public void setWarmupWeight(Integer warmupWeight) {
        this.warmupWeight = warmupWeight;
    }

    public Integer getWarmupDuration() {
        return warmupDuration;
    }

    public void setWarmupDuration(Integer warmupDuration) {
        this.warmupDuration = warmupDuration;
    }

    /**
     * 添加事件监听器
     *
     * @param handler
     */
    public void addEventHandler(final EventHandler<NodeEvent> handler) {
        if (handler != null) {
            eventHandlers.add(handler);
        }
    }

    /**
     * 移除事件监听器
     *
     * @param handler
     */
    public void removeEventHandler(final EventHandler<NodeEvent> handler) {
        if (handler != null) {
            eventHandlers.remove(handler);
        }
    }

    @Override
    protected Map<String, String> addAttribute2Map(Map<String, String> params) {
        super.addAttribute2Map(params);
        if (url != null) {
            try {
                addElement2Map(params, Constants.URL_OPTION, URL.encode(url));
            } catch (UnsupportedEncodingException e) {
                throw new InitializationException("Value of \"url\" value is not encode in consumer config with key " + url + " !", ExceptionCode.COMMON_URL_ENCODING);
            }
        }
        addElement2Map(params, Constants.GENERIC_OPTION, generic);
        addElement2Map(params, Constants.ROUTE_OPTION, cluster);
        addElement2Map(params, Constants.RETRIES_OPTION, retries);
        addElement2Map(params, Constants.LOADBALANCE_OPTION, loadbalance);
        addElement2Map(params, Constants.IN_JVM_OPTION, injvm);
        addElement2Map(params, Constants.STICKY_OPTION, sticky);
        addElement2Map(params, Constants.CHECK_OPTION, check);
        addElement2Map(params, Constants.SERIALIZATION_OPTION, serialization);
        addElement2Map(params, Constants.ROUTER_OPTION, router);
        addElement2Map(params, INIT_SIZE_OPTION, initSize);
        addElement2Map(params, MIN_SIZE_OPTION, minSize);
        addElement2Map(params, Constants.CANDIDATURE_OPTION, candidature);
        addElement2Map(params, Constants.ROLE_OPTION, Constants.SIDE_CONSUMER);
        addElement2Map(params, Constants.TIMESTAMP_KEY, String.valueOf(SystemClock.now()));
        addElement2Map(params, Constants.FAILOVER_WHEN_THROWABLE_OPTION, failoverWhenThrowable);
        addElement2Map(params, Constants.FAILOVER_PREDICATION_OPTION, failoverPredication);
        addElement2Map(params, Constants.CHANNEL_FACTORY_OPTION, channelFactory);
        addElement2Map(params, WARMUP_ORIGIN_WEIGHT_OPTION, warmupWeight);
        addElement2Map(params, WARMUP_DURATION_OPTION, warmupDuration);
        return params;
    }

    /**
     * 控制器
     */
    protected static abstract class AbstractConsumerController<T, C extends AbstractConsumerConfig<T>>
            extends AbstractController<C> implements InvocationHandler {

        /**
         * 注册中心配置
         */
        protected RegistryConfig registry;
        /**
         * 注册中心地址
         */
        protected URL registryUrl;
        /**
         * 用于注册的地址
         */
        protected URL registerUrl;
        /**
         * 代理类
         */
        protected Class<?> proxyClass;
        /**
         * 注册和订阅的接口名称
         */
        protected String interfaceClazz;
        /**
         * 调用handler
         */
        protected volatile ConsumerInvokeHandler invokeHandler;

        /**
         * 构造函数
         *
         * @param config
         */
        public AbstractConsumerController(C config) {
            super(config);
        }

        @Override
        protected boolean isClose() {
            return config.isClose() || config.controller != this;
        }

        @Override
        protected boolean isOpened() {
            return config.status == OPENED;
        }

        /**
         * 打开
         *
         * @return
         */
        public CompletableFuture<Void> open() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            try {
                config.validate();
                proxyClass = config.getProxyClass();
                interfaceClazz = getPseudonym(config.getInterfaceClass(), config.getInterfaceClazz());
                registry = (config.url != null && !config.url.isEmpty()) ?
                        new RegistryConfig(Constants.FIX_REGISTRY, config.url)
                        : config.registry != null ? config.registry : RegistryConfig.DEFAULT_REGISTRY_SUPPLIER.get();
                //注册中心地址
                registryUrl = parse(registry);
                String host = getLocalHost(registryUrl.getString(Constants.ADDRESS_OPTION));
                //构造原始URL
                url = new URL(GlobalContext.getString(PROTOCOL_KEY), host, 0, interfaceClazz, config.addAttribute2Map());
                //加上动态配置的服务URL
                serviceUrl = configure(null);
                doOpen().whenComplete((v, e) -> {
                    if (e == null) {
                        future.complete(null);
                    } else {
                        future.completeExceptionally(e);
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            return future;
        }

        /**
         * 打开操作
         *
         * @return
         */
        protected abstract CompletableFuture<Void> doOpen();

        /**
         * 关闭
         *
         * @return
         */
        public CompletableFuture<Void> close() {
            return close(GlobalContext.asParametric().getBoolean(Constants.GRACEFULLY_SHUTDOWN_OPTION));
        }

        /**
         * 关闭
         *
         * @param gracefully 是否优雅关闭
         * @return
         */
        public CompletableFuture<Void> close(boolean gracefully) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> update(final URL newUrl) {
            return CompletableFuture.completedFuture(null);
        }

        /**
         * 获取服务的别名
         *
         * @param clazz        接口
         * @param defaultValue 默认值
         * @return
         */
        protected String getPseudonym(final Class<?> clazz, final String defaultValue) {
            String result = defaultValue;
            if (clazz != null && !GENERIC.test(clazz)) {
                Service service = clazz.getAnnotation(Service.class);
                if (service != null && !service.name().isEmpty()) {
                    //判断服务名
                    result = service.name();
                } else {
                    Alias alias = clazz.getAnnotation(Alias.class);
                    if (alias != null && !alias.value().isEmpty()) {
                        result = alias.value();
                    }
                }
            }
            return result;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            ConsumerInvokeHandler handler = invokeHandler;
            if (handler == null) {
                switch (config.status) {
                    case CLOSING:
                        throw new RpcException("Consumer config is closing. " + config.name());
                    case CLOSED:
                        throw new RpcException("Consumer config is closed. " + config.name());
                    default:
                        throw new RpcException("Consumer config is opening. " + config.name());
                }
            } else {
                return handler.invoke(proxy, method, args);
            }
        }

    }

    /**
     * 消费者调用
     *
     * @date: 2 /19/2019
     */
    protected static class ConsumerInvokeHandler implements InvocationHandler {
        /**
         * The Method name toString.
         */
        static String METHOD_NAME_TO_STRING = "toString";
        /**
         * The Method name hashcode.
         */
        static String METHOD_NAME_HASHCODE = "hashCode";
        /**
         * The Method name equals.
         */
        static String METHOD_NAME_EQUALS = "equals";
        /**
         * The Invoker.
         */
        protected Invoker invoker;
        /**
         * 接口名称
         */
        protected Class<?> iface;
        /**
         * 是否为异步
         */
        protected boolean async;
        /**
         * 是否是泛化调用
         */
        protected boolean generic;
        /**
         * 默认方法构造器
         */
        protected volatile Constructor<MethodHandles.Lookup> constructor;
        /**
         * 默认方法处理器
         */
        protected Map<String, Optional<MethodHandle>> handles = new ConcurrentHashMap<>();

        /**
         * 构造函数
         *
         * @param invoker
         * @param iface
         * @param serviceUrl
         */
        public ConsumerInvokeHandler(final Invoker invoker, final Class<?> iface, final URL serviceUrl) {
            this.invoker = invoker;
            this.iface = iface;
            this.async = serviceUrl.getBoolean(Constants.ASYNC_OPTION);
            this.generic = GENERIC.test(iface);
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] param) throws Throwable {
            //Java8允许在接口上定义静态方法和默认方法（仅用与GenericService接口类及其子接口类）
            if (generic && method.isDefault()) {
                return invokeDefaultMethod(proxy, method, param);
            }
            //Java8允许在接口上定义静态方法
            if (Modifier.isStatic(method.getModifiers())) {
                //静态方法
                return method.invoke(proxy, param);
            }

            boolean isReturnFuture = isReturnFuture(iface, method);
            boolean isAsync = this.async || isReturnFuture;
            //请求上下文
            RequestContext context = RequestContext.getContext();
            //上下文的异步必须设置成completeFuture
            context.setAsync(isReturnFuture);
            //构造请求消息
            RequestMessage<Invocation> request = RequestMessage.build(new Invocation(iface, method, param));
            //分组Failover调用，需要在这里设置创建时间和超时时间，不能再Refer里面。否则会重置。
            request.setCreateTime(SystemClock.now());
            //超时时间为0，Refer会自动修正，便于分组重试
            request.getHeader().

                    setTimeout(0);
            //当前线程
            request.setThread(Thread.currentThread());
            //当前线程上下文
            request.setContext(context);
            Object response = doInvoke(invoker, request, isAsync);
            if (isAsync) {
                if (isReturnFuture) {
                    //方法返回值为 future
                    return response;
                } else {
                    //手动异步
                    context.setFuture((CompletableFuture<?>) response);
                    return null;
                }
            } else {
                // 返回同步结果
                return response;
            }
        }

        /**
         * 调用默认方法
         *
         * @param proxy
         * @param method
         * @param param
         * @return
         * @throws Throwable
         */
        protected Object invokeDefaultMethod(final Object proxy, final Method method, final Object[] param) throws Throwable {
            Object[] args = param;
            String name = method.getName();
            if (generic) {
                args = (Object[]) param[2];
                name = (String) param[0];
            }
            int count = args == null ? 0 : args.length;
            if (count == 0) {
                if (METHOD_NAME_TO_STRING.equals(name)) {
                    return invoker.getName();
                } else if (METHOD_NAME_HASHCODE.equals(name)) {
                    return invoker.hashCode();
                }
            } else if (count == 1 && METHOD_NAME_EQUALS.equals(name)) {
                return invoker.equals(args[0]);
            }
            if (constructor == null) {
                synchronized (this) {
                    if (constructor == null) {
                        constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                        constructor.setAccessible(true);
                    }
                }
            }
            if (constructor != null) {
                Optional<MethodHandle> optional = handles.computeIfAbsent(method.getName(), o -> {
                    Class<?> declaringClass = method.getDeclaringClass();
                    try {
                        return Optional.of(constructor.
                                newInstance(declaringClass, MethodHandles.Lookup.PRIVATE).
                                unreflectSpecial(method, declaringClass).
                                bindTo(proxy));
                    } catch (Throwable e) {
                        return Optional.empty();
                    }
                });
                if (optional.isPresent()) {
                    return optional.get().invokeWithArguments(param);
                }
            }
            throw new UnsupportedOperationException();
        }

        /**
         * 这个方法用来做 Trace 追踪的增强点，不要随便修改
         */
        protected Object doInvoke(Invoker invoker, RequestMessage<Invocation> request, boolean async) throws Throwable {
            try {
                return async ? asyncInvoke(invoker, request) : syncInvoke(invoker, request);
            } finally {
                //调用结束，使用新的上下文
                Map<String, Object> session = request.getContext().getSession();
                RequestContext.restore(new RequestContext(session == null ? null : new HashMap<>(session)));
            }
        }

        /**
         * 同步调用
         *
         * @param invoker 调用器
         * @param request 请求
         * @return
         * @throws Throwable
         */
        protected Object syncInvoke(final Invoker invoker, final RequestMessage<Invocation> request) throws Throwable {
            CompletableFuture<Result> future = invoker.invoke(request);
            try {
                //正常同步返回，处理Java8的future.get内部先自循环造成的性能问题
                Result result = future.get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                if (result.isException()) {
                    throw result.getException();
                }
                return result.getValue();
            } catch (ExecutionException e) {
                throw e.getCause() != null ? e.getCause() : e;
            }
        }

        /**
         * 异步调用
         */
        protected Object asyncInvoke(final Invoker invoker, final RequestMessage<Invocation> request) throws Throwable {
            //异步调用，业务逻辑执行完毕，不清理IO线程的上下文
            CompletableFuture<Object> response = new CompletableFuture<>();
            CompletableFuture<Result> future = invoker.invoke(request);
            future.whenComplete((res, err) -> {
                //需要在这里判断是否要进行恢复
                if (request.getThread() != Thread.currentThread()) {
                    //确保在whenComplete执行的用户业务代码能拿到调用上下文
                    RequestContext.restore(request.getContext());
                }
                Throwable throwable = err == null ? res.getException() : err;
                if (throwable != null) {
                    response.completeExceptionally(throwable);
                } else {
                    response.complete(res.getValue());
                }
            });
            return response;
        }

    }


}
