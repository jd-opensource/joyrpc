package io.joyrpc.invoker.option.inner;

import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.distribution.CircuitBreaker;
import io.joyrpc.cluster.distribution.FailoverPolicy;
import io.joyrpc.cluster.distribution.Router;
import io.joyrpc.cluster.distribution.circuitbreaker.McCircuitBreaker;
import io.joyrpc.cluster.distribution.circuitbreaker.McCircuitBreakerConfig;
import io.joyrpc.cluster.distribution.circuitbreaker.McMethodBreakerConfig;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptivePolicy;
import io.joyrpc.invoker.option.CallbackOption;
import io.joyrpc.invoker.option.*;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transaction.TransactionOption;
import io.joyrpc.util.GenericMethod;
import io.joyrpc.util.IDLMethod;

import javax.validation.Validator;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * 方法选项
 */
public class InnerConsumerMethodOption extends AbstractMethodOption implements ConsumerMethodOption {
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
    protected MethodAdaptiveOption adaptiveConfig;
    /**
     * 方法熔断静态配置
     */
    protected McMethodBreakerConfig staticBreakerConfig;
    /**
     * 方法熔断动态配置
     */
    protected McMethodBreakerConfig dynamicBreakerConfig;
    /**
     * 方法熔断合并后的配置
     */
    protected McCircuitBreakerConfig breakerConfig;
    /**
     * 熔断器提供者
     */
    protected volatile CircuitBreaker circuitBreaker;
    /**
     * 是否自动计算方法指标阈值
     */
    protected volatile boolean autoScore;
    /**
     * Mock数据
     */
    protected volatile Map<String, Object> mock;

    public InnerConsumerMethodOption(final IDLMethod grpcMethod,
                                     final GenericMethod genericMethod,
                                     final Map<String, ?> implicits,
                                     final int timeout,
                                     final Concurrency concurrency,
                                     final CacheOption cachePolicy,
                                     final Validator validator,
                                     final TransactionOption transactionOption,
                                     final String token,
                                     final boolean async,
                                     final boolean trace,
                                     final CallbackOption callback,
                                     final int forks,
                                     final Supplier<BiPredicate<Shard, RequestMessage<Invocation>>> selector,
                                     final Router router,
                                     final FailoverPolicy failoverPolicy,
                                     final MethodAdaptiveOption adaptiveConfig,
                                     final McMethodBreakerConfig staticBreakerConfig,
                                     final McMethodBreakerConfig dynamicBreakerConfig,
                                     final Map<String, Object> mock) {
        super(grpcMethod, genericMethod, implicits, timeout, concurrency, cachePolicy, validator, transactionOption, token, async, trace, callback);
        this.forks = forks;
        this.selector = selector;
        this.router = router;
        this.failoverPolicy = failoverPolicy;
        this.adaptiveConfig = adaptiveConfig;
        this.staticBreakerConfig = staticBreakerConfig;
        update(dynamicBreakerConfig);
        this.mock = mock;
    }

    protected void update(McMethodBreakerConfig dynamicConfig) {
        this.dynamicBreakerConfig = dynamicConfig;
        McCircuitBreakerConfig cfg = dynamicConfig == null ? staticBreakerConfig.compute(null) : dynamicConfig.compute(staticBreakerConfig);
        if (!cfg.equals(breakerConfig)) {
            breakerConfig = cfg;
            circuitBreaker = cfg.getEnabled() != null && cfg.getEnabled() ? new McCircuitBreaker(cfg) : null;
        }
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
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
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
