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

import io.joyrpc.cluster.Region;
import io.joyrpc.extension.Extensible;

import java.util.*;
import java.util.function.Predicate;

import static io.joyrpc.Plugin.CONFIG_EVENT_HANDLER;

/**
 * 配置器，用于生成生产者和消费者的配置
 */
@Extensible("configurator")
public interface Configurator {

    /**
     * 全局配置和接口配置过滤掉的属性
     */
    Predicate<String> GLOBAL_ALLOWED = new ExcludeAttrPredicate();

    /**
     * 服务提供者和消费者过滤掉的属性
     */
    Predicate<String> CONFIG_ALLOWED = new ExcludeAttrPredicate(new HashSet<>(Arrays.asList(
            Region.DATA_CENTER, Region.REGION, "proxy", "generic", "dynamic", "register", "subscribe")));

    /**
     * 动态配置
     *
     * @param interfaceClazz 接口名称
     * @return 参数
     */
    Map<String, String> configure(String interfaceClazz);

    /**
     * 更新数据
     *
     * @param from
     * @param to
     * @param predicate
     * @param <T>
     */
    static <T> void update(final Map<String, T> from, final Map<String, String> to, final Predicate<String> predicate) {
        if (from != null && to != null) {
            from.forEach((k, v) -> {
                if (v instanceof String && (predicate == null || predicate.test(k))) {
                    to.put(k, (String) v);
                }
            });
        }
    }

    /**
     * 排除属性预测
     */
    class ExcludeAttrPredicate implements Predicate<String> {

        protected Set<String> excludes;
        protected boolean loaded;

        public ExcludeAttrPredicate() {
        }

        public ExcludeAttrPredicate(final Set<String> excludes) {
            this.excludes = excludes;
        }

        @Override
        public boolean test(String name) {
            if (!loaded) {
                synchronized (this) {
                    if (!loaded) {
                        excludes = excludes == null ? new HashSet<>(30) : excludes;
                        excludes.add("id");
                        excludes.add("interfaceClazz");
                        excludes.add("ref");
                        excludes.add("server");
                        excludes.add("genericClass");
                        excludes.add("delay");
                        excludes.add("registry");
                        excludes.add("interfaceValidator");
                        CONFIG_EVENT_HANDLER.extensions().forEach(o -> {
                            String[] keys = o.getKeys();
                            if (keys != null) {
                                Collections.addAll(excludes, keys);
                            }
                        });
                    }
                }
            }

            return !excludes.contains(name);
        }
    }
}
