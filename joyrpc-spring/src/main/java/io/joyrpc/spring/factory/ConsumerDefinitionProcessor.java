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

import io.joyrpc.extension.Extension;
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.util.AnnotationUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;

import java.lang.reflect.Modifier;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.ReflectionUtils.doWithFields;
import static org.springframework.util.ReflectionUtils.doWithMethods;

/**
 * 处理含有consumer注解bean定义的处理类
 */
@Extension("consumer")
public class ConsumerDefinitionProcessor extends AbstractDefinitionProcessor {

    @Override
    public void processBean(final BeanDefinition beanDefinition, final BeanDefinitionRegistry registry,
                            final Environment environment, final ClassLoader classLoader) {
        Class<?> beanClass = resolveClassName(beanDefinition.getBeanClassName(), classLoader);
        //处理属上的consumer注解
        doWithFields(beanClass,
                field -> registryBean(field.getType(), field.getAnnotation(Consumer.class), registry, environment),
                field -> !Modifier.isFinal(field.getModifiers())
                        && !Modifier.isStatic(field.getModifiers())
                        && field.getAnnotation(Consumer.class) != null
        );

        //处理方法上的consumer注解
        doWithMethods(beanClass,
                method -> registryBean(method.getParameterTypes()[1], method.getAnnotation(Consumer.class), registry, environment),
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
     * @param registry
     * @param env
     */
    protected void registryBean(final Class interfaceClazz, final Consumer consumer,
                                final BeanDefinitionRegistry registry, final Environment env) {
        String name = buildBeanName(interfaceClazz.getName(), consumer, env);
        if (!registry.containsBeanDefinition(name)) {
            registry.registerBeanDefinition(name, buildDefinition(interfaceClazz, consumer, name, env));
        }
    }

    /**
     * 构造Bean名称
     *
     * @param interfaceClazz
     * @param consumer
     * @param env
     * @return
     */
    protected String buildBeanName(final String interfaceClazz, final Consumer consumer, final Environment env) {
        return interfaceClazz + "#" + env.resolvePlaceholders(consumer.alias());
    }

    /**
     * 构造Bean的定义
     *
     * @param interfaceClass
     * @param consumer
     * @param beanName
     * @param env
     * @return
     */
    protected BeanDefinition buildDefinition(final Class<?> interfaceClass, final Consumer consumer,
                                             final String beanName, final Environment env) {
        BeanDefinitionBuilder builder = rootBeanDefinition(ConsumerBean.class);

        String[] ignoreAttributeNames = new String[]{"id", "registry", "methods", "interfaceClazz", "parameters", "filter"};
        builder.getBeanDefinition().getPropertyValues().addPropertyValues(new MutablePropertyValues(
                AnnotationUtils.getAttributes(consumer, env, true, ignoreAttributeNames)));

        builder.addPropertyValue("id", beanName);
        builder.addPropertyValue("filter", String.join(",", consumer.filter()));
        builder.addPropertyValue("interfaceClazz", interfaceClass.getName());

        if (!consumer.registry().isEmpty()) {
            builder.addPropertyReference("registry", env.resolvePlaceholders(consumer.registry()));
        }

        builder.addPropertyValue("methods", build(consumer.methods(), env));
        builder.addPropertyValue("parameters", buildParameters(consumer.parameters(), env));
        return builder.getBeanDefinition();

    }

}
