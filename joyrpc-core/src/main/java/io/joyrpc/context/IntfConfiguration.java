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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 接口配置监听器
 */
public class IntfConfiguration<K, V> implements Consumer<ConfigEvent<K, V>>, Supplier<V> {

    /**
     * 接口配置
     */
    protected final AbstractInterfaceConfiguration<K, V> configuration;
    /**
     * 键
     */
    protected final K key;
    /**
     * 变更通知
     */
    protected final Consumer<V> consumer;
    /**
     * 当前接口配置
     */
    protected volatile V config;

    public IntfConfiguration(final AbstractInterfaceConfiguration<K, V> configuration, final K key) {
        this(configuration, key, null);
    }

    public IntfConfiguration(final AbstractInterfaceConfiguration<K, V> configuration, final K key, final Consumer<V> consumer) {
        this.configuration = configuration;
        this.key = key;
        this.consumer = consumer;
        configuration.addListener(this);
        this.config = configuration.get(key);
        if (config != null && consumer != null) {
            consumer.accept(config);
        }
    }

    /**
     * 通知
     */
    protected void publish() {
        if (consumer != null) {
            consumer.accept(config);
        }
    }

    @Override
    public void accept(final ConfigEvent<K, V> event) {
        //本接口的配置
        if (Objects.equals(key, event.getKey())) {
            switch (event.getType()) {
                case ADD:
                case UPDATE:
                    config = event.getValue();
                    publish();
                    break;
                case REMOVE:
                    config = null;
                    publish();
            }
        }
    }

    @Override
    public V get() {
        return config;
    }

    public void close() {
        configuration.removeListener(this);
    }
}
