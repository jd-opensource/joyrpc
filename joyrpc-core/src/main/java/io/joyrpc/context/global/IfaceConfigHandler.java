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
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;

import java.util.Map;

/**
 * 注册中心接口级配置参数处理handler
 *
 * @date: 2019/8/2
 */
@Extension(value = "iface", order = ConfigEventHandler.IFACE_ORDER)
public class IfaceConfigHandler implements ConfigEventHandler {

    @Override
    public void handle(String className, Map<String, String> attrs) {
        if (!Constants.GLOBAL_SETTING.equals(className)) {
            GlobalContext.put(className, attrs);
        }
    }
}
