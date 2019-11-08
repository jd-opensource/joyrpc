package io.joyrpc.util;

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

/**
 * 键值对
 *
 * @param <K>
 * @param <V>
 */
public class Pair<K, V> {
    /**
     * 键
     */
    private final K key;
    /**
     * 值
     */
    private final V value;

    /**
     * 构造函数
     *
     * @param k
     * @param v
     */
    public Pair(K k, V v) {
        this.key = k;
        this.value = v;
    }

    /**
     * 构造函数
     *
     * @param entry
     */
    public Pair(final Pair<? extends K, ? extends V> entry) {
        this(entry.getKey(), entry.getValue());
    }

    public K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    /**
     * 构造方法
     *
     * @param k
     * @param v
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Pair<K, V> of(K k, V v) {
        return new Pair<>(k, v);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (key != null ? !key.equals(pair.key) : pair.key != null) {
            return false;
        }
        return value != null ? value.equals(pair.value) : pair.value == null;

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    /**
     * 创建
     *
     * @param k
     * @param v
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Pair<K, V> create(K k, V v) {
        return new Pair(k, v);
    }
}
