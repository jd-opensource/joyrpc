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
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.limiter.LimiterConfiguration;
import io.joyrpc.exception.RateLimiterException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.AbstractProviderFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.cluster.distribution.RateLimiter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.cluster.distribution.RateLimiter.DELIMITER;

/**
 * 限流
 */
@Extension(value = "limiter", order = ProviderFilter.INVOKER_LIMITER_ORDER)
public class LimiterFilter extends AbstractProviderFilter {

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        //获取接口的限流器
        Map<String, RateLimiter> classLimiters = LimiterConfiguration.LIMITERS.get(invocation.getClassName());
        if (classLimiters != null && !classLimiters.isEmpty()) {
            //获取应用信息，已经从会话里面恢复为HIDDEN_KEY_APPID
            String appId = invocation.getAttachment(Constants.HIDDEN_KEY_APPID);
            if (appId == null) {
                appId = "";
            }
            String methodName = invocation.getMethodName();
            String alias = invocation.getAlias();
            //限流配置的组合
            String[] keys = new String[]{
                    String.join(DELIMITER, "", "", ""),
                    String.join(DELIMITER, methodName, "", ""),
                    String.join(DELIMITER, methodName, alias, ""),
                    String.join(DELIMITER, methodName, alias, appId)
            };
            RateLimiter limiter;
            //按照优先级获取限流配置
            for (int index = keys.length - 1; index >= 0; index--) {
                //查找限流器
                limiter = classLimiters.get(keys[index]);
                if (limiter != null) {
                    //找到了最佳限流器
                    if (!limiter.getPermission()) {
                        return CompletableFuture.completedFuture(new Result(request.getContext(),
                                new RateLimiterException("Invocation of " + invocation.getClassName() + "." + methodName + " of app " + appId
                                        + " is over invoke limit, please wait next period or add upper limit.", ExceptionCode.FILTER_INVOKE_LIMIT)
                        ));
                    } else {
                        break;
                    }
                }
            }

        }
        return invoker.invoke(request);
    }

    @Override
    public boolean test(final URL url) {
        //该接口启用限流
        if (Boolean.TRUE.equals(url.getBoolean(Constants.LIMITER_OPTION))) {
            return true;
        }
        return false;
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }

}
