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

import io.joyrpc.Result;
import io.joyrpc.config.InterfaceOption.Concurrency;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.OverloadException;
import io.joyrpc.extension.Extension;
import io.joyrpc.filter.AbstractConcurrencyFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

/**
 * 服务端方法级别的并发限制
 */
@Extension(value = "concurrency", order = ProviderFilter.CONCURRENCY_ORDER)
public class ConcurrencyFilter extends AbstractConcurrencyFilter implements ProviderFilter {

    @Override
    protected Result onExceed(final RequestMessage<Invocation> request, final Concurrency concurrency) {
        Invocation invocation = request.getPayLoad();
        //TODO 被限流后会抛出大量异常，很耗CPU
        return new Result(request.getContext(),
                new OverloadException("Failed to invoke method " + invocation.getClassName() + "." + invocation.getMethodName()
                        + ", The service using threads greater than: " + concurrency.getMax() + ". Change it by interface or method concurrency",
                        ExceptionCode.FILTER_CONCURRENT_PROVIDER_TIMEOUT, 0, true));
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }

}
