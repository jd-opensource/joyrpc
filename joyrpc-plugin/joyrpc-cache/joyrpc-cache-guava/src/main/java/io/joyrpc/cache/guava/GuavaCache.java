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

import io.joyrpc.cache.AbstractCache;
import io.joyrpc.cache.CacheConfig;
import io.joyrpc.cache.CacheObject;

import java.util.concurrent.CompletableFuture;

/**
 * GuavaCache
 *
 * @param <K>
 * @param <V>
 */
public class GuavaCache<K, V> extends AbstractCache<K, V> {
    /**
     * Cache
     */
    protected com.google.common.cache.Cache<K, CacheObject<V>> cache;

    /**
     * 构造函数
     *
     * @param cache  缓存
     * @param config 配置
     */
    public GuavaCache(com.google.common.cache.Cache<K, CacheObject<V>> cache, CacheConfig<K, V> config) {
        this.cache = cache;
        this.config = config == null ? new CacheConfig<>() : config;
    }

    @Override
    protected CompletableFuture<Void> doPut(final K key, final V value) {
        cache.put(key, new CacheObject<>(value));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<CacheObject<V>> doGet(final K key) {
        return CompletableFuture.completedFuture(cache.getIfPresent(key));
    }

    @Override
    protected CompletableFuture<Void> doRemove(final K key) {
        cache.invalidate(key);
        return CompletableFuture.completedFuture(null);
    }

}
