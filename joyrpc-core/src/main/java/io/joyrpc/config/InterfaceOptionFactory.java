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

import io.joyrpc.cluster.distribution.Router;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptiveConfig;
import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.URL;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 接口选项工厂类
 */
@Extensible("interfaceOptionFactory")
public interface InterfaceOptionFactory {

    /**
     * 为服务提供者构造接口选项
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名
     * @param url            url
     * @param ref            引用对象
     * @return 接口选项
     */
    InterfaceOption create(Class<?> interfaceClass, String interfaceName, URL url, Object ref);

    /**
     * 为消费者构造接口选项
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名
     * @param url            url
     * @param configure      分发策略配置器
     * @param scorer         方法自适应指标计算
     * @return 接口选项
     */
    InterfaceOption create(Class<?> interfaceClass, String interfaceName, URL url,
                           Consumer<Router> configure,
                           BiFunction<String, AdaptiveConfig, AdaptiveConfig> scorer);
}
