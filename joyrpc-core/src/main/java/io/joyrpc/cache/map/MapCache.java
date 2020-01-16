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

import io.joyrpc.cache.AbstractCache;
import io.joyrpc.cache.CacheConfig;
import io.joyrpc.cache.CacheObject;
import io.joyrpc.util.SystemClock;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Map的缓存，缓存空值
 */
public class MapCache<K, V> extends AbstractCache<K, V> {
    /**
     * 名称
     */
    protected String name;

    /**
     * 缓存
     */
    protected Map<K, MapCacheObject<V>> caches;

    /**
     * 构造函数
     *
     * @param name   名称
     * @param config 配置
     */
    public MapCache(String name, CacheConfig<K, V> config) {
        this.name = name;
        this.config = config == null ? new CacheConfig<>() : config;
        this.caches = createCaches();
    }

    /**
     * 创建缓存
     *
     * @return
     */
    protected Map<K, MapCacheObject<V>> createCaches() {
        return config.getCapacity() <= 0 ? new ConcurrentHashMap<>(1024) : Collections.synchronizedMap(new LRUHashMap<>(config.getCapacity()));
    }

    @Override
    protected CompletableFuture<Void> doPut(final K key, final V value) {
        long expireTime = config.getExpireAfterWrite() > 0 ? SystemClock.now() + config.getExpireAfterWrite() : -1;
        caches.put(key, new MapCacheObject<>(value, expireTime));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<CacheObject<V>> doGet(final K key) {
        //获取缓存
        MapCacheObject<V> cache = caches.get(key);
        if (cache != null && cache.isExpire()) {
            //过期了
            if (cache.getCounter().compareAndSet(0, 1)) {
                //让一个进行操作
                caches.remove(key);
            }
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(cache);
    }

    @Override
    protected CompletableFuture<Void> doRemove(final K key) {
        caches.remove(key);
        return CompletableFuture.completedFuture(null);
    }

}
