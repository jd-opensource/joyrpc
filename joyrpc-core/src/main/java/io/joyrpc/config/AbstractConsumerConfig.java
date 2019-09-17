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
import io.joyrpc.Plugin;
import io.joyrpc.cluster.candidate.Candidature;
import io.joyrpc.cluster.distribution.ExceptionPredication;
import io.joyrpc.cluster.distribution.LoadBalance;
import io.joyrpc.cluster.distribution.Route;
import io.joyrpc.cluster.distribution.Router;
import io.joyrpc.cluster.event.NodeEvent;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.EventHandler;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.ChannelManagerFactory;
import io.joyrpc.util.Futures;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;

/**
 * 抽象消费者配置
 */
public abstract class AbstractConsumerConfig<T> extends AbstractInterfaceConfig {

    private final static Logger logger = LoggerFactory.getLogger(AbstractConsumerConfig.class);

    /**
     * 注册中心配置，只能配置一个
     */
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
    protected String cluster;

    /**
     * The Loadbalance. 负载均衡
     */
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
    protected String failoverPredication;

    /**
     * channel创建模式
     * shared:共享(默认),unshared:独享
     */
    protected String channelFactory;

    /**
     * 路由规则
     */
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
     * 代理实现类
     */
    protected transient volatile T stub;
    /**
     * 事件监听器
     */
    protected transient List<EventHandler<NodeEvent>> eventHandlers = new ArrayList<>();
    /**
     * 注册中心地址
     */
    protected transient URL registryUrl;

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

    @Override
    protected void validate() {
        super.validate();
        checkExtension(CHANNEL_MANAGER_FACTORY, ChannelManagerFactory.class, "channelFactory", channelFactory);
        checkExtension(CANDIDATURE, Candidature.class, "candidature", candidature);
        checkExtension(SERIALIZATION, Serialization.class, "serialization", serialization);
        checkExtension(LOADBALANCE, LoadBalance.class, "loadbalance", loadbalance);
        checkExtension(ROUTE, Route.class, "cluster", cluster);
        checkExtension(Plugin.ROUTER, Router.class, "router", router);
        checkExtension(EXCEPTION_PREDICATION, ExceptionPredication.class, "failoverPredication", failoverPredication);
        checkFilterConfig(name(), filter, Plugin.CONSUMER_FILTER);
        if (registry != null) {
            registry.validate();
        }
    }

    @Override
    protected void validateAlias() {
        if (alias == null || alias.isEmpty()) {
            throw new InitializationException("Value of \"alias\" is not specified in consumer" +
                    " config with key " + name() + " !", ExceptionCode.CONSUMER_ALIAS_IS_NULL);
        }
        checkNormalWithColon("alias", alias);
    }

    /**
     * 异步引用
     *
     * @return
     */
    public CompletableFuture<T> asyncRefer() {
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
     * 同步引用
     *
     * @return
     */
    public T refer() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        T result = refer(future);
        future.get();
        return result;
    }

    /**
     * 引用一个远程服务
     *
     * @param future 操作结果消费者
     * @return 接口代理类
     */
    public T refer(final CompletableFuture<Void> future) {
        if (stub != null) {
            Futures.complete(future, null);
            return stub;
        }
        try {
            validate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Futures.completeExceptionally(future, e);
            return null;
        }
        logger.info(String.format("Refer consumer config : %s with bean id %s", name(), getId()));
        //同步调用，会在doRefer里面创建好stub
        CompletableFuture<Void> result = switcher.open(f -> {
            //直连，则生成FixRegistry，非直连，使用配置的注册中心，若配置的注册中心为空，则生成默认注册中心
            registry = (url != null && !url.isEmpty()) ?
                    new RegistryConfig(Constants.FIX_REGISTRY, url)
                    : registry != null ? registry : RegistryConfig.DEFAULT_REGISTRY_SUPPLIER.get();
            registryUrl = parse(registry);
            //构造serviceUrl
            serviceUrl = new URL(GlobalContext.getString(PROTOCOL_KEY), getLocalHost(), 0, interfaceClazz, addAttribute2Map());
            //创建等到配置初始化
            waitingConfig = new CompletableFuture<>();
            doRefer(f);
            return f;
        });
        result.whenComplete((v, t) -> {
            if (t == null) {
                logger.info(String.format("Success referring %s with bean id %s", name(), getId()));
                Futures.complete(future, v);
            } else {
                logger.error(String.format("Error occurs while referring %s with bean id %s,caused by. ", name(), getId()), t);
                Futures.completeExceptionally(future, t);
            }
        });
        return stub;
    }

    /**
     * 获取本地地址
     *
     * @return
     */
    protected String getLocalHost() {
        //连接远程地址，获取本地网卡地址
        return getLocalHost(registryUrl.getString(Constants.ADDRESS_OPTION));
    }


    /**
     * 执行引用操作
     *
     * @param future
     * @return
     */
    protected void doRefer(final CompletableFuture<Void> future) {
        future.complete(null);
    }

    /**
     * 注销
     *
     * @return
     */
    public CompletableFuture<Void> unrefer() {
        return switcher.close(f -> {
            doUnRefer(f);
            return f;
        });
    }

    /**
     * 注销引用
     *
     * @param future
     */
    protected void doUnRefer(final CompletableFuture<Void> future) {
        future.complete(null);
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
            return GenericService.class;
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

    /**
     * 设置注册中心
     *
     * @param registries
     */
    protected void setRegistry(final Map<String, RegistryConfig> registries) {
        if (registries == null || registries.isEmpty()) {
            return;
        }
        registry = registries.values().iterator().next();
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


}
