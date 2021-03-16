package io.joyrpc.option;

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
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.proxy.MethodCaller;

/**
 * 服务提供者方法选项
 */
public interface ProviderMethodOption extends MethodOption {

    /**
     * 方法黑白名单
     *
     * @return 方法黑白名单
     */
    BlackWhiteList<String> getMethodBlackWhiteList();

    /**
     * 获取IP限制
     *
     * @return IP限制
     */
    IPPermission getIPPermission();

    /**
     * 获取限流配置
     *
     * @return 限流配置
     */
    LimiterConfiguration.ClassLimiter getLimiter();

    /**
     * 获取动态方法
     *
     * @return 动态方法
     */
    MethodCaller getCaller();

}
