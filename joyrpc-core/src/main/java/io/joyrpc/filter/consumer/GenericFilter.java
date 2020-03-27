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

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.constants.Constants;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.AbstractConsumerFilter;
import io.joyrpc.filter.ConsumerFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.concurrent.CompletableFuture;

/**
 * @description: 调用端的泛化调用过滤器
 * 将泛化调用拼成普通调用，注意有可能参数值和参数类型不匹配，要服务端处理
 */
@Extension(value = "generic", order = ConsumerFilter.GENERIC_ORDER)
public class GenericFilter extends AbstractConsumerFilter {

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {

        Invocation invocation = request.getPayLoad();
        // generic 调用 consumer不处理，服务端做转换
        if (invocation.isGeneric()) {
            //真实的方法名
            invocation.setMethodName(request.getMethodName());
            //设置泛化标示
            invocation.addAttachment(Constants.GENERIC_OPTION.getName(), true);
        }

        return invoker.invoke(request);
    }

    @Override
    public boolean test(URL url) {
        return url.getBoolean(Constants.GENERIC_OPTION);
    }

    @Override
    public int type() {
        return INNER;
    }
}
