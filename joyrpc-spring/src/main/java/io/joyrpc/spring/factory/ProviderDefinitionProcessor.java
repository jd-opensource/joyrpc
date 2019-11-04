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

import io.joyrpc.config.ServerConfig;
import io.joyrpc.extension.Extension;
import io.joyrpc.spring.ProviderBean;
import io.joyrpc.spring.annotation.Provider;
import io.joyrpc.spring.util.AnnotationUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.Introspector;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * 处理含有provider注解bean定义的处理类
 */
@Extension("provider")
public class ProviderDefinitionProcessor extends AbstractDefinitionProcessor {

    @Override
    public void processBean(final BeanDefinition definition, final BeanDefinitionRegistry registry,
                            final Environment environment, final ClassLoader classLoader) {
        Class<?> refClass = resolveClassName(definition.getBeanClassName(), classLoader);
        Provider provider = findAnnotation(refClass, Provider.class);
        if (provider == null) {
            return;
        }
        //如果没有配置server，且没有注册默认的serverConfig，注册默认的serverConfig
        registerServer(registry, provider);
        //获取或注册实现
        String refBeanName = getOrRegisterComponent(definition, refClass, provider, registry, environment);
        //注册provider
        Class<?> interfaceClazz = provider.interfaceClass();
        if (void.class.equals(interfaceClazz)) {
            Class<?>[] allInterfaces = refClass.getInterfaces();
            if (allInterfaces.length > 0) {
                interfaceClazz = allInterfaces[0];
            }
        }
        String beanName = refBeanName + "#provider";
        registry.registerBeanDefinition(beanName, buildProvider(
                provider, interfaceClazz, environment, refBeanName, beanName));
    }

    /**
     * 注册服务
     *
     * @param registry
     * @param provider
     */
    protected void registerServer(BeanDefinitionRegistry registry, Provider provider) {
        if (!StringUtils.isEmpty(provider.server()) && !registry.containsBeanDefinition(SERVER_NAME)) {
            registry.registerBeanDefinition(SERVER_NAME, new RootBeanDefinition(ServerConfig.class));
        }
    }

    /**
     * 获取或注册服务
     *
     * @param definition
     * @param definitionClass
     * @param provider
     * @param registry
     * @return
     */
    protected String getOrRegisterComponent(final BeanDefinition definition, final Class<?> definitionClass,
                                            final Provider provider, final BeanDefinitionRegistry registry,
                                            final Environment environment) {
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
            name = provider.name();
            if (name.isEmpty()) {
                name = Introspector.decapitalize(ClassUtils.getShortName(definition.getBeanClassName()));
            } else {
                name = environment.resolvePlaceholders(name);
            }
        } else {
            name = environment.resolvePlaceholders(name);
        }
        if (register && !registry.containsBeanDefinition(name)) {
            registry.registerBeanDefinition(name, definition);
        }
        return name;
    }

    /**
     * 构建Provider定义
     *
     * @param provider
     * @param interfaceClass
     * @param environment
     * @param refBeanName
     * @param beanName
     * @return
     */
    protected BeanDefinition buildProvider(final Provider provider, final Class<?> interfaceClass,
                                           final Environment environment, final String refBeanName,
                                           final String beanName) {

        BeanDefinitionBuilder builder = rootBeanDefinition(ProviderBean.class);

        String[] ignoreAttributeNames = new String[]{"ref", "id", "registry", "server", "methods", "interfaceClazz",
                "parameters", "include", "exclude", "filter"};
        builder.getBeanDefinition().getPropertyValues().addPropertyValues(
                new MutablePropertyValues(AnnotationUtils.getAttributes(
                        provider, environment, true, ignoreAttributeNames)));
        builder.addPropertyReference("ref", refBeanName);
        builder.addPropertyValue("id", beanName);
        builder.addPropertyValue("include", String.join(",", provider.include()));
        builder.addPropertyValue("exclude", String.join(",", provider.exclude()));
        builder.addPropertyValue("filter", String.join(",", provider.filter()));
        builder.addPropertyValue("interfaceClazz", interfaceClass.getName());
        //引用注册中心
        String[] registryNames = provider.registry();
        if (registryNames.length > 0) {
            ManagedList<RuntimeBeanReference> runtimeBeanReferences = new ManagedList<>();
            for (String registryName : registryNames) {
                runtimeBeanReferences.add(new RuntimeBeanReference(environment.resolvePlaceholders(registryName)));
            }
            builder.addPropertyValue("registry", runtimeBeanReferences);
        }
        //引用Server
        if (!provider.server().isEmpty()) {
            builder.addPropertyReference("serverConfig", environment.resolvePlaceholders(provider.server()));
        }
        //方法配置
        if (provider.methods().length > 0) {
            builder.addPropertyValue("methods", build(provider.methods(), environment));
        }
        //参数配置
        if (provider.parameters().length > 0) {
            builder.addPropertyValue("parameters", buildParameters(provider.parameters(), environment));
        }

        return builder.getBeanDefinition();

    }


}
