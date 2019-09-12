package io.joyrpc.cache.cache2k;

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
import io.joyrpc.extension.condition.ConditionalOnClass;
import org.cache2k.Cache2kBuilder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.joyrpc.cache.CacheFactory.CACHE2K_ORDER;

/**
 * Cache2k实现
 */
@Extension(value = "cache2k", provider = "cache2k", order = CACHE2K_ORDER)
@ConditionalOnClass("org.cache2k.Cache2kBuilder")
public class Cache2kCacheFactory implements CacheFactory {
    @Override
    public <K, V> Cache<K, V> build(final String name, final CacheConfig<K, V> config) {
        Cache2kBuilder<K, Optional> cacheBuilder = Cache2kBuilder.forUnknownTypes();
        if (config.getKeyClass() != null) {
            cacheBuilder.keyType(config.getKeyClass());
        }
        cacheBuilder.valueType(Optional.class);
        cacheBuilder.permitNullValues(config.isNullable());
        cacheBuilder.entryCapacity(config.getCapacity() > 0 ? config.getCapacity() : Long.MAX_VALUE);

        if (config.getExpireAfterWrite() > 0) {
            cacheBuilder.expireAfterWrite(config.getExpireAfterWrite(), TimeUnit.MILLISECONDS);
        } else {
            cacheBuilder.eternal(true);
        }

        return new Cache2kCache(cacheBuilder.build(), config);
    }
}
