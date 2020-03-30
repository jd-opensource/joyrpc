package io.joyrpc.cache;

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

import io.joyrpc.exception.CacheException;
import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.Prototype;
import io.joyrpc.protocol.message.Call;

/**
 * 缓存键生成器
 */
@Extensible(value = "cacheKeyGenerator")
public interface CacheKeyGenerator {

    int SPEL_ORDER = 100;

    int JEXL_ORDER = SPEL_ORDER + 10;

    /**
     * 产生缓存的Key
     *
     * @param invocation 调用请求
     * @return 键
     * @throws CacheException 缓存异常
     */
    Object generate(Call invocation) throws CacheException;

    /**
     * 基于表达式的键生成器
     */
    interface ExpressionGenerator extends CacheKeyGenerator, Prototype {

        /**
         * 设置参数
         *
         * @param parameters 参数
         */
        void setParametric(Parametric parameters);

        /**
         * 构建表达式
         */
        void setup();
    }
}
