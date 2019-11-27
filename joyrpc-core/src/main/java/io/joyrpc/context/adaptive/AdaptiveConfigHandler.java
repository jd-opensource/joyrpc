package io.joyrpc.context.adaptive;

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


import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptiveConfig;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.extension.Extension;

import java.util.Map;
import java.util.Objects;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.GLOBAL_SETTING;
import static io.joyrpc.constants.Constants.SETTING_LOADBALANCE_ADAPTIVE;
import static io.joyrpc.context.ConfigEventHandler.ADAPTIVE_ORDER;
import static io.joyrpc.context.adaptive.AdaptiveConfiguration.ADAPTIVE;


/**
 * 自适应负载均衡配置信息
 */
@Extension(value = "adaptiveLoadbalance", order = ADAPTIVE_ORDER)
public class AdaptiveConfigHandler implements ConfigEventHandler {

    @Override
    public void handle(final String className, final Map<String, String> oldAttrs, final Map<String, String> newAttrs) {
        if (!GLOBAL_SETTING.equals(className)) {
            String oldAttr = oldAttrs.get(SETTING_LOADBALANCE_ADAPTIVE);
            String newAttr = newAttrs.get(SETTING_LOADBALANCE_ADAPTIVE);
            if (!Objects.equals(oldAttr, newAttr)) {
                AdaptiveConfig config = newAttr == null || newAttr.isEmpty() ? null : JSON.get().parseObject(newAttr, AdaptiveConfig.class);
                ADAPTIVE.update(className, config);
            }
        }
    }

    @Override
    public String[] getKeys() {
        return new String[]{SETTING_LOADBALANCE_ADAPTIVE};
    }
}
