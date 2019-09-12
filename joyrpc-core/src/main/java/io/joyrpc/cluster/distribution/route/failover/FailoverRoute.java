package io.joyrpc.cluster.distribution.route.failover;

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

import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.distribution.*;
import io.joyrpc.cluster.distribution.route.AbstractRoute;
import io.joyrpc.cluster.distribution.route.failover.simple.SimpleFailoverSelector;
import io.joyrpc.exception.FailoverException;
import io.joyrpc.exception.LafException;
import io.joyrpc.extension.Extension;
import io.joyrpc.util.Futures;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.joyrpc.cluster.distribution.Route.FAIL_OVER;
import static io.joyrpc.cluster.distribution.Route.ORDER_FAILOVER;

/**
 * 异常重试
 */
@Extension(value = FAIL_OVER, order = ORDER_FAILOVER)
public class FailoverRoute<T, R> extends AbstractRoute<T, R> implements RouteFailover<T, R> {

    /**
     * 最大重试次数
     */
    protected FailoverPolicy retryPolicy;
    /**
     * 超过最大次数
     */
    protected Function<Integer, Throwable> overloadFunction = (maxRetry) -> new FailoverException(String.format("Maximum number %d of retries reached", maxRetry));

    /**
     * 负责均衡没有选择出合适的节点
     */
    protected BiFunction<Integer, Boolean, Throwable> emptyFunction = (count, retry) -> new FailoverException(String.format("there is not any node after retrying %d", count), retry);
    /**
     * 重试函数
     */
    protected Function<T, FailoverPolicy> retryFunction = (request) -> retryPolicy;

    @Override
    public void setRetryFunction(Function<T, FailoverPolicy> retryFunction) {
        this.retryFunction = retryFunction;
    }

    @Override
    public CompletableFuture<R> invoke(T request, Candidate candidate) {
        //获取重试策略
        FailoverPolicy policy = retryFunction.apply(request);
        //判断是否需要重试
        boolean retry = policy != null && policy.getMaxRetry() > 0;
        if (!retry) {
            //不重试
            Node node = loadBalance.select(candidate, request);
            return node != null ? function.apply(node, request) :
                    Futures.completeExceptionally(emptyFunction.apply(0, true));
        }
        CompletableFuture<R> result = new CompletableFuture<>();
        retry(request, candidate, 0, policy == null ? retryPolicy : policy, candidate.getNodes(), result);
        return result;
    }

    /**
     * 递归重试
     *
     * @param request   请求
     * @param candidate 候选者
     * @param retry     当前重试次数
     * @param policy    重试策略
     * @param origins   原始节点
     * @param future    结束Future
     */
    protected void retry(final T request, final Candidate candidate,
                         final int retry, final FailoverPolicy policy,
                         final List<Node> origins, final CompletableFuture<R> future) {
        //TODO 考虑不同的节点协议不同或环境不同，是否需要重新创建一份Invocation并进行绑定
        //负载均衡选择节点
        final Node node = loadBalance.select(candidate, request);
        //调用，如果节点不存在，则抛出Failover异常。
        CompletableFuture<R> result = node != null ? function.apply(node, request) :
                Futures.completeExceptionally(emptyFunction.apply(retry, candidate.getNodes().size() != origins.size()));
        result.whenComplete((r, t) -> {
            ExceptionPolicy<R> exceptionPolicy = policy.getExceptionPolicy();
            t = t == null && exceptionPolicy != null ? exceptionPolicy.getThrowable(r) : t;
            if (t == null) {
                future.complete(r);
            } else {
                TimeoutPolicy<T> timeoutPolicy = policy.getTimeoutPolicy();
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
                    //删除失败的节点进行重试
                    List<Node> shards = candidate.getNodes();
                    int size = shards.size();
                    if (size == 1 && policy.isOnlyOncePerNode()) {
                        //每个节点只重试一次
                        Futures.completeExceptionally(future, emptyFunction.apply(retry, false));
                    } else {
                        FailoverSelector selector = policy.getRetrySelector();
                        if (selector == null) {
                            selector = SimpleFailoverSelector.INSTANCE;
                        }
                        if (timeoutPolicy != null) {
                            //设置新的超时时间
                            timeoutPolicy.reset(request);
                        }
                        retry(request, selector.select(candidate, node, retry, null, origins),
                                retry + 1, policy, origins, future);
                    }
                }
            }
        });
    }

}
