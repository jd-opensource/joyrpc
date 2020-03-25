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
import io.joyrpc.cluster.distribution.RateLimiter;
import io.joyrpc.config.InterfaceOption.ProviderMethodOption;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.limiter.LimiterConfiguration.ClassLimiter;
import io.joyrpc.context.limiter.LimiterConfiguration.Option;
import io.joyrpc.exception.RateLimiterException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.AbstractProviderFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 限流
 */
@Extension(value = "limiter", order = ProviderFilter.INVOKER_LIMITER_ORDER)
public class LimiterFilter extends AbstractProviderFilter {

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        ProviderMethodOption methodOption = (ProviderMethodOption) request.getOption();
        //获取接口的限流器
        ClassLimiter classLimiters = methodOption.getLimiter();
        if (classLimiters != null) {
            //获取应用信息，已经从会话里面恢复为HIDDEN_KEY_APPID
            String appId = invocation.getAttachment(Constants.HIDDEN_KEY_APPID, "");
            String methodName = invocation.getMethodName();
            String alias = invocation.getAlias();
            //限流配置的组合
            Option option = new Option(methodName, alias, appId);
            //获取最佳限流配置
            RateLimiter limiter = classLimiters.get(option);
            if (limiter != null && !limiter.getPermission()) {
                return CompletableFuture.completedFuture(new Result(request.getContext(),
                        new RateLimiterException("Invocation of " + invocation.getClassName() + "." + methodName + " of app " + appId
                                + " is over invoke limit, please wait next period or add upper limit.", ExceptionCode.FILTER_INVOKE_LIMIT)
                ));
            }
        }
        return invoker.invoke(request);
    }

    @Override
    public boolean test(final URL url) {
        return url.getBoolean(Constants.LIMITER_OPTION);
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }

}
