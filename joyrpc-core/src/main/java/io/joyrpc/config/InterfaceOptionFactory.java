package io.joyrpc.config;

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

import io.joyrpc.cluster.distribution.Route;
import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.URL;

import java.util.function.Consumer;

/**
 * 接口选项工厂类
 */
@Extensible("interfaceOptionFactory")
public interface InterfaceOptionFactory {

    /**
     * 构造
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名
     * @param url            url
     * @return 接口选项
     */
    InterfaceOption create(Class<?> interfaceClass, String interfaceName, URL url);

    /**
     * 构造
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名
     * @param url            url
     * @param configure      分发策略配置器
     * @return 接口选项
     */
    InterfaceOption create(Class<?> interfaceClass, String interfaceName, URL url, Consumer<Route> configure);
}
