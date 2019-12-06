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

import io.joyrpc.annotation.Export;
import io.joyrpc.cluster.discovery.registry.Registry;
import io.joyrpc.cluster.discovery.registry.RegistryFactory;
import io.joyrpc.config.validator.InterfaceValidator;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.exception.IllegalConfigureException;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.Futures;
import io.joyrpc.util.StringUtils;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.network.Ipv4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;


/**
 * 服务发布者配置
 */
public class ProviderConfig<T> extends AbstractInterfaceConfig implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(ProviderConfig.class);

    /**
     * 注册中心配置，可配置多个
     */
    protected List<RegistryConfig> registry;
    /**
     * 接口实现类引用
     */
    protected transient T ref;
    /**
     * 配置的协议列表
     */
    protected ServerConfig serverConfig;
    /**
     * 服务发布延迟,单位毫秒，默认0，配置为-1代表spring加载完毕（通过spring才生效）
     */
    protected Integer delay;
    /**
     * 权重
     */
    protected Integer weight;
    /**
     * 包含的方法
     */
    protected String include;
    /**
     * 不发布的方法列表，逗号分隔
     */
    protected String exclude;
    /**
     * 是否动态注册，默认为true，配置为false代表不主动发布，需要到管理端进行上线操作
     */
    protected Boolean dynamic;

    protected Boolean enableValidator = true;

    /**
     * 接口验证器插件
     */
    protected String interfaceValidator;

    /**
     * 已发布
     */
    protected transient volatile Exporter<T> exporter;
    /**
     * 注册中心URL
     */
    protected transient List<URL> registryUrls;
    /**
     * 注册中心
     */
    protected transient List<Registry> registries = new ArrayList<>(3);

    /**
     * 预热插件
     */
    protected String warmup;

    @Override
    protected void validate() {
        super.validate();
        //验证接口
        if (enableValidator) {
            validateInterface(getProxyClass());
        }
        checkFilterConfig(name(), filter, PROVIDER_FILTER);
        //验证注册中心
        if (registry != null) {
            for (RegistryConfig config : registry) {
                config.validate();
            }
        }
        //验证服务配置
        if (serverConfig == null) {
            throw new InitializationException("Value of \"server\" is null in provider" +
                    " config with interfaceClazz " + interfaceClazz + " !", ExceptionCode.PROVIDER_SERVER_IS_NULL);
        }
        serverConfig.validate();
        // 检查注入的ref是否接口实现类
        if (!getProxyClass().isInstance(ref)) {
            throw new IllegalConfigureException("provider.ref", ref.getClass().getName(),
                    "This is not an instance of " + interfaceClazz
                            + " in provider config with key " + name() + " !", ExceptionCode.PROVIDER_REF_NO_FOUND);
        }
        checkExtension(WARMUP, Warmup.class, "warmup", warmup);
    }

    /**
     * 验证接口规范
     *
     * @param clazz
     */
    protected void validateInterface(final Class clazz) {
        //确保是接口
        if (!clazz.isInterface()) {
            throw new IllegalConfigureException("interfaceClazz", clazz.getName(),
                    "interfaceClazz must be a interface", ExceptionCode.COMMON_NOT_RIGHT_INTERFACE);
        }
        if (StringUtils.isBlank(interfaceValidator)) {
            interfaceValidator = Constants.INTERFACE_VALIDATOR_OPTION.get();
        }

        if (interfaceValidator != null && !interfaceValidator.isEmpty()) {
            InterfaceValidator validator = checkExtension(INTERFACE_VALIDATOR, InterfaceValidator.class,
                    "interfaceValidator", interfaceValidator);
            validator.validate(clazz);
        }
    }


    @Override
    protected void validateAlias() {
        if (alias == null || alias.isEmpty()) {
            throw new InitializationException("The alias must not be empty of provider " + name(),
                    ExceptionCode.PROVIDER_ALIAS_IS_NULL);
        }
        checkNormalWithColon("alias", alias);
    }

    /**
     * 创建服务并且开启
     *
     * @return
     */
    public CompletableFuture<Void> exportAndOpen() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        export().whenComplete((v, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
            } else {
                Futures.chain(open(), future);
            }
        });
        return future;
    }

    /**
     * 订阅全局配置并创建服务，不开启服务
     */
    public CompletableFuture<Void> export() {
        //验证
        try {
            validate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Futures.completeExceptionally(e);
        }
        //发布服务
        return switcher.open(f -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            // 延迟加载,单位毫秒
            if (null == delay || delay <= 0) {
                doExport(future);
            } else {
                Thread thread = new Thread(() -> {
                    try {
                        Thread.sleep(delay);
                        //run会检查打开状态
                        if (!switcher.writer().run(() -> doExport(future))) {
                            f.completeExceptionally(new InitializationException("illegal state. " + name()));
                        }
                    } catch (InterruptedException e) {
                        f.completeExceptionally(new InitializationException("InterruptedException " + name()));
                    }
                });
                thread.setDaemon(true);
                thread.setName("DelayExportThread");
                thread.start();
            }
            future.whenComplete((v, t) -> {
                if (t != null) {
                    logger.error(String.format("Error occurs while export %s with bean id %s,caused by. ", name(), getId()), t);
                    Futures.chain(unexport(), f);
                } else {
                    warmup();
                    f.complete(v);
                    logger.info(String.format("Success exporting %s with bean id %s", name(), getId()));
                }
            });
            return f;
        });
    }

    /**
     * 开启服务
     */
    public CompletableFuture<Void> open() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        exporter.open().whenComplete((v, t) -> {
            if (t == null) {
                future.complete(null);
            } else {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * 取消发布
     */
    public CompletableFuture<Void> unexport() {
        return switcher.close(f -> {
            doUnexport(f);
            return f;
        });
    }

    /**
     * 关闭
     *
     * @param future
     */
    protected void doUnexport(CompletableFuture<Void> future) {
        if (exporter != null) {
            logger.info(String.format("Unexport provider config : %s with bean id %s", name(), getId()));
            exporter.close();
            exporter = null;
            configureRef = null;
        }
        if (!waitingConfig.isDone()) {
            waitingConfig.completeExceptionally(new InitializationException("Unexport interrupted waiting config."));
        }
        future.complete(null);
    }

    /**
     * 输出服务
     */
    protected void doExport(final CompletableFuture<Void> future) {
        //创建注册中心
        registry = registry != null ? registry : Arrays.asList(RegistryConfig.DEFAULT_REGISTRY_SUPPLIER.get());
        registryUrls = parse(registry);
        String host;
        String bindIp = null;
        if (Ipv4.isLocalHost(serverConfig.getHost())) {
            //拿到本机地址
            host = getLocalHost(registryUrls.get(0).getString(ADDRESS_OPTION));
            bindIp = "0.0.0.0";
        } else {
            host = serverConfig.getHost();
        }
        int port = serverConfig.getPort() == null ? PORT_OPTION.getValue() : serverConfig.getPort();

        //创建等到配置初始化
        waitingConfig = new CompletableFuture<>();

        //生成注册的URL
        Map<String, String> map = addAttribute2Map(serverConfig.addAttribute2Map());
        if (bindIp != null) {
            map.put(BIND_IP_KEY, bindIp);
        }
        serviceUrl = new URL(GlobalContext.getString(PROTOCOL_KEY), host, port, interfaceClazz, map);

        //构造注册中心对象
        RegistryFactory factory;
        Registry registry;
        String name;
        URL registryURL;
        //订阅成功标识
        AtomicBoolean subscribed = new AtomicBoolean(false);

        //过滤掉重复的注册中心
        Set<Registry> unique = new HashSet<>(3);
        //多注册中心
        for (int i = 0; i < registryUrls.size(); i++) {
            //注册中心工厂插件
            registryURL = registryUrls.get(i);
            name = registryURL.getProtocol();
            factory = REGISTRY.get(name);
            if (factory == null) {
                if (!register && !subscribe) {
                    factory = REGISTRY.get("memory");
                } else {
                    future.completeExceptionally(new InitializationException(String.format("Registry factory is not found. %s", name)));
                    return;
                }
            }
            //获取注册中心
            registry = factory.getRegistry(registryURL);
            if (unique.add(registry)) {
                //既不注册也不订阅，不执行open操作
                CompletableFuture<Void> f = !register && !subscribe ? CompletableFuture.completedFuture(null) : registry.open();
                if (subscribe) {
                    f.whenComplete(new RegistryConsumer(registry, subscribed, future));
                }
                registries.add(registry);
            }
        }
        //不需要订阅
        if (!subscribe) {
            waitingConfig.complete(serviceUrl);
        }
        waitingConfig.whenComplete((url, e) -> {
            if (e != null) {
                future.completeExceptionally(e);
            } else {
                //检查动态配置是否修改了别名，需要重新订阅
                resubscribe(serviceUrl, url);
                try {
                    exporter = InvokerManager.export(this, configureRef, subscribeUrl);
                    future.complete(null);
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            }
        });
    }

    @Override
    protected void onChanged(final URL newUrl, final long version) {
        Exporter<T> oldExporter = exporter;
        exporter.close().whenComplete((v, t) -> {
            Exporter<T> newExporter = InvokerManager.export(this, newUrl, configureRef, subscribeUrl);
            newExporter.open().whenComplete((r, ex) -> {
                if (r == null) {
                    //异步并发，需要进行版本比较
                    synchronized (counter) {
                        long newVersion = counter.get();
                        if (newVersion == version) {
                            //检查动态配置是否修改了别名，需要重新订阅
                            resubscribe(serviceUrl, newUrl);
                            exporter = newExporter;
                            serviceUrl = newUrl;
                            oldExporter.close(true);
                        } else {
                            logger.info(String.format("Discard out-of-date config. old=%d, current=%d", version, newVersion));
                            newExporter.close();
                        }
                    }
                } else {
                    logger.error("Error occurs while exporting after attribute changed", t);
                    newExporter.close();
                }
            });
        });
    }

    @Override
    protected Map<String, String> addAttribute2Map(final Map<String, String> params) {
        super.addAttribute2Map(params);
        addElement2Map(params, Constants.WEIGHT_OPTION, weight);

        addElement2Map(params, Constants.DYNAMIC_OPTION, dynamic);
        addElement2Map(params, Constants.DELAY_OPTION, delay);
        addElement2Map(params, Constants.ROLE_OPTION, Constants.SIDE_PROVIDER);
        addElement2Map(params, Constants.TIMESTAMP_KEY, String.valueOf(SystemClock.now()));
        addElement2Map(params, Constants.WARMUP_OPTION, warmup);
        addElement2Map(params, Constants.ENABLE_VALIDATOR_OPTION, enableValidator);
        addElement2Map(params, Constants.INTERFACE_VALIDATOR_OPTION, interfaceValidator);
        //从serverConfig获取SSL_ENABLE配置
        String sslEnable = serverConfig.parameters == null ? "false" : serverConfig.parameters.getOrDefault(SSL_ENABLE.getName(), String.valueOf(SSL_ENABLE.getValue()));
        addElement2Map(params, SSL_ENABLE, sslEnable);
        //分析过滤的方法
        Set<String> excludes = new HashSet<>();
        Set<String> includes = new HashSet<>();
        limit(excludes, includes);
        if (!includes.isEmpty()) {
            addElement2Map(params, Constants.METHOD_INCLUDE_OPTION, String.join(",", includes));
        }
        if (!excludes.isEmpty()) {
            addElement2Map(params, Constants.METHOD_EXCLUDE_OPTION, String.join(",", excludes));
        }

        return params;
    }

    /**
     * 方法输出限制
     *
     * @param excludes
     * @param includes
     */
    protected void limit(final Set<String> excludes, final Set<String> includes) {
        //根据注解获取默认配置
        if (ref != null) {
            List<Method> methods = ClassUtils.getPublicMethod(ref.getClass());
            Export export;
            for (Method method : methods) {
                export = method.getAnnotation(Export.class);
                if (export != null) {
                    //判断是否输出
                    if (!export.value()) {
                        excludes.add(method.getName());
                    } else {
                        includes.add(method.getName());
                    }
                }
            }
        }
        //手动静态配置覆盖默认注解配置
        if (include != null && !include.isEmpty()) {
            String[] names = split(include, SEMICOLON_COMMA_WHITESPACE);
            for (String name : names) {
                //如果手动开启了，则删除默认禁用
                excludes.remove(name);
                includes.add(name);
            }
        }
        //黑名单优先级高
        if (exclude != null && !exclude.isEmpty()) {
            String[] names = split(exclude, SEMICOLON_COMMA_WHITESPACE);
            for (String name : names) {
                excludes.add(name);
            }
        }
    }

    public List<RegistryConfig> getRegistry() {
        return registry;
    }

    public void setRegistry(List<RegistryConfig> registry) {
        this.registry = registry;
    }

    /**
     * 设置注册中心
     *
     * @param registry RegistryConfig
     */
    public void setRegistry(RegistryConfig registry) {
        if (registry != null) {
            if (this.registry == null) {
                this.registry = new ArrayList<>();
            }
            this.registry.add(registry);
        }
    }

    @Override
    public String name() {
        if (name == null) {
            name = interfaceClazz + "/" + Constants.ALIAS_OPTION.getName() + "=" + alias + "&" + Constants.ROLE_OPTION.getName() + "=" + Constants.SIDE_PROVIDER;
        }
        return name;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
    }

    public String getExclude() {
        return exclude;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public Boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public URL getServiceUrl() {
        return serviceUrl;
    }

    public List<URL> getRegistryUrls() {
        return registryUrls;
    }

    public List<Registry> getRegistries() {
        return registries;
    }

    public String getWarmup() {
        return warmup;
    }

    public void setWarmup(String warmup) {
        this.warmup = warmup;
    }

    public String getInterfaceValidator() {
        return interfaceValidator;
    }

    public void setInterfaceValidator(String interfaceValidator) {
        this.interfaceValidator = interfaceValidator;
    }

    public Boolean getEnableValidator() {
        return enableValidator;
    }

    public void setEnableValidator(Boolean enableValidator) {
        this.enableValidator = enableValidator;
    }

    /**
     * 注册中心打开消费者
     */
    protected class RegistryConsumer implements BiConsumer<Void, Throwable> {
        /**
         * 注册中心
         */
        protected Registry registry;

        /**
         * 订阅配置标识（多个注册中心，只订阅配置一份）
         */
        protected AtomicBoolean subscribed;
        /**
         * 等到Future
         */
        protected CompletableFuture<Void> future;

        /**
         * 构造函数
         *
         * @param registry
         * @param subscribed
         * @param future
         */
        public RegistryConsumer(final Registry registry, final AtomicBoolean subscribed,
                                final CompletableFuture<Void> future) {
            this.registry = registry;
            this.subscribed = subscribed;
            this.future = future;
        }

        @Override
        public void accept(Void value, Throwable throwable) {
            // todo subscribed不能确保多个里面一个成功
            if (throwable != null) {
                future.completeExceptionally(new InitializationException(
                        String.format("Registry open error. %s", registry.getUrl().toString(false, false))));
            } else if (!subscribed.get()) {
                //保存订阅注册中心
                configureRef = configure == null ? registry : configure;
                //订阅的URL
                subscribeUrl = configureRef.normalize(serviceUrl);
                configureRef.subscribe(subscribeUrl, configHandler);
                //只向满足条件的第一个注册中心订阅配置变化，多个注册中心同时订阅有问题
                subscribed.set(true);
            }
        }
    }

    /**
     * 预热
     */
    protected void warmup() {
        if (warmup != null && !warmup.isEmpty()) {
            Warmup wm = WARMUP.get(warmup);
            if (wm != null) {
                wm.setup(this);
            }
        }
    }
}
