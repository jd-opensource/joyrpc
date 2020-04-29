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

import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.distribution.ExceptionPolicy;
import io.joyrpc.cluster.distribution.ExceptionPredication;
import io.joyrpc.cluster.distribution.FailoverPolicy;
import io.joyrpc.cluster.distribution.FailoverPolicy.DefaultFailoverPolicy;
import io.joyrpc.cluster.distribution.Router;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptiveConfig;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptivePolicy;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.Judge;
import io.joyrpc.config.AbstractInterfaceOption;
import io.joyrpc.context.IntfConfiguration;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.ExtensionMeta;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.WrapperParametric;
import io.joyrpc.invoker.CallbackMethod;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.permission.ExceptionBlackWhiteList;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.GrpcMethod;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.constants.ExceptionCode.CONSUMER_FAILOVER_CLASS;
import static io.joyrpc.context.adaptive.AdaptiveConfiguration.ADAPTIVE;
import static io.joyrpc.context.mock.MockConfiguration.MOCK;
import static io.joyrpc.context.router.SelectorConfiguration.SELECTOR;
import static io.joyrpc.util.ClassUtils.forName;
import static io.joyrpc.util.ClassUtils.isReturnFuture;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;
import static io.joyrpc.util.Timer.timer;

/**
 * 消费者接口选项
 */
public class InnerConsumerOption extends AbstractInterfaceOption {

    private static final Logger logger = LoggerFactory.getLogger(InnerConsumerOption.class);

    /**
     * 扫描Provider提供的META-INF/retry/xxx.xxx.xxx.xx文件的异常配置
     */
    protected static final Map<String, Set<String>> INNER_EXCEPTIONS = new ConcurrentHashMap<>();
    /**
     * 内置的异常类资源配置文件路径
     */
    protected static final String RETRY_RESOURCE_PATH = "META-INF/retry/";
    /**
     * 分发策略配置
     */
    protected Consumer<Router> configure;
    /**
     * 打分器，用于没有自适应负载均衡的时候根据请求动态计算出来
     */
    protected BiFunction<String, AdaptiveConfig, AdaptiveConfig> scorer;
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
     * 路由选择器
     */
    protected BiPredicate<Shard, RequestMessage<Invocation>> selector;
    /**
     * 默认的分发策略
     */
    protected Router router;
    /**
     * 接口级别并行度
     */
    protected int forks;
    /**
     * 重试异常
     */
    protected BlackWhiteList<Class<? extends Throwable>> failoverBlackWhiteList;
    /**
     * 自适应负载均衡静态配置
     */
    protected AdaptiveConfig intfConfig;
    /**
     * 自适应负载均衡动态配置
     */
    protected IntfConfiguration<String, AdaptiveConfig> dynamicConfig;
    /**
     * 路由选择器配置
     */
    protected IntfConfiguration<String, BiPredicate<Shard, RequestMessage<Invocation>>> selectorConfig;
    /**
     * MOCK数据
     */
    protected IntfConfiguration<String, Map<String, Map<String, Object>>> mockConfig;
    /**
     * 裁决者
     */
    protected List<Judge> judges;

    /**
     * 构造函数
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名称
     * @param url            URL
     * @param configure      路由配置器
     * @param scorer         打分器，用于没有自适应负载均衡的时候根据请求动态计算出来
     */
    public InnerConsumerOption(final Class<?> interfaceClass, final String interfaceName, final URL url,
                               final Consumer<Router> configure,
                               final BiFunction<String, AdaptiveConfig, AdaptiveConfig> scorer) {
        super(interfaceClass, interfaceName, url);
        this.configure = configure;
        this.scorer = scorer;
        setup();
        buildOptions();
    }

    @Override
    protected void setup() {
        super.setup();
        this.maxRetry = url.getInteger(RETRIES_OPTION);
        this.retryOnlyOncePerNode = url.getBoolean(RETRY_ONLY_ONCE_PER_NODE_OPTION);
        this.failoverSelector = url.getString(FAILOVER_SELECTOR_OPTION);
        this.failoverPredication = url.getString(FAILOVER_PREDICATION_OPTION);
        //需要放在failoverPredication后面，里面加载配置文件的时候需要判断failoverPredication
        this.failoverBlackWhiteList = buildFailoverBlackWhiteList();
        this.forks = url.getInteger(FORKS_OPTION);
        this.mockConfig = new IntfConfiguration<>(MOCK, interfaceName, config -> {
            if (options != null) {
                options.forEach((method, mo) -> {
                    InnerConsumerMethodOption cmo = ((InnerConsumerMethodOption) mo);
                    cmo.mock = config == null ? null : config.get(method);
                });
            }
        });
        //路由策略
        if (configure != null) {
            this.selectorConfig = new IntfConfiguration<>(SELECTOR, interfaceName, v -> this.selector = v);
            this.router = ROUTER.get(url.getString(ROUTER_OPTION));
            configure.accept(router);
        }
        if (scorer != null) {
            this.intfConfig = new AdaptiveConfig(url);
            this.judges = new LinkedList<>();
            JUDGE.extensions().forEach(judges::add);
            //最后监听自适应负载均衡变化
            this.dynamicConfig = new IntfConfiguration<>(ADAPTIVE, interfaceName,
                    config -> {
                        if (options != null) {
                            options.forEach((method, op) -> ((InnerConsumerMethodOption) op).adaptiveConfig.setDynamicIntfConfig(config));
                        }
                    });
            timer().add(new Scorer("scorer-" + interfaceName,
                    () -> {
                        //如果动态和静态配置都已经配置了完整的评分，则不需要再进行动态配置
                        AdaptiveConfig dynamic = dynamicConfig.get();
                        return !closed.get() && (intfConfig.getAvailabilityScore() == null && (dynamic == null || dynamic.getAvailabilityScore() == null)
                                || intfConfig.getConcurrencyScore() == null && (dynamic == null || dynamic.getConcurrencyScore() == null)
                                || intfConfig.getQpsScore() == null && (dynamic == null || dynamic.getQpsScore() == null)
                                || intfConfig.getTpScore() == null && (dynamic == null || dynamic.getTpScore() == null));
                    },
                    () -> {
                        options.forEach((method, mo) -> {
                            InnerConsumerMethodOption cmo = ((InnerConsumerMethodOption) mo);
                            if (cmo.autoScore) {
                                //过滤掉没有调用过的方法
                                cmo.adaptiveConfig.setScore(scorer.apply(method, cmo.adaptiveConfig.config));
                                cmo.autoScore = false;
                            }
                        });
                    }));
        }
    }

    @Override
    protected void doClose() {
        if (selectorConfig != null) {
            selectorConfig.close();
        }
        if (dynamicConfig != null) {
            dynamicConfig.close();
        }
        if (mockConfig != null) {
            mockConfig.close();
        }
    }

    @Override
    protected InnerMethodOption create(final WrapperParametric parametric) {
        GrpcMethod grpcMethod = getMethod(parametric.getName());
        Method method = grpcMethod == null ? null : grpcMethod.getMethod();
        Map<String, Map<String, Object>> methodMocks = mockConfig.get();
        return new InnerConsumerMethodOption(
                grpcMethod,
                getImplicits(parametric.getName()),
                parametric.getPositive(TIMEOUT_OPTION.getName(), timeout),
                new Concurrency(parametric.getInteger(CONCURRENCY_OPTION.getName(), concurrency)),
                getCachePolicy(parametric),
                getValidator(parametric),
                parametric.getString(HIDDEN_KEY_TOKEN, token),
                method != null && isReturnFuture(interfaceClass, method),
                getCallback(method),
                parametric.getInteger(FORKS_OPTION.getName(), forks),
                () -> selector,
                getRoute(parametric),
                new DefaultFailoverPolicy(
                        parametric.getInteger(RETRIES_OPTION.getName(), maxRetry),
                        parametric.getBoolean(RETRY_ONLY_ONCE_PER_NODE_OPTION.getName(), retryOnlyOncePerNode),
                        new MyTimeoutPolicy(),
                        new MyExceptionPolicy(failoverBlackWhiteList, EXCEPTION_PREDICATION.get(failoverPredication)),
                        FAILOVER_SELECTOR.get(parametric.getString(FAILOVER_SELECTOR_OPTION.getName(), failoverSelector))),
                scorer == null ? null : new MethodAdaptiveConfig(intfConfig, new AdaptiveConfig(parametric), dynamicConfig.get(), judges),
                methodMocks == null ? null : methodMocks.get(parametric.getName()));
    }

    /**
     * 获取分发策略
     *
     * @param parametric 参数
     * @return 分发策略
     */
    protected Router getRoute(final WrapperParametric parametric) {
        //方法分发策略
        Router methodRouter = null;
        if (configure != null) {
            methodRouter = ROUTER.get(parametric.getString(ROUTER_OPTION.getName()));
            if (methodRouter != null) {
                configure.accept(methodRouter);
            } else {
                methodRouter = router;
            }

        }
        return methodRouter;
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
    protected static class InnerConsumerMethodOption extends InnerMethodOption implements ConsumerMethodOption {
        /**
         * 并行度
         */
        protected int forks;
        /**
         * 节点选择器算法提供者
         */
        protected Supplier<BiPredicate<Shard, RequestMessage<Invocation>>> selector;
        /**
         * 分发策略
         */
        protected Router router;
        /**
         * 重试策略
         */
        protected FailoverPolicy failoverPolicy;
        /**
         * 自适应负载均衡配置
         */
        protected MethodAdaptiveConfig adaptiveConfig;

        /**
         * 是否自动计算方法指标阈值
         */
        protected volatile boolean autoScore;
        /**
         * Mock数据
         */
        protected volatile Map<String, Object> mock;

        public InnerConsumerMethodOption(final GrpcMethod method, final Map<String, ?> implicits, final int timeout, final Concurrency concurrency,
                                         final CachePolicy cachePolicy, final Validator validator,
                                         final String token, final boolean async, final CallbackMethod callback, final int forks,
                                         final Supplier<BiPredicate<Shard, RequestMessage<Invocation>>> selector,
                                         final Router router, final FailoverPolicy failoverPolicy,
                                         final MethodAdaptiveConfig adaptiveConfig,
                                         final Map<String, Object> mock) {
            super(method, implicits, timeout, concurrency, cachePolicy, validator, token, async, callback);
            this.forks = forks;
            this.selector = selector;
            this.router = router;
            this.failoverPolicy = failoverPolicy;
            this.adaptiveConfig = adaptiveConfig;
            this.mock = mock;
        }

        @Override
        public int getForks() {
            return forks;
        }

        @Override
        public BiPredicate<Shard, RequestMessage<Invocation>> getSelector() {
            return selector == null ? null : selector.get();
        }

        @Override
        public Router getRouter() {
            return router;
        }

        @Override
        public FailoverPolicy getFailoverPolicy() {
            return failoverPolicy;
        }

        @Override
        public AdaptivePolicy getAdaptivePolicy() {
            return adaptiveConfig.getPolicy();
        }

        @Override
        public Map<String, Object> getMock() {
            return mock;
        }

        @Override
        public void setAutoScore(final boolean autoScore) {
            if (this.autoScore != autoScore) {
                this.autoScore = autoScore;
            }
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
        public boolean test(final Throwable throwable) {
            //暂时不需要增加动态配置支持，这些一般都需要提前测试配置好。
            return failoverBlackWhiteList.isValid(throwable.getClass()) || (exceptionPredication != null && exceptionPredication.test(throwable));
        }
    }

    /**
     * 方法自适应配置
     */
    protected static class MethodAdaptiveConfig {
        /**
         * 接口静态配置
         */
        protected final AdaptiveConfig intfConfig;
        /**
         * 接口动态配置
         */
        protected AdaptiveConfig dynamicIntfConfig;
        /**
         * 方法静态配置
         */
        protected final AdaptiveConfig methodConfig;
        /**
         * 裁决者
         */
        protected final List<Judge> judges;
        /**
         * 方法动态评分
         */
        protected AdaptiveConfig score;
        /**
         * 配置合并
         */
        protected volatile AdaptiveConfig config;
        /**
         * 最终配置，包括自动计算评分的结果
         */
        protected volatile AdaptivePolicy policy;

        /**
         * 构造函数
         *
         * @param intfConfig        接口静态配置
         * @param methodConfig      方法静态配置
         * @param dynamicIntfConfig 接口动态配置
         * @param judges            裁判
         */
        public MethodAdaptiveConfig(final AdaptiveConfig intfConfig, final AdaptiveConfig methodConfig,
                                    final AdaptiveConfig dynamicIntfConfig, final List<Judge> judges) {
            this.intfConfig = intfConfig;
            this.methodConfig = methodConfig;
            this.dynamicIntfConfig = dynamicIntfConfig;
            this.judges = judges;
            this.policy = new AdaptivePolicy(intfConfig, judges);
            update();
        }

        public void setDynamicIntfConfig(AdaptiveConfig dynamicIntfConfig) {
            if (dynamicIntfConfig != this.dynamicIntfConfig) {
                this.dynamicIntfConfig = dynamicIntfConfig;
                update();
            }
        }

        public void setScore(AdaptiveConfig score) {
            if (score != this.score) {
                this.score = score;
                update();
            }
        }

        public AdaptivePolicy getPolicy() {
            return policy;
        }

        /**
         * 更新
         */
        protected synchronized void update() {
            AdaptiveConfig result = new AdaptiveConfig(intfConfig);
            result.merge(dynamicIntfConfig);
            result.merge(methodConfig);
            //配置合并
            config = new AdaptiveConfig(result);
            //加上自动计算的评分
            result.merge(score);
            policy = new AdaptivePolicy(result, judges);
        }
    }

    /**
     * 打分器任务
     */
    protected static class Scorer implements Timer.TimeTask {
        /**
         * 名称
         */
        protected String name;
        /**
         * 是否要继续
         */
        protected Supplier<Boolean> condition;
        /**
         * 执行任务
         */
        protected Runnable runnable;

        public Scorer(String name, Supplier<Boolean> condition, Runnable runnable) {
            this.name = name;
            this.condition = condition;
            this.runnable = runnable;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getTime() {
            //各个接口的评分分布在10秒钟之内
            return SystemClock.now() + 10000L + ThreadLocalRandom.current().nextLong(10000L);
        }

        @Override
        public void run() {
            //判断是否要继续
            if (condition.get()) {
                runnable.run();
                if (condition.get()) {
                    //继续执行
                    timer().add(this);
                }
            }
        }
    }

}
