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

import io.joyrpc.extension.Extension;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.boot.properties.MergeServiceBeanProperties;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.lang.reflect.Modifier;

import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.ReflectionUtils.doWithFields;
import static org.springframework.util.ReflectionUtils.doWithMethods;

/**
 * 处理含有consumer注解bean定义的处理类
 */
@Extension("consumer")
public class AnnotationConsumerDefinitionProcessor implements AnnotationBeanDefinitionProcessor {

    @Override
    public void processBean(final BeanDefinition beanDefinition, final BeanDefinitionRegistry registry,
                            final Environment environment, final MergeServiceBeanProperties mergeProperties,
                            final ClassLoader classLoader) {
        Class<?> beanClass = resolveClassName(beanDefinition.getBeanClassName(), classLoader);
        //处理属上的consumer注解
        doWithFields(beanClass,
                field -> doProcess(field.getType(), field.getName(), field.getAnnotation(Consumer.class), mergeProperties,
                        beanDefinition, environment),
                field -> !Modifier.isFinal(field.getModifiers())
                        && !Modifier.isStatic(field.getModifiers())
                        && field.getAnnotation(Consumer.class) != null
        );

        //处理方法上的consumer注解
        doWithMethods(beanClass,
                method -> doProcess(method.getParameterTypes()[1], method.getName().substring(2),
                        method.getAnnotation(Consumer.class), mergeProperties, beanDefinition, environment),
                method -> method.getName().startsWith("set")
                        && method.getParameterCount() == 1
                        && method.getAnnotation(Consumer.class) != null
        );
    }

    /**
     * 注册Bean
     *
     * @param interfaceClazz
     * @param consumer
     * @param beanDefinition
     * @param env
     */
    protected void doProcess(final Class interfaceClazz, String filedName, final Consumer consumer, final MergeServiceBeanProperties properties,
                             final BeanDefinition beanDefinition, final Environment env) {
        String name = buildBeanName(interfaceClazz.getName(), consumer, env);
        properties.mergeConsumer(name, interfaceClazz.getName(), env.resolvePlaceholders(consumer.alias()));
        //beanDefinition.getPropertyValues().add(filedName, new RuntimeBeanReference(name));
    }

    /**
     * 构造Bean名称
     *
     * @param interfaceClazz
     * @param consumer
     * @param env
     * @return
     */
    public static String buildBeanName(final String interfaceClazz, final Consumer consumer, final Environment env) {
        if (StringUtils.hasText(consumer.name())) {
            return env.resolvePlaceholders(consumer.name());
        }
        String name = "consumer-" + interfaceClazz;
        if (StringUtils.hasText(consumer.alias())) {
            name = name + "-" + env.resolvePlaceholders(consumer.alias());
        }
        return name;
    }

}
