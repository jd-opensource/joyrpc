package io.joyrpc.spring.boot;

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

import io.joyrpc.context.ContextSupplier;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * RPC上下文变量识别
 *
 * @description:
 */
public class RpcContextSupplier implements ContextSupplier {

    public static final String BEAN_NAME = "rpcContextSupplier";

    /**
     * 环境配置
     */
    protected ConfigurableEnvironment environment;

    public RpcContextSupplier(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Object recognize(String key) {
        return environment.getProperty(key);
    }
}
