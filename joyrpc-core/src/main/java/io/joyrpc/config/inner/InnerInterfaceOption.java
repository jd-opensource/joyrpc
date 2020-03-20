package io.joyrpc.config.inner;

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

import io.joyrpc.Result;
import io.joyrpc.cache.Cache;
import io.joyrpc.cache.CacheConfig;
import io.joyrpc.cache.CacheFactory;
import io.joyrpc.cache.CacheKeyGenerator;
import io.joyrpc.cache.CacheKeyGenerator.ExpressionGenerator;
import io.joyrpc.cluster.distribution.*;
import io.joyrpc.cluster.distribution.FailoverPolicy.DefaultFailoverPolicy;
import io.joyrpc.config.InterfaceOption;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.extension.ExtensionMeta;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.WrapperParametric;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.permission.ExceptionBlackWhiteList;
import io.joyrpc.permission.StringBlackWhiteList;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.MethodOption.NameKeyOption;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.MethodDescriptor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static io.joyrpc.GenericService.GENERIC;
import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.constants.ExceptionCode.CONSUMER_FAILOVER_CLASS;
import static io.joyrpc.util.ClassUtils.forName;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 内部的接口选项
 */
public class InnerInterfaceOption implements InterfaceOption {

    private static final Logger logger = LoggerFactory.getLogger(InnerInterfaceOption.class);

    /**
     * 扫描Provider提供的META-INF/retry/xxx.xxx.xxx.xx文件的异常配置
     */
    protected static final Map<String, Set<String>> INNER_EXCEPTIONS = new ConcurrentHashMap<>();
    /**
     * 内置的异常类资源配置文件路径
     */
    protected static final String RETRY_RESOURCE_PATH = "META-INF/retry/";
    /**
     * 接口类型
     */
    protected Class<?> interfaceClass;
    /**
     * 接口名称
     */
    protected String interfaceName;
    /**
     * url
     */
    protected URL url;
    /**
     * 接口的隐藏参数
     */
    protected Map<String, String> implicits;
    /**
     * 接口级别最大重试次数
     */
    protected int maxRetry;
    /**
     * 接口级别的每个节点重试一次
     */
    protected boolean retryOnlyOncePerNode;
    /**
     * 接口级别的重试目标节点选择器
     */
    protected String failoverSelector;
    /**
     * 异常检测
     */
    protected String failoverPredication;
    /**
     * 默认的分发策略
     */
    protected Route route;
    /**
     * 接口级别超时时间
     */
    protected int timeout;
    /**
     * 接口级别并行度
     */
    protected int forks;
    /**
     * 接口级别并发数配置
     */
    protected int concurrency;
    /**
     * 是否启用缓存
     */
    protected boolean cacheEnable;
    /**
     * 是否缓存空值
     */
    protected boolean cacheNullable;
    /**
     * 缓存容量
     */
    protected int cacheCapacity;
    /**
     * 缓存过期时间
     */
    protected int cacheExpireTime;
    /**
     * 缓存键生成器
     */
    protected String cacheKeyGenerator;
    /**
     * 缓存提供者
     */
    protected String cacheProvider;
    /**
     * 缓存工厂类
     */
    protected CacheFactory cacheFactory;
    /**
     * 重试异常
     */
    protected BlackWhiteList<Class<? extends Throwable>> failoverBlackWhiteList;
    /**
     * 方法黑白名单
     */
    protected BlackWhiteList<String> methodBlackWhiteList;
    /**
     * 是否要进行方法参数验证
     */
    protected boolean validation;
    /**
     * 验证器
     */
    protected Validator validator;
    /**
     * 对象类描述符
     */
    protected BeanDescriptor beanDescriptor;
    /**
     * 令牌
     */
    protected String token;
    /**
     * 方法透传参数
     */
    protected NameKeyOption<InnerMethodOption> options;
    /**
     * 分发策略配置
     */
    protected Consumer<Route> configure;

    /**
     * 构造函数
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名称
     * @param url            URL
     */
    public InnerInterfaceOption(final Class<?> interfaceClass, final String interfaceName, final URL url) {
        this(interfaceClass, interfaceName, url, null);
    }

    /**
     * 构造函数
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名称
     * @param url            URL
     * @param configure      路由配置器
     */
    public InnerInterfaceOption(final Class<?> interfaceClass, final String interfaceName, final URL url,
                                final Consumer<Route> configure) {
        this.interfaceClass = interfaceClass;
        this.interfaceName = interfaceName;
        this.url = url;
        this.implicits = url.startsWith(String.valueOf(HIDE_KEY_PREFIX));
        this.timeout = url.getPositiveInt(TIMEOUT_OPTION);
        this.maxRetry = url.getInteger(RETRIES_OPTION);
        this.retryOnlyOncePerNode = url.getBoolean(RETRY_ONLY_ONCE_PER_NODE_OPTION);
        this.failoverSelector = url.getString(FAILOVER_SELECTOR_OPTION);
        this.failoverPredication = url.getString(FAILOVER_PREDICATION_OPTION);
        //需要放在failoverPredication后面，里面加载配置文件的时候需要判断failoverPredication
        this.failoverBlackWhiteList = buildFailoverBlackWhiteList();
        this.forks = url.getInteger(FORKS_OPTION);
        this.concurrency = url.getInteger(CONCURRENCY_OPTION);
        this.token = url.getString(HIDDEN_KEY_TOKEN);
        //缓存配置
        this.cacheEnable = url.getBoolean(CACHE_OPTION);
        this.cacheNullable = url.getBoolean(CACHE_NULLABLE_OPTION);
        this.cacheCapacity = url.getInteger(CACHE_CAPACITY_OPTION);
        this.cacheExpireTime = url.getInteger(CACHE_EXPIRE_TIME_OPTION);
        this.cacheKeyGenerator = url.getString(CACHE_KEY_GENERATOR_OPTION);
        this.cacheProvider = url.getString(CACHE_PROVIDER_OPTION);
        this.cacheFactory = CACHE.get(cacheProvider);
        this.configure = configure;
        if (configure != null) {
            this.route = ROUTE.get(url.getString(ROUTE_OPTION));
            configure.accept(route);
        }
        String include = url.getString(METHOD_INCLUDE_OPTION.getName());
        String exclude = url.getString(METHOD_EXCLUDE_OPTION.getName());
        this.methodBlackWhiteList = (include == null || include.isEmpty()) && (exclude == null || exclude.isEmpty()) ? null :
                new StringBlackWhiteList(include, exclude);

        //方法级别的隐藏参数，保留以"."开头
        boolean generic = GENERIC.test(interfaceClass);
        //默认是否认证
        this.validation = url.getBoolean(VALIDATION_OPTION);
        if (!generic) {
            this.validator = Validation.buildDefaultValidatorFactory().getValidator();
            if (validator != null) {
                this.beanDescriptor = validator.getConstraintsForClass(interfaceClass);
            }
        }

        this.options = new NameKeyOption<>(generic ? null : interfaceClass, generic ? interfaceName : null, this::create);
    }

    /**
     * 构建方法选项
     *
     * @param method 方法
     * @return 方法选项
     */
    protected InnerMethodOption create(final String method) {
        String prefix = URL_METHOD_PREX + method + ".";
        WrapperParametric parametric = new WrapperParametric(url, method, METHOD_KEY_FUNC, key -> key.startsWith(prefix));
        return new InnerMethodOption(
                getImplicits(method),
                parametric.getPositive(TIMEOUT_OPTION.getName(), timeout),
                parametric.getInteger(FORKS_OPTION.getName(), forks),
                getRoute(parametric),
                new Concurrency(parametric.getInteger(CONCURRENCY_OPTION.getName(), concurrency)),
                new DefaultFailoverPolicy(
                        parametric.getInteger(RETRIES_OPTION.getName(), maxRetry),
                        parametric.getBoolean(RETRY_ONLY_ONCE_PER_NODE_OPTION.getName(), retryOnlyOncePerNode),
                        new MyTimeoutPolicy(),
                        new MyExceptionPolicy(failoverBlackWhiteList, EXCEPTION_PREDICATION.get(failoverPredication)),
                        FAILOVER_SELECTOR.get(parametric.getString(FAILOVER_SELECTOR_OPTION.getName(), failoverSelector))),
                getCachePolicy(parametric),
                methodBlackWhiteList,
                getValidator(parametric),
                parametric.getString(HIDDEN_KEY_TOKEN, token));
    }

    /**
     * 获取方法隐藏参数，合并了接口的隐藏参数
     *
     * @param method 方法
     * @return 方法隐藏参数
     */
    protected Map<String, String> getImplicits(final String method) {
        Map<String, String> result = url.startsWith(METHOD_KEY_FUNC.apply(method, String.valueOf(HIDE_KEY_PREFIX)), (k, v) -> v.substring(k.length() - 1));
        if (result.isEmpty()) {
            return implicits;
        } else if (implicits.isEmpty()) {
            return result;
        } else {
            implicits.forEach(result::putIfAbsent);
            return result;
        }
    }

    /**
     * 获取方法参数验证器
     *
     * @param parametric 参数
     * @return 参数验证器
     */
    protected Validator getValidator(final WrapperParametric parametric) {
        //过滤掉了不验证的方法或泛型接口
        if (validator == null || !parametric.getBoolean(VALIDATION_OPTION.getName(), validation)) {
            return null;
        }
        try {
            Method method = ClassUtils.getPublicMethod(interfaceClass, parametric.getName());
            //判断该方法上是有有验证注解
            MethodDescriptor descriptor = beanDescriptor.getConstraintsForMethod(method.getName(), method.getParameterTypes());
            return descriptor != null && descriptor.hasConstrainedParameters() ? validator : null;
        } catch (NoSuchMethodException | MethodOverloadException e) {
            return null;
        }
    }

    /**
     * 获取分发策略
     *
     * @param parametric 参数
     * @return 分发策略
     */
    protected Route getRoute(final WrapperParametric parametric) {
        //方法分发策略
        Route methodRoute = null;
        if (configure != null) {
            methodRoute = ROUTE.get(parametric.getString(ROUTE_OPTION.getName()));
            if (methodRoute != null) {
                configure.accept(methodRoute);
            } else {
                methodRoute = route;
            }

        }
        return methodRoute;
    }

    /**
     * 构造缓存策略
     *
     * @param parametric 参数
     * @return 缓存策略
     */
    protected CachePolicy getCachePolicy(final WrapperParametric parametric) {
        CachePolicy cachePolicy = null;
        //判断是否开启了缓存
        boolean enable = cacheFactory == null ? false : parametric.getBoolean(CACHE_OPTION.getName(), cacheEnable);
        if (enable) {
            //获取缓存键生成器
            CacheKeyGenerator generator = CACHE_KEY_GENERATOR.get(parametric.getString(CACHE_KEY_GENERATOR_OPTION.getName(), cacheKeyGenerator));
            if (generator != null) {
                //看看是否是表达式
                if (generator instanceof ExpressionGenerator) {
                    ExpressionGenerator gen = (ExpressionGenerator) generator;
                    gen.setParametric(parametric);
                    gen.setup();
                }
                //判断是否缓存空值
                //创建缓存
                CacheConfig<Object, Object> cacheConfig = CacheConfig.builder().
                        nullable(parametric.getBoolean(CACHE_NULLABLE_OPTION.getName(), cacheNullable)).
                        capacity(parametric.getInteger(CACHE_CAPACITY_OPTION.getName(), cacheCapacity)).
                        expireAfterWrite(parametric.getInteger(CACHE_EXPIRE_TIME_OPTION.getName(), cacheExpireTime)).
                        build();
                Cache<Object, Object> cache = cacheFactory.build(parametric.getName(), cacheConfig);
                cachePolicy = new CachePolicy(cache, generator);
            }
        }
        return cachePolicy;
    }

    /**
     * 构建异常重试类
     *
     * @return 异常黑白名单
     */
    protected BlackWhiteList<Class<? extends Throwable>> buildFailoverBlackWhiteList() {
        //内置的异常类名
        Set<String> names = new HashSet<>(INNER_EXCEPTIONS.computeIfAbsent(interfaceName, this::getInnerExceptions));
        //当前URL配置的异常
        String value = url.getString(FAILOVER_WHEN_THROWABLE_OPTION);
        if (value != null && !value.isEmpty()) {
            String[] classes = split(value, SEMICOLON_COMMA_WHITESPACE);
            Collections.addAll(names, classes);
        }
        Set<Class<? extends Throwable>> failoverClass = new HashSet<>();
        Class<?> c;
        for (String name : names) {
            try {
                c = forName(name);
                if (!Throwable.class.isAssignableFrom(c)) {
                    logger.error("Failover exception class is not implement Throwable. " + name);
                }
                failoverClass.add((Class<? extends Throwable>) c);
            } catch (ClassNotFoundException e) {
                logger.error("Failover exception class is not found. " + name);
            }
        }
        return new ExceptionBlackWhiteList(failoverClass, null, false);
    }

    /**
     * 获取方法选项
     *
     * @param methodName 方法名称
     * @return 方法选项
     */
    public InnerMethodOption getOption(final String methodName) {
        return options.get(methodName);
    }

    /**
     * 读取内置的异常配置信息
     *
     * @param interfaceName 接口名称
     * @return 异常类名
     */
    protected Set<String> getInnerExceptions(final String interfaceName) {
        Set<String> names = new HashSet<>();
        ClassLoader loader = ClassUtils.getCurrentClassLoader();
        String line;
        String name;
        ExtensionMeta<ExceptionPredication, String> meta;
        ExtensionMeta<ExceptionPredication, String> max = null;
        try {
            Enumeration<java.net.URL> urls = loader.getResources(RETRY_RESOURCE_PATH + interfaceName);
            while ((urls.hasMoreElements())) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(urls.nextElement().openStream(), StandardCharsets.UTF_8))) {
                    while ((line = reader.readLine()) != null) {
                        //异常判断扩展插件
                        if (line.startsWith("[") && line.endsWith("]")) {
                            //没有人工设置异常判断插件
                            if (failoverPredication == null || failoverPredication.isEmpty()) {
                                name = line.substring(1, line.length() - 1);
                                //获取优先级最高的异常判断插件
                                meta = EXCEPTION_PREDICATION.meta(name);
                                if (meta != null && (max == null || max.getOrder() > meta.getOrder())) {
                                    max = meta;
                                }
                            }
                        } else {
                            //异常类
                            names.add(line);
                        }
                    }
                } catch (IOException e) {
                    throw new InitializationException(e.getMessage(), CONSUMER_FAILOVER_CLASS);
                }
            }
        } catch (IOException e) {
            throw new InitializationException(e.getMessage(), CONSUMER_FAILOVER_CLASS);
        }
        if (max != null) {
            failoverPredication = max.getExtension().getName();
        }
        return names;

    }

    /**
     * 方法选项
     */
    protected static class InnerMethodOption implements MethodOption {
        /**
         * 方法级别隐式传参，合并了接口的隐藏参数，只读
         */
        protected Map<String, ?> implicits;
        /**
         * 超时时间
         */
        protected int timeout;
        /**
         * 并行度
         */
        protected int forks;
        /**
         * 分发策略
         */
        protected Route route;
        /**
         * 并发数配置
         */
        protected Concurrency concurrency;
        /**
         * 重试策略
         */
        protected FailoverPolicy failoverPolicy;
        /**
         * 缓存策略
         */
        protected CachePolicy cachePolicy;
        /**
         * 方法的黑白名单
         */
        protected BlackWhiteList<String> methodBlackWhiteList;
        /**
         * 方法参数验证器
         */
        protected Validator validator;
        /**
         * 令牌
         */
        protected String token;

        /**
         * 构造函数
         *
         * @param implicits            隐式传参
         * @param timeout              超时时间
         * @param forks                并行度
         * @param route                分发策略
         * @param concurrency          并发数配置
         * @param failoverPolicy       重试策略
         * @param cachePolicy          缓存策略
         * @param methodBlackWhiteList 方法黑白名单
         * @param validator            方法参数验证器
         * @param token                令牌
         */
        public InnerMethodOption(Map<String, ?> implicits, int timeout, int forks, Route route,
                                 final Concurrency concurrency,
                                 final FailoverPolicy failoverPolicy,
                                 final CachePolicy cachePolicy,
                                 final BlackWhiteList<String> methodBlackWhiteList,
                                 final Validator validator,
                                 final String token) {
            this.implicits = implicits == null ? null : Collections.unmodifiableMap(implicits);
            this.timeout = timeout;
            this.forks = forks;
            this.route = route;
            this.concurrency = concurrency;
            this.failoverPolicy = failoverPolicy;
            this.cachePolicy = cachePolicy;
            this.methodBlackWhiteList = methodBlackWhiteList;
            this.validator = validator;
            this.token = token;
        }

        @Override
        public Map<String, ?> getImplicits() {
            return implicits;
        }

        @Override
        public int getTimeout() {
            return timeout;
        }

        @Override
        public int getForks() {
            return forks;
        }

        @Override
        public Route getRoute() {
            return route;
        }

        @Override
        public Concurrency getConcurrency() {
            return concurrency;
        }

        @Override
        public FailoverPolicy getFailoverPolicy() {
            return failoverPolicy;
        }

        @Override
        public CachePolicy getCachePolicy() {
            return cachePolicy;
        }

        @Override
        public BlackWhiteList<String> getMethodBlackWhiteList() {
            return methodBlackWhiteList;
        }

        @Override
        public Validator getValidator() {
            return validator;
        }

        @Override
        public String getToken() {
            return token;
        }
    }

    /**
     * 异常策略
     */
    protected static class MyExceptionPolicy implements ExceptionPolicy {
        /**
         * 异常黑白名单
         */
        protected BlackWhiteList<Class<? extends Throwable>> failoverBlackWhiteList;
        /**
         * 异常检测
         */
        protected ExceptionPredication exceptionPredication;

        /**
         * 构造函数
         *
         * @param failoverBlackWhiteList 异常黑白名单
         * @param exceptionPredication   异常断言
         */
        public MyExceptionPolicy(final BlackWhiteList<Class<? extends Throwable>> failoverBlackWhiteList,
                                 final ExceptionPredication exceptionPredication) {
            this.failoverBlackWhiteList = failoverBlackWhiteList;
            this.exceptionPredication = exceptionPredication;
        }

        @Override
        public Throwable getThrowable(final Result result) {
            return result.getException();
        }

        @Override
        public boolean test(final Throwable throwable) {
            //暂时不需要增加动态配置支持，这些一般都需要提前测试配置好。
            return failoverBlackWhiteList.isValid(throwable.getClass()) || (exceptionPredication != null && exceptionPredication.test(throwable));
        }
    }

    /**
     * 超时策略
     */
    protected static class MyTimeoutPolicy implements TimeoutPolicy {

        @Override
        public boolean test(final RequestMessage<Invocation> request) {
            return request.isTimeout();
        }

        @Override
        public void reset(final RequestMessage<Invocation> request) {
            request.getHeader().setTimeout((int) (request.getTimeout() + request.getCreateTime() - SystemClock.now()));
        }
    }

}
