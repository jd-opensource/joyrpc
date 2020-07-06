package io.joyrpc.extension;

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

import java.util.*;

/**
 * 扩展点加载器
 */
public interface ExtensionLoader {

    /**
     * 加载扩展点
     *
     * @param extensible 可扩展的接口
     * @return 扩展点列表
     */
    <T> Collection<Plugin<T>> load(Class<T> extensible);

    /**
     * 包装器
     */
    class Wrapper implements ExtensionLoader {
        protected Set<ExtensionLoader> loaders = new LinkedHashSet<ExtensionLoader>();

        public Wrapper(final ExtensionLoader... loaders) {
            if (loaders != null) {
                for (ExtensionLoader loader : loaders) {
                    add(loader);
                }
            }
        }

        public Wrapper(final Collection<ExtensionLoader> loaders) {
            if (loaders != null) {
                for (ExtensionLoader loader : loaders) {
                    add(loader);
                }
            }
        }

        protected void add(final ExtensionLoader loader) {
            if (loader == null) {
                return;
            } else if (loader instanceof Wrapper) {
                for (ExtensionLoader l : ((Wrapper) loader).loaders) {
                    add(l);
                }
            } else {
                loaders.add(loader);
            }
        }

        @Override
        public <T> Collection<Plugin<T>> load(final Class<T> extensible) {

            List<Plugin<T>> result = new LinkedList<>();

            if (loaders != null) {
                for (ExtensionLoader loader : loaders) {
                    Collection<Plugin<T>> plugins = loader.load(extensible);
                    if (plugins != null) {
                        result.addAll(plugins);
                    }
                }
            }

            return result;
        }
    }

}
