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

import io.joyrpc.context.ConfigEvent.EventType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 抽象的接口动态配置，配置都是单线程更新
 */
public class AbstractInterfaceConfiguration<K, V> {

    public static final String DEFAULT = "*";

    /**
     * 配置
     */
    protected Map<K, V> configs = new ConcurrentHashMap<>();
    /**
     * 配置条数
     */
    protected volatile int size;
    /**
     * 监听器
     */
    protected List<Consumer<ConfigEvent<K, V>>> consumers = new CopyOnWriteArrayList<>();

    /**
     * 获取配置
     *
     * @param key 键
     * @return 值
     */
    public V get(final K key) {
        return key == null || size == 0 ? null : configs.get(key);
    }

    /**
     * 修改配置
     *
     * @param key   键
     * @param value 值
     */
    public synchronized void update(final K key, final V value) {
        if (key != null) {
            if (value == null) {
                remove(key);
            } else {
                V old = configs.put(key, value);
                size++;
                publish(new ConfigEvent<>(old == null ? EventType.ADD : EventType.UPDATE, key, value));
            }
        }
    }

    /**
     * 删除配置
     *
     * @param key 键
     */
    public synchronized void remove(final K key) {
        if (key != null) {
            publish(new ConfigEvent<>(EventType.REMOVE, key, null));
            if (configs.remove(key) != null) {
                size--;
            }
        }
    }

    /**
     * 通知事件
     *
     * @param event 事件
     */
    protected void publish(final ConfigEvent<K, V> event) {
        if (event != null) {
            for (Consumer<ConfigEvent<K, V>> consumer : consumers) {
                consumer.accept(event);
            }
        }
    }

    /**
     * 添加监听器
     *
     * @param consumer 监听器
     */
    public void addListener(final Consumer<ConfigEvent<K, V>> consumer) {
        if (consumer != null) {
            consumers.add(consumer);
        }
    }

    /**
     * 移除监听器
     *
     * @param consumer 监听器
     * @return 是否成功
     */
    public boolean removeListener(final Consumer<ConfigEvent<K, V>> consumer) {
        return consumer != null && consumers.remove(consumer);
    }

}