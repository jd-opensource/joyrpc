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

/**
 * 配置事件
 *
 * @param <K>
 * @param <V>
 */
public class ConfigEvent<K, V> {
    /**
     * 事件类型
     */
    protected final EventType type;
    /**
     * 键
     */
    protected final K key;
    /**
     * 值
     */
    protected final V value;

    public ConfigEvent(EventType type, K key, V value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public EventType getType() {
        return type;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    /**
     * 事件类型
     */
    public enum EventType {
        /**
         * 新增
         */
        ADD,
        /**
         * 修改
         */
        UPDATE,
        /**
         * 删除
         */
        REMOVE
    }

}
