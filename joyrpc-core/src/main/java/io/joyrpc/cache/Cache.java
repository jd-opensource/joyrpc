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

import java.util.concurrent.CompletableFuture;

/**
 * 缓存接口，都改成异步，便于支持本地的分布式缓存，如Ignite
 */
public interface Cache<K, V> {

    /**
     * 放入缓存
     *
     * @param key   键
     * @param value 缓存数据
     */
    CompletableFuture<Void> put(K key, V value);

    /**
     * 从缓存中获取
     *
     * @param key 键
     * @return
     */
    CompletableFuture<CacheObject<V>> get(K key);

    /**
     * 移除缓存
     *
     * @param key 键
     */
    CompletableFuture<Void> remove(final K key);

}
