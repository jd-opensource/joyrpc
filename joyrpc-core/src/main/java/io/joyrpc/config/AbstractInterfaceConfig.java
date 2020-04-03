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

import io.joyrpc.annotation.Alias;
import io.joyrpc.cache.CacheFactory;
import io.joyrpc.cache.CacheKeyGenerator;
import io.joyrpc.cluster.Region;
import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.cluster.discovery.registry.Registry;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.config.validator.*;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.Configurator;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.URL;
import io.joyrpc.proxy.ProxyFactory;
import io.joyrpc.util.Shutdown;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.network.Ipv4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.CONFIGURATOR;
import static io.joyrpc.Plugin.PROXY;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.Configurator.CONFIG_ALLOWED;
import static io.joyrpc.context.Configurator.GLOBAL_ALLOWED;
import static io.joyrpc.util.StringUtils.isNotEmpty;
import static io.joyrpc.util.Timer.timer;

/**
 * 抽象接口配置
 */
@ValidateAlias
@ValidateInterface
@ValidateFilter
public abstract class AbstractInterfaceConfig extends AbstractIdConfig {
    private final static Logger logger = LoggerFactory.getLogger(AbstractInterfaceConfig.class);

    /**
     * 不管普通调用和泛化调用，都是设置实际的接口类名称
     */
    protected String interfaceClazz;
    /**
     * 服务别名
     */
    protected String alias;
    /**
     * 过滤器配置，多个用逗号隔开
     */
    protected String filter;
    /**
     * 方法配置，可配置多个
     */
    @Valid
    protected Map<String, MethodConfig> methods;
    /**
     * 是否注册，如果是false只订阅不注册，默认为true
     */
    protected boolean register = true;
    /**
     * 是否订阅服务，默认为true
     */
    protected boolean subscribe = true;
    /**
     * 远程调用超时时间(毫秒)
     */
    protected Integer timeout;
    /**
     * 代理类型
     */
    @ValidatePlugin(extensible = ProxyFactory.class, name = "PROXY", defaultValue = DEFAULT_PROXY)
    protected String proxy;
    /**
     * 自定义参数
     */
    @ValidateParameter
    protected Map<String, String> parameters;
    /**
     * 接口下每方法的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制
     */
    protected Integer concurrency = -1;
    /**
     * 是否开启参数验证(jsr303)
     */
    protected Boolean validation;
    /**
     * 压缩算法，为空则不压缩
     */
    @ValidatePlugin(extensible = Compression.class, name = "COMPRESSION")
    protected String compress;
    /**
     * 是否启动结果缓存
     */
    protected Boolean cache;
    /**
     * 结果缓存插件名称
     */
    @ValidatePlugin(extensible = CacheFactory.class, name = "CACHE")
    protected String cacheProvider;
    /**
     * cache key 生成器
     */
    @ValidatePlugin(extensible = CacheKeyGenerator.class, name = "CACHE_KEY_GENERATOR")
    protected String cacheKeyGenerator;
    /**
     * cache过期时间
     */
    protected Long cacheExpireTime;
    /**
     * cache最大容量
     */
    protected Integer cacheCapacity;
    /**
     * 缓存值是否可空
     */
    protected Boolean cacheNullable;
    /**
     * 外部注入的配置中心
     */
    protected transient Configure configure;
    /**
     * 代理接口类，和T对应，主要针对泛化调用
     */
    protected transient volatile Class interfaceClass;
    /**
     * 名称
     */
    protected transient volatile String name;
    /**
     * 配置事件监听器
     */
    protected transient List<ConfigHandler> configHandlers = new CopyOnWriteArrayList<>();

    public AbstractInterfaceConfig() {
    }

    public AbstractInterfaceConfig(AbstractInterfaceConfig config) {
        super(config);
        this.interfaceClazz = config.interfaceClazz;
        this.alias = config.alias;
        this.filter = config.filter;
        this.methods = config.methods;
        this.register = config.register;
        this.subscribe = config.subscribe;
        this.timeout = config.timeout;
        this.proxy = config.proxy;
        this.parameters = config.parameters;
        this.concurrency = config.concurrency;
        this.validation = config.validation;
        this.compress = config.compress;
        this.cacheProvider = config.cacheProvider;
        this.cacheKeyGenerator = config.cacheKeyGenerator;
        this.cache = config.cache;
        this.cacheExpireTime = config.cacheExpireTime;
        this.cacheCapacity = config.cacheCapacity;
        this.cacheNullable = config.cacheNullable;
        this.name = config.name;
        this.interfaceClass = config.interfaceClass;
        this.configure = config.configure;
    }

    /**
     * 获取接口类
     *
     * @return
     */
    public Class getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class interfaceClass) {
        this.interfaceClass = interfaceClass;
        if (interfaceClass != null) {
            if (interfaceClazz == null || interfaceClazz.isEmpty()) {
                interfaceClazz = interfaceClass.getName();
            }
        }
    }

    /**
     * 获取代理类
     *
     * @return
     */
    public Class<?> getProxyClass() {
        return getInterfaceClass();
    }

    /**
     * 构造接口名字方法
     *
     * @return 唯一标识 string
     */
    public abstract String name();

    public String getInterfaceClazz() {
        return interfaceClazz;
    }

    @Alias("interface")
    public void setInterfaceClazz(String interfaceClazz) {
        this.interfaceClazz = interfaceClazz;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isRegister() {
        return register;
    }

    public void setRegister(boolean register) {
        this.register = register;
    }

    public boolean isSubscribe() {
        return subscribe;
    }

    public void setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getCompress() {
        return compress;
    }

    public void setCompress(String compress) {
        this.compress = compress;
    }

    public void setValidation(Boolean validation) {
        this.validation = validation;
    }

    public Boolean getValidation() {
        return validation;
    }

    /**
     * add methods.
     *
     * @param methods the methods
     */
    public void addMethods(List<MethodConfig> methods) {
        if (this.methods == null) {
            this.methods = new ConcurrentHashMap<>();
        }
        if (methods != null) {
            for (MethodConfig config : methods) {
                this.methods.put(config.getName(), config);
            }
        }
    }

    /**
     * set methods.
     *
     * @param methods
     */
    public void setMethods(List<MethodConfig> methods) {
        if (this.methods == null) {
            this.methods = new ConcurrentHashMap<>();
        } else {
            this.methods.clear();
        }
        if (methods != null) {
            for (MethodConfig config : methods) {
                this.methods.put(config.getName(), config);
            }
        }
    }

    /**
     * Sets parameter.
     *
     * @param key   the key
     * @param value the value
     */
    public void setParameter(final String key, final String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        if (key == null) {
            return;
        } else if (value == null) {
            parameters.remove(key);
        } else {
            parameters.put(key, value);
        }
    }

    /**
     * Sets parameter.
     *
     * @param key   the key
     * @param value the value
     */
    public void setParameter(final String key, final Number value) {
        setParameter(key, value == null ? null : value.toString());
    }

    /**
     * Gets parameter.
     *
     * @param key the key
     * @return the value
     */
    public String getParameter(final String key) {
        return parameters == null || key == null ? null : parameters.get(key);
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getCacheProvider() {
        return cacheProvider;
    }

    public void setCacheProvider(String cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    public String getCacheKeyGenerator() {
        return cacheKeyGenerator;
    }

    public void setCacheKeyGenerator(String cacheKeyGenerator) {
        this.cacheKeyGenerator = cacheKeyGenerator;
    }

    public Boolean getCache() {
        return cache;
    }

    public void setCache(Boolean cache) {
        this.cache = cache;
    }

    public Long getCacheExpireTime() {
        return cacheExpireTime;
    }

    public void setCacheExpireTime(Long cacheExpireTime) {
        this.cacheExpireTime = cacheExpireTime;
    }

    public Integer getCacheCapacity() {
        return cacheCapacity;
    }

    public void setCacheCapacity(Integer cacheCapacity) {
        this.cacheCapacity = cacheCapacity;
    }

    public Boolean getCacheNullable() {
        return cacheNullable;
    }

    public void setCacheNullable(Boolean cacheNullable) {
        this.cacheNullable = cacheNullable;
    }

    public Configure getConfigure() {
        return configure;
    }

    public void setConfigure(Configure configure) {
        this.configure = configure;
    }

    /**
     * 添加事件监听器
     *
     * @param handler
     */
    public void addEventHandler(final ConfigHandler handler) {
        if (handler != null) {
            configHandlers.add(handler);
        }
    }

    /**
     * 移除事件监听器
     *
     * @param handler
     */
    public void removeEventHandler(final ConfigHandler handler) {
        if (handler != null) {
            configHandlers.remove(handler);
        }
    }

    /**
     * 获取代理工厂类
     *
     * @return
     */
    protected ProxyFactory getProxyFactory() {
        return PROXY.getOrDefault(proxy == null || proxy.isEmpty() ? DEFAULT_PROXY : proxy);
    }

    /**
     * 根据远端检查获取本地出口地址
     *
     * @param remoteUrls
     */
    protected static String getLocalHost(String remoteUrls) {
        InetSocketAddress remote = null;
        if (!remoteUrls.isEmpty()) {
            int nodeEnd = remoteUrls.indexOf(",");
            int shardEnd = remoteUrls.indexOf(";");
            int end = Math.min(nodeEnd, shardEnd);
            end = end > 0 ? end : Math.max(nodeEnd, shardEnd);
            String delimiter = end == nodeEnd ? "," : ";";
            String remoteUrlString = end > 0 ? remoteUrls.substring(0, remoteUrls.indexOf(delimiter)) : remoteUrls;
            URL remoteUrl = URL.valueOf(remoteUrlString, GlobalContext.getString(PROTOCOL_KEY));
            remote = new InetSocketAddress(remoteUrl.getHost(), remoteUrl.getPort());
            if ("http".equals(remoteUrl.getProtocol()) || "https".equals(remoteUrl.getProtocol())) {
                InetAddress address = remote.getAddress();
                String ip = address.getHostAddress();
                int port = remote.getPort() == 0 ? 80 : remote.getPort();
                remote = new InetSocketAddress(ip, port);
            }
        }
        return Ipv4.getLocalIp(remote);
    }

    /**
     * 获取本地服务地址
     *
     * @param config 服务配置
     * @param remote 注册中心远程地址，用于检测本地出口地址
     * @return
     */
    protected static ProviderConfig.ServerAddress getAddress(final ServerConfig config, final String remote) {
        String host;
        String bindIp = null;
        if (Ipv4.isLocalHost(config.getHost())) {
            //拿到本机地址
            host = getLocalHost(remote);
            //绑定地址
            bindIp = "0.0.0.0";
        } else {
            host = config.getHost();
        }
        int port = config.getPort() == null ? PORT_OPTION.getValue() : config.getPort();
        return new ProviderConfig.ServerAddress(host, bindIp, port);
    }

    /**
     * 获取注册中心地址
     *
     * @param configs
     * @return
     */
    protected static List<URL> parse(final List<RegistryConfig> configs) {
        List<URL> result = new ArrayList<>(configs.size());
        for (RegistryConfig config : configs) {
            result.add(parse(config));
        }
        return result;
    }

    /**
     * 获取注册中心地址
     *
     * @param config 注册中心配置
     * @return url
     */
    protected static URL parse(final RegistryConfig config) {
        Map<String, String> parameters = new HashMap<>();
        //带上全局参数
        GlobalContext.getContext().forEach((key, value) -> parameters.put(key, value.toString()));
        if (isNotEmpty(config.getId())) {
            parameters.put(REGISTRY_NAME_KEY, config.getId());
        }
        //设置注册中心资源地址参数
        String address = config.getAddress();
        if (isNotEmpty(address)) {
            parameters.put(Constants.ADDRESS_OPTION.getName(), address);
        }
        //regConfig添加参数
        config.addAttribute2Map(parameters);
        //返回注册中心url
        return new URL(config.getRegistry(), Ipv4.ANYHOST, 0, parameters);
    }

    @Override
    protected Map<String, String> addAttribute2Map(final Map<String, String> params) {
        super.addAttribute2Map(params);
        addElement2Map(params, Constants.INTERFACE_CLAZZ_OPTION, interfaceClazz);
        addElement2Map(params, Constants.ALIAS_OPTION, alias);
        addElement2Map(params, Constants.FILTER_OPTION, filter);
        //register与subscribe默认值为true，防止url过长，为true的情况，不加入params
        if (!register) {
            addElement2Map(params, Constants.REGISTER_OPTION, register);
        }
        if (!subscribe) {
            addElement2Map(params, Constants.SUBSCRIBE_OPTION, subscribe);
        }
        addElement2Map(params, Constants.TIMEOUT_OPTION, timeout);
        addElement2Map(params, Constants.PROXY_OPTION, proxy);
        addElement2Map(params, Constants.VALIDATION_OPTION, validation);
        addElement2Map(params, Constants.COMPRESS_OPTION, compress);
        addElement2Map(params, Constants.CONCURRENCY_OPTION, concurrency);
        addElement2Map(params, Constants.CACHE_OPTION, cache);
        addElement2Map(params, Constants.CACHE_EXPIRE_TIME_OPTION, cacheExpireTime);
        addElement2Map(params, Constants.CACHE_PROVIDER_OPTION, cacheProvider);
        addElement2Map(params, Constants.CACHE_KEY_GENERATOR_OPTION, cacheKeyGenerator);
        addElement2Map(params, Constants.CACHE_CAPACITY_OPTION, cacheCapacity);
        addElement2Map(params, Constants.CACHE_NULLABLE_OPTION, cacheNullable);

        if (null != parameters) {
            parameters.forEach((k, v) -> addElement2Map(params, k, v));
        }
        if (null != methods) {
            methods.forEach((k, v) -> v.addAttribute2Map(params));
        }
        return params;
    }

    /**
     * 广播配置变更消息
     *
     * @param event
     */
    protected void publish(final ConfigEvent event) {
        if (event != null && !configHandlers.isEmpty()) {
            for (ConfigHandler handler : configHandlers) {
                try {
                    handler.handle(event);
                } catch (Throwable e) {
                    logger.error("Error occurs while publish config event. caused by " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 是否在关闭
     *
     * @return
     */
    protected boolean isClose() {
        return Shutdown.isShutdown();
    }

    /**
     * 抽象的控制器
     *
     * @param <T>
     */
    protected abstract static class AbstractController<T extends AbstractInterfaceConfig> {
        //TODO 检查别名，订阅URL
        /**
         * 配置
         */
        protected T config;
        /**
         * 服务原始地址
         */
        protected URL url;
        /**
         * 服务地址
         */
        protected volatile URL serviceUrl;
        /**
         * 订阅的URL
         */
        protected volatile URL subscribeUrl;
        /**
         * consumer bean 计数
         */
        protected CompletableFuture<URL> waitingConfig = new CompletableFuture<>();
        /**
         * 实际的配置中心，如果没有注入配置中心则使用注册中心的配置
         */
        protected Configure configureRef;
        /**
         * 自身的配置变化监听器
         */
        protected ConfigHandler configHandler = this::onConfigEvent;
        /**
         * 更新配置的拥有者
         */
        protected final AtomicBoolean updateOwner = new AtomicBoolean(false);
        /**
         * 事件队列
         */
        protected AtomicReference<Map<String, String>> events = new AtomicReference<>();
        /**
         * 更新任务名称2
         */
        protected String updateTask;
        /**
         * 配置属性
         */
        protected Map<String, String> attributes;
        /**
         * 数据版本
         */
        protected long version = Long.MIN_VALUE;

        public AbstractController(T config) {
            this.config = config;
            this.updateTask = "UpdateTask-" + config.name();
        }

        /**
         * 中断
         */
        public void broken() {
            if (!waitingConfig.isDone()) {
                waitingConfig.completeExceptionally(new InitializationException("Unexport interrupted waiting config."));
            }
        }

        /**
         * 判断是否关闭
         *
         * @return
         */
        protected boolean isClose() {
            return config.isClose();
        }

        /**
         * 是否打开了
         *
         * @return
         */
        protected abstract boolean isOpened();

        /**
         * 是否修改
         *
         * @param updates
         * @return
         */
        protected boolean isChanged(final Map<String, String> updates) {
            if (updates.isEmpty()) {
                return attributes != null && !attributes.isEmpty();
            } else {
                return attributes == null || attributes.size() != updates.size() || !updates.equals(attributes);
            }
        }

        /**
         * 配置变更事件，每次都是全量更新
         *
         * @param event
         */
        protected void onConfigEvent(final ConfigEvent event) {
            if (isClose()) {
                return;
            }
            if (event.getVersion() <= version) {
                //丢弃过期数据
                return;
            }
            //每次都是全量更新
            Map<String, String> updates = event.getDatum();
            if (!waitingConfig.isDone()) {
                //触发URL变化
                logger.info("Success subscribing global config " + config.name());
                attributes = updates;
                //加上全局配置和接口配置，再添加实例配置
                waitingConfig.complete(configure(updates));
            } else if (!isChanged(updates)) {
                //没有变化
                return;
            } else {
                //有变化，则
                String alias = updates.get(Constants.ALIAS_OPTION.getName());
                if (alias == null || alias.isEmpty()) {
                    return;
                }
                //备份一份，防止添加参数后前面的判断是否修改失效。
                attributes = new HashMap<>(updates);
                //添加计数器参数，用于生成唯一的Invoker名称
                updates.put(Constants.COUNTER, String.valueOf(event.getVersion()));
                events.set(updates);
                if (isOpened()) {
                    //只有在打开的情况下在触发更新，避免其它情况并发
                    update();
                }
            }
            config.publish(event);
        }

        /**
         * 链
         *
         * @param source
         * @param target
         * @param consumer
         */
        protected <U> void chain(final CompletableFuture<U> source, final CompletableFuture<Void> target, final Consumer<U> consumer) {
            source.whenComplete((v, e) -> {
                if (e != null) {
                    target.completeExceptionally(e);
                } else if (isClose()) {
                    target.completeExceptionally(new InitializationException("Status is illegal."));
                } else if (consumer != null) {
                    consumer.accept(v);
                } else {
                    target.complete(null);
                }
            });
        }

        /**
         * 更新配置
         */
        protected void update() {
            //判断是否启动了更新任务
            if (!isClose() && events.get() != null && updateOwner.compareAndSet(false, true)) {
                //下一跳进行更新
                timer().add(updateTask, SystemClock.now() + 200, () -> {
                    //获取最后的事件
                    Map<String, String> updates = events.get();
                    if (!isClose() && updates != null) {
                        //使用原始URL添加变更
                        update(configure(updates)).whenComplete((v, t) -> {
                            events.compareAndSet(updates, null);
                            updateOwner.set(false);
                            if (!isClose()) {
                                if (t != null) {
                                    logger.error(String.format("Error occurs while updating attribute. caused by %s. %s", t.getMessage(), config.name()));
                                }
                                //再次判断是否更新
                                update();
                            }
                        });
                    }
                });
            }
        }

        /**
         * 配置发生变化
         *
         * @param newUrl 变化的URL
         */
        protected abstract CompletableFuture<Void> update(final URL newUrl);

        /**
         * 订阅，打开注册中心
         *
         * @param registry
         * @param subscribed
         */
        protected CompletableFuture<Void> subscribe(final Registry registry, final AtomicBoolean subscribed) {
            final CompletableFuture<Void> future = new CompletableFuture<>();
            if (!config.subscribe && !config.register) {
                future.complete(null);
            } else if (!config.register && configureRef != null && configureRef != registry) {
                //不注册，需要订阅配置，并且有注入配置中心
                if (subscribed.compareAndSet(false, true)) {
                    subscribeUrl = buildSubscribedUrl(configureRef, serviceUrl);
                    configureRef.subscribe(subscribeUrl, configHandler);
                }
                future.complete(null);
            } else {
                registry.open().whenComplete((v, t) -> {
                    if (t != null) {
                        future.completeExceptionally(new InitializationException(
                                String.format("Registry open error. %s", registry.getUrl().toString(false, false))));
                    } else if (subscribed.compareAndSet(false, true)) {
                        //保存订阅注册中心
                        if (configureRef == null) {
                            configureRef = config.configure == null ? registry : config.configure;
                        }
                        //订阅配置
                        if (config.subscribe) {
                            subscribeUrl = buildSubscribedUrl(configureRef, serviceUrl);
                            logger.info("Start subscribing global config " + config.name());
                            configureRef.subscribe(subscribeUrl, configHandler);
                        }
                        future.complete(null);
                    }
                });
            }
            return future;
        }

        /**
         * 判断别名是否发生变化，重新订阅配置
         *
         * @param newUrl 新的订阅URL
         * @param force  是否要强制修改
         */
        protected boolean resubscribe(final URL newUrl, final boolean force) {
            URL oldUrl = subscribeUrl;
            //对象相同，没有订阅
            if (configureRef == null || oldUrl == newUrl) {
                return false;
            }
            //连接注册中心
            boolean oldSubscribe = oldUrl == null ? false : oldUrl.getBoolean(Constants.SUBSCRIBE_OPTION);
            String oldAlias = oldUrl == null ? null : oldUrl.getString(Constants.ALIAS_OPTION.getName());
            boolean newSubscribe = newUrl == null ? false : newUrl.getBoolean(Constants.SUBSCRIBE_OPTION);
            String newAlias = newUrl == null ? null : newUrl.getString(Constants.ALIAS_OPTION.getName());
            if (newSubscribe && oldSubscribe && !force && Objects.equals(oldAlias, newAlias)) {
                //都需要订阅，分组没有变化
                return false;
            } else if (newSubscribe) {
                //当前要订阅
                if (oldSubscribe) {
                    //原来也订阅了
                    configureRef.unsubscribe(oldUrl, configHandler);
                }
                logger.info("Start resubscribing config " + config.name());
                configureRef.subscribe(newUrl, configHandler);
                subscribeUrl = newUrl;
                return true;
            } else if (oldSubscribe) {
                configureRef.unsubscribe(oldUrl, configHandler);
                subscribeUrl = null;
                return true;
            } else {
                subscribeUrl = null;
                return false;
            }
        }

        /**
         * 动态配置
         *
         * @param updates 当前动态配置
         * @return
         */
        protected URL configure(final Map<String, String> updates) {
            Map<String, String> result = new HashMap<>(32);
            //真实配置的接口名称
            String path = url.getPath();
            //本地全局静态配置
            Configurator.update(GlobalContext.getContext(), result, GLOBAL_ALLOWED);
            //注册中心下发的全局动态配置，主要是一些开关
            Configurator.update(GlobalContext.getInterfaceConfig(Constants.GLOBAL_SETTING), result, GLOBAL_ALLOWED);
            //本地接口静态配置,数据中心和区域在注册中心里面会动态更新到全局上下文里面
            Map<String, String> parameters = url.getParameters();
            parameters.remove(Region.DATA_CENTER);
            parameters.remove(Region.REGION);
            result.putAll(parameters);
            //注册中心下发的接口动态配置
            Configurator.update(GlobalContext.getInterfaceConfig(path), result, GLOBAL_ALLOWED);
            //调用插件
            CONFIGURATOR.extensions().forEach(o -> Configurator.update(o.configure(path), result, CONFIG_ALLOWED));
            //动态配置
            Configurator.update(updates, result, CONFIG_ALLOWED);

            return new URL(url.getProtocol(), url.getUser(), url.getPassword(), url.getHost(), url.getPort(), path, result);
        }

        /**
         * 获取注册的URL
         *
         * @param url
         * @return
         */
        protected URL buildRegisteredUrl(final Registry registry, final URL url) {
            return registry == null ? null : registry.normalize(url);
        }

        /**
         * 获取订阅配置的URL
         *
         * @param url
         * @return
         */
        protected URL buildSubscribedUrl(final Configure configure, final URL url) {
            return configure == null ? null : configure.normalize(url);
        }

        public URL getServiceUrl() {
            return serviceUrl;
        }
    }

}
