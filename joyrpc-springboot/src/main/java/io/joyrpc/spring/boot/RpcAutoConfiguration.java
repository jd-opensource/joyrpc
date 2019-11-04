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

import io.joyrpc.spring.factory.ConsumerInjectedPostProcessor;
import io.joyrpc.spring.factory.ServiceBeanDefinitionPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

import static io.joyrpc.spring.boot.RpcProperties.PREFIX;

/**
 * RPC自动配置
 *
 * @description:
 */
@Configuration
@EnableConfigurationProperties(RpcProperties.class)
@ConditionalOnProperty(prefix = PREFIX, name = "enabled", matchIfMissing = true)
public class RpcAutoConfiguration {

    @Resource
    protected RpcProperties rpcProperties;

    @ConditionalOnMissingBean
    @Bean
    public RpcPropertiesPostProcessor propertiesDefinitionPostProcessor() {
        return new RpcPropertiesPostProcessor(rpcProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public ServiceBeanDefinitionPostProcessor serviceBeanDefinitionPostProcessor() {
        return new ServiceBeanDefinitionPostProcessor(rpcProperties.getPackages());
    }

    @ConditionalOnMissingBean
    @Bean(name = ConsumerInjectedPostProcessor.BEAN_NAME)
    public ConsumerInjectedPostProcessor consumerInjectedPostProcessor() {
        return new ConsumerInjectedPostProcessor();
    }

}
