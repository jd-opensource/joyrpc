package io.joyrpc.cluster.distribution.router.failover;

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
import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.distribution.ExceptionPolicy;
import io.joyrpc.cluster.distribution.FailoverPolicy;
import io.joyrpc.cluster.distribution.FailoverSelector;
import io.joyrpc.cluster.distribution.TimeoutPolicy;
import io.joyrpc.cluster.distribution.router.AbstractRouter;
import io.joyrpc.cluster.distribution.router.failover.simple.SimpleFailoverSelector;
import io.joyrpc.config.InterfaceOption.ConsumerMethodOption;
import io.joyrpc.exception.FailoverException;
import io.joyrpc.exception.LafException;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.Futures;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.cluster.distribution.Router.FAIL_OVER;
import static io.joyrpc.cluster.distribution.Router.ORDER_FAILOVER;

/**
 * 异常重试
 */
@Extension(value = FAIL_OVER, order = ORDER_FAILOVER)
public class FailoverRouter extends AbstractRouter {

    /**
     * 构建过载异常
     *
     * @param maxRetry 最大重试次数
     * @return 异常
     */
    protected Throwable createOverloadException(final int maxRetry, final Throwable cause) {
        return new FailoverException(String.format("Maximum number %d of retries reached. The last exception caused by %s ",
                maxRetry, cause.getMessage()), cause);
    }

    /**
     * 构建没有节点异常
     *
     * @param retrys     重试次数
     * @param candidates 候选者数量
     * @param retry      是否可以重试
     * @return 异常
     */
    protected Throwable createEmptyException(final int retrys, final int candidates, final boolean retry) {
        return new FailoverException(String.format("there is not any suitable node after retrying %d. candidates size %d", retrys, candidates), retry);
    }

    @Override
    public CompletableFuture<Result> route(final RequestMessage<Invocation> request, final Candidate candidate) {
        //获取重试策略
        ConsumerMethodOption option = (ConsumerMethodOption) request.getOption();
        FailoverPolicy policy = option.getFailoverPolicy();
        //判断是否需要重试
        boolean retry = policy != null && policy.getMaxRetry() > 0;
        if (!retry) {
            //不重试
            Node node = loadBalance.select(candidate, request);
            return node != null ? operation.apply(node, null, request) :
                    Futures.completeExceptionally(createEmptyException(0, candidate.getSize(), true));
        }
        CompletableFuture<Result> result = new CompletableFuture<>();
        retry(request, null, candidate, 0, policy, candidate.getNodes(), result);
        return result;
    }

    /**
     * 递归重试
     *
     * @param request   请求
     * @param last      前一次重试节点
     * @param candidate 候选者
     * @param retry     当前重试次数
     * @param policy    重试策略
     * @param origins   原始节点
     * @param future    结束Future
     */
    protected void retry(final RequestMessage<Invocation> request,
                         final Node last,
                         final Candidate candidate,
                         final int retry,
                         final FailoverPolicy policy,
                         final List<Node> origins,
                         final CompletableFuture<Result> future) {
        //负载均衡选择节点
        try {
            final Node node = loadBalance.select(candidate, request);
            if (retry > 0) {
                //便于向服务端注入重试次数
                request.setRetryTimes(retry);
            }
            //调用，如果节点不存在，则抛出Failover异常。
            CompletableFuture<Result> result = node != null ? operation.apply(node, last, request) :
                    Futures.completeExceptionally(createEmptyException(retry, origins.size(), candidate.getNodes().size() != origins.size()));
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
                        future.completeExceptionally(createOverloadException(policy.getMaxRetry(), t));
                    } else {
                        //删除失败的节点进行重试
                        List<Node> shards = candidate.getNodes();
                        int size = shards.size();
                        if (size == 1 && policy.isOnlyOncePerNode()) {
                            //每个节点只重试一次
                            Futures.completeExceptionally(future, createEmptyException(retry, origins.size(), false));
                        } else {
                            FailoverSelector selector = policy.getRetrySelector();
                            if (selector == null) {
                                selector = SimpleFailoverSelector.INSTANCE;
                            }
                            if (timeoutPolicy != null) {
                                //设置新的超时时间
                                timeoutPolicy.reset(request);
                            }
                            retry(request, node, selector.select(candidate, node, retry, null, origins),
                                    retry + 1, policy, origins, future);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            //系统运行时异常不重试了
            future.completeExceptionally(e);
        }
    }

}
