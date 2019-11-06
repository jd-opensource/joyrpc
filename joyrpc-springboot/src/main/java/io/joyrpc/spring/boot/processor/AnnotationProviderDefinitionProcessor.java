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

import io.joyrpc.config.ServerConfig;
import io.joyrpc.extension.Extension;
import io.joyrpc.spring.ProviderBean;
import io.joyrpc.spring.annotation.Provider;
import io.joyrpc.spring.boot.properties.MergeServiceBeanProperties;
import io.joyrpc.spring.boot.properties.RpcProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.util.List;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * 处理含有provider注解bean定义的处理类
 */
@Extension("provider")
public class AnnotationProviderDefinitionProcessor implements AnnotationBeanDefinitionProcessor {

    @Override
    public void processBean(final BeanDefinition beanDefinition, final BeanDefinitionRegistry registry,
                            final Environment environment, final MergeServiceBeanProperties mergeProperties,
                            final ClassLoader classLoader) throws BeansException {
        Class<?> refClass = resolveClassName(beanDefinition.getBeanClassName(), classLoader);
        Provider provider = findAnnotation(refClass, Provider.class);
        if (provider == null) {
            return;
        }
        //获取服务类名，若没注册这注册
        String refBeanName = getOrRegisterComponent(beanDefinition, refClass, registry);
        //设置providerBean的bean名称
        String beanName = getBeanName(provider, refBeanName, environment);
        //获取接口类名
        String interfaceClazz = provider.interfaceClazz();
        if (!StringUtils.hasText(interfaceClazz)) {
            Class<?>[] allInterfaces = refClass.getInterfaces();
            if (allInterfaces.length > 0) {
                interfaceClazz = allInterfaces[0].getName();
            }
        } else {
            interfaceClazz = environment.resolvePlaceholders(provider.interfaceClazz());
        }
        //从配置中获取并合并providerBean
        String alias = environment.resolvePlaceholders(provider.alias());
        ProviderBean providerBean = mergeProperties.mergeProvider(beanName, interfaceClazz, alias, refBeanName);
        //如果没有配置server，且没有注册默认的serverConfig，注册默认的serverConfig
        registerServer(registry, providerBean);
    }

    /**
     * 注册服务
     *
     * @param registry
     * @param provider
     */
    protected void registerServer(BeanDefinitionRegistry registry, ProviderBean provider) {
        if (!StringUtils.isEmpty(provider.getServerRef()) && !registry.containsBeanDefinition(SERVER_NAME)) {
            registry.registerBeanDefinition(SERVER_NAME, new RootBeanDefinition(ServerConfig.class));
        }
    }

    /**
     * 获取或注册服务
     *
     * @param definition
     * @param definitionClass
     * @param registry
     * @return
     */
    protected String getOrRegisterComponent(final BeanDefinition definition, final Class<?> definitionClass,
                                            final BeanDefinitionRegistry registry) {
        String name = null;
        //判断重复注册
        Component component = findAnnotation(definitionClass, Component.class);
        if (component == null) {
            Service service = findAnnotation(definitionClass, Service.class);
            if (service == null) {
                Repository repository = findAnnotation(definitionClass, Repository.class);
                if (repository == null) {
                    Controller controller = findAnnotation(definitionClass, Controller.class);
                    if (controller != null) {
                        name = controller.value();
                    }
                } else {
                    name = repository.value();
                }
            } else {
                name = service.value();
            }
        } else {
            name = component.value();
        }
        boolean register = name == null;
        if (name == null || name.isEmpty()) {
            name = Introspector.decapitalize(ClassUtils.getShortName(definition.getBeanClassName()));
        }
        if (register && !registry.containsBeanDefinition(name)) {
            registry.registerBeanDefinition(name, definition);
        }
        return name;
    }

    protected String getBeanName(final Provider provider, final String refBeanName, final Environment environment) {
        if (provider.name().isEmpty()) {
            return refBeanName + "-provider";
        }
        String name = environment.resolvePlaceholders(provider.name());
        if (name.equals(refBeanName)) {
            throw new BeanDefinitionValidationException("provider annotation name is not validation, can not equals ref bean name");
        }
        return provider.name();
    }

}
