package io.joyrpc.extension.spring;

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

import io.joyrpc.extension.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * Spring加载器
 */
public class SpringLoader implements ExtensionLoader, PriorityOrdered, ApplicationContextAware,
        BeanDefinitionRegistryPostProcessor, ApplicationListener<ContextClosedEvent> {

    protected ApplicationContext context;

    protected Instantiation instance;

    protected BeanDefinitionRegistry registry;

    @Override
    public <T> Collection<Plugin<T>> load(final Class<T> extensible) {
        if (extensible == null) {
            return null;
        }
        List<Plugin<T>> result = new LinkedList<>();
        if (registry != null) {
            BeanDefinition definition;
            Class<?> clazz;
            for (String name : registry.getBeanDefinitionNames()) {
                definition = registry.getBeanDefinition(name);
                if (!definition.isAbstract() && !isEmpty(definition.getBeanClassName())) {
                    try {
                        clazz = ClassUtils.forName(definition.getBeanClassName(), Thread.currentThread().getContextClassLoader());
                        //工程方法创建Bean，不支持FactoryBean
                        String factoryMethodName = definition.getFactoryMethodName();
                        if (!isEmpty(factoryMethodName) && isEmpty(definition.getFactoryBeanName())) {
                            //找到方法
                            Method[] methods = clazz.getMethods();
                            for (Method method : methods) {
                                if (method.getName().equals(factoryMethodName)) {
                                    //获取方法的返回类型
                                    clazz = method.getReturnType();
                                    break;
                                }
                            }
                        }
                        if (extensible.isAssignableFrom(clazz)) {
                            //延迟加载，防止Bean还没有初始化好
                            result.add(new Plugin<T>(new Name<>((Class<T>) clazz, name), instance,
                                    definition.isSingleton(), null, this));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return result;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setApplicationContext(final ApplicationContext context) throws BeansException {
        this.context = context;
        this.instance = new Instantiation() {
            @Override
            public <T, M> T newInstance(final Name<T, M> name) {
                try {
                    return context.getBean(name.getName().toString(), name.getClazz());
                } catch (BeansException e) {
                    return null;
                }
            }
        };
        //注册当前插件加载器
        ExtensionManager.register(this);
    }

    @Override
    public void onApplicationEvent(final ContextClosedEvent event) {
        //容器停止的时候，注销当前插件加载器
        if (event.getSource() == context) {
            ExtensionManager.deregister(this);
        }
    }
}
