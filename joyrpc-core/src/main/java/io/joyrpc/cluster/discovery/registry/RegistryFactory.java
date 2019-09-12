package io.joyrpc.cluster.discovery.registry;

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

import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.URL;

import java.util.function.Function;

/**
 * 注册中心工厂
 */
@Extensible("registryfactory")
public interface RegistryFactory {

    Function<URL, String> KEY_FUNC = o -> o.toString(false, false);

    /**
     * 获取注册中心
     *
     * @param url
     * @return
     */
    default Registry getRegistry(URL url) {
        return getRegistry(url, KEY_FUNC);
    }

    /**
     * 获取注册中心
     *
     * @param url
     * @param function
     * @return
     */
    Registry getRegistry(URL url, Function<URL, String> function);

}
