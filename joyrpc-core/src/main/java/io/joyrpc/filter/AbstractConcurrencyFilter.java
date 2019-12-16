package io.joyrpc.filter;

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

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.extension.Converts;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.GenericMethodOption;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static io.joyrpc.constants.Constants.CONCURRENCY_OPTION;
import static io.joyrpc.constants.Constants.TIMEOUT_OPTION;

/**
 * 调用端并发限制器，按接口和方法进行限制
 */
public abstract class AbstractConcurrencyFilter extends AbstractFilter {

    /**
     * 方法的并发选项
     */
    protected GenericMethodOption<ConcurrencyOption> concurrencys;

    @Override
    public void setup() {
        final int defTimeout = url.getPositiveInt(TIMEOUT_OPTION);
        final int defConcurrency = url.getInteger(CONCURRENCY_OPTION);
        concurrencys = new GenericMethodOption<>(clazz, className, methodName ->
                new ConcurrencyOption(
                        url.getInteger(getOption(methodName, CONCURRENCY_OPTION.getName(), defConcurrency)),
                        url.getInteger(getOption(methodName, TIMEOUT_OPTION.getName(), defTimeout)),
                        getMetric(methodName)));
    }

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        ConcurrencyOption option = concurrencys.get(invocation.getMethodName());
        //并发数
        int concurrency = option == null ? 0 : option.concurrency;
        if (concurrency <= 0) {
            return invoker.invoke(request);
        }
        //指标
        final Concurrency metric = option.getMetric();
        if (null == metric) {
            return invoker.invoke(request);
        }
        //判断是否超出并发数
        if (metric.getActives() >= concurrency) {
            //等待
            Result result = onExceed(request, concurrency, option.timeout, metric);
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }
        }
        //执行调用
        CompletableFuture<Result> future = null;
        try {
            onInvoke(metric);
            future = invoker.invoke(request);
            return future.whenComplete((result, throwable) -> onInvokeComplete(metric));
        } finally {
            if (future == null) {
                onInvokeException(metric);
            }
        }
    }

    /**
     * 获取并发数指标
     *
     * @param methodName
     * @return
     */
    protected Concurrency getMetric(final String methodName) {
        return new Concurrency();
    }

    /**
     * 调用前
     *
     * @param metric
     */
    protected void onInvoke(final Concurrency metric) {
        metric.add();
    }

    /**
     * 调用异常
     *
     * @param metric
     */
    protected void onInvokeException(final Concurrency metric) {
        metric.decrement();
    }

    /**
     * 调用完成
     *
     * @param metric
     */
    protected void onInvokeComplete(final Concurrency metric) {
        metric.decrement();
    }

    /**
     * 超出并发数
     *
     * @param request
     * @param concurrency
     * @param timeout
     * @param metric
     * @return
     */
    protected abstract Result onExceed(RequestMessage<Invocation> request, int concurrency, int timeout, Concurrency metric);

    @Override
    public boolean test(final URL url) {
        if (url.getInteger(CONCURRENCY_OPTION) > 0) {
            return true;
        }
        Map<String, String> tokens = url.endsWith("." + CONCURRENCY_OPTION.getName());
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            if (Converts.getPositive(entry.getValue(), 0) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }

    /**
     * 并发选项
     */
    protected static class ConcurrencyOption {
        /**
         * 并发数
         */
        protected int concurrency;
        /**
         * 超时时间
         */
        protected int timeout;
        /**
         * 指标的名称
         */
        protected Concurrency metric;

        /**
         * 构造函数
         *
         * @param concurrency
         * @param timeout
         */
        public ConcurrencyOption(final int concurrency, final int timeout, final Concurrency metric) {
            this.concurrency = concurrency;
            this.timeout = timeout;
            this.metric = metric;
        }

        public int getConcurrency() {
            return concurrency;
        }

        public int getTimeout() {
            return timeout;
        }

        public Concurrency getMetric() {
            return metric;
        }
    }

    /**
     * 并发数指标
     */
    protected static class Concurrency {

        /**
         * 活动并发
         */
        protected AtomicLong actives;

        /**
         * 构造函数
         */
        public Concurrency() {
            actives = new AtomicLong();
        }

        /**
         * 当前并发数
         *
         * @return
         */
        public long getActives() {
            return actives.get();
        }

        /**
         * 增加
         */
        public void add() {
            actives.incrementAndGet();
        }

        /**
         * 减少并发数
         */
        public void decrement() {
            actives.decrementAndGet();
        }

        /**
         * 唤醒
         */
        public void wakeup() {
            synchronized (this) {
                // 调用结束 通知等待的人
                notifyAll();
            }
        }

        /**
         * 等到
         *
         * @param time
         * @return
         */
        public boolean await(final long time) {
            if (time <= 0) {
                return true;
            }
            synchronized (this) {
                try {
                    // 等待执行
                    wait(time);
                    return true;
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }

    }
}
