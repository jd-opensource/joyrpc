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

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * MAP工具类
 */
public abstract class Maps {

    /**
     * 不存在的计算
     *
     * @param map      Map对象
     * @param key      键
     * @param function 构造函数
     * @param consumer 消费者
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> V computeIfAbsent(final Map<K, V> map, final K key, final Function<K, V> function,
                                           final BiConsumer<V, Boolean> consumer) {
        AtomicBoolean add = new AtomicBoolean(false);
        V value = map.computeIfAbsent(key, o -> {
            add.set(true);
            return function.apply(o);
        });
        //先要添加到Map里面在消费，避免通知事件过早执行
        consumer.accept(value, add.get());
        return value;
    }

    /**
     * 修改
     *
     * @param map   Map对象
     * @param key   键
     * @param value 值
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> V put(final Map<K, V> map, final K key, final V value) {
        return key == null || value == null ? null : map.put(key, value);
    }

    /**
     * 获取值
     *
     * @param map       Map对象
     * @param key       键
     * @param candidate 候选键
     * @return 值
     */
    public static <K, V> V get(final Map<K, V> map, final K key, final K candidate) {
        V result = map.get(key);
        if (result == null) {
            result = map.get(candidate);
        }
        return result;
    }
}
