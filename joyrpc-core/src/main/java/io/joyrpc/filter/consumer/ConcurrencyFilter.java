package io.joyrpc.filter.consumer;

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
import io.joyrpc.filter.ConsumerFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.SystemClock;

/**
 * 按接口和方法进行限制<br>
 */
@Extension(value = "concurrency", order = ConsumerFilter.CONCURRENCY_ORDER)
public class ConcurrencyFilter extends AbstractConcurrencyFilter implements ConsumerFilter {

    @Override
    protected void onInvokeException(final Concurrency concurrency) {
        concurrency.decrement();
        concurrency.wakeup();
    }

    @Override
    protected void onInvokeComplete(final Concurrency concurrency) {
        concurrency.decrement();
        concurrency.wakeup();
    }

    @Override
    protected Result onExceed(final RequestMessage<Invocation> request, final Concurrency concurrency) {
        long start = SystemClock.now();
        Invocation invocation = request.getPayLoad();
        long active;
        int timeout = request.getHeader().getTimeout();
        long remain = timeout;
        long elapsed;
        long max = concurrency.getMax();
        while ((active = concurrency.getActives()) >= max) {
            concurrency.await(remain);
            elapsed = SystemClock.now() - start;
            remain = timeout - elapsed;
            if (remain <= 0) {
                //TODO 被限流后会抛出大量异常，很耗CPU
                return new Result(request.getContext(),
                        new OverloadException("Waiting concurrent timeout in client-side when invoke"
                                + invocation.getClassName() + "." + invocation.getMethodName() + ", elapsed: " + elapsed
                                + ", timeout: " + timeout + ". concurrent invokes: " + active
                                + ". max concurrency: " + max + ". You can change it by interface or method concurrency",
                                ExceptionCode.FILTER_CONCURRENT_CONSUMER_TIMEOUT, 0, false));
            } else {
                request.getHeader().setTimeout((int) remain);
            }
        }
        return null;
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }

}
