package io.joyrpc.cache.guava;

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

import com.google.common.cache.CacheBuilder;
import io.joyrpc.cache.Cache;
import io.joyrpc.cache.CacheConfig;
import io.joyrpc.cache.CacheFactory;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.joyrpc.cache.CacheFactory.GUAVA_ORDER;

/**
 * GuavaCache实现
 */
@Extension(value = "guava", provider = "google", order = GUAVA_ORDER)
@ConditionalOnClass("com.google.common.cache.CacheBuilder")
public class GuavaCacheFactory implements CacheFactory {
    @Override
    public <K, V> Cache<K, V> build(final String name, final CacheConfig<K, V> config) {
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (config.getExpireAfterWrite() > 0) {
            cacheBuilder.expireAfterWrite(config.getExpireAfterWrite(), TimeUnit.MILLISECONDS);
        }
        cacheBuilder.maximumSize(config.getCapacity() > 0 ? config.getCapacity() : Long.MAX_VALUE);
        com.google.common.cache.Cache<K, Optional<V>> cache = cacheBuilder.build();
        return new GuavaCache(cache, config);
    }
}
