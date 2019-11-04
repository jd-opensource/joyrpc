package io.joyrpc.spring.factory;

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

import io.joyrpc.spring.annotation.Consumer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * congsumer注解注入
 */
public class ConsumerInjectedPostProcessor extends AnnotationInjectedBeanPostProcessor<Consumer> implements ApplicationContextAware {

    public static final String BEAN_NAME = "consumerInjectedPostProcessor";

    private ApplicationContext applicationContext;

    @Override
    protected Object doGetInjectedBean(Consumer annotation, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) throws Exception {
        return applicationContext.getBean(buildConsumerBeanName(annotation, injectedType.getName()), injectedType);
    }

    @Override
    protected String buildInjectedObjectCacheKey(Consumer annotation, Class<?> injectedType) {
        return buildConsumerBeanName(annotation, injectedType.getName());
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    protected String buildConsumerBeanName(Consumer consumerAnnotation, String interfaceClazz) {
        return interfaceClazz + "#" + consumerAnnotation.alias();
    }
}
