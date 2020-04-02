package io.joyrpc.invoker.failover;

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
import io.joyrpc.cluster.distribution.ExceptionPolicy;
import io.joyrpc.cluster.distribution.FailoverPolicy;
import io.joyrpc.cluster.distribution.TimeoutPolicy;
import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.config.InterfaceOption;
import io.joyrpc.config.InterfaceOption.ConsumerMethodOption;
import io.joyrpc.exception.FailoverException;
import io.joyrpc.exception.LafException;
import io.joyrpc.extension.Extension;
import io.joyrpc.invoker.AbstractGroupInvoker;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.Futures;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.joyrpc.cluster.distribution.Router.FAIL_FAST;

/**
 * Failover分组路由
 */
@Extension("failover")
public class FailoverGroupInvoker extends AbstractGroupInvoker {

    /**
     * 超过最大次数
     */
    protected static final Function<Integer, Throwable> overloadFunction = (maxRetry) -> new FailoverException(String.format("Maximum number %d of retries reached", maxRetry));
    /**
     * 负责均衡没有选择出合适的节点
     */
    protected static final BiFunction<Integer, Boolean, Throwable> emptyFunction = (count, retry) -> new FailoverException(String.format("there is no another group after retrying %d", count), retry);
    /**
     * 分组配置
     */
    protected ConsumerConfig<?>[] configs;
    /**
     * 接口参数
     */
    protected InterfaceOption intfOption;

    @Override
    public CompletableFuture<Void> refer() {
        //创建消费引用
        CompletableFuture<Void>[] futures = new CompletableFuture[aliasMeta.size()];
        configs = new ConsumerConfig[aliasMeta.size()];
        int i = 0;
        for (String alias : aliasMeta.getArrays()) {
            futures[i] = new CompletableFuture<>();
            configs[i] = consumerFunction.apply(alias);
            //单个分组不进行重试
            configs[i].setCluster(FAIL_FAST);
            configs[i].refer(futures[i]);
            configMap.put(alias, configs[i]);
            i++;
        }
        return CompletableFuture.allOf(futures).thenAccept(v -> {
            if (configs.length > 0) {
                intfOption = configs[0].getRefer().getOptions();
            }
        });
    }

    @Override
    public CompletableFuture<Result> invoke(final RequestMessage<Invocation> request) {
        ConsumerMethodOption option = (ConsumerMethodOption) intfOption.getOption(request.getMethodName());
        request.setTimeout(option.getTimeout());
        request.getHeader().setTimeout(option.getTimeout());
        CompletableFuture<Result> future = new CompletableFuture<>();
        retry(request, 0, option.getFailoverPolicy(), future);
        return future;
    }

    /**
     * 递归重试
     *
     * @param request 请求
     * @param retry   当前重试次数
     * @param policy  重试策略
     * @param future  结束Future
     */
    protected void retry(final RequestMessage<Invocation> request,
                         final int retry,
                         final FailoverPolicy policy,
                         final CompletableFuture<Result> future) {
        if (retry > 0) {
            //便于向服务端注入重试次数
            request.setRetryTimes(retry);
        }
        //调用，如果节点不存在，则抛出Failover异常。
        ConsumerConfig<?> config = configs[retry % configs.length];
        CompletableFuture<Result> result = config.getRefer().invoke(request);
        result.whenComplete((r, t) -> {
            t = t == null ? r.getException() : t;
            if (t == null) {
                future.complete(r);
            } else {
                ExceptionPolicy exceptionPolicy = policy.getExceptionPolicy();
                TimeoutPolicy timeoutPolicy = policy.getTimeoutPolicy();
                if (timeoutPolicy != null && timeoutPolicy.test(request)) {
                    //请求超时了
                    future.completeExceptionally(t);
                } else if ((!(t instanceof LafException) || !((LafException) t).isRetry())
                        && (exceptionPolicy == null || !exceptionPolicy.test(t))) {
                    //不需要重试的异常
                    future.completeExceptionally(t);
                } else if (retry >= policy.getMaxRetry()) {
                    //超过重试次数
                    future.completeExceptionally(overloadFunction.apply(policy.getMaxRetry()));
                } else {
                    int size = configs.length;
                    if (retry == size - 1 && policy.isOnlyOncePerNode()) {
                        //每个节点只重试一次
                        Futures.completeExceptionally(future, emptyFunction.apply(retry, false));
                    } else {
                        Invocation invocation = request.getPayLoad();
                        //重新设置剩余超时时间
                        policy.getTimeoutPolicy().reset(request);
                        //TODO 考虑不同的环境和协议版本需要不同的参数，切换分组的时候重新生成一份对象（是否合理）
                        request.setPayLoad(new Invocation(invocation.getClazz(), invocation.getMethod(), invocation.getArgs()));
                        retry(request, retry + 1, policy, future);
                    }
                }
            }
        });
    }

}
