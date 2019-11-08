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

import io.joyrpc.cache.CacheFactory;
import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.exception.IllegalConfigureException;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.cache.CacheKeyGenerator;
import io.joyrpc.proxy.ProxyFactory;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.StringUtils;
import io.joyrpc.util.Switcher;
import io.joyrpc.util.network.Ipv4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.PROTOCOL_KEY;
import static io.joyrpc.constants.Constants.REGISTRY_NAME_KEY;

/**
 * 抽象接口配置
 */
public abstract class AbstractInterfaceConfig extends AbstractIdConfig {
    private final static Logger logger = LoggerFactory.getLogger(AbstractInterfaceConfig.class);

    /*-------------配置项开始----------------*/
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
    protected String proxy;

    /**
     * 自定义参数
     */
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
    protected String compress;

    /**
     * 是否启动结果缓存
     */
    protected Boolean cache;

    /**
     * 结果缓存插件名称
     */
    protected String cacheProvider;

    /**
     * cache key 生成器
     */
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

    /*-------------配置项结束----------------*/

    /**
     * 代理接口类，和T对应，主要针对泛化调用
     */
    protected transient volatile Class interfaceClass;

    /**
     * 名称
     */
    protected transient volatile String name;

    /**
     * 服务地址
     */
    protected transient URL serviceUrl;

    /**
     * 配置事件监听器
     */
    protected transient List<ConfigHandler> configHandlers;

    /**
     * 开关
     */
    protected transient Switcher switcher = new Switcher();

    /**
     * 配置变更计数器，每次发送变化URL上的counter计数器加一
     */
    protected transient AtomicLong counter = new AtomicLong(0);

    /**
     * consumer bean 计数
     */
    protected transient CompletableFuture<URL> waitingConfig;

    /**
     * 订阅配置的注销中心
     */
    protected transient Configure configure;

    /**
     * 自身的配置变化监听器
     */
    protected transient ConfigHandler configHandler = this::onConfigEvent;


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
    }

    /**
     * 获取接口类
     *
     * @return
     */
    public Class getInterfaceClass() {
        if (interfaceClass == null) {
            if (interfaceClazz == null || interfaceClazz.isEmpty()) {
                throw new IllegalConfigureException("interfaceClazz", "null",
                        "interfaceClazz must be not null", ExceptionCode.COMMON_NOT_RIGHT_INTERFACE);
            }
            try {
                interfaceClass = ClassUtils.forName(interfaceClazz);
            } catch (ClassNotFoundException e) {
                throw new IllegalConfigureException(e.getMessage(), ExceptionCode.COMMON_CLASS_NOT_FOUND);
            }
        }
        return interfaceClass;
    }

    public void setInterfaceClass(Class interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    /**
     * 获取代理类
     *
     * @return
     */
    public Class getProxyClass() {
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

    public void setInterfaceClazz(String interfaceClazz) {
        this.interfaceClazz = interfaceClazz;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Map<String, MethodConfig> getMethods() {
        return methods;
    }

    public void setMethods(Map<String, MethodConfig> methods) {
        this.methods = methods;
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

    public Boolean isValidation() {
        return validation;
    }

    /**
     * Sets methods.
     *
     * @param methods the methods
     */
    public void setMethods(List<MethodConfig> methods) {
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
     * 得到方法名对应的方法配置
     *
     * @param methodName 方法名，不支持重载
     * @return method config
     */
    public MethodConfig getMethodConfig(final String methodName) {
        return methods == null || methodName == null ? null : methods.get(methodName);
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
        setParameter(key, (String) (value == null ? null : value.toString()));
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

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

    /**
     * 添加事件监听器
     *
     * @param handler
     */
    public void addEventHandler(final ConfigHandler handler) {
        if (handler != null) {
            if (configHandlers == null) {
                configHandlers = new ArrayList<>();
            }
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
            if (configHandlers != null) {
                configHandlers.add(handler);
            }
        }
    }

    @Override
    protected void validate() {
        super.validate();
        //验证别名
        validateAlias();
        //验证方法配置
        if (methods != null) {
            for (Map.Entry<String, MethodConfig> entry : methods.entrySet()) {
                entry.getValue().validate();
            }
        }
        //验证扩展点
        checkExtension(CACHE, CacheFactory.class, "cacheProvider", cacheProvider);
        checkExtension(CACHE_KEY_GENERATOR, CacheKeyGenerator.class, "cacheKeyGenerator", cacheKeyGenerator);
        checkExtension(COMPRESSION, Compression.class, "compress", compress);
        checkExtension(PROXY, ProxyFactory.class, "proxy", proxy);
    }

    /**
     * 验证别名，消费组可以配置多个别名，可以运行有逗号，使用方法便于覆盖
     */
    protected void validateAlias() {
        if (alias == null || alias.isEmpty()) {
            throw new IllegalConfigureException("alias can not be empty.", ExceptionCode.COMMON_VALUE_ILLEGAL);
        }
        checkNormalWithColon("alias", alias);
    }


    /**
     * 获取代理工厂类
     *
     * @return
     */
    protected ProxyFactory getProxyFactory() {
        String name = proxy == null || proxy.isEmpty() ? Constants.PROXY_OPTION.getValue() : proxy;
        ProxyFactory proxyFactory = PROXY.get(name);
        if (proxyFactory == null) {
            proxyFactory = PROXY.get();
            if (proxyFactory == null) {
                logger.warn(String.format("proxyFactory %s is not found. use default.", name));
                throw new InitializationException("there is not any proxyFactory implement.");
            }
        }
        return proxyFactory;
    }

    /**
     * 根据远端检查获取本地出口地址
     *
     * @param remoteUrls
     */
    protected String getLocalHost(String remoteUrls) {
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
     * 获取注册中心地址
     *
     * @param config
     * @return
     */
    protected URL parse(final RegistryConfig config) {
        Map<String, String> parameters = new HashMap<>();

        if (StringUtils.isNotEmpty(config.getId())) {
            parameters.put(REGISTRY_NAME_KEY, config.getId());
        }
        //设置注册中心资源地址参数
        String address = config.getAddress();
        if (StringUtils.isNotEmpty(address)) {
            parameters.put(Constants.ADDRESS_OPTION.getName(), address);
        }
        //regConfig添加参数
        config.addAttribute2Map(parameters);
        //返回注册中心url
        return new URL(config.getRegistry(), Ipv4.ANYHOST, 0, parameters);
    }

    /**
     * 获取注册中心地址
     *
     * @param configs
     * @return
     */
    protected List<URL> parse(final List<RegistryConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            throw new IllegalConfigureException("Value of registry is empty, interfaceClazz: " + interfaceClazz + ".", ExceptionCode.REGISTRY_NOT_CONFIG);
        }
        List<URL> result = new ArrayList<>(configs.size());
        for (RegistryConfig config : configs) {
            result.add(parse(config));
        }
        return result;
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
     * 配置发生变化
     *
     * @param newUrl  新的服务URL
     * @param version 版本
     */
    protected void onChanged(final URL newUrl, final long version) {

    }

    /**
     * 判断别名是否发生变化，重新订阅配置
     *
     * @param oldUrl
     * @param newUrl
     */
    protected void resubscribe(final URL oldUrl, final URL newUrl) {
        //对象相同，没有订阅
        if (oldUrl == newUrl || configure == null) {
            return;
        }
        //连接注册中心
        boolean subscribe = oldUrl.getBoolean(Constants.SUBSCRIBE_OPTION);
        String oldAlias = oldUrl.getString(Constants.ALIAS_OPTION.getName());
        String newAlias = newUrl.getString(Constants.ALIAS_OPTION.getName());
        if (subscribe && !Objects.equals(oldAlias, newAlias)) {
            //动态配置修改了别名，需要重新订阅
            configure.unsubscribe(oldUrl, configHandler);
            configure.subscribe(newUrl, configHandler);
        }
    }

    /**
     * 事件变更
     *
     * @param event
     */
    public void onConfigEvent(final ConfigEvent event) {
        if (!switcher.isOpened()) {
            return;
        }
        try {
            Map<String, String> updates = event.changed(Constants.EXCLUDE_CHANGED_ATTR_MAP::containsKey,
                    o -> serviceUrl.getString(o));
            if (waitingConfig != null && !waitingConfig.isDone()) {
                //触发URL变化
                switcher.writer().run(() -> waitingConfig.complete(serviceUrl.add(updates)));
            } else if (updates == null || updates.isEmpty()) {
                return;
            } else {
                String alias = updates.get(Constants.ALIAS_OPTION.getName());
                if (alias == null || alias.isEmpty()) {
                    return;
                }
                //添加计数器参数，用于生成
                long version = counter.incrementAndGet();
                updates.put(Constants.COUNTER, String.valueOf(version));
                //服务端可以切换别名
                switcher.writer().run(() -> onChanged(serviceUrl.add(updates), version));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (configHandlers != null && !configHandlers.isEmpty()) {
                configHandlers.forEach(h -> h.handle(event));
            }
        }
    }
}
