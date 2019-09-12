package io.joyrpc.context.limiter;

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

import io.joyrpc.context.AbstractInterfaceConfiguration;
import io.joyrpc.cluster.distribution.RateLimiter;

import java.util.Map;

/**
 * 限流管理器
 */
public class LimiterConfiguration extends AbstractInterfaceConfiguration<String, Map<String, RateLimiter>> {

    /**
     * 结果缓存
     */
    public static final LimiterConfiguration LIMITERS = new LimiterConfiguration();

    /**
     * 读取限流数据
     *
     * @param className
     * @param key
     * @return 结果
     */
    public RateLimiter get(final String className, final String key) {
        if (className == null || key == null) {
            return null;
        }
        Map<String, RateLimiter> classLimiter = LIMITERS.get(className);
        return classLimiter == null ? null : classLimiter.get(key);
    }

}
