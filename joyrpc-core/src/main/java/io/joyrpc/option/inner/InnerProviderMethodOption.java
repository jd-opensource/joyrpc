package io.joyrpc.option.inner;

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

import io.joyrpc.context.auth.IPPermission;
import io.joyrpc.context.limiter.LimiterConfiguration;
import io.joyrpc.invoker.CallbackMethod;
import io.joyrpc.option.AbstractMethodOption;
import io.joyrpc.option.CacheOption;
import io.joyrpc.option.Concurrency;
import io.joyrpc.option.ProviderMethodOption;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.proxy.MethodCaller;
import io.joyrpc.transaction.TransactionOption;
import io.joyrpc.util.GenericMethod;
import io.joyrpc.util.IDLMethod;

import javax.validation.Validator;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 方法选项
 */
public class InnerProviderMethodOption extends AbstractMethodOption implements ProviderMethodOption {
    /**
     * 方法的黑白名单
     */
    protected BlackWhiteList<String> methodBlackWhiteList;
    /**
     * IP限制
     */
    protected Supplier<IPPermission> iPPermission;
    /**
     * 限流
     */
    protected Supplier<LimiterConfiguration.ClassLimiter> limiter;
    /**
     * 动态生成的方法调用
     */
    protected MethodCaller caller;

    public InnerProviderMethodOption(final IDLMethod idlMethod,
                                     final GenericMethod genericMethod,
                                     final Map<String, ?> implicits,
                                     final int timeout,
                                     final Concurrency concurrency,
                                     final CacheOption cachePolicy,
                                     final Validator validator,
                                     final TransactionOption transactionOption,
                                     final String token,
                                     final boolean async,
                                     final boolean trace,
                                     final CallbackMethod callback,
                                     final BlackWhiteList<String> methodBlackWhiteList,
                                     final Supplier<IPPermission> iPPermission,
                                     final Supplier<LimiterConfiguration.ClassLimiter> limiter,
                                     final MethodCaller caller) {
        super(idlMethod, genericMethod, implicits, timeout, concurrency, cachePolicy, validator, transactionOption, token, async, trace, callback);
        this.methodBlackWhiteList = methodBlackWhiteList;
        this.iPPermission = iPPermission;
        this.limiter = limiter;
        this.caller = caller;
    }

    @Override
    public BlackWhiteList<String> getMethodBlackWhiteList() {
        return methodBlackWhiteList;
    }

    @Override
    public IPPermission getIPPermission() {
        return iPPermission.get();
    }

    @Override
    public LimiterConfiguration.ClassLimiter getLimiter() {
        return limiter.get();
    }

    @Override
    public MethodCaller getCaller() {
        return caller;
    }
}
