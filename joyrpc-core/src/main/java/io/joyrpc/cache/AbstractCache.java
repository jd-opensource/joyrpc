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
import io.joyrpc.util.Futures;

import java.util.concurrent.CompletableFuture;

/**
 * 同步缓存抽象实现
 *
 * @param <K>
 * @param <V>
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    /**
     * 配置
     */
    protected CacheConfig<K, V> config;

    @Override
    public CompletableFuture<Void> put(final K key, final V value) {
        if (key == null) {
            return Futures.completeExceptionally(new NullPointerException("key can not be null."));
        }
        try {
            if (value != null || config.nullable) {
                //判断是否缓存空值
                return doPut(key, value);
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return Futures.completeExceptionally(new CacheException(e.getMessage(), e));
        }

    }

    /**
     * 同步修改
     *
     * @param key   键
     * @param value 值
     */
    protected abstract CompletableFuture<Void> doPut(K key, V value);

    @Override
    public CompletableFuture<CacheObject<V>> get(final K key) {
        if (key == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return doGet(key);
        } catch (Exception e) {
            return Futures.completeExceptionally(new CacheException(e.getMessage(), e));
        }
    }

    /**
     * 同步获取
     *
     * @param key 键
     * @return 缓存对象
     */
    protected abstract CompletableFuture<CacheObject<V>> doGet(K key);

    @Override
    public CompletableFuture<Void> remove(final K key) {
        if (key == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return doRemove(key);
        } catch (Exception e) {
            return Futures.completeExceptionally(new CacheException(e.getMessage(), e));
        }

    }

    /**
     * 同步删除
     *
     * @param key 键
     */
    protected abstract CompletableFuture<Void> doRemove(K key);
}
