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

import io.joyrpc.config.MethodConfig;
import io.joyrpc.exception.IllegalConfigureException;
import io.joyrpc.spring.ProviderBean;
import io.joyrpc.spring.annotation.Provider;
import io.joyrpc.spring.context.DefaultClassPathBeanDefinitionScanner;
import io.joyrpc.spring.util.AnnotationUtils;
import io.joyrpc.spring.util.MethodConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.joyrpc.constants.ExceptionCode.PROVIDER_DUPLICATE_EXPORT;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * provider注解解析器
 */

public class ProviderAnnotationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware
        , ResourceLoaderAware, BeanClassLoaderAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Set<String> packagesToScan;
    private Environment environment;
    private ResourceLoader resourceLoader;
    private ClassLoader classLoader;

    public ProviderAnnotationBeanPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<String> resolvedPackagesToScan = resolvePackagesToScan(packagesToScan);

        if (!CollectionUtils.isEmpty(resolvedPackagesToScan)) {
            registerProviderBeans(resolvedPackagesToScan, registry);
        } else {
            logger.warn("packagesToScan is empty , ProviderBean registry will be ignored!");
        }
    }

    /**
     * 扫描package，进行provider bean注册
     *
     * @param packagesToScan
     * @param registry
     */
    private void registerProviderBeans(Set<String> packagesToScan, BeanDefinitionRegistry registry) {

        DefaultClassPathBeanDefinitionScanner scanner = new DefaultClassPathBeanDefinitionScanner(registry, environment, resourceLoader);

        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);
        scanner.setBeanNameGenerator(beanNameGenerator);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));

        for (String packageToScan : packagesToScan) {
            scanner.scan(packageToScan);
            Set<BeanDefinitionHolder> beanDefinitionHolders = findServiceBeanDefinitionHolders(scanner, packageToScan, registry, beanNameGenerator);

            if (!CollectionUtils.isEmpty(beanDefinitionHolders)) {
                for (BeanDefinitionHolder beanDefinitionHolder : beanDefinitionHolders) {
                    registerProviderBean(beanDefinitionHolder, registry, scanner);
                }
            } else {
                logger.warn(String.format("No Spring Bean annotating @Provider was found under package %s", packageToScan));
            }
        }

    }

    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {

        BeanNameGenerator beanNameGenerator = null;

        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
        }

        if (beanNameGenerator == null) {
            logger.info(String.format("BeanNameGenerator will be a instance of %s , it maybe a potential problem on bean name generation."
                    , AnnotationBeanNameGenerator.class.getName()));
            beanNameGenerator = new AnnotationBeanNameGenerator();
        }
        return beanNameGenerator;

    }


    private Set<BeanDefinitionHolder> findServiceBeanDefinitionHolders(ClassPathBeanDefinitionScanner scanner, String packageToScan,
                                                                       BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {

        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(packageToScan);

        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<BeanDefinitionHolder>(beanDefinitions.size());

        for (BeanDefinition beanDefinition : beanDefinitions) {
            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);
        }

        return beanDefinitionHolders;

    }

    /**
     * @param beanDefinitionHolder
     * @param registry
     * @param scanner
     */
    private void registerProviderBean(BeanDefinitionHolder beanDefinitionHolder, BeanDefinitionRegistry registry,
                                      DefaultClassPathBeanDefinitionScanner scanner) {

        Class<?> beanClass = resolveClass(beanDefinitionHolder);

        Provider service = findAnnotation(beanClass, Provider.class);

        Class<?> interfaceClass = resolveServiceInterfaceClass(beanClass, service);
        String annotatedServiceBeanName = beanDefinitionHolder.getBeanName();
        AbstractBeanDefinition serviceBeanDefinition = buildServiceBeanDefinition(service, interfaceClass, annotatedServiceBeanName);
        String beanName = generateServiceBeanName(service, interfaceClass);

        // check duplicated candidate bean
        if (scanner.checkCandidate(beanName, serviceBeanDefinition)) {
            registry.registerBeanDefinition(beanName, serviceBeanDefinition);
        } else {
            throw new IllegalConfigureException(String.format("Duplicate provider config with key %s has been exported.", beanName), PROVIDER_DUPLICATE_EXPORT);
        }
    }

    private String generateServiceBeanName(Provider service, Class<?> interfaceClass) {
        return AnnotationBeanNameBuilder.builder().alias(service.alias()).name(service.name())
                .interfaceClassName(interfaceClass.getName()).environment(environment).build();
    }

    private Class<?> resolveServiceInterfaceClass(Class<?> annotatedServiceBeanClass, Provider service) {
        if (void.class.equals(service.interfaceClass())) {
            Class<?>[] allInterfaces = annotatedServiceBeanClass.getInterfaces();
            if (allInterfaces.length > 0) {
                return allInterfaces[0];
            }
            return null;
        } else {
            return service.interfaceClass();
        }

    }

    private Class<?> resolveClass(BeanDefinitionHolder beanDefinitionHolder) {
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return resolveClassName(beanClassName, classLoader);
    }

    private Set<String> resolvePackagesToScan(Set<String> packagesToScan) {
        Set<String> resolvedPackagesToScan = new LinkedHashSet<String>(packagesToScan.size());
        for (String packageToScan : packagesToScan) {
            if (org.springframework.util.StringUtils.hasText(packageToScan)) {
                String resolvedPackageToScan = environment.resolvePlaceholders(packageToScan.trim());
                resolvedPackagesToScan.add(resolvedPackageToScan);
            }
        }
        return resolvedPackagesToScan;
    }

    private AbstractBeanDefinition buildServiceBeanDefinition(Provider provider, Class<?> interfaceClass,
                                                              String annotatedServiceBeanName) {

        BeanDefinitionBuilder builder = rootBeanDefinition(ProviderBean.class);

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();

        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();

        String[] ignoreAttributeNames = new String[]{"ref", "id", "registry", "server", "methods", "interfaceClazz", "parameters"};
        propertyValues.addPropertyValues(new MutablePropertyValues(AnnotationUtils.getAttributes(provider, environment, true, ignoreAttributeNames)));

        addPropertyReference(builder, "ref", annotatedServiceBeanName);

        String providerConfigBeanName = provider.name();
        if (org.springframework.util.StringUtils.hasText(providerConfigBeanName)) {
            builder.addPropertyValue("id", providerConfigBeanName);
        }

        String[] registryConfigBeanNames = provider.registry();
        List<RuntimeBeanReference> registryRuntimeBeanReferences = toRuntimeBeanReferences(registryConfigBeanNames);
        if (!registryRuntimeBeanReferences.isEmpty()) {
            builder.addPropertyValue("registry", registryRuntimeBeanReferences);
        }

        String serverBeanName = provider.server();
        if (org.springframework.util.StringUtils.hasText(serverBeanName)) {
            addPropertyReference(builder, "serverConfig", serverBeanName);
        }

        Map<String, MethodConfig> methodConfigs = MethodConfigUtils.constructMethodConfig(provider.methods());
        builder.addPropertyValue("methods", methodConfigs);

        builder.addPropertyValue("interfaceClazz", interfaceClass.getName());

        builder.addPropertyValue("parameters", MethodConfigUtils.toStringMap(provider.parameters()));

        return builder.getBeanDefinition();

    }


    private ManagedList<RuntimeBeanReference> toRuntimeBeanReferences(String... beanNames) {
        ManagedList<RuntimeBeanReference> runtimeBeanReferences = new ManagedList<RuntimeBeanReference>();
        if (!ObjectUtils.isEmpty(beanNames)) {
            for (String beanName : beanNames) {
                String resolvedBeanName = environment.resolvePlaceholders(beanName);
                runtimeBeanReferences.add(new RuntimeBeanReference(resolvedBeanName));
            }
        }
        return runtimeBeanReferences;

    }

    private void addPropertyReference(BeanDefinitionBuilder builder, String propertyName, String beanName) {
        String resolvedBeanName = environment.resolvePlaceholders(beanName);
        builder.addPropertyReference(propertyName, resolvedBeanName);
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

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
}
