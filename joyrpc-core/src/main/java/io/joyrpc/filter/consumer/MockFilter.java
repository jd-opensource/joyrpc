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
import io.joyrpc.config.InterfaceOption.ConsumerMethodOption;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.AbstractConsumerFilter;
import io.joyrpc.filter.ConsumerFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @description: Mock过滤器<br>
 * 服务端和客户端通用，就是如果设置mock=true就走本地mock实现<br>
 */
@Extension(value = "mock", order = ConsumerFilter.MOCK_ORDER)
public class MockFilter extends AbstractConsumerFilter {

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        Map<String, Object> mocks = ((ConsumerMethodOption) request.getOption()).getMock();
        Object result = mocks == null ? null : mocks.get(invocation.getAlias());
        return result != null ?
                CompletableFuture.completedFuture(
                        new Result(request.getContext(), result)) :
                invoker.invoke(request);
    }

    @Override
    public boolean test(final URL url) {
        //兼容老版本，测试环境默认开启，如果环境设置了mock=false则禁用
        Parametric parametric = new MapParametric(GlobalContext.getGlobalSetting());
        return parametric.getBoolean(Constants.MOCK_OPTION);
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }
}
