package io.joyrpc.context.circuit;

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

import io.joyrpc.codec.serialization.TypeReference;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.GLOBAL_SETTING;
import static io.joyrpc.context.ConfigEventHandler.CIRCUIT_ORDER;

/**
 * 跨机房访问首选的机房
 *
 * @date: 2019/8/2
 */
@Extension(value = "circuit", order = CIRCUIT_ORDER)
public class CircuitConfigHandler implements ConfigEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CircuitConfigHandler.class);

    @Override
    public void handle(final String className, final Map<String, String> attrs) {
        //跨机房首选访问放在全局配置里面
        if (GLOBAL_SETTING.equals(className)) {
            String value = attrs.remove(Constants.CIRCUIT_KEY);
            if (value != null && !value.isEmpty()) {
                try {
                    Map<String, List<String>> results = JSON.get().parseObject(value, new TypeReference<Map<String, List<String>>>() {
                    });
                    CircuitConfiguration.INSTANCE.update(results);
                } catch (Exception e) {
                    logger.error("Error occurs while parsing circuit config. caused by " + e.getMessage(), e);
                }
            }
        }
    }
}
