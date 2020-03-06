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

import io.joyrpc.spring.context.SpringContextSupplier;
import io.joyrpc.spring.context.SpringEnvironmentSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;

/**
 * RPC自动配置
 *
 * @description:
 */
@ConditionalOnProperty(prefix = "rpc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RpcAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean(name = RpcDefinitionPostProcessor.BEAN_NAME)
    public static RpcDefinitionPostProcessor rpcDefinitionPostProcessor(ApplicationContext applicationContext,
                                                                        ConfigurableEnvironment environment,
                                                                        ResourceLoader resourceLoader) {
        return new RpcDefinitionPostProcessor(applicationContext, environment, resourceLoader);
    }

    @ConditionalOnMissingBean
    @Bean(name = SpringContextSupplier.BEAN_NAME)
    public static SpringContextSupplier springContextSupplier(ConfigurableEnvironment environment) {
        return new SpringContextSupplier(environment);
    }

    @ConditionalOnMissingBean
    @Bean(name = SpringEnvironmentSupplier.BEAN_NAME)
    public static SpringEnvironmentSupplier springEnvironmentSupplier(ConfigurableEnvironment environment) {
        return new SpringEnvironmentSupplier(environment);
    }

}
