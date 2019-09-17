package io.joyrpc.spring.util;

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

import io.joyrpc.config.MethodConfig;
import io.joyrpc.spring.annotation.Method;

import java.util.HashMap;
import java.util.Map;

/**
 * @date 16/8/2019
 */
public class MethodConfigUtils {

    public static Map<String, MethodConfig> constructMethodConfig(Method[] methods) {
        if (methods == null || methods.length == 0) {
            return new HashMap<>();
        }

        Map<String, MethodConfig> methodConfigs = new HashMap<>(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            MethodConfig config = new MethodConfig();
            config.setName(m.name());
            config.setRetries(m.retries());
            config.setValidation(m.validation());
            config.setConcurrency(m.concurrency());
            config.setCache(m.cache());
            config.setCacheProvider(m.cacheProvider());
            config.setCacheKeyGenerator(m.cacheKeyGenerator());
            config.setCacheExpireTime(m.cacheExpireTime());
            config.setCacheCapacity(m.cacheCapacity());
            config.setCompress(m.compress());
            config.setDstParam(m.dstParam());

            if (m.parameters().length > 0) {
                config.setParameters(toStringMap(m.parameters()));
            }
            methodConfigs.put(m.name(), config);
        }
        return methodConfigs;
    }


    public static Map<String, String> toStringMap(String[] pairs) {
        if (null == pairs || pairs.length <= 0) {
            return new HashMap<>();
        }
        Map<String, String> p = new HashMap<>(pairs.length);
        for (int l = 0; l < pairs.length; l = l + 2) {
            p.put(pairs[l], pairs[l + 1]);
        }
        return p;
    }
}
