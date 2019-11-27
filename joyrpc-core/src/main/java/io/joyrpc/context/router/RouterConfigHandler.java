package io.joyrpc.context.router;

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


import io.joyrpc.cluster.distribution.router.method.MethodRouterBuilder;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

import static io.joyrpc.constants.Constants.GLOBAL_SETTING;
import static io.joyrpc.constants.Constants.SETTING_ROUTER_RULE;
import static io.joyrpc.context.ConfigEventHandler.BIZ_ORDER;
import static io.joyrpc.context.router.RouterConfiguration.ROUTER;


/**
 * 方法条件路由配置
 */
@Extension(value = "router", order = BIZ_ORDER)
public class RouterConfigHandler implements ConfigEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(RouterConfigHandler.class);

    @Override
    public void handle(final String className, final Map<String, String> oldAttrs, final Map<String, String> newAttrs) {
        if (!GLOBAL_SETTING.equals(className)) {
            String oldAttr = oldAttrs.get(SETTING_ROUTER_RULE);
            String newAttr = newAttrs.get(SETTING_ROUTER_RULE);
            if (!Objects.equals(oldAttr, newAttr)) {
                try {
                    ROUTER.update(className, MethodRouterBuilder.build(newAttr));
                } catch (Exception e) {
                    logger.error("Error occurs while parsing router config. caused by " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public String[] getKeys() {
        return new String[]{SETTING_ROUTER_RULE};
    }
}
