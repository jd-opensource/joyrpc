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

/**
 * 方法熔断配置
 */
public class McMethodBreakerConfig {
    /**
     * 方法名称
     */
    protected String name;
    /**
     * 接口熔断配置
     */
    protected McCircuitBreakerConfig intfConfig;
    /**
     * 方法熔断配置
     */
    protected McCircuitBreakerConfig methodConfig;

    public McMethodBreakerConfig(String name, McCircuitBreakerConfig intfConfig, McCircuitBreakerConfig methodConfig) {
        this.name = name;
        this.intfConfig = intfConfig;
        this.methodConfig = methodConfig;
    }

    public McCircuitBreakerConfig getIntfConfig() {
        return intfConfig;
    }

    public McCircuitBreakerConfig getMethodConfig() {
        return methodConfig;
    }

    /**
     * 合并方法的配置
     *
     * @param cfg
     * @return 方法的配置
     */
    public McCircuitBreakerConfig compute(McMethodBreakerConfig cfg) {
        McCircuitBreakerConfig result = new McCircuitBreakerConfig(name);
        result.merge(methodConfig);
        if (cfg != null) {
            result.merge(cfg.methodConfig);
        }
        result.merge(intfConfig);
        if (cfg != null) {
            result.merge(cfg.intfConfig);
        }
        return result;
    }
}
