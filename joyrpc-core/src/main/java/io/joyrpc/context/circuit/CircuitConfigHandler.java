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
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.CIRCUIT_KEY;
import static io.joyrpc.constants.Constants.GLOBAL_SETTING;
import static io.joyrpc.context.ConfigEventHandler.CIRCUIT_ORDER;
import static io.joyrpc.context.circuit.CircuitConfiguration.CIRCUIT;

/**
 * 跨机房访问首选的机房
 *
 * @date: 2019/8/2
 */
@Extension(value = "circuit", order = CIRCUIT_ORDER)
public class CircuitConfigHandler implements ConfigEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CircuitConfigHandler.class);

    @Override
    public void handle(final String className, final Map<String, String> oldAttrs, final Map<String, String> newAttrs) {
        //跨机房首选访问放在全局配置里面
        if (GLOBAL_SETTING.equals(className)) {
            String oldAttr = oldAttrs.get(CIRCUIT_KEY);
            String newAttr = newAttrs.get(CIRCUIT_KEY);
            if (!Objects.equals(oldAttr, newAttr)) {
                try {
                    Map<String, List<String>> results = newAttr == null || newAttr.isEmpty() ? null :
                            JSON.get().parseObject(newAttr, new TypeReference<Map<String, List<String>>>() {
                            });
                    CIRCUIT.update(results);
                } catch (Exception e) {
                    logger.error("Error occurs while parsing circuit config. caused by " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public String[] getKeys() {
        return new String[]{CIRCUIT_KEY};
    }
}
