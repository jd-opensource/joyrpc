package io.joyrpc.invoker;

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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.joyrpc.constants.Constants.ALIAS_EMPTY_OPTION;
import static io.joyrpc.context.Variable.VARIABLE;

/**
 * Exporter管理器
 */
public interface ExporterManager {

    /**
     * 遍历
     *
     * @param consumer 消费者
     */
    void foreach(Consumer<Exporter> consumer);

    /**
     * 遍历
     *
     * @param className 接口名称
     * @param consumer  消费者
     */
    void foreach(String className, Consumer<Exporter> consumer);

    /**
     * 获取输出服务
     *
     * @param className 接口名称
     * @param alias     分组
     * @param port      端口
     * @return 输出服务
     */
    Exporter get(String className, String alias, int port);

    /**
     * 根据接口名称获取输出服务
     *
     * @param className 接口名称
     * @param alias     分组
     * @return 输出服务
     */
    Exporter getFirst(String className, String alias);

    /**
     * 添加
     *
     * @param className 接口名称
     * @param alias     分组
     * @param port      端口
     * @param supplier  提供者
     * @return 服务
     */
    Exporter add(String className, String alias, int port, Supplier<Exporter> supplier);

    /**
     * 移除
     *
     * @param className 接口名称
     * @param alias     分组
     * @param port      端口
     * @return 服务
     */
    Exporter remove(String className, String alias, int port);

    /**
     * 移除
     *
     * @param exporter 服务
     * @return 服务
     */
    default void remove(final Exporter exporter) {
        if (exporter != null) {
            remove(exporter.getInterfaceName(), exporter.getAlias(), exporter.getPort());
        }
    }

    /**
     * 基于Map的ExporterManager
     */
    class MapExporterManager implements ExporterManager {

        protected Map<String, Map<Integer, Map<String, Exporter>>> exports = new ConcurrentHashMap<>();

        protected boolean allowEmptyAlias = VARIABLE.getBoolean(ALIAS_EMPTY_OPTION);

        @Override
        public void foreach(final Consumer<Exporter> consumer) {
            if (consumer != null) {
                exports.forEach((className, c) -> c.forEach((port, a) -> a.forEach((alias, e) -> consumer.accept(e))));
            }
        }

        @Override
        public void foreach(final String className, final Consumer<Exporter> consumer) {
            if (className != null && consumer != null) {
                Map<Integer, Map<String, Exporter>> ports = exports.get(className);
                if (ports != null) {
                    ports.forEach((port, a) -> a.forEach((alias, e) -> consumer.accept(e)));
                }
            }
        }

        @Override
        public Exporter get(final String className, final String alias, final int port) {
            Map<Integer, Map<String, Exporter>> ports = className == null ? null : exports.get(className);
            if (ports != null) {
                Map<String, Exporter> aliases = ports.get(port);
                if (aliases != null && !aliases.isEmpty()) {
                    //如果别名为空，则获取该端口输出的该服务唯一的Exporter
                    if (alias != null && !alias.isEmpty()) {
                        return aliases.get(alias);
                    } else if (aliases.size() == 1 && allowEmptyAlias) {
                        return aliases.values().iterator().next();
                    }
                }
            }
            return null;
        }

        @Override
        public Exporter getFirst(final String className, final String alias) {
            Map<Integer, Map<String, Exporter>> ports = className == null ? null : exports.get(className);
            if (ports != null) {
                Map<String, Exporter> aliases;
                Exporter exporter;
                for (Map.Entry<Integer, Map<String, Exporter>> entry : ports.entrySet()) {
                    aliases = entry.getValue();
                    if (alias == null || alias.isEmpty()) {
                        if (!aliases.isEmpty()) {
                            return aliases.values().iterator().next();
                        }
                    } else {
                        exporter = aliases.get(alias);
                        if (exporter != null) {
                            return exporter;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public Exporter add(final String className, final String alias, final int port, final Supplier<Exporter> supplier) {
            return exports.computeIfAbsent(className, c -> new ConcurrentHashMap<>()).
                    computeIfAbsent(port, p -> new ConcurrentHashMap<>()).
                    computeIfAbsent(alias, v -> supplier.get());
        }

        @Override
        public Exporter remove(final String className, final String alias, final int port) {
            Map<Integer, Map<String, Exporter>> ports = exports.get(className);
            if (ports != null) {
                Map<String, Exporter> aliases = ports.get(port);
                if (aliases != null) {
                    return aliases.remove(alias);
                }
            }
            return null;
        }
    }

}
