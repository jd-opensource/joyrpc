package io.joyrpc.cache.map;

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
import io.joyrpc.cache.CacheConfig;
import io.joyrpc.cache.CacheFactory;
import io.joyrpc.extension.Extension;

import static io.joyrpc.cache.CacheFactory.MAP_ORDER;

@Extension(value = "map", order = MAP_ORDER)
public class MapCacheFactory implements CacheFactory {

    @Override
    public <K, V> Cache<K, V> build(final String name, final CacheConfig<K, V> config) {
        return new MapCache<>(name, config);
    }
}
