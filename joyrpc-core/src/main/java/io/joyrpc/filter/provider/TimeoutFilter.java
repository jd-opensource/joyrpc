package io.joyrpc.filter.provider;

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
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.extension.Extension;
import io.joyrpc.filter.AbstractProviderFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 超时过滤器
 */
@Extension(value = "timeout", order = ProviderFilter.TIMEOUT_ORDER)
public class TimeoutFilter extends AbstractProviderFilter {

    private final static Logger logger = LoggerFactory.getLogger(TimeoutFilter.class);

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        //根据请求头的实际来进行超声判断
        long timeout = request.getHeader().getTimeout();
        long start = SystemClock.now();
        return invoker.invoke(request).whenComplete((res, err) -> {
            Invocation invocation = request.getPayLoad();
            if (timeout > 0) {
                long elapsed = SystemClock.now() - start;
                if (elapsed > timeout) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(ExceptionCode.format(ExceptionCode.FILTER_PROVIDER_TIMEOUT) + "Provider invoke method [" + invocation.getClassName() + "."
                                + invocation.getMethodName() + "] timeout. "
                                + ", timeout is " + timeout + " ms, invoke elapsed " + elapsed + " ms.");
                    }
                }
            }
        });
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }
}
