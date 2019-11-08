package io.joyrpc.spring;

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

import io.joyrpc.config.ParameterConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import org.springframework.beans.factory.InitializingBean;

/**
 * 全局参数
 *
 * @description:
 */
public class GlobalParameterBean extends ParameterConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        if (key != null && !key.isEmpty() && value != null) {
            if (hide) {
                GlobalContext.putIfAbsent(Constants.HIDE_KEY_PREFIX + key, value);
            } else {
                GlobalContext.putIfAbsent(key, value);
            }
        }
    }
}
