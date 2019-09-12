package io.joyrpc.cluster.distribution;

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

import io.joyrpc.cluster.distribution.limiter.RateLimiterConfig;
import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.Prototype;
import io.joyrpc.extension.Type;


/**
 * 服务端限流
 */
@Extensible("rateLimiter")
public interface RateLimiter extends Prototype, Type<String> {

    /**
     * 分隔符
     */
    String DELIMITER = "#";

    /**
     * 获取许可
     *
     * @return
     */
    boolean getPermission();

    /**
     * 加载配置
     *
     * @param config
     * @return
     */
    boolean reload(RateLimiterConfig config);
}
