package io.joyrpc.context.global;

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

import io.joyrpc.constants.Constants;
import io.joyrpc.context.Configurator;
import io.joyrpc.context.Environment;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;

import java.util.*;
import java.util.function.Predicate;

import static io.joyrpc.Plugin.CONFIG_EVENT_HANDLER;

/**
 * 全局配置
 */
@Extension("global")
public class GlobalConfigurator implements Configurator {

    /**
     * 过滤掉不需要拼接在url中的属性
     */
    protected static Predicate<String> EXCLUDES_ATTR = new Predicate<String>() {

        protected Set<String> excludes;

        @Override
        public boolean test(String name) {
            if (excludes == null) {
                synchronized (this) {
                    if (excludes == null) {
                        excludes = new HashSet<>(30);
                        excludes.add("id");
                        excludes.add("interfaceClazz");
                        excludes.add("ref");
                        excludes.add("server");
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
    };

    @Override
    public URL configure(final URL url) {

        Map<String, String> parameters = new HashMap<>();
        //本地全局静态配置
        update(GlobalContext.getContext(), parameters);
        //注册中心下发的全局动态配置，主要是一些开关
        update(GlobalContext.getInterfaceConfig(Constants.GLOBAL_SETTING), parameters);
        //本地接口静态配置
        Map<String, String> map = url.getParameters();
        //数据中心和区域在注册中心里面会动态更新到全局上下文里面
        map.remove(Environment.DATA_CENTER);
        map.remove(Environment.REGION);
        update(map, parameters);
        //注册中心下发的接口动态配置
        update(GlobalContext.getInterfaceConfig(url.getPath()), parameters);

        return new URL(url.getProtocol(), url.getUser(), url.getPassword(), url.getHost(), url.getPort(), url.getPath(), parameters);
    }

    /**
     * 更新数据
     *
     * @param from
     * @param to
     * @param <T>
     */
    protected <T> void update(final Map<String, T> from, final Map<String, String> to) {
        if (from != null) {
            from.forEach((k, v) -> {
                if (v instanceof String && EXCLUDES_ATTR.test(k)) {
                    to.put(k, (String) v);
                }
            });
        }
    }
}
