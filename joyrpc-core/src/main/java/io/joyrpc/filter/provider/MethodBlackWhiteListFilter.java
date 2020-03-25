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
import io.joyrpc.config.InterfaceOption.ProviderMethodOption;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.AuthorizationException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.AbstractProviderFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 方法调用黑白名单<br>
 * 检查服务端接口发布了哪些方法，放在此处是为了generic解析后再判断<br>
 */
@Extension(value = "methodBlackWhiteList", order = ProviderFilter.METHOD_BLACK_WHITE_LIST_ORDER)
public class MethodBlackWhiteListFilter extends AbstractProviderFilter {
    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        ProviderMethodOption option = (ProviderMethodOption) request.getOption();
        BlackWhiteList<String> blackWhiteList = option.getMethodBlackWhiteList();
        //校验方法黑白名单
        if (blackWhiteList != null && !blackWhiteList.isValid(invocation.getMethodName())) {
            return CompletableFuture.completedFuture(new Result(request.getContext(),
                    new AuthorizationException(
                            String.format("invocation %s is not passed through method blackWhiteList of %s. ",
                                    invocation.getMethodName(), invocation.getClassName()), null)));
        } else {
            // 调用
            return invoker.invoke(request);
        }
    }

    @Override
    public boolean test(final URL url) {
        //判断是否配置了方法黑白名单
        String include = url.getString(Constants.METHOD_INCLUDE_OPTION.getName(), null);
        String exclude = url.getString(Constants.METHOD_EXCLUDE_OPTION.getName(), null);
        return include != null && !include.isEmpty() || exclude != null && !exclude.isEmpty();
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }
}
