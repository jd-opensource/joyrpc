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
import io.joyrpc.config.InterfaceOption.Concurrency;
import io.joyrpc.extension.Converts;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.constants.Constants.CONCURRENCY_OPTION;

/**
 * 调用端并发限制器，按接口和方法进行限制
 */
public abstract class AbstractConcurrencyFilter extends AbstractFilter {

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Concurrency concurrency = request.getOption().getConcurrency();
        //并发数
        int max = concurrency.getMax();
        if (max <= 0) {
            return invoker.invoke(request);
        }
        //判断是否超出并发数
        if (concurrency.getActives() >= max) {
            //等待
            Result result = onExceed(request, concurrency);
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }
        }
        //执行调用
        CompletableFuture<Result> future = null;
        try {
            onInvoke(concurrency);
            future = invoker.invoke(request);
            return future.whenComplete((result, throwable) -> onInvokeComplete(concurrency));
        } finally {
            if (future == null) {
                onInvokeException(concurrency);
            }
        }
    }

    /**
     * 调用前
     *
     * @param concurrency 并发配置
     */
    protected void onInvoke(final Concurrency concurrency) {
        concurrency.add();
    }

    /**
     * 调用异常
     *
     * @param concurrency 并发配置
     */
    protected void onInvokeException(final Concurrency concurrency) {
        concurrency.decrement();
    }

    /**
     * 调用完成
     *
     * @param concurrency 并发配置
     */
    protected void onInvokeComplete(final Concurrency concurrency) {
        concurrency.decrement();
    }

    /**
     * 超出并发数
     *
     * @param request     请求
     * @param concurrency 并发配置
     * @return 结果
     */
    protected abstract Result onExceed(RequestMessage<Invocation> request, Concurrency concurrency);

    @Override
    public boolean test(final URL url) {
        if (url.getInteger(CONCURRENCY_OPTION) > 0) {
            return true;
        }
        Map<String, String> tokens = url.endsWith("." + CONCURRENCY_OPTION.getName());
        if (tokens.isEmpty()) {
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

}
