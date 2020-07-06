package io.joyrpc.config.inner;

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
import io.joyrpc.config.InterfaceOption;
import io.joyrpc.config.InterfaceOptionFactory;
import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.URL;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 接口选项工厂类
 */
@Extensible("inner")
public class InnerInterfaceOptionFactory implements InterfaceOptionFactory {

    @Override
    public InterfaceOption create(final Class<?> interfaceClass, final String interfaceName, final URL url, final Object ref) {
        return new InnerProviderOption(interfaceClass, interfaceName, url, ref);
    }

    @Override
    public InterfaceOption create(final Class<?> interfaceClass, final String interfaceName, final URL url,
                                  final Consumer<Router> configure,
                                  final BiFunction<String, AdaptiveConfig, AdaptiveConfig> scorer) {
        return new InnerConsumerOption(interfaceClass, interfaceName, url, configure, scorer);
    }
}
