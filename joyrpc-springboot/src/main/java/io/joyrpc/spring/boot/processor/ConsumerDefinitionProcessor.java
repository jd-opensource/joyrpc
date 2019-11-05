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
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.boot.properties.RpcProperties;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.lang.reflect.Modifier;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.ReflectionUtils.doWithFields;
import static org.springframework.util.ReflectionUtils.doWithMethods;

/**
 * 处理含有consumer注解bean定义的处理类
 */
@Extension("consumer")
public class ConsumerDefinitionProcessor implements ServiceBeanDefinitionProcessor {

    @Override
    public void processBean(final BeanDefinition beanDefinition, final BeanDefinitionRegistry registry,
                            final Environment environment, final RpcProperties rpcProperties,
                            final ClassLoader classLoader) {
        Class<?> beanClass = resolveClassName(beanDefinition.getBeanClassName(), classLoader);
        //处理属上的consumer注解
        doWithFields(beanClass,
                field -> registryBean(field.getType(), field.getAnnotation(Consumer.class), rpcProperties, registry, environment),
                field -> !Modifier.isFinal(field.getModifiers())
                        && !Modifier.isStatic(field.getModifiers())
                        && field.getAnnotation(Consumer.class) != null
        );

        //处理方法上的consumer注解
        doWithMethods(beanClass,
                method -> registryBean(method.getParameterTypes()[1], method.getAnnotation(Consumer.class), rpcProperties, registry, environment),
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
    protected void registryBean(final Class interfaceClazz, final Consumer consumer, final RpcProperties properties,
                                final BeanDefinitionRegistry registry, final Environment env) {
        String name = buildBeanName(interfaceClazz.getName(), consumer, env);
        ConsumerBean consumerBean = properties.getConfigBean(name, ConsumerBean.class, ConsumerBean::new);
        consumerBean.setBeanName(name);
        consumerBean.setInterfaceClazz(interfaceClazz.getName());
        if (!registry.containsBeanDefinition(name)) {
            registry.registerBeanDefinition(name, buildConsumer(interfaceClazz, consumer, consumerBean, env));
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

    /**
     * 构造Bean的定义
     *
     * @param interfaceClass
     * @param consumer
     * @param consumerBean
     * @param env
     * @return
     */
    protected BeanDefinition buildConsumer(final Class<?> interfaceClass, final Consumer consumer,
                                           final ConsumerBean consumerBean, final Environment env) {
        BeanDefinitionBuilder builder = genericBeanDefinition(ConsumerBean.class, () -> consumerBean);

        builder.addPropertyValue("id", consumerBean.getId());
        builder.addPropertyValue("interfaceClazz", interfaceClass.getName());
        //配置别名
        if (!StringUtils.hasText(consumerBean.getAlias()) && StringUtils.hasText(consumer.alias())) {
            consumerBean.setAlias(env.resolvePlaceholders(consumer.alias()));
        }
        //引用reistry
        if (StringUtils.hasText(consumerBean.getRegistryRef())) {
            builder.addPropertyReference("registry", env.resolvePlaceholders(consumerBean.getRegistryRef()));
        }
        return builder.getBeanDefinition();
    }

}
