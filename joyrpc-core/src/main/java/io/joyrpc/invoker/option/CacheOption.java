package io.joyrpc.invoker.option;

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

import io.joyrpc.cache.Cache;
import io.joyrpc.cache.CacheKeyGenerator;

/**
 * 缓存选项
 */
public class CacheOption {
    /**
     * 缓存接口
     */
    protected final Cache<Object, Object> cache;
    /**
     * 缓存键生成器
     */
    protected final CacheKeyGenerator generator;

    public CacheOption(Cache<Object, Object> cache, CacheKeyGenerator generator) {
        this.cache = cache;
        this.generator = generator;
    }

    public Cache<Object, Object> getCache() {
        return cache;
    }

    public CacheKeyGenerator getGenerator() {
        return generator;
    }
}
