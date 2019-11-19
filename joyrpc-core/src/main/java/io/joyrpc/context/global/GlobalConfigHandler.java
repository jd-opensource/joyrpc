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


import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.invoker.InvokerManager;

import java.util.Map;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.ConfigEventHandler.GLOBAL_ORDER;


/**
 * 注册中心全局配置参数处理handler
 *
 * @date: 2019/6/21
 */
@Extension(value = "global", order = GLOBAL_ORDER)
public class GlobalConfigHandler implements ConfigEventHandler {

    @Override
    public void handle(final String className, final Map<String, String> oldAttrs, final Map<String, String> newAttrs) {
        if (GLOBAL_SETTING.equals(className)) {
            newAttrs.putIfAbsent(SETTING_REGISTRY_HEARTBEAT_INTERVAL, "15000");
            newAttrs.putIfAbsent(SETTING_REGISTRY_CHECK_INTERVAL, "300000");
            //修改回调线程池
            InvokerManager.updateThreadPool(InvokerManager.getCallbackThreadPool(), "callback",
                    new MapParametric(newAttrs),
                    SETTING_CALLBACK_POOL_CORE_SIZE,
                    SETTING_CALLBACK_POOL_MAX_SIZE);
        }
    }
}
