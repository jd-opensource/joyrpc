package io.joyrpc.cluster.distribution.circuitbreaker;

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

import java.util.Map;

/**
 * 接口熔断配置
 */
public class McIntfCircuitBreakerConfig {
    /**
     * 默认熔断
     */
    protected McCircuitBreakerConfig config;
    /**
     * 方法配置
     */
    protected Map<String, McCircuitBreakerConfig> configs;

    public McIntfCircuitBreakerConfig(McCircuitBreakerConfig config, Map<String, McCircuitBreakerConfig> configs) {
        this.config = config;
        this.configs = configs;
    }

    public McCircuitBreakerConfig getConfig() {
        return config;
    }

    public Map<String, McCircuitBreakerConfig> getConfigs() {
        return configs;
    }

    /**
     * 获取方法的熔断配置
     *
     * @param method 方法名
     * @return 熔断配置
     */
    public McMethodBreakerConfig getConfig(String method) {
        McCircuitBreakerConfig cfg = configs == null ? null : configs.get(method);
        return cfg == null && config == null ? null : new McMethodBreakerConfig(method, config, cfg);
    }
}
