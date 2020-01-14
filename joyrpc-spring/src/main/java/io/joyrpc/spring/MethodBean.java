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

import io.joyrpc.config.MethodConfig;
import io.joyrpc.config.ParameterConfig;
import io.joyrpc.util.StringUtils;

import java.util.List;

/**
 * 方法配置
 */
public class MethodBean extends MethodConfig {

    /**
     * 参数配置
     */
    protected List<ParameterConfig> params;

    public List<ParameterConfig> getParams() {
        return params;
    }

    public void setParams(List<ParameterConfig> params) {
        this.params = params;
        if (params != null) {
            params.forEach(param -> {
                if (param != null
                        && StringUtils.isNotEmpty(param.getKey())
                        && StringUtils.isNotEmpty(param.getValue())) {
                    String key = param.isHide() && !param.getKey().startsWith(".") ? "." + param.getKey() : param.getKey();
                    setParameter(key, param.getValue());
                }
            });
        }
    }
}
