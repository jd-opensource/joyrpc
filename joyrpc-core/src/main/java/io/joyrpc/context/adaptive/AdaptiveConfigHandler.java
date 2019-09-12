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
import io.joyrpc.constants.Constants;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;

import java.util.Map;

import static io.joyrpc.Plugin.JSON;


/**
 * 自适应负载均衡配置信息
 */
@Extension(value = "adaptiveLoadbalance", order = ConfigEventHandler.ZERO_ORDER)
public class AdaptiveConfigHandler implements ConfigEventHandler {

    @Override
    public void handle(final String className, final Map<String, String> attrs) {
        if (GlobalContext.update(className, attrs, Constants.SETTING_LOADBALANCE_ADAPTIVE, null)) {
            AdaptiveConfig config = JSON.get().parseObject(GlobalContext.asParametric(className).getString(Constants.SETTING_LOADBALANCE_ADAPTIVE), AdaptiveConfig.class);
            AdaptiveConfiguration.ADAPTIVE.update(className, config);
        }
    }

}
