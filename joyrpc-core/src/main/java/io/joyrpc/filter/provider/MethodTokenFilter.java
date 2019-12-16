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
import io.joyrpc.exception.AuthenticationException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.AbstractProviderFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.GenericMethodOption;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.constants.Constants.HIDDEN_KEY_TOKEN;

/**
 * 方法鉴权<br>
 *
 * @description: 请求携带方法token，用于判断是否有权限调用</br>
 */
@Extension(value = "token", order = ProviderFilter.TOKEN_ODER)
public class MethodTokenFilter extends AbstractProviderFilter {

    /**
     * 缓存令牌
     */
    protected GenericMethodOption<Optional<String>> tokens;

    @Override
    public void setup() {
        final String defToken = url.getString(HIDDEN_KEY_TOKEN);
        tokens = new GenericMethodOption<>(clazz, className,
                methodName -> Optional.ofNullable(url.getString(getOption(methodName, HIDDEN_KEY_TOKEN, defToken))));
    }

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        //方法鉴权
        Invocation invocation = request.getPayLoad();
        //providerToken在配置中
        Optional<String> optional = tokens.get(invocation.getMethodName());
        String token = optional.orElse("");
        if (!token.isEmpty() && !token.equals(invocation.getAttachment(HIDDEN_KEY_TOKEN))) {
            AuthenticationException exception = new AuthenticationException("Invalid token! Invocation of "
                    + invocation.getClassName() + "." + invocation.getMethodName()
                    + " from consumer " + request.getRemoteAddress()
                    + " to provider " + request.getLocalAddress()
                    + " are forbidden by server. ", ExceptionCode.PROVIDER_INVALID_TOKEN);
            return CompletableFuture.completedFuture(new Result(request.getContext(), exception));
        }
        return invoker.invoke(request);
    }

    @Override
    public boolean test(final URL url) {
        String token = url.getString(HIDDEN_KEY_TOKEN);
        if (token == null || token.isEmpty()) {
            Map<String, String> tokens = url.endsWith("." + HIDDEN_KEY_TOKEN);
            return tokens == null || tokens.isEmpty();
        }
        return true;
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }
}
