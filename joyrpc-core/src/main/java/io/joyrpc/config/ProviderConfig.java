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
import io.joyrpc.config.validator.ValidateInterface;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.EventHandler;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.ServiceManager;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.Futures;
import io.joyrpc.util.StateController.ExStateController;
import io.joyrpc.util.StateEvent;
import io.joyrpc.util.StateFuture.ExStateFuture;
import io.joyrpc.util.StateMachine.ExStateMachine;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.network.Ipv4;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.joyrpc.Plugin.REGISTRY;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.Variable.VARIABLE;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;


/**
 * 服务发布者配置
 */
@ValidateInterface
public class ProviderConfig<T> extends AbstractInterfaceConfig implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ProviderConfig.class);
    /**
     * 全局的应用服务名称提供者
     */
    protected static final Supplier<String> APP_SERVICE_SUPPLIER = () -> GlobalContext.getString(KEY_APPSERVICE);

    /**
     * 全局的应用分组提供者
     */
    protected static final Supplier<String> APP_GROUP_SUPPLIER = () -> GlobalContext.getString(KEY_APPGROUP);

    /**
     * 注册中心配置，可配置多个
     */
    @Valid
    protected List<RegistryConfig> registry;
    /**
     * 接口实现类引用
     */
    @NotNull(message = "ref can not be null.")
    protected transient T ref;
    /**
     * 配置的协议列表
     */
    @Valid
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
     * 注册中心收到注册后主动发布服务
     */
    protected Boolean dynamic;
    /**
     * 是否启用验证
     */
    protected Boolean enableValidator;
    /**
     * 接口验证器插件
     */
    protected String interfaceValidator;
    /**
     * 预热插件
     */
    protected transient Warmup warmup;
    /**
     * 控制器
     */
    protected ExStateMachine<Void, ExStateController<Void>> stateMachine = new ExStateMachine<>(
            () -> new ProviderController<>(this),
            error -> new InitializationException(error),
            new ExStateFuture<>(() -> delay(), null, null, null));

    @Override
    public String getAlias() {
        if (alias == null || alias.isEmpty()) {
            //服务提供者如果没有设置别名，则可以采用应用分组
            alias = APP_GROUP_SUPPLIER.get();
        }
        return alias;
    }

    @Override
    public String getServiceName() {
        return getServiceName(APP_SERVICE_SUPPLIER);
    }

    @Override
    public void validate() {
        registry = registry != null && !registry.isEmpty() ? registry : Arrays.asList(RegistryConfig.DEFAULT_REGISTRY_SUPPLIER.get());
        super.validate();
    }

    @Override
    protected boolean isClose() {
        return isClose(null);
    }

    protected boolean isClose(final ExStateController<Void> controller) {
        return stateMachine.isClose(controller) || super.isClose();
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
     * 延迟加载
     *
     * @return
     */
    protected CompletableFuture<Void> delay() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        // 延迟加载,单位毫秒
        if (null == delay || delay <= 0) {
            future.complete(null);
        } else {
            logger.info(String.format("Delay exporting service %s(%s) %d(ms)", getServiceName(), getAlias(), delay));
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    future.complete(null);
                    //run会检查打开状态
                } catch (InterruptedException e) {
                    future.completeExceptionally(new InitializationException("InterruptedException " + name()));
                }
            });
            thread.setDaemon(true);
            thread.setName("DelayExportThread");
            thread.start();
        }
        return future;
    }

    /**
     * 订阅全局配置并创建服务，不开启服务
     */
    public CompletableFuture<Void> export() {
        return stateMachine.export();
    }

    /**
     * 开启服务
     */
    public CompletableFuture<Void> open() {
        return stateMachine.open();
    }

    /**
     * 取消发布
     */
    public CompletableFuture<Void> unexport() {
        Parametric parametric = new MapParametric(GlobalContext.getContext());
        return unexport(parametric.getBoolean(Constants.GRACEFULLY_SHUTDOWN_OPTION));
    }

    /**
     * 取消发布
     *
     * @param gracefully 优雅关闭
     */
    public CompletableFuture<Void> unexport(final boolean gracefully) {
        return stateMachine.close(gracefully);
    }

    @Override
    protected Map<String, String> addAttribute2Map(final Map<String, String> params) {
        super.addAttribute2Map(params);
        addElement2Map(params, Constants.WEIGHT_OPTION, weight);
        addElement2Map(params, Constants.DYNAMIC_OPTION, dynamic);
        addElement2Map(params, Constants.DELAY_OPTION, delay);
        addElement2Map(params, Constants.ROLE_OPTION, Constants.SIDE_PROVIDER);
        addElement2Map(params, Constants.TIMESTAMP_KEY, String.valueOf(SystemClock.now()));
        //从serverConfig获取SSL_ENABLE配置
        String sslEnable = serverConfig == null || serverConfig.parameters == null ? "false" :
                serverConfig.parameters.getOrDefault(SSL_ENABLE.getName(), String.valueOf(SSL_ENABLE.getValue()));
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
            name = new StringBuilder(100).append(interfaceClazz).append("/")
                    .append(Constants.ALIAS_OPTION.getName()).append("=").append(alias).append("&")
                    .append(Constants.ROLE_OPTION.getName()).append("=").append(Constants.SIDE_PROVIDER)
                    .toString();
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

    public Boolean getDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Warmup getWarmup() {
        return warmup;
    }

    public void setWarmup(Warmup warmup) {
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
     * 获取本地服务地址
     *
     * @param config 服务配置
     * @param remote 注册中心远程地址，用于检测本地出口地址
     * @return 服务地址信息
     */
    protected static ServerAddress getAddress(final ServerConfig config, final String remote) {
        String host;
        String bindIp = null;
        if (config == null || Ipv4.isLocalHost(config.getHost())) {
            //拿到本机地址
            host = getLocalHost(remote);
            //绑定地址
            bindIp = Ipv4.getAnyHost();
        } else {
            host = config.getHost();
        }
        //可以从环境变量里面配置默认端口
        int port = config == null || config.getPort() == null ? VARIABLE.getPositive(SERVER_PORT_KEY, DEFAULT_PORT) : config.getPort();
        return new ServerAddress(host, bindIp, port);
    }

    /**
     * 控制器
     */
    protected static class ProviderController<T> extends AbstractController<ProviderConfig<T>> implements ExStateController<Void>, EventHandler<StateEvent> {
        /**
         * 已发布
         */
        protected transient volatile Exporter exporter;
        /**
         * 注册中心
         */
        protected transient List<Registry> registries = new ArrayList<>(3);

        /**
         * 构造函数
         *
         * @param config
         */
        public ProviderController(ProviderConfig<T> config) {
            super(config);
        }

        @Override
        protected boolean isClose() {
            return config.isClose(this);
        }

        @Override
        protected boolean isOpened() {
            return config.stateMachine.isOpened(this);
        }

        @Override
        public void fireClose() {
            //TODO 终止Delay线程
            super.fireClose();
        }

        @Override
        public void handle(final StateEvent event) {
            switch (event.getType()) {
                case StateEvent.START_EXPORT:
                    GlobalContext.getContext();
                    logger.info("Start exporting provider " + config.name());
                    break;
                case StateEvent.SUCCESS_EXPORT:
                    logger.info("Success exporting provider " + config.name());
                    break;
                case StateEvent.FAIL_EXPORT:
                    logger.info(String.format("Failed exporting provider %s. caused by %s", config.name(), event.getThrowable().getMessage()));
                    break;
                case StateEvent.FAIL_EXPORT_ILLEGAL_STATE:
                    logger.info(String.format("Failed exporting provider %s. caused by state is illegal.", config.name()));
                    break;
                case StateEvent.START_OPEN:
                    logger.info(String.format("Start opening provider %s.", config.name()));
                    break;
                case StateEvent.SUCCESS_OPEN:
                    logger.info(String.format("Success opening provider %s.", config.name()));
                    //触发配置更新
                    update();
                    break;
                case StateEvent.FAIL_OPEN:
                    logger.info(String.format("Failed exporting provider %s. caused by %s", config.name(), event.getThrowable().getMessage()));
                    break;
                case StateEvent.FAIL_OPEN_ILLEGAL_STATE:
                    logger.info(String.format("Failed exporting provider %s. caused by state is illegal", config.name()));
                    break;
                case StateEvent.START_CLOSE:
                    logger.info("Start unexporting provider " + config.name());
                    break;
                case StateEvent.SUCCESS_CLOSE:
                    logger.info("Success unexporting provider " + config.name());
                    break;
            }
        }

        @Override
        public CompletableFuture<Void> export() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            try {
                config.validate();
                ServerConfig serverConfig = config.getServerConfig();
                //注册中心地址
                List<URL> urls = parse(config.getRegistry());
                //服务地址
                String remote = urls.get(0).getString(ADDRESS_OPTION);
                ServerAddress address = getAddress(serverConfig, remote);
                //生成注册的URL
                Map<String, String> map = config.addAttribute2Map(serverConfig==null?new HashMap<>():serverConfig.addAttribute2Map());
                if (address.bindIp != null) {
                    map.put(BIND_IP_KEY, address.bindIp);
                }
                //原始URL
                url = new URL(GlobalContext.getString(PROTOCOL_KEY), address.host, address.port, config.interfaceClazz, map);
                //加上动态配置的服务URL
                serviceUrl = configure(null);
                //订阅
                chain(subscribe(urls), future, v -> chain(waitingConfig, future, u -> {
                    //检查动态配置是否修改了别名，需要重新订阅
                    resubscribe(buildSubscribedUrl(configureRef, u), false);
                    //保存新的配置
                    serviceUrl = u;
                    try {
                        List<URL> registerUrls = registries.stream().map(registry -> buildRegisteredUrl(registry, serviceUrl)).collect(Collectors.toList());
                        exporter = ServiceManager.export(serviceUrl, config, registries, registerUrls, configureRef, subscribeUrl, configHandler);
                        future.complete(null);
                    } catch (Exception ex) {
                        future.completeExceptionally(ex);
                    }
                }));
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
            return future;
        }

        @Override
        public CompletableFuture<Void> open() {
            return exporter.open();
        }

        @Override
        public CompletableFuture<Void> close(final boolean gracefully) {
            Exporter r = exporter;
            return r == null ? CompletableFuture.completedFuture(null) : r.close(gracefully);
        }

        /**
         * 订阅，打开注册中心
         *
         * @param urls
         * @return
         */
        protected CompletableFuture<Void> subscribe(final List<URL> urls) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            //订阅成功标识
            AtomicBoolean subscribed = new AtomicBoolean(false);
            //过滤掉重复的注册中心
            Set<Registry> unique = new HashSet<>(3);
            List<CompletableFuture<Void>> futures = new LinkedList<>();
            //多注册中心
            for (URL url : urls) {
                //注册中心工厂插件
                Registry registry = getRegistry(url);
                if (registry == null) {
                    futures.add(Futures.completeExceptionally(
                            new InitializationException(String.format("Create registry error. %s", url.getProtocol()))));
                } else if (unique.add(registry)) {
                    futures.add(subscribe(registry, subscribed));
                    registries.add(registry);
                }
            }
            //不订阅配置
            if (!config.subscribe) {
                waitingConfig.complete(serviceUrl);
            }
            //等到所有的注册中心
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((v, t) -> {
                if (t != null) {
                    future.completeExceptionally(t);
                } else {
                    future.complete(null);
                }
            });
            return future;
        }

        /**
         * 构建注册中心
         *
         * @param url
         * @return
         */
        protected Registry getRegistry(final URL url) {
            String name = url.getProtocol();
            RegistryFactory factory = REGISTRY.get(name);
            if (factory == null) {
                if (!config.register && !config.subscribe) {
                    factory = REGISTRY.get("memory");
                } else {
                    return null;
                }
            }
            //获取注册中心
            return factory.getRegistry(url);
        }

        @Override
        protected CompletableFuture<Void> update(final URL newUrl) {
            //只在opened状态触发
            CompletableFuture<Void> future = new CompletableFuture<>();
            //先关闭老的，再打开新的，否则报错
            chain(exporter.close(true), future, v -> {
                //异步，再次判断是否关闭了
                if (!isClose()) {
                    List<URL> registerUrls = registries.stream().map(registry -> buildRegisteredUrl(registry, newUrl)).collect(Collectors.toList());
                    URL newSubscribeUrl = config.subscribe ? buildSubscribedUrl(configureRef, newUrl) : null;
                    Exporter newExporter = ServiceManager.export(newUrl, config, registries, registerUrls, configureRef, newSubscribeUrl, configHandler);
                    //打开
                    chain(newExporter.open(), future, s -> {
                        //异步，再次判断是否关闭了
                        if (!isClose()) {
                            //检查动态配置是否修改了别名，需要重新订阅
                            resubscribe(newSubscribeUrl, true);
                            exporter = newExporter;
                            serviceUrl = newUrl;
                            if (isClose()) {
                                //再次判断防止并发
                                newExporter.close(true);
                            }
                        } else {
                            newExporter.close(false);
                        }
                    });
                }
            });

            return future;
        }
    }

    /**
     * 服务地址
     */
    protected static class ServerAddress {
        /**
         * 对外服务的地址
         */
        protected String host;
        /**
         * 监听的IP
         */
        protected String bindIp;
        /**
         * 端口
         */
        protected int port;

        /**
         * 构造函数
         *
         * @param host
         * @param bindIp
         * @param port
         */
        public ServerAddress(String host, String bindIp, int port) {
            this.host = host;
            this.bindIp = bindIp;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public String getBindIp() {
            return bindIp;
        }

        public int getPort() {
            return port;
        }
    }

}
