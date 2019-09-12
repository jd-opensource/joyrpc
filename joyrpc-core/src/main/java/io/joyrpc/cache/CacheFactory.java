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

import io.joyrpc.extension.Extensible;

/**
 * 缓存提供者
 */
@Extensible("cacheFactory")
public interface CacheFactory {

    int CAFFEINE_ORDER = 100;

    int CACHE2K_ORDER = CAFFEINE_ORDER + 1;

    int GUAVA_ORDER = CACHE2K_ORDER + 1;

    int MAP_ORDER = Short.MAX_VALUE;

    /**
     * 根据名称获取缓存
     *
     * @param name   名称
     * @param config 缓存配置
     * @return
     */
    <K, V> Cache<K, V> build(String name, CacheConfig<K, V> config);
}
