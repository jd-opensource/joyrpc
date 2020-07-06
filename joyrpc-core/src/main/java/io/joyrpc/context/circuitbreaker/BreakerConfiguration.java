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


import io.joyrpc.cluster.distribution.circuitbreaker.McIntfCircuitBreakerConfig;
import io.joyrpc.context.AbstractInterfaceConfiguration;

/**
 * 熔断管理器
 */
public class BreakerConfiguration extends AbstractInterfaceConfiguration<String, McIntfCircuitBreakerConfig> {

    /**
     * 结果缓存
     */
    public static final BreakerConfiguration BREAKER = new BreakerConfiguration();

}
