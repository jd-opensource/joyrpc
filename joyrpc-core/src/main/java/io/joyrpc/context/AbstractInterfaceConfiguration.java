package io.joyrpc.context;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽象的接口配置，配置都是单线程更新
 */
public class AbstractInterfaceConfiguration<K, V> {

    public static final String DEFAULT = "*";

    /**
     * 配置
     */
    protected Map<K, V> configs = new ConcurrentHashMap<>();
    protected volatile int size;

    /**
     * 获取配置
     *
     * @param key
     * @return
     */
    public V get(final K key) {
        return key == null || size == 0 ? null : configs.get(key);
    }

    /**
     * 修改配置
     *
     * @param key
     * @param value
     */
    public synchronized void update(final K key, final V value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            if (configs.remove(key) != null) {
                size--;
            }
        } else {
            configs.put(key, value);
            size++;
        }
    }

    /**
     * 删除配置
     *
     * @param key
     */
    public synchronized void remove(final K key) {
        if (key != null) {
            if (configs.remove(key) != null) {
                size--;
            }
        }
    }

}
