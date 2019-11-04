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

import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.extension.ExtensionPointLazy;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.annotation.Provider;
import io.joyrpc.spring.context.DefaultClassPathBeanDefinitionScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * 注解扫描处理类
 */
public class ServiceBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware,
        ResourceLoaderAware, BeanClassLoaderAware {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBeanDefinitionPostProcessor.class);

    private static final ExtensionPoint<ServiceBeanDefinitionProcessor, String> REGISTRY_PROCESSOR = new ExtensionPointLazy<>(ServiceBeanDefinitionProcessor.class);

    private static final ExtensionPoint<ConfigPropertiesProcessor, String> PROPERTIES_PROCESSOR = new ExtensionPointLazy<>(ConfigPropertiesProcessor.class);

    protected final Set<String> basePackages;

    protected Environment environment;

    protected ResourceLoader resourceLoader;

    protected ClassLoader classLoader;

    /**
     * 构造方法
     *
     * @param basePackages
     */
    public ServiceBeanDefinitionPostProcessor(Set<String> basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        //处理配置信息
        PROPERTIES_PROCESSOR.extensions().forEach(processor -> processor.processProperties(registry, environment));
        //收集packagesToScan配置，获取rpc要扫描的包路径
        Set<String> packages = new LinkedHashSet<>(basePackages.size());
        for (String packageToScan : basePackages) {
            if (StringUtils.hasText(packageToScan)) {
                packages.add(environment.resolvePlaceholders(packageToScan.trim()));
            }
        }
        //处理包下的class类
        if (!CollectionUtils.isEmpty(packages)) {
            processPackages(packages, registry);
        } else {
            logger.warn("basePackages is empty , auto scanning package annotation will be ignored!");
        }
    }

    /**
     * 处理rpc扫描的包下的class类
     *
     * @param packages
     * @param registry
     */
    protected void processPackages(Set<String> packages, BeanDefinitionRegistry registry) {
        //构造
        DefaultClassPathBeanDefinitionScanner scanner = new DefaultClassPathBeanDefinitionScanner(registry, environment, resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));
        scanner.addIncludeFilter(new ConsumerAnnotationFilter());
        //获取配置的rpc扫描包下的所有bean定义
        for (String basePackage : packages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);
            if (!CollectionUtils.isEmpty(beanDefinitions)) {
                for (BeanDefinition beanDefinition : beanDefinitions) {
                    REGISTRY_PROCESSOR.extensions().forEach(
                            processor -> processor.processBean(beanDefinition, registry, environment, classLoader));
                }
            }
        }

    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 扫描类过滤（主要用来过滤含有某一个注解的类）
     */
    protected class ConsumerAnnotationFilter implements TypeFilter {

        @Override
        public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
            ClassMetadata classMetadata = metadataReader.getClassMetadata();
            if (classMetadata.isConcrete() && !classMetadata.isAnnotation()) {
                //找到类
                Class clazz = resolveClassName(classMetadata.getClassName(), classLoader);
                //判断是否Public
                if (Modifier.isPublic(clazz.getModifiers())) {
                    //Consumer只能在字段、方法上设置
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (!Modifier.isFinal(field.getModifiers())
                                && !Modifier.isStatic(field.getModifiers())
                                && field.getDeclaringClass() != Object.class
                                && field.getAnnotation(Consumer.class) != null) {
                            return true;
                        }
                    }
                    //处理setter注入
                    Method[] methods = clazz.getMethods();
                    for (Method method : methods) {
                        if (method.getDeclaringClass() != Object.class
                                && method.getName().startsWith("set")
                                && method.getParameterCount() == 1
                                && method.getAnnotation(Consumer.class) != null) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
