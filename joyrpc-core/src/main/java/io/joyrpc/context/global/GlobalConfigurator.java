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
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局配置
 */
@Extension("global")
public class GlobalConfigurator implements Configurator {

    @Override
    public URL configure(final URL url) {

        Map<String, String> parameters = new HashMap<>();
        //本地全局静态配置
        update(GlobalContext.getContext(), parameters);
        //注册中心下发的全局动态配置，主要是一些开关
        update(GlobalContext.getInterfaceConfig(Constants.GLOBAL_SETTING), parameters);
        //本地接口静态配置
        update(url.getParameters(), parameters);
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
    protected <T> void update(Map<String, T> from, Map<String, String> to) {

        if (from != null) {
            from.forEach((k, v) -> {
                if (v instanceof String) {
                    if (!Constants.EXCLUDE_CHANGED_ATTR_MAP.containsKey(v)) {
                        to.put(k, (String) v);
                    }
                }
            });
        }


    }
}
