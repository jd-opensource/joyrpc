package io.joyrpc.context.circuitbreaker;

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


import io.joyrpc.cluster.distribution.CircuitBreaker;
import io.joyrpc.cluster.distribution.circuitbreaker.McCircuitBreaker;
import io.joyrpc.cluster.distribution.circuitbreaker.McCircuitBreakerConfig;
import io.joyrpc.context.AbstractInterfaceConfiguration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔断管理器
 */
public class BreakerConfiguration extends AbstractInterfaceConfiguration<String, BreakerConfiguration.MethodBreaker> {

    /**
     * 结果缓存
     */
    public static final BreakerConfiguration BREAKER = new BreakerConfiguration();

    /**
     * 读取限流数据
     *
     * @param className  类名
     * @param methodName 方法名
     * @return 结果
     */
    public CircuitBreaker get(final String className, final String methodName) {
        if (className == null || methodName == null) {
            return null;
        }
        MethodBreaker methodBreaker = BREAKER.get(className);
        return methodBreaker == null ? null : methodBreaker.getBreaker(methodName);
    }

    /**
     * 构建并缓存
     *
     * @param defConfig
     * @param configs
     * @param breakers
     * @param methodName
     * @return
     */
    protected static CircuitBreaker build(final McCircuitBreakerConfig defConfig,
                                          final Map<String, McCircuitBreakerConfig> configs,
                                          final Map<String, Optional<CircuitBreaker>> breakers,
                                          final String methodName) {
        Optional<CircuitBreaker> optional = breakers.computeIfAbsent(methodName, o -> {
            McCircuitBreakerConfig breaker = configs.get(methodName);
            if (breaker == null && (defConfig == null || !defConfig.isEnabled())) {
                return Optional.ofNullable(null);
            } else if (breaker != null && !breaker.isEnabled()) {
                return Optional.ofNullable(null);
            } else if (defConfig == null) {
                return Optional.ofNullable(new McCircuitBreaker(defConfig));
            } else {
                breaker.merge(defConfig);
                return Optional.ofNullable(new McCircuitBreaker(breaker));
            }
        });
        return optional == null ? null : optional.orElse(null);
    }

    /**
     * 方法熔断
     */
    public static class MethodBreaker {
        /**
         * 默认熔断
         */
        protected McCircuitBreakerConfig defConfig;
        /**
         * 方法配置
         */
        protected Map<String, McCircuitBreakerConfig> configs;
        /**
         * 熔断
         */
        protected Map<String, Optional<CircuitBreaker>> breakers;

        /**
         * 构造函数
         *
         * @param defConfig
         * @param configs
         */
        public MethodBreaker(McCircuitBreakerConfig defConfig, Map<String, McCircuitBreakerConfig> configs) {
            this.defConfig = defConfig;
            this.configs = configs;
            this.breakers = new ConcurrentHashMap<>();
        }

        /**
         * 构造函数
         *
         * @param breakers
         */
        public MethodBreaker(final Map<String, Optional<CircuitBreaker>> breakers) {
            this.breakers = breakers;
        }

        /**
         * 获取熔断配置
         *
         * @param methodName
         * @return
         */
        public CircuitBreaker getBreaker(final String methodName) {
            if (configs == null) {
                Optional<CircuitBreaker> optional = breakers.get(methodName);
                return optional == null ? null : optional.orElse(null);
            } else {
                return build(defConfig, configs, breakers, methodName);
            }
        }


    }

}
