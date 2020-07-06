package io.joyrpc.filter;

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

import io.joyrpc.extension.Extensible;

/**
 * 服务提供者过滤器接口
 * filter order
 * ProviderExceptionFilter
 * ProviderTimeCheckFilter
 * ProviderContextFilter
 * ProviderTokenFilter
 * ProviderGenericFilter
 * ProviderSecurityFilter
 * ProviderMethodCheckFilter
 * ProviderValidationFilter
 * ProviderConcurrentsFilter
 * ProviderHttpGWFilter
 * ProviderTimeoutFilter
 * ProviderCacheFilter
 *
 * @date: 8/1/2019
 */
@Extensible("providerFilter")
@FunctionalInterface
public interface ProviderFilter extends Filter {

    int TRACE_ORDER = -130;

    int EXCEPTION_ORDER = -120;

    int AUTHORIZATION_ODER = -100;

    int IP_WHITE_BLACK_LIST_ORDER = -90;

    int GENERIC_ORDER = -80;

    /**
     * 方法检测，需要在泛化调用之后
     */
    int METHOD_BLACK_WHITE_LIST_ORDER = GENERIC_ORDER + 10;

    int VALIDATION_ORDER = METHOD_BLACK_WHITE_LIST_ORDER + 10;

    int CONCURRENCY_ORDER = VALIDATION_ORDER + 10;

    int INVOKER_LIMITER_ORDER = CONCURRENCY_ORDER + 10;

    int TIMEOUT_ORDER = INVOKER_LIMITER_ORDER + 10;

    int CACHE_ORDER = TIMEOUT_ORDER + 10;

}
