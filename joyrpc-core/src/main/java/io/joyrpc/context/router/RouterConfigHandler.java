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
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;

import java.util.Map;

import static io.joyrpc.constants.Constants.SETTING_ROUTER_RULE;
import static io.joyrpc.context.ConfigEventHandler.BIZ_ORDER;


/**
 * 方法条件路由配置
 */
@Extension(value = "router", order = BIZ_ORDER)
public class RouterConfigHandler implements ConfigEventHandler {

    @Override
    public void handle(final String className, final Map<String, String> attrs) {
        if (GlobalContext.update(className, attrs, SETTING_ROUTER_RULE, null)) {
            RouterConfiguration.ROUTER.update(className, MethodRouterBuilder.build(GlobalContext.get(className, SETTING_ROUTER_RULE, "")));
        }
    }
}
