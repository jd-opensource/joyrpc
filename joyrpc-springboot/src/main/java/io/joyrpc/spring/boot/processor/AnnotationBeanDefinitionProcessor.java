package io.joyrpc.spring.boot.processor;

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

import io.joyrpc.extension.Extensible;
import io.joyrpc.spring.boot.properties.MergeServiceBeanProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;

/**
 * 含有consumer与provider注解的bean定义的处理类插件
 */
@Extensible("serviceBeanDefinitionProcessor")
public interface AnnotationBeanDefinitionProcessor {
    /**
     * 服务名称
     */
    String SERVER_NAME = "server";
    /**
     * 注册中心名称
     */
    String REGISTRY_NAME = "registry";

    /**
     * 处理Bean定义
     *
     * @param beanDefinition
     * @param registry
     * @param environment
     * @param mergeProperties
     * @param classLoader
     */
    void processBean(BeanDefinition beanDefinition, BeanDefinitionRegistry registry,
                     Environment environment, MergeServiceBeanProperties mergeProperties, ClassLoader classLoader) throws BeansException;

}
