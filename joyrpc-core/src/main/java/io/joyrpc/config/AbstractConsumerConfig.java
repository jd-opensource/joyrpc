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
import io.joyrpc.cluster.candidate.Candidature;
import io.joyrpc.cluster.distribution.*;
import io.joyrpc.cluster.event.NodeEvent;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.config.validator.ValidatePlugin;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.EventHandler;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.InvokerCaller;
import io.joyrpc.transport.channel.ChannelManagerFactory;
import io.joyrpc.util.*;
import io.joyrpc.util.StateMachine.IntStateMachine;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static io.joyrpc.GenericService.GENERIC;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.ClassUtils.forName;
import static io.joyrpc.util.ClassUtils.isReturnFuture;

/**
 * 抽象消费者配置
 */
public abstract class AbstractConsumerConfig<T> extends AbstractInterfaceConfig {
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
     * 集群处理算法
     */
    @ValidatePlugin(extensible = Router.class, name = "ROUTER", defaultValue = DEFAULT_ROUTER)
    protected String cluster;
    /**
     * 负载均衡算法
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
     * 失败最大重试次数
     */
    protected Integer retries;
    /**
     * 每个节点只调用一次
     */
    protected Boolean retryOnlyOncePerNode;
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
     * 异常重试目标节点选择器
     */
    @ValidatePlugin(extensible = FailoverSelector.class, name = "FAILOVER_SELECTOR", defaultValue = DEFAULT_FAILOVER_SELECTOR)
    protected String failoverSelector;
    /**
     * 并行分发数量，在采用并行分发策略有效
     */
    protected Integer forks;
    /**
     * channel创建模式
     * shared:共享(默认),unshared:独享
     */
    @ValidatePlugin(extensible = ChannelManagerFactory.class, name = "CHANNEL_MANAGER_FACTORY", defaultValue = DEFAULT_CHANNEL_FACTORY)
    protected String channelFactory;
    /**
     * 节点选择器
     */
    @ValidatePlugin(extensible = NodeSelector.class, name = "NODE_SELECTOR", multiple = true)
    protected String nodeSelector;
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
     * 状态机
     */
    protected transient volatile IntStateMachine<Void, ConsumerPilot> stateMachine = new IntStateMachine<>(() -> create());

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
        this.retryOnlyOncePerNode = config.retryOnlyOncePerNode;
        this.failoverWhenThrowable = config.failoverWhenThrowable;
        this.failoverPredication = config.failoverPredication;
        this.failoverSelector = config.failoverSelector;
        this.forks = config.forks;
        this.channelFactory = config.channelFactory;
        this.nodeSelector = config.nodeSelector;
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
     * @return 代理
     */
    public T proxy() {
        if (stub == null) {
            final Class<T> proxyClass = getProxyClass();
            stub = getProxyFactory().getProxy(proxyClass, (proxy, method, args) -> {
                try {
                    ConsumerPilot pilot = stateMachine.getController(s -> s.isOpened());
                    if (pilot == null) {
                        throw new RpcException("Consumer config is not opened. " + name());
                    } else {
                        return pilot.invoke(proxy, method, args);
                    }
                } catch (Throwable e) {
                    if (isReturnFuture(proxyClass, method)) {
                        return Futures.completeExceptionally(e);
                    }
                    throw e;
                }
            });
        }
        return stub;
    }

    @Override
    protected boolean isClose() {
        return stateMachine.isClose(null) || super.isClose();
    }

    protected boolean isClose(final AbstractConsumerPilot<?, ?> controller) {
        return stateMachine.isClose(controller) || super.isClose();
    }

    /**
     * 异步引用
     *
     * @return CompletableFuture
     */
    public CompletableFuture<T> refer() {
        CompletableFuture<T> result = new CompletableFuture<>();
        stateMachine.open().whenComplete((v, e) -> {
            if (e == null) {
                result.complete(stub);
            } else {
                result.completeExceptionally(e);
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
        stateMachine.open().whenComplete((v, e) -> {
            if (e == null) {
                Optional.ofNullable(future).ifPresent(o -> o.complete(null));
            } else {
                Optional.ofNullable(future).ifPresent(o -> o.completeExceptionally(e));
            }
        });

        return stub;


    }

    /**
     * 创建消费控制器
     *
     * @return 控制器
     */
    protected abstract ConsumerPilot create();

    /**
     * 注销
     *
     * @return CompletableFuture
     */
    public CompletableFuture<Void> unrefer() {
        Parametric parametric = new MapParametric<>(GlobalContext.getContext());
        return unrefer(parametric.getBoolean(Constants.GRACEFULLY_SHUTDOWN_OPTION));
    }

    /**
     * 注销
     *
     * @param gracefully 优雅标识
     * @return CompletableFuture
     */
    public CompletableFuture<Void> unrefer(final boolean gracefully) {
        return stateMachine.close(gracefully);
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
                        Class<?> clazz = forName(className);
                        genericClass = GENERIC.test(clazz) ? clazz : GenericService.class;
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

    public Boolean getRetryOnlyOncePerNode() {
        return retryOnlyOncePerNode;
    }

    public void setRetryOnlyOncePerNode(Boolean retryOnlyOncePerNode) {
        this.retryOnlyOncePerNode = retryOnlyOncePerNode;
    }

    public String getFailoverWhenThrowable() {
        return failoverWhenThrowable;
    }

    public void setFailoverWhenThrowable(String failoverWhenThrowable) {
        this.failoverWhenThrowable = failoverWhenThrowable;
    }

    public String getFailoverPredication() {
        return failoverPredication;
    }

    public void setFailoverPredication(String failoverPredication) {
        this.failoverPredication = failoverPredication;
    }

    public String getFailoverSelector() {
        return failoverSelector;
    }

    public void setFailoverSelector(String failoverSelector) {
        this.failoverSelector = failoverSelector;
    }

    public Integer getForks() {
        return forks;
    }

    public void setForks(Integer forks) {
        this.forks = forks;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public Boolean getGeneric() {
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

    public Boolean getCheck() {
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

    public Boolean getInjvm() {
        return injvm;
    }

    public void setInjvm(Boolean injvm) {
        this.injvm = injvm;
    }

    public String getNodeSelector() {
        return nodeSelector;
    }

    public void setNodeSelector(String nodeSelector) {
        this.nodeSelector = nodeSelector;
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
     * @param handler 事件处理器
     */
    public void addEventHandler(final EventHandler<NodeEvent> handler) {
        if (handler != null) {
            eventHandlers.add(handler);
        }
    }

    /**
     * 移除事件监听器
     *
     * @param handler 事件处理器
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
        addElement2Map(params, Constants.ROUTER_OPTION, cluster);
        addElement2Map(params, Constants.RETRIES_OPTION, retries);
        addElement2Map(params, Constants.RETRY_ONLY_ONCE_PER_NODE_OPTION, retryOnlyOncePerNode);
        addElement2Map(params, Constants.FAILOVER_WHEN_THROWABLE_OPTION, failoverWhenThrowable);
        addElement2Map(params, Constants.FAILOVER_PREDICATION_OPTION, failoverPredication);
        addElement2Map(params, Constants.FAILOVER_SELECTOR_OPTION, failoverSelector);
        addElement2Map(params, Constants.FORKS_OPTION, forks);
        addElement2Map(params, Constants.LOADBALANCE_OPTION, loadbalance);
        addElement2Map(params, Constants.IN_JVM_OPTION, injvm);
        addElement2Map(params, Constants.STICKY_OPTION, sticky);
        addElement2Map(params, Constants.CHECK_OPTION, check);
        addElement2Map(params, Constants.SERIALIZATION_OPTION, serialization);
        addElement2Map(params, Constants.NODE_SELECTOR_OPTION, nodeSelector);
        addElement2Map(params, Constants.INIT_SIZE_OPTION, initSize);
        addElement2Map(params, Constants.MIN_SIZE_OPTION, minSize);
        addElement2Map(params, Constants.CANDIDATURE_OPTION, candidature);
        addElement2Map(params, Constants.ROLE_OPTION, Constants.SIDE_CONSUMER);
        addElement2Map(params, Constants.TIMESTAMP_KEY, String.valueOf(SystemClock.now()));
        addElement2Map(params, Constants.CHANNEL_FACTORY_OPTION, channelFactory);
        addElement2Map(params, Constants.WARMUP_ORIGIN_WEIGHT_OPTION, warmupWeight);
        addElement2Map(params, Constants.WARMUP_DURATION_OPTION, warmupDuration);
        return params;
    }

    /**
     * 消费者控制器接口
     */
    protected interface ConsumerPilot extends InvocationHandler, StateController<Void> {

    }

    /**
     * 控制器
     */
    protected static abstract class AbstractConsumerPilot<T, C extends AbstractConsumerConfig<T>>
            extends AbstractController<C> implements ConsumerPilot, EventHandler<StateEvent> {

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
         * 调用handler
         */
        protected volatile InvokerCaller invocationHandler;
        /**
         * 等待open结束，invokeHandler初始化
         */
        protected CountDownLatch latch = new CountDownLatch(1);

        /**
         * 构造函数
         *
         * @param config 配置
         */
        public AbstractConsumerPilot(C config) {
            super(config);
        }

        @Override
        public void handle(final StateEvent event) {
            Throwable e = event.getThrowable();
            //控制器事件
            switch (event.getType()) {
                case StateEvent.START_OPEN:
                    GlobalContext.getContext();
                    logger.info(String.format("Start refering consumer %s with bean id %s", config.name(), config.id));
                    break;
                case StateEvent.SUCCESS_OPEN:
                    logger.info(String.format("Success refering consumer %s with bean id %s", config.name(), config.id));
                    //触发配置更新
                    update();
                    break;
                case StateEvent.FAIL_OPEN_ILLEGAL_STATE:
                    logger.error(String.format("Error occurs while referring %s with bean id %s,caused by state is illegal. ", config.name(), config.id));
                    break;
                case StateEvent.FAIL_OPEN:
                    logger.error(String.format("Error occurs while referring %s with bean id %s,caused by %s. ", config.name(), config.id, e.getMessage()), e);
                    break;
                case StateEvent.START_CLOSE:
                    logger.info(String.format("Start unrefering consumer %s with bean id %s", config.name(), config.id));
                    break;
                case StateEvent.SUCCESS_CLOSE:
                    logger.info("Success unrefering consumer " + config.name());
                    break;
            }
        }

        @Override
        protected boolean isClose() {
            return config.isClose(this);
        }

        @Override
        protected boolean isOpened() {
            return config.stateMachine.isOpened(this);
        }

        /**
         * 打开
         *
         * @return CompletableFuture
         */
        public CompletableFuture<Void> open() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            try {
                registry = (config.url != null && !config.url.isEmpty()) ?
                        new RegistryConfig(Constants.FIX_REGISTRY, config.url) :
                        (config.registry != null ? config.registry : RegistryConfig.DEFAULT_REGISTRY_SUPPLIER.get());
                config.validate();
                if (config.registry != registry) {
                    //做一下注册中心验证
                    registry.validate();
                }
                //代理接口
                proxyClass = config.getProxyClass();
                //注册中心地址
                registryUrl = parse(registry);
                String host = getLocalHost(registryUrl.getString(Constants.ADDRESS_OPTION));
                //构造原始URL，调用远程的真实接口名称
                url = new URL(GlobalContext.getString(PROTOCOL_KEY), host, 0, config.getInterfaceTarget(), config.addAttribute2Map());
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
         * @return CompletableFuture
         */
        protected abstract CompletableFuture<Void> doOpen();

        /**
         * 关闭
         *
         * @return CompletableFuture
         */
        public CompletableFuture<Void> close() {
            Parametric parametric = new MapParametric<>(GlobalContext.getContext());
            return close(parametric.getBoolean(Constants.GRACEFULLY_SHUTDOWN_OPTION));
        }

        @Override
        public CompletableFuture<Void> close(boolean gracefully) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> update(final URL newUrl) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            InvokerCaller handler = invocationHandler;
            if (handler == null) {
                State state = config.stateMachine.getState();
                if (state.isOpened()) {
                    handler = invocationHandler;
                    if (handler == null) {
                        throw new RpcException("Consumer config is opening. " + config.name());
                    }
                } else if (state.isClosed()) {
                    throw new RpcException("Consumer config is closed. " + config.name());
                } else if (state.isClosing()) {
                    throw new RpcException("Consumer config is closing. " + config.name());
                } else if (state.isOpening()) {
                    //等待初始化
                    latch.await();
                }
            }
            return handler.invoke(proxy, method, args);
        }

    }

}
